package com.circleguard.promotion.service;

import com.circleguard.promotion.repository.jpa.SystemSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatusLifecycleService {
    private final Neo4jClient neo4jClient;
    private final SystemSettingsRepository systemSettingsRepository;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String STATUS_KEY_PREFIX = "user:status:";
    private static final String TOPIC_STATUS_CHANGED = "promotion.status.changed";

    /**
     * Automated status cleanup. 
     * Runs every hour to transition SUSPECT/PROBABLE users back to ACTIVE after their fence window expires.
     */
    @Scheduled(cron = "0 0 * * * *") // Every hour
    @Transactional("neo4jTransactionManager")
    public void processAutomaticTransitions() {
        var settings = systemSettingsRepository.getSettings()
                .orElseThrow(() -> new IllegalStateException("System Settings not initialized"));

        long expirationThreshold = System.currentTimeMillis() - 
                ((long)settings.getMandatoryFenceDays() * 24 * 60 * 60 * 1000);

        log.info("Status lifecycle check - Mandatory Fence: {} days, Threshold: {}", 
                settings.getMandatoryFenceDays(), expirationThreshold);

        String query = 
            "MATCH (u:User) " +
            "WHERE u.status IN ['SUSPECT', 'PROBABLE'] " +
            "RETURN u.anonymousId as id, u.statusUpdatedAt as updated, $threshold as thresh";

        neo4jClient.query(query).bind(expirationThreshold).to("threshold").fetch().all().forEach(m -> {
            log.info("User {} has statusUpdatedAt {} (Threshold: {})", m.get("id"), m.get("updated"), m.get("thresh"));
        });

        String updateQuery = 
            "MATCH (u:User) " +
            "WHERE u.status IN ['SUSPECT', 'PROBABLE'] " +
            "  AND coalesce(u.statusUpdatedAt, 0) < $threshold " +
            "SET u.status = 'ACTIVE', u.statusUpdatedAt = timestamp() " +
            "RETURN collect(u.anonymousId) as releasedIds";

        var result = neo4jClient.query(updateQuery)
                .bind(expirationThreshold).to("threshold")
                .fetch().one();

        if (result.isPresent()) {
            @SuppressWarnings("unchecked")
            List<String> releasedIds = (List<String>) result.get().get("releasedIds");
            if (releasedIds != null && !releasedIds.isEmpty()) {
                log.info("Automatically released users back to ACTIVE: {}", releasedIds);
                
                Map<String, String> cacheUpdates = new HashMap<>();
                releasedIds.forEach(id -> {
                    cacheUpdates.put(STATUS_KEY_PREFIX + id, "ACTIVE");
                    
                    // Broadcast individual change
                    kafkaTemplate.send(TOPIC_STATUS_CHANGED, id, Map.of(
                        "anonymousId", id,
                        "status", "ACTIVE",
                        "reason", "AUTO_WINDOW_EXPIRY",
                        "timestamp", System.currentTimeMillis()
                    ));
                });

                // Batch update Redis
                redisTemplate.opsForValue().multiSet(cacheUpdates);
            }
        }
    }
}
