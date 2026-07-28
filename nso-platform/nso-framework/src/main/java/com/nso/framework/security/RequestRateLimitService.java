package com.nso.framework.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;

@Service
public class RequestRateLimitService {
    private static final String PREFIX = "nso:security:rate:";

    private final StringRedisTemplate redisTemplate;

    public RequestRateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean tryAcquire(String scope, String identity, int limit, Duration window) {
        if (limit < 1 || window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("rate limit configuration is invalid");
        }
        try {
            String key = PREFIX + scope + ":" + fingerprint(identity);
            Long count = redisTemplate.opsForValue().increment(key);
            if (count == null) {
                return true;
            }
            if (count == 1) {
                redisTemplate.expire(key, window);
            }
            return count <= limit;
        } catch (RuntimeException ignored) {
            // Redis is an acceleration and protection layer, not the source of business truth.
            return true;
        }
    }

    private String fingerprint(String identity) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest((identity == null ? "" : identity).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("cannot create rate limit key", ex);
        }
    }
}
