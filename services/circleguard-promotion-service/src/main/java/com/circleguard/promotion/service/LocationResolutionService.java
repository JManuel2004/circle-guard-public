package com.circleguard.promotion.service;

import com.circleguard.promotion.model.AccessPoint;
import com.circleguard.promotion.repository.jpa.AccessPointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationResolutionService {
    private final AccessPointRepository accessPointRepository;
    private final MacSessionRegistry sessionRegistry;
    private final GraphService graphService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final StringRedisTemplate redisTemplate;

    private static final String TOPIC_PROXIMITY_DETECTED = "proximity.detected";
    private static final String SPATIAL_PREFIX = "spatial:location:";

    /**
     * Resolves a physical detection from a WiFi controller log into an anonymized proximity event.
     */
    public void processSignal(String apMac, String deviceMac, Double rssi) {
        // 1. Resolve AP Location
        AccessPoint ap = accessPointRepository.findByMacAddress(apMac)
                .orElse(null);
        if (ap == null) {
            log.warn("Unknown Access Point detected: {}", apMac);
            return;
        }

        // 2. Resolve Anonymous User ID from Session Registry
        String anonymousId = sessionRegistry.getAnonymousId(deviceMac);
        if (anonymousId == null) {
            // Devices with randomized MACs not currently in an active app session are ignored
            log.debug("Unmapped MAC detected: {}. Monitoring only.", deviceMac);
            return;
        }

        // 3. Construct Proximity Event
        Map<String, Object> event = Map.of(
            "anonymousId", anonymousId,
            "buildingId", ap.getFloor().getBuilding().getId(),
            "floorId", ap.getFloor().getId(),
            "coordinates", Map.of("x", ap.getCoordinateX(), "y", ap.getCoordinateY()),
            "rssi", rssi,
            "timestamp", System.currentTimeMillis()
        );

        // 4. Emit to Kafka for Graph Processing
        kafkaTemplate.send(TOPIC_PROXIMITY_DETECTED, anonymousId, event);
        
        // 5. Update Graph & Detect Circles
        updateGraph(ap.getId().toString(), anonymousId);
        
        log.info("Proximity Resolved: User {} is near AP {} (Floor {})", anonymousId, ap.getName(), ap.getFloor().getFloorNumber());
    }

    private void updateGraph(String locationId, String anonymousId) {
        String key = SPATIAL_PREFIX + locationId;
        
        // Get other users at this location
        Set<String> others = redisTemplate.opsForSet().members(key);
        if (others != null) {
            others.forEach(otherId -> {
                if (!otherId.equals(anonymousId)) {
                    graphService.recordEncounter(anonymousId, otherId, locationId);
                }
            });
        }

        // Add self to location set
        redisTemplate.opsForSet().add(key, anonymousId);
        redisTemplate.expire(key, java.time.Duration.ofMinutes(10));

        // Trigger circle detection
        graphService.detectAndFormCircles(locationId);
    }
}
