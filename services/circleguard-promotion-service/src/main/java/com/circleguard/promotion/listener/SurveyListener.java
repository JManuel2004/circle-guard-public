package com.circleguard.promotion.listener;

import com.circleguard.promotion.service.HealthStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SurveyListener {
    private final HealthStatusService healthStatusService;

    @KafkaListener(topics = "survey.submitted", groupId = "promotion-service-group")
    public void onSurveySubmitted(Map<String, Object> event) {
        log.info("Received survey submission event: {}", event);
        
        try {
            String anonymousId = (String) event.get("anonymousId");
            Boolean hasSymptoms = (Boolean) event.get("hasSymptoms");
            
            if (anonymousId != null && Boolean.TRUE.equals(hasSymptoms)) {
                log.info("Promoting user {} to SUSPECT due to symptoms", anonymousId);
                healthStatusService.updateStatus(anonymousId, "SUSPECT");
            }
        } catch (Exception e) {
            log.error("Failed to process survey event: {}", event, e);
        }
    }

    @KafkaListener(topics = "certificate.validated", groupId = "promotion-service-group")
    public void onCertificateValidated(Map<String, Object> event) {
        log.info("Received certificate validation event: {}", event);
        
        try {
            String anonymousId = (String) event.get("anonymousId");
            String status = (String) event.get("status");
            
            if (anonymousId != null && "APPROVED".equals(status)) {
                log.info("Restoring user {} to ACTIVE due to approved certificate", anonymousId);
                healthStatusService.updateStatus(anonymousId, "ACTIVE");
            }
        } catch (Exception e) {
            log.error("Failed to process certificate validation event: {}", event, e);
        }
    }
}
