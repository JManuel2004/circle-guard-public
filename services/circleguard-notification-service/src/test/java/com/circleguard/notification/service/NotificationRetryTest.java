package com.circleguard.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
public class NotificationRetryTest {

    @Autowired
    private EmailService emailService;

    @MockBean
    private JavaMailSender mailSender;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void testEmailRetryLogic() throws Exception {
        // Force failure for all attempts
        doThrow(new RuntimeException("Mail server down"))
            .when(mailSender).send(any(SimpleMailMessage.class));

        CompletableFuture<Void> future = emailService.sendAsync("user-1", "test message");
        
        // Wait for retries to complete (3 attempts with 2s backoff)
        try {
            future.join();
        } catch (Exception e) {
            // Expected
        }

        // Verify mailSender.send was called exactly 3 times
        verify(mailSender, times(3)).send(any(SimpleMailMessage.class));
        
        // Verify audit logs were emitted (2 retries, 1 failed)
        // KafkaTemplate.send(topic, key, payload)
        verify(kafkaTemplate, atLeast(3)).send(eq("notification.audit"), anyString(), anyMap());
    }
}
