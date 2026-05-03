package com.circleguard.notification.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class SmsServiceImpl implements SmsService {

    @Value("${twilio.account-sid:AC_MOCK_SID}")
    private String accountSid;

    @Value("${twilio.auth-token:MOCK_TOKEN}")
    private String authToken;

    @Value("${twilio.from-number:+15550000000}")
    private String fromNumber;

    @jakarta.annotation.Resource
    private AuditLogService auditLogService;

    @PostConstruct
    public void init() {
        if (!accountSid.startsWith("AC_MOCK")) {
            Twilio.init(accountSid, authToken);
        }
    }

    @Override
    @Async
    @org.springframework.retry.annotation.Retryable(
        retryFor = { Exception.class },
        maxAttempts = 3,
        backoff = @org.springframework.retry.annotation.Backoff(delay = 2000)
    )
    public CompletableFuture<Void> sendAsync(String userId, String messageContent) {
        String correlationId = java.util.UUID.randomUUID().toString();
        if (accountSid.startsWith("AC_MOCK")) {
            log.info("[MOCK SMS] To: {}, Content: {}", userId, messageContent);
            auditLogService.logDelivery(userId, "SMS", "SUCCESS", correlationId);
            return CompletableFuture.completedFuture(null);
        }

        try {
            log.debug("Attempting to send SMS to user: {}", userId);
            // Mocking phone number lookup
            String toPhone = "+15551112222"; 
            
            Message.creator(
                new PhoneNumber(toPhone),
                new PhoneNumber(fromNumber),
                messageContent
            ).create();

            log.info("SMS sent successfully to user: {}", userId);
            auditLogService.logDelivery(userId, "SMS", "SUCCESS", correlationId);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.warn("Failed to send SMS to user {} (correlationId: {}): {}", userId, correlationId, e.getMessage());
            auditLogService.logDelivery(userId, "SMS", "RETRY", correlationId);
            throw e;
        }
    }

    @org.springframework.retry.annotation.Recover
    public CompletableFuture<Void> recover(Exception e, String userId, String messageContent) {
        log.error("SMS delivery failed after max retries for user: {}. Error: {}", userId, e.getMessage());
        auditLogService.logDelivery(userId, "SMS", "FAILED", null);
        return CompletableFuture.failedFuture(e);
    }
}
