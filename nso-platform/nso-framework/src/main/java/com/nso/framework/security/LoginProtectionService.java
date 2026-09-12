package com.nso.framework.security;

import com.nso.shared.exception.BusinessException;
import com.nso.shared.exception.enums.ErrorCode;
import com.nso.framework.config.NsoSecurityProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;

// 登录失败保护服务。
@Service
public class LoginProtectionService {

    private static final String FAILED_PREFIX = "nso:security:login:failed:";
    private static final String LOCKED_PREFIX = "nso:security:login:locked:";
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
    // NSO安全配置
    private final NsoSecurityProperties properties;

    public LoginProtectionService(StringRedisTemplate redisTemplate, NsoSecurityProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    // 检查账号与客户端是否处于登录锁定期。
    public void assertAllowed(String username, String clientId) {
        String key = LOCKED_PREFIX + subject(username, clientId);
        Long seconds = redisTemplate.getExpire(key);
        if (seconds != null && seconds > 0) {
            throw new BusinessException(ErrorCode.LOGIN_LOCKED, "登录失败次数过多，请稍后再试");
        }
    }

    // 记录一次登录失败并在达到阈值时锁定。
    public void recordFailure(String username, String clientId) {
        String subject = subject(username, clientId);
        String failedKey = FAILED_PREFIX + subject;
        Long failures = redisTemplate.execute(INCREMENT_WITH_EXPIRY, List.of(failedKey),
                Long.toString(Math.max(1L, Duration.ofMinutes(properties.getLoginFailureWindowMinutes()).toSeconds())));
        if (failures == null) {
            return;
        }
        if (failures >= properties.getLoginMaxFailures()) {
            redisTemplate.opsForValue().set(
                    LOCKED_PREFIX + subject, "1", Duration.ofMinutes(properties.getLoginLockMinutes()));
            redisTemplate.delete(failedKey);
        }
    }

    // 清除登录失败计数。
    public void clearFailures(String username, String clientId) {
        redisTemplate.delete(FAILED_PREFIX + subject(username, clientId));
    }

    private String subject(String username, String clientId) {
        String value = (username == null ? "" : username.trim().toLowerCase())
                + ":" + (clientId == null ? "" : clientId);
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("cannot protect login key", ex);
        }
    }
}
