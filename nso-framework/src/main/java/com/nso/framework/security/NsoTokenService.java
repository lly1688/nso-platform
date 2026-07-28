package com.nso.framework.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nso.framework.config.NsoSecurityProperties;
import com.nso.system.domain.SysUser;
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

@Service
public class NsoTokenService {
    private static final String ACCESS_PREFIX = "nso:auth:access:";
    private static final String REFRESH_PREFIX = "nso:auth:refresh:";

    private final NsoSecurityProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final ISysUserService userService;
    private final Algorithm algorithm;
    private final JWTVerifier verifier;

    public NsoTokenService(NsoSecurityProperties properties, StringRedisTemplate redisTemplate, ISysUserService userService) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.userService = userService;
        if (properties.getSecret() == null || properties.getSecret().length() < 32) {
            throw new IllegalStateException("NSO_JWT_SECRET must be at least 32 characters");
        }
        this.algorithm = Algorithm.HMAC256(properties.getSecret());
        this.verifier = JWT.require(algorithm).withIssuer(properties.getIssuer()).build();
    }

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
                .withClaim("clientId", principal.clientId())
                .withClaim("authVersion", principal.authVersion())
                .withIssuedAt(Date.from(now))
                .withExpiresAt(Date.from(now.plus(refreshTtl)))
                .sign(algorithm);
        redisTemplate.opsForValue().set(ACCESS_PREFIX + accessId, principal.userId().toString(), accessTtl);
        redisTemplate.opsForValue().set(REFRESH_PREFIX + sha256(refreshToken), principal.userId().toString(), refreshTtl);
        return new TokenPair(accessToken, refreshToken);
    }

    public NsoPrincipal parseAccessToken(String token) {
        DecodedJWT jwt = verify(token);
        if (!redisTemplate.hasKey(ACCESS_PREFIX + jwt.getId())) {
            throw new JWTVerificationException("session revoked");
        }
        Long tenantId = jwt.getClaim("tenantId").isNull() ? 1L : jwt.getClaim("tenantId").asLong();
        Long userId = Long.valueOf(jwt.getSubject());
        int authVersion = jwt.getClaim("authVersion").isNull() ? 0 : jwt.getClaim("authVersion").asInt();
        validateSessionUser(userId, tenantId, authVersion);
        return new NsoPrincipal(tenantId, userId, jwt.getClaim("username").asString(), jwt.getClaim("clientId").asString(),
                claimList(jwt, "roles"), claimList(jwt, "permissions"), authVersion);
    }

    public Long consumeRefreshToken(String refreshToken, String clientId) {
        DecodedJWT jwt = verify(refreshToken);
        if (!clientId.equals(jwt.getClaim("clientId").asString())) {
            throw new JWTVerificationException("client mismatch");
        }
        String key = REFRESH_PREFIX + sha256(refreshToken);
        String userId = redisTemplate.opsForValue().get(key);
        if (userId == null || !Boolean.TRUE.equals(redisTemplate.delete(key))) {
            throw new JWTVerificationException("refresh token revoked");
        }
        Long parsedUserId = Long.valueOf(userId);
        int authVersion = jwt.getClaim("authVersion").isNull() ? 0 : jwt.getClaim("authVersion").asInt();
        SysUser user = userService.findById(parsedUserId).orElseThrow(() -> new JWTVerificationException("session user missing"));
        validateSessionUser(parsedUserId, user.getTenantId(), authVersion);
        return parsedUserId;
    }

    public void revoke(String accessToken, String refreshToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            try { redisTemplate.delete(ACCESS_PREFIX + verify(accessToken).getId()); } catch (JWTVerificationException ignored) { }
        }
        if (refreshToken != null && !refreshToken.isBlank()) {
            redisTemplate.delete(REFRESH_PREFIX + sha256(refreshToken));
        }
    }

    private DecodedJWT verify(String token) { return verifier.verify(token); }

    private List<String> claimList(DecodedJWT jwt, String name) {
        List<String> values = jwt.getClaim(name).asList(String.class);
        return values == null ? List.of() : List.copyOf(values);
    }

    private void validateSessionUser(Long userId, Long tenantId, int authVersion) {
        SysUser user = userService.findById(userId).orElseThrow(() -> new JWTVerificationException("session user missing"));
        int currentVersion = user.getAuthVersion() == null ? 0 : user.getAuthVersion();
        if (!tenantId.equals(user.getTenantId()) || !"ENABLED".equals(user.getStatus()) || currentVersion != authVersion) {
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

    public record TokenPair(String accessToken, String refreshToken) { }
}
