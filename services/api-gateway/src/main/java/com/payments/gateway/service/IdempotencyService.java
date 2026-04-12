package com.payments.gateway.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class IdempotencyService {

    private final RedisTemplate<String, Object> redisTemplate;

    public IdempotencyService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isDuplicate(String transactionId) {
        String key = "idempotency:" + transactionId;
        // setIfAbsent returns true if the key didn't exist and was set.
        // It returns false if the key already exists (duplicate request).
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(key, "PROCESSING", Duration.ofHours(24));
        return isNew != null && !isNew;
    }
}
