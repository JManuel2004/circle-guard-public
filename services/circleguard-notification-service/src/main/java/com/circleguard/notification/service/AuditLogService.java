package com.circleguard.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private static final String TOPIC_AUDIT = "notification.audit";

    public void logDelivery(String userId, String channel, String status, String correlationId) {
        log.info("Logging notification audit: User={}, Channel={}, Status={}, CorrelationId={}", 
                userId, channel, status, correlationId);
        
        Map<String, Object> auditEvent = new HashMap<>();
        auditEvent.put("eventId", UUID.randomUUID().toString());
        auditEvent.put("timestamp", Instant.now().toString());
        auditEvent.put("userId", userId); // This is an anonymousId, so it's compliant
        auditEvent.put("channel", channel);
        auditEvent.put("status", status);
        auditEvent.put("correlationId", correlationId != null ? correlationId : UUID.randomUUID().toString());
        
        kafkaTemplate.send(TOPIC_AUDIT, userId, auditEvent);
    }
}
