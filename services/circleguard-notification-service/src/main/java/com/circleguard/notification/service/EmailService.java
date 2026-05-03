package com.circleguard.notification.service;

import java.util.concurrent.CompletableFuture;

public interface EmailService {
    CompletableFuture<Void> sendAsync(String userId, String message);
}
