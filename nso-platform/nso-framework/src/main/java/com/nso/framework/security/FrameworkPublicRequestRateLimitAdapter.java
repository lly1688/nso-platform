package com.nso.framework.security;

import com.nso.framework.config.NsoSecurityProperties;
import com.nso.system.security.PublicRequestRateLimitPort;
import org.springframework.stereotype.Component;

import java.time.Duration;

// 公开请求限流适配器。
@Component
public class FrameworkPublicRequestRateLimitAdapter implements PublicRequestRateLimitPort {

    // 请求速率限流服务
    private final RequestRateLimitService rateLimits;
    // NSO安全配置
    private final NsoSecurityProperties properties;

    public FrameworkPublicRequestRateLimitAdapter(RequestRateLimitService rateLimits,
                                                  NsoSecurityProperties properties) {
        this.rateLimits = rateLimits;
        this.properties = properties;
    }

    // 校验密码找回请求的 IP 和账号双重限流。
    @Override
    public boolean allowPasswordRecovery(String clientIp, String username) {
        String ip = normalize(clientIp);
        int limit = properties.getPasswordRecoveryRateLimitPerMinute();
        boolean ipAllowed = rateLimits.tryAcquire("password-recovery-ip", ip, limit, Duration.ofMinutes(1));
        boolean accountAllowed = rateLimits.tryAcquire(
                "password-recovery-account", ip + ":" + username.trim().toLowerCase(), limit, Duration.ofMinutes(1));
        return ipAllowed && accountAllowed;
    }

    // 校验访客支持请求的客户端限流。
    @Override
    public boolean allowGuestSupport(String clientIp) {
        return rateLimits.tryAcquire(
                "guest-support",
                normalize(clientIp),
                properties.getGuestSupportRateLimitPerMinute(),
                Duration.ofMinutes(1));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }
}
