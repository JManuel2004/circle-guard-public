package com.circleguard.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final AuditLogService auditLogService;

    @Override
    @Async
    @org.springframework.retry.annotation.Retryable(
        retryFor = { Exception.class },
        maxAttempts = 3,
        backoff = @org.springframework.retry.annotation.Backoff(delay = 2000)
    )
    public CompletableFuture<Void> sendAsync(String userId, String message) {
        String correlationId = java.util.UUID.randomUUID().toString();
        try {
            log.debug("Attempting to send email to user: {}", userId);
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo(userId + "@example.com"); 
            mailMessage.setSubject("CircleGuard Health Alert");
            mailMessage.setText(message);
            
            mailSender.send(mailMessage);
            log.info("Email sent successfully to user: {}", userId);
            auditLogService.logDelivery(userId, "EMAIL", "SUCCESS", correlationId);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            log.warn("Failed to send email to user {} (correlationId: {}): {}", userId, correlationId, e.getMessage());
            auditLogService.logDelivery(userId, "EMAIL", "RETRY", correlationId);
            throw e; // Rethrow to trigger retry
        }
    }

    @org.springframework.retry.annotation.Recover
    public CompletableFuture<Void> recover(Exception e, String userId, String message) {
        log.error("Email delivery failed after max retries for user: {}. Error: {}", userId, e.getMessage());
        auditLogService.logDelivery(userId, "EMAIL", "FAILED", null);
        return CompletableFuture.failedFuture(e);
    }
}
