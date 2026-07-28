package com.nso.framework.security;

import com.nso.common.exception.BusinessException;
import com.nso.common.exception.enums.ErrorCode;
import com.nso.framework.config.NsoSecurityProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;

@Service
public class LoginProtectionService {
    private static final String FAILED_PREFIX = "nso:security:login:failed:";
    private static final String LOCKED_PREFIX = "nso:security:login:locked:";

    private final StringRedisTemplate redisTemplate;
    private final NsoSecurityProperties properties;

    public LoginProtectionService(StringRedisTemplate redisTemplate, NsoSecurityProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    public void assertAllowed(String username, String clientId) {
        String key = LOCKED_PREFIX + subject(username, clientId);
        Long seconds = redisTemplate.getExpire(key);
        if (seconds != null && seconds > 0) {
            throw new BusinessException(ErrorCode.LOGIN_LOCKED, "登录失败次数过多，请稍后再试");
        }
    }

    public void recordFailure(String username, String clientId) {
        String subject = subject(username, clientId);
        String failedKey = FAILED_PREFIX + subject;
        Long failures = redisTemplate.opsForValue().increment(failedKey);
        if (failures == null) {
            return;
        }
        if (failures == 1) {
            redisTemplate.expire(failedKey, Duration.ofMinutes(properties.getLoginFailureWindowMinutes()));
        }
        if (failures >= properties.getLoginMaxFailures()) {
            redisTemplate.opsForValue().set(LOCKED_PREFIX + subject, "1", Duration.ofMinutes(properties.getLoginLockMinutes()));
            redisTemplate.delete(failedKey);
        }
    }

    public void clearFailures(String username, String clientId) {
        redisTemplate.delete(FAILED_PREFIX + subject(username, clientId));
    }

    private String subject(String username, String clientId) {
        String value = (username == null ? "" : username.trim().toLowerCase()) + ":" + (clientId == null ? "" : clientId);
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("cannot protect login key", ex);
        }
    }
}
