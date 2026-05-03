package com.circleguard.promotion.service;

import com.circleguard.promotion.exception.FenceException;
import com.circleguard.promotion.repository.graph.UserNodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class HealthStatusService {
    private final UserNodeRepository userNodeRepository;
    private final Neo4jClient neo4jClient;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final com.circleguard.promotion.repository.jpa.SystemSettingsRepository systemSettingsRepository;
    private final com.circleguard.promotion.repository.graph.CircleNodeRepository circleNodeRepository;

    private static final String STATUS_KEY_PREFIX = "user:status:";
    private static final String TOPIC_STATUS_CHANGED = "promotion.status.changed";

    /**
     * Updates a user's health status and triggers recursive fencing if required.
     * Consolidated into a single transaction with optimized Cypher to meet NFR-1 (<1s target).
     */
    public void updateStatus(String anonymousId, String status) {
        updateStatus(anonymousId, status, false);
    }

    @Transactional("neo4jTransactionManager")
    @CacheEvict(cacheNames = "userStatus", allEntries = true)
    public void updateStatus(String anonymousId, String status, boolean adminOverride) {
        log.info("Updating status: {} -> {} (Admin Override: {})", anonymousId, status, adminOverride);

        if ("ACTIVE".equals(status) && !adminOverride) {
            checkFenceWindow(anonymousId);
        }
        
        var settings = systemSettingsRepository.getSettings()
                .orElse(com.circleguard.promotion.model.jpa.SystemSettings.builder()
                        .encounterWindowDays(14)
                        .mandatoryFenceDays(14)
                        .unconfirmedFencingEnabled(true)
                        .autoThresholdSeconds(3600L)
                        .build());
        
        long threshold = System.currentTimeMillis() - ((long)settings.getEncounterWindowDays() * 24 * 60 * 60 * 1000);

        // Robust Multi-Tier High-Confidence Propagation Cypher - Augmented with isValid checks and timing
        String unifiedQuery = 
            "MATCH (source:User {anonymousId: $id}) " +
            "SET source.status = $status, source.statusUpdatedAt = timestamp() " +
            "WITH source " +
            "OPTIONAL MATCH (source)-[r1]-(c1:User) " +
            "WHERE ( " +
            "  (type(r1)='ENCOUNTERED' AND coalesce(r1.isValid, true) AND r1.startTime > $threshold) OR " +
            "  (type(r1)='MEMBER_OF' AND EXISTS { MATCH (source)-[:MEMBER_OF]->(circ:Circle)<-[:MEMBER_OF]-(c1) WHERE coalesce(circ.isValid, true) }) " +
            ") " +
            "  AND c1.status <> 'CONFIRMED' AND c1.status <> 'RECOVERED' " +
            "WITH source, c1, " +
            "     CASE WHEN $status = 'CONFIRMED' THEN 'SUSPECT' " +
            "          WHEN $status = 'SUSPECT' THEN 'PROBABLE' " +
            "          ELSE c1.status END as l1Status " +
            "WHERE c1 IS NOT NULL AND c1.status <> l1Status " +
            "SET c1.status = l1Status, c1.statusUpdatedAt = timestamp() " +
            "WITH source, collect(DISTINCT {id: c1.anonymousId, status: l1Status}) as l1 " +
            "OPTIONAL MATCH (source)-[r1]-(c1_node:User)-[r2]-(c2:User) " +
            "WHERE $status = 'CONFIRMED' " +
            "  AND ( " +
            "    (type(r1)='ENCOUNTERED' AND coalesce(r1.isValid, true) AND r1.startTime > $threshold) OR " +
            "    (type(r1)='MEMBER_OF' AND EXISTS { MATCH (source)-[:MEMBER_OF]->(circ1:Circle)<-[:MEMBER_OF]-(c1_node) WHERE coalesce(circ1.isValid, true) }) " +
            "  ) " +
            "  AND ( " +
            "    (type(r2)='ENCOUNTERED' AND coalesce(r2.isValid, true) AND r2.startTime > $threshold) OR " +
            "    (type(r2)='MEMBER_OF' AND EXISTS { MATCH (c1_node)-[:MEMBER_OF]->(circ2:Circle)<-[:MEMBER_OF]-(c2) WHERE coalesce(circ2.isValid, true) }) " +
            "  ) " +
            "  AND c2.status = 'ACTIVE' AND c2.anonymousId <> source.anonymousId " +
            "SET c2.status = 'PROBABLE' " +
            "WITH source, l1, collect(DISTINCT {id: c2.anonymousId, status: 'PROBABLE'}) as l2 " +
            "RETURN source.anonymousId as sourceId, [x in (l1 + l2) WHERE x.id IS NOT NULL] as affectedContacts";

        var result = neo4jClient.query(unifiedQuery)
                .bind(anonymousId).to("id")
                .bind(status).to("status")
                .bind(threshold).to("threshold")
                .fetch().one();

        if (result.isPresent()) {
            Map<String, String> cacheUpdates = new HashMap<>();
            cacheUpdates.put(STATUS_KEY_PREFIX + anonymousId, status);
            
            @SuppressWarnings("unchecked")
            List<Map<String, String>> affected = (List<Map<String, String>>) result.get().get("affectedContacts");
            if (affected != null) {
                affected.forEach(m -> {
                    if (m != null && m.get("id") != null) {
                        cacheUpdates.put(STATUS_KEY_PREFIX + m.get("id"), m.get("status"));
                    }
                });
            }

            log.info("Batch updating {} Redis entries based on consolidated propagation", cacheUpdates.size());
            updateRedisInBatches(cacheUpdates);

            // Broadcast change
            Map<String, Object> payload = new HashMap<>();
            payload.put("anonymousId", anonymousId);
            payload.put("status", status);
            payload.put("timestamp", System.currentTimeMillis());

            kafkaTemplate.send(TOPIC_STATUS_CHANGED, anonymousId, payload);

            // Story 5.4: Automated Room Reservation Cancellation
            checkAndBroadcastFencedCircles(anonymousId);

            // Story 5.5: Administrative Alerting for Priority Roles
            int affectedCount = (affected != null) ? affected.size() : 0;
            if ("CONFIRMED".equals(status) || affectedCount > 20) {
                log.info("Priority Alert triggered. Status: {}, Affected Count: {}", status, affectedCount);
                Map<String, Object> priorityPayload = new HashMap<>();
                priorityPayload.put("anonymousId", anonymousId);
                priorityPayload.put("status", status);
                priorityPayload.put("affectedCount", affectedCount);
                priorityPayload.put("timestamp", System.currentTimeMillis());
                priorityPayload.put("eventType", "CONFIRMED".equals(status) ? "CONFIRMED_CASE" : "LARGE_OUTBREAK");
                
                kafkaTemplate.send("alert.priority", anonymousId, priorityPayload);
            }
        }
    }

    private void checkAndBroadcastFencedCircles(String anonymousId) {
        var fencedCircles = circleNodeRepository.findNewlyFencedCircles(anonymousId);
        for (var circle : fencedCircles) {
            log.info("Circle {} is now fully fenced. Broadcasting circle.fenced event.", circle.getName());
            Map<String, Object> circlePayload = new HashMap<>();
            circlePayload.put("circleId", circle.getId().toString());
            circlePayload.put("locationId", circle.getLocationId());
            circlePayload.put("name", circle.getName());
            circlePayload.put("timestamp", System.currentTimeMillis());
            
            kafkaTemplate.send("circle.fenced", circle.getId().toString(), circlePayload);
        }
    }

    private void updateRedisInBatches(Map<String, String> updates) {
        Map<String, String> batch = new HashMap<>();
        updates.forEach((key, value) -> {
            batch.put(key, value);
            if (batch.size() >= 2000) {
                redisTemplate.opsForValue().multiSet(batch);
                batch.clear();
            }
        });
        if (!batch.isEmpty()) {
            redisTemplate.opsForValue().multiSet(batch);
        }
    }

    @CacheEvict(cacheNames = "userStatus", key = "#anonymousId")
    public void evictUserCache(String anonymousId) {
        // Method used for programmatic eviction
    }

    @Cacheable(cacheNames = "userStatus", key = "#anonymousId")
    public String getCachedStatus(String anonymousId) {
        return redisTemplate.opsForValue().get(STATUS_KEY_PREFIX + anonymousId);
    }

    /**
     * Resolves a user's status to ACTIVE and re-evaluates downstream contacts.
     * Implements the "Pulse Recovery" algorithm for Story 4.4.
     */
    public void resolveStatus(String anonymousId) {
        resolveStatus(anonymousId, false);
    }

    @Transactional("neo4jTransactionManager")
    public void resolveStatus(String anonymousId, boolean adminOverride) {
        log.info("Resolving status for user: {} (Admin Override: {})", anonymousId, adminOverride);

        if (!adminOverride) {
            checkFenceWindow(anonymousId);
        }

        // 1. Resolve source
        neo4jClient.query("MATCH (u:User {anonymousId: $id}) SET u.status = 'ACTIVE', u.statusUpdatedAt = timestamp()")
                .bind(anonymousId).to("id")
                .run();

        // 2. Refined Two-Hop Pulse Recovery
        // Phase 1: Release direct SUSPECT neighbors that have no other CONFIRMED paths
        String phase1Query = 
            "MATCH (source:User {anonymousId: $id}) " +
            "OPTIONAL MATCH (source)-[:ENCOUNTERED|MEMBER_OF]-(target:User) " +
            "WHERE target.status = 'SUSPECT' " +
            "AND NOT EXISTS { " +
            "  MATCH (target)-[:ENCOUNTERED|MEMBER_OF]-(risk:User) " +
            "  WHERE risk.status = 'CONFIRMED' AND risk.anonymousId <> $id " +
            "} " +
            "SET target.status = 'ACTIVE', target.statusUpdatedAt = timestamp() " +
            "RETURN collect(DISTINCT target.anonymousId) as releasedIds";

        var phase1Result = neo4jClient.query(phase1Query)
                .bind(anonymousId).to("id")
                .fetch().one();

        List<String> releasedL1 = new ArrayList<>();
        if (phase1Result.isPresent()) {
            @SuppressWarnings("unchecked")
            List<String> ids = (List<String>) phase1Result.get().get("releasedIds");
            if (ids != null) releasedL1.addAll(ids);
        }

        // Phase 2: Release L2 PROBABLE neighbors that have no other risk paths (CONFIRMED or remaining SUSPECT)
        String phase2Query = 
            "MATCH (l1:User) WHERE l1.anonymousId IN $l1Ids " +
            "OPTIONAL MATCH (l1)-[:ENCOUNTERED|MEMBER_OF]-(target:User) " +
            "WHERE target.status = 'PROBABLE' " +
            "AND NOT EXISTS { " +
            "  MATCH (target)-[:ENCOUNTERED|MEMBER_OF]-(risk:User) " +
            "  WHERE (risk.status = 'CONFIRMED' OR risk.status = 'SUSPECT') " +
            "  AND NOT risk.anonymousId IN $l1Ids " +
            "  AND risk.anonymousId <> $sourceId " +
            "} " +
            "SET target.status = 'ACTIVE', target.statusUpdatedAt = timestamp() " +
            "RETURN collect(DISTINCT target.anonymousId) as releasedIds";

        var phase2Result = neo4jClient.query(phase2Query)
                .bind(releasedL1).to("l1Ids")
                .bind(anonymousId).to("sourceId")
                .fetch().one();

        Map<String, String> cacheUpdates = new HashMap<>();
        cacheUpdates.put(STATUS_KEY_PREFIX + anonymousId, "ACTIVE");

        // Add phase 1 released IDs
        releasedL1.forEach(id -> {
            if (id != null) cacheUpdates.put(STATUS_KEY_PREFIX + id, "ACTIVE");
        });

        // Add phase 2 released IDs
        if (phase2Result.isPresent()) {
            @SuppressWarnings("unchecked")
            List<String> releasedIds = (List<String>) phase2Result.get().get("releasedIds");
            if (releasedIds != null) {
                releasedIds.forEach(id -> {
                    if (id != null) cacheUpdates.put(STATUS_KEY_PREFIX + id, "ACTIVE");
                });
            }
        }

        updateRedisInBatches(cacheUpdates);

        Map<String, Object> payload = new HashMap<>();
        payload.put("anonymousId", anonymousId);
        payload.put("status", "ACTIVE");
        payload.put("timestamp", System.currentTimeMillis());

        kafkaTemplate.send(TOPIC_STATUS_CHANGED, anonymousId, payload);
    }

    /**
     * Promotion to RECOVERED (Immunity Window)
     */
    @Transactional("neo4jTransactionManager")
    public void promoteToRecovered(String anonymousId) {
        resolveStatus(anonymousId);
        // Note: resolveStatus sets to ACTIVE first, then we update to RECOVERED
        neo4jClient.query("MATCH (u:User {anonymousId: $id}) SET u.status = 'RECOVERED'")
                .bind(anonymousId).to("id").run();
        
        // Immunize in Redis for 30 days
        redisTemplate.opsForValue().set(STATUS_KEY_PREFIX + anonymousId, "RECOVERED");
        redisTemplate.expire(STATUS_KEY_PREFIX + anonymousId, java.time.Duration.ofDays(30));
    }
    private void checkFenceWindow(String anonymousId) {
        var userOpt = userNodeRepository.findById(anonymousId);
        if (userOpt.isPresent()) {
            var user = userOpt.get();
            if (("SUSPECT".equals(user.getStatus()) || "PROBABLE".equals(user.getStatus())) 
                && user.getStatusUpdatedAt() != null) {
                
                var settings = systemSettingsRepository.getSettings()
                        .orElseThrow(() -> new IllegalStateException("System Settings not initialized"));
                
                long fenceDurationMs = (long) settings.getMandatoryFenceDays() * 24 * 60 * 60 * 1000;
                long elapsed = System.currentTimeMillis() - user.getStatusUpdatedAt();
                
                if (elapsed < fenceDurationMs) {
                    long remainingDays = (fenceDurationMs - elapsed) / (24 * 60 * 60 * 1000);
                    throw new FenceException("Cannot transition to ACTIVE. User is in mandatory fence window for " + remainingDays + " more days.");
                }
            }
        }
    }
}
