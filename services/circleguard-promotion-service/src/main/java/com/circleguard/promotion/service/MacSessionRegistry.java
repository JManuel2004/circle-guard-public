package com.circleguard.promotion.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class MacSessionRegistry {
    private final StringRedisTemplate redisTemplate;
    private static final String KEY_PREFIX = "session:mac:";
    private static final Duration DEFAULT_TTL = Duration.ofHours(8);

    public void registerSession(String macAddress, String anonymousId) {
        String key = KEY_PREFIX + macAddress.toLowerCase();
        redisTemplate.opsForValue().set(key, anonymousId, DEFAULT_TTL);
    }

    public String getAnonymousId(String macAddress) {
        String key = KEY_PREFIX + macAddress.toLowerCase();
        return redisTemplate.opsForValue().get(key);
    }

    public void closeSession(String macAddress) {
        String key = KEY_PREFIX + macAddress.toLowerCase();
        redisTemplate.delete(key);
    }
}
