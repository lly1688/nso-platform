package com.nso.framework.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;

// 请求频率限制服务。
@Service
public class RequestRateLimitService {

    private static final String PREFIX = "nso:security:rate:";
    private static final DefaultRedisScript<Long> INCREMENT_WITH_EXPIRY = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            local ttl = redis.call('TTL', KEYS[1])
            if count == 1 or ttl < 0 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """, Long.class);

    // 字符串Redis模板
    private final StringRedisTemplate redisTemplate;

    public RequestRateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 尝试占用指定限流窗口的请求配额。
    public boolean tryAcquire(String scope, String identity, int limit, Duration window) {
        if (limit < 1 || window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("rate limit configuration is invalid");
        }
        try {
            String key = PREFIX + scope + ":" + fingerprint(identity);
            Long count = redisTemplate.execute(INCREMENT_WITH_EXPIRY, List.of(key),
                    Long.toString(Math.max(1L, window.toSeconds())));
            if (count == null) {
                return false;
            }
            return count <= limit;
        } catch (RuntimeException ignored) {
            // Redis 不可用时拒绝请求，避免绕过保护层。
            return false;
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
