package com.circleguard.notification.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PriorityAlertListener {

    private final ObjectMapper objectMapper;
    private final TemplateService templateService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${auth.api.url:http://circleguard-auth-service:8080}")
    private String authApiUrl;

    @KafkaListener(topics = "alert.priority", groupId = "notification-priority-group")
    public void handlePriorityAlert(String message) {
        log.info("Received alert.priority event: {}", message);
        try {
            Map<String, Object> payload = objectMapper.readValue(message, new TypeReference<Map<String, Object>>() {});
            String eventType = (String) payload.get("eventType");
            Integer affectedCount = (Integer) payload.get("affectedCount");
            
            log.info("Processing {} Priority Alert. Affected: {}", eventType, affectedCount);

            // Fetch users with the alert:receive_priority permission
            String url = authApiUrl + "/api/v1/users/permissions/alert:receive_priority";
            @SuppressWarnings("unchecked")
            List<Map<String, String>> admins = restTemplate.getForObject(url, List.class);
            
            if (admins != null && !admins.isEmpty()) {
                for (Map<String, String> admin : admins) {
                    String email = admin.get("email");
                    String username = admin.get("username");
                    if (email != null && !email.isEmpty()) {
                        log.info("Dispatching priority alert to admin email: {}", email);
                        // Using TemplateService to mock the dispatch for now.
                        templateService.generateEmailContent(eventType, username);
                    }
                }
            } else {
                log.warn("No administrators found with alert:receive_priority permission.");
            }
        } catch (Exception e) {
            log.error("Failed to process alert.priority event: {}", e.getMessage(), e);
        }
    }
}
