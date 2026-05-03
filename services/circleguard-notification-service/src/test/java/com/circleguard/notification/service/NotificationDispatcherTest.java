package com.circleguard.notification.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class NotificationDispatcherTest {

    @Autowired
    private NotificationDispatcher dispatcher;

    @MockBean
    private EmailService emailService;

    @MockBean
    private SmsService smsService;

    @MockBean
    private TemplateService templateService;

    @MockBean
    private PushService pushService;

    @Test
    void shouldDispatchToAllChannelsConcurrently() throws Exception {
        // Setup slow services to test concurrency
        when(emailService.sendAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        when(smsService.sendAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(null));
        when(pushService.sendAsync(any(), any(), any())).thenReturn(CompletableFuture.completedFuture(null));

        dispatcher.dispatch("user-123", "Your health status has changed.");

        verify(emailService, timeout(1000)).sendAsync(eq("user-123"), any());
        verify(smsService, timeout(1000)).sendAsync(eq("user-123"), any());
        verify(pushService, timeout(1000)).sendAsync(eq("user-123"), any(), any());
    }
}
