package com.circleguard.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class PushServiceImpl implements PushService {

    private final WebClient webClient;

    @Value("${push.gotify.url:http://localhost:8080}")
    private String gotifyUrl;

    @Value("${push.gotify.token:MOCK_TOKEN}")
    private String gotifyToken;

    @jakarta.annotation.Resource
    private AuditLogService auditLogService;

    public PushServiceImpl(WebClient.Builder webClientBuilder, 
                           @Value("${push.gotify.url:http://localhost:8080}") String gotifyUrl) {
        this.webClient = webClientBuilder.baseUrl(gotifyUrl).build();
    }

    @Override
    @Async
    public CompletableFuture<Void> sendAsync(String userId, String message) {
        return sendAsync(userId, message, Map.of());
    }

    @Override
    @Async
    @org.springframework.retry.annotation.Retryable(
        retryFor = { Exception.class },
        maxAttempts = 3,
        backoff = @org.springframework.retry.annotation.Backoff(delay = 2000)
    )
    public CompletableFuture<Void> sendAsync(String userId, String message, Map<String, String> metadata) {
        String correlationId = java.util.UUID.randomUUID().toString();
        if (gotifyToken.equals("MOCK_TOKEN")) {
            log.info("[MOCK PUSH] To: {}, Content: {}, Metadata: {}", userId, message, metadata);
            auditLogService.logDelivery(userId, "PUSH", "SUCCESS", correlationId);
            return CompletableFuture.completedFuture(null);
        }

        try {
            log.debug("Attempting to send push notification with metadata to user: {}", userId);
            
            Map<String, Object> body = new HashMap<>();
            body.put("title", "CircleGuard Alert");
            body.put("message", message);
            body.put("priority", 5);
            
            if (!metadata.isEmpty()) {
                body.put("extras", Map.of("client::notification", metadata));
            }
            
            webClient.post()
                .uri("/message?token=" + gotifyToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .toBodilessEntity()
                .toFuture()
                .join(); // Block in the @Async thread to support @Retryable

            log.info("Push notification with metadata sent successfully to user: {}", userId);
            auditLogService.logDelivery(userId, "PUSH", "SUCCESS", correlationId);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.warn("Failed to send push notification to user {} (correlationId: {}): {}", userId, correlationId, e.getMessage());
            auditLogService.logDelivery(userId, "PUSH", "RETRY", correlationId);
            throw e;
        }
    }

    @org.springframework.retry.annotation.Recover
    public CompletableFuture<Void> recover(Exception e, String userId, String message, Map<String, String> metadata) {
        log.error("Push delivery failed after max retries for user: {}. Error: {}", userId, e.getMessage());
        auditLogService.logDelivery(userId, "PUSH", "FAILED", null);
        return CompletableFuture.failedFuture(e);
    }
}
