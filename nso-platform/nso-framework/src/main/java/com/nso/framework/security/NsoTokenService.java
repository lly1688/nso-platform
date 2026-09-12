package com.nso.framework.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nso.framework.config.NsoSecurityProperties;
import com.nso.business.core.TenantContext;
import com.nso.system.domain.SysUser;
import com.nso.system.security.UserLifecycle;
import com.nso.system.service.ISysUserService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

// 访问令牌与刷新令牌管理服务。
@Service
public class NsoTokenService {

    private static final String ACCESS_PREFIX = "nso:auth:access:";
    private static final String REFRESH_PREFIX = "nso:auth:refresh:";

    // NSO安全配置
    private final NsoSecurityProperties properties;
    // 字符串Redis模板
    private final StringRedisTemplate redisTemplate;
    // 系统用户服务
    private final ISysUserService userService;
    // 算法
    private final Algorithm algorithm;
    // JWT验证器
    private final JWTVerifier verifier;

    public NsoTokenService(NsoSecurityProperties properties,
                            StringRedisTemplate redisTemplate,
                            ISysUserService userService) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.userService = userService;
        if (properties.getSecret() == null || properties.getSecret().length() < 32) {
            throw new IllegalStateException("NSO_JWT_SECRET must be at least 32 characters");
        }
        this.algorithm = Algorithm.HMAC256(properties.getSecret());
        this.verifier = JWT.require(algorithm).withIssuer(properties.getIssuer()).build();
    }

    // 为当前身份签发访问令牌和刷新令牌。
    public TokenPair issue(NsoPrincipal principal) {
        Instant now = Instant.now();
        String accessId = UUID.randomUUID().toString();
        String refreshId = UUID.randomUUID().toString();
        Duration accessTtl = Duration.ofMinutes(properties.getAccessTokenMinutes());
        Duration refreshTtl = Duration.ofDays(properties.getRefreshTokenDays());
        String accessToken = JWT.create()
                .withIssuer(properties.getIssuer())
                .withSubject(principal.userId().toString())
                .withJWTId(accessId)
                .withClaim("tenantId", principal.tenantId())
                .withClaim("username", principal.username())
                .withClaim("clientId", principal.clientId())
                .withClaim("roles", principal.roles())
                .withClaim("permissions", principal.permissions())
                .withClaim("authVersion", principal.authVersion())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plus(accessTtl)))
                .sign(algorithm);
        String refreshToken = JWT.create()
                .withIssuer(properties.getIssuer())
                .withSubject(principal.userId().toString())
                .withJWTId(refreshId)
                .withClaim("tenantId", principal.tenantId())
                .withClaim("clientId", principal.clientId())
                .withClaim("authVersion", principal.authVersion())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plus(refreshTtl)))
                .sign(algorithm);
        redisTemplate.opsForValue().set(ACCESS_PREFIX + accessId, principal.userId().toString(), accessTtl);
        redisTemplate.opsForValue().set(REFRESH_PREFIX + sha256(refreshToken), principal.userId().toString(), refreshTtl);
        return new TokenPair(accessToken, refreshToken);
    }

    // 解析并校验访问令牌。
    public NsoPrincipal parseAccessToken(String token) {
        DecodedJWT jwt = verify(token);
        if (!redisTemplate.hasKey(ACCESS_PREFIX + jwt.getId())) {
            throw new JWTVerificationException("session revoked");
        }
        Long tenantId = jwt.getClaim("tenantId").asLong();
        if (tenantId == null || tenantId < 1) throw new JWTVerificationException("tenant claim missing");
        Long userId = Long.valueOf(jwt.getSubject());
        int authVersion = jwt.getClaim("authVersion").isNull() ? 0 : jwt.getClaim("authVersion").asInt();
        validateSessionUser(userId, tenantId, authVersion);
        return new NsoPrincipal(
                tenantId, userId, jwt.getClaim("username").asString(), jwt.getClaim("clientId").asString(),
                claimList(jwt, "roles"), claimList(jwt, "permissions"), authVersion);
    }

    public NsoPrincipal parseRefreshPrincipal(String token, String clientId) {
        DecodedJWT jwt = verify(token);
        if (!clientId.equals(jwt.getClaim("clientId").asString())) {
            throw new JWTVerificationException("client mismatch");
        }
        Long tenantId = jwt.getClaim("tenantId").asLong();
        if (tenantId == null || tenantId < 1) throw new JWTVerificationException("tenant claim missing");
        return new NsoPrincipal(tenantId, Long.valueOf(jwt.getSubject()), null, clientId, List.of(), List.of(),
                jwt.getClaim("authVersion").isNull() ? 0 : jwt.getClaim("authVersion").asInt());
    }

    // 一次性消费刷新令牌。
    public Long consumeRefreshToken(String refreshToken, String clientId) {
        DecodedJWT jwt = verify(refreshToken);
        if (!clientId.equals(jwt.getClaim("clientId").asString())) {
            throw new JWTVerificationException("client mismatch");
        }
        String key = REFRESH_PREFIX + sha256(refreshToken);
        String userId = redisTemplate.opsForValue().get(key);
        // 读取后立即删除，保证刷新令牌不可重放。
        if (userId == null || !Boolean.TRUE.equals(redisTemplate.delete(key))) {
            throw new JWTVerificationException("refresh token revoked");
        }
        Long parsedUserId = Long.valueOf(userId);
        if (!parsedUserId.toString().equals(jwt.getSubject())) {
            throw new JWTVerificationException("refresh subject mismatch");
        }
        Long tenantId = jwt.getClaim("tenantId").asLong();
        if (tenantId == null || tenantId < 1) throw new JWTVerificationException("tenant claim missing");
        int authVersion = jwt.getClaim("authVersion").isNull() ? 0 : jwt.getClaim("authVersion").asInt();
        TenantContext.withTenant(tenantId, () -> userService.findById(parsedUserId)
                .orElseThrow(() -> new JWTVerificationException("session user missing")));
        validateSessionUser(parsedUserId, tenantId, authVersion);
        return parsedUserId;
    }

    // 撤销访问令牌和刷新令牌。
    public void revoke(String accessToken, String refreshToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            try {
                redisTemplate.delete(ACCESS_PREFIX + verify(accessToken).getId());
            } catch (JWTVerificationException ignored) {
            }
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            redisTemplate.delete(REFRESH_PREFIX + sha256(refreshToken));
        }
    }

    private DecodedJWT verify(String token) {
        return verifier.verify(token);
    }

    private List<String> claimList(DecodedJWT jwt, String name) {
        List<String> values = jwt.getClaim(name).asList(String.class);
        return values == null ? List.of() : List.copyOf(values);
    }

    private void validateSessionUser(Long userId, Long tenantId, int authVersion) {
        SysUser user = TenantContext.withTenant(tenantId, () -> userService.findById(userId)
                .orElseThrow(() -> new JWTVerificationException("session user missing")));
        int currentVersion = user.getAuthVersion() == null ? 0 : user.getAuthVersion();
        if (!tenantId.equals(user.getTenantId()) || !"INTERNAL".equals(user.getUserType())
                || !UserLifecycle.isActive(user.getStatus()) || currentVersion != authVersion) {
            throw new JWTVerificationException("session revoked");
        }
    }

    private String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("cannot hash token", ex);
        }
    }

    public record TokenPair(
        // 访问令牌
        String accessToken,
        // 刷新令牌
        String refreshToken
    ) {
    }
}
