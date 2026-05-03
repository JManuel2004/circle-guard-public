package com.circleguard.notification.service;

import java.util.concurrent.CompletableFuture;

public interface SmsService {
    CompletableFuture<Void> sendAsync(String userId, String message);
}
