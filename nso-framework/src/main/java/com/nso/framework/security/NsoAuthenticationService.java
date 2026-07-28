package com.nso.framework.security;

import com.nso.common.exception.BusinessException;
import com.nso.common.exception.enums.ErrorCode;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.nso.framework.config.NsoBootstrapProperties;
import com.nso.system.domain.SysUser;
import com.nso.system.service.ISysUserService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NsoAuthenticationService implements ApplicationRunner {
    private final ISysUserService userService;
    private final NsoTokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final NsoBootstrapProperties bootstrap;
    private final WechatIdentityResolver wechatIdentityResolver;
    private final LoginProtectionService loginProtectionService;

    public NsoAuthenticationService(ISysUserService userService, NsoTokenService tokenService,
                                    PasswordEncoder passwordEncoder, NsoBootstrapProperties bootstrap,
                                    WechatIdentityResolver wechatIdentityResolver, LoginProtectionService loginProtectionService) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.bootstrap = bootstrap;
        this.wechatIdentityResolver = wechatIdentityResolver;
        this.loginProtectionService = loginProtectionService;
    }

    public AuthenticatedSession passwordLogin(String username, String password, String clientId) {
        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            throw new BusinessException("用户名和密码不能为空");
        }
        loginProtectionService.assertAllowed(username, clientId);
        SysUser user = userService.findByUsername(username)
                .orElse(null);
        if (user == null || !"ENABLED".equals(user.getStatus()) || !matchesAndUpgradeLegacyPassword(user, password)) {
            loginProtectionService.recordFailure(username, clientId);
            throw new BusinessException("用户名或密码错误");
        }
        loginProtectionService.clearFailures(username, clientId);
        return session(user, clientId);
    }

    public AuthenticatedSession refresh(String refreshToken, String clientId) {
        Long userId;
        try {
            userId = tokenService.consumeRefreshToken(refreshToken, clientId);
        } catch (JWTVerificationException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        SysUser user = userService.findById(userId)
                .filter(item -> "ENABLED".equals(item.getStatus()))
                .orElseThrow(() -> new BusinessException("登录会话已失效"));
        return session(user, clientId);
    }

    public AuthenticatedSession wechatLogin(String code) {
        return session(wechatIdentityResolver.resolveBoundUser(code), "mp");
    }

    public AuthenticatedSession current(NsoPrincipal principal) {
        SysUser user = userService.findById(principal.userId())
                .filter(item -> "ENABLED".equals(item.getStatus()))
                .orElseThrow(() -> new BusinessException("用户已禁用或不存在"));
        return describe(user, principal.clientId(), null);
    }

    public void changePassword(NsoPrincipal principal, String currentPassword, String newPassword) {
        if (principal == null || currentPassword == null || currentPassword.isBlank()
                || newPassword == null || newPassword.length() < 8) {
            throw new BusinessException("当前密码和至少 8 位的新密码不能为空");
        }
        SysUser user = userService.findById(principal.userId())
                .filter(item -> "ENABLED".equals(item.getStatus()))
                .orElseThrow(() -> new BusinessException("用户已禁用或不存在"));
        if (!matchesPassword(user, currentPassword)) {
            throw new BusinessException("当前密码不正确");
        }
        userService.upgradePassword(user.getId(), passwordEncoder.encode(newPassword));
    }

    public void logout(String accessToken, String refreshToken) { tokenService.revoke(accessToken, refreshToken); }

    private AuthenticatedSession session(SysUser user, String clientId) {
        List<String> roles = userService.roleCodes(user.getId());
        List<String> permissions = userService.permissionCodes(user.getId());
        int authVersion = userService.authVersion(user.getId());
        NsoTokenService.TokenPair pair = tokenService.issue(new NsoPrincipal(user.getTenantId(), user.getId(), user.getUsername(), clientId, roles, permissions, authVersion));
        return describe(user, clientId, pair);
    }

    private AuthenticatedSession describe(SysUser user, String clientId, NsoTokenService.TokenPair pair) {
        List<String> roles = userService.roleCodes(user.getId());
        List<String> permissions = userService.permissionCodes(user.getId());
        return new AuthenticatedSession(user.getTenantId(), user.getId(), user.getUsername(), user.getNickname(), clientId, roles,
                permissions, userService.menuRoutes(user.getId()),
                pair == null ? null : pair.accessToken(), pair == null ? null : pair.refreshToken());
    }

    private boolean matchesAndUpgradeLegacyPassword(SysUser user, String rawPassword) {
        if (user.getPasswordHash() != null && user.getPasswordHash().startsWith("{noop}")) {
            boolean matched = matchesPassword(user, rawPassword);
            if (matched) { userService.upgradePassword(user.getId(), passwordEncoder.encode(rawPassword)); }
            return matched;
        }
        return matchesPassword(user, rawPassword);
    }

    private boolean matchesPassword(SysUser user, String rawPassword) {
        String stored = user.getPasswordHash();
        if (stored != null && stored.startsWith("{noop}")) {
            return rawPassword.equals(stored.substring("{noop}".length()));
        }
        return stored != null && passwordEncoder.matches(rawPassword, stored);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (bootstrap.isEnabled() && bootstrap.getAdminUsername() != null && !bootstrap.getAdminUsername().isBlank()
                && bootstrap.getAdminPassword() != null && !bootstrap.getAdminPassword().isBlank()) {
            userService.ensureBootstrapAdmin(bootstrap.getAdminUsername(), passwordEncoder.encode(bootstrap.getAdminPassword()));
        }
    }

    public record AuthenticatedSession(Long tenantId, Long userId, String username, String nickname, String clientId,
                                       List<String> roles, List<String> permissions, List<String> menus,
                                       String accessToken, String refreshToken) { }
}
