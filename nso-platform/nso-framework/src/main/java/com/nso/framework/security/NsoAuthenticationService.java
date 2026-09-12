package com.nso.framework.security;

import com.nso.shared.exception.BusinessException;
import com.nso.shared.exception.enums.ErrorCode;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.nso.framework.config.NsoBootstrapProperties;
import com.nso.framework.config.NsoSecurityProperties;
import com.nso.business.core.TenantContext;
import com.nso.system.domain.SysUser;
import com.nso.system.security.UserLifecycle;
import com.nso.system.service.ISysUserService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

// 统一认证服务。
@Service
public class NsoAuthenticationService implements ApplicationRunner {

    // 系统用户服务
    private final ISysUserService userService;
    // NSO令牌服务
    private final NsoTokenService tokenService;
    // 密码编码器
    private final PasswordEncoder passwordEncoder;
    // NSO启动配置
    private final NsoBootstrapProperties bootstrap;
    // 登录Protection服务
    private final LoginProtectionService loginProtectionService;
    // 请求速率限流服务
    private final RequestRateLimitService requestRateLimitService;
    // NSO安全配置
    private final NsoSecurityProperties securityProperties;

    public NsoAuthenticationService(ISysUserService userService,
                                    NsoTokenService tokenService,
                                    PasswordEncoder passwordEncoder,
                                    NsoBootstrapProperties bootstrap,
                                    LoginProtectionService loginProtectionService,
                                    RequestRateLimitService requestRateLimitService,
                                    NsoSecurityProperties securityProperties) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.bootstrap = bootstrap;
        this.loginProtectionService = loginProtectionService;
        this.requestRateLimitService = requestRateLimitService;
        this.securityProperties = securityProperties;
    }

    // 使用用户名和密码登录。
    public AuthenticatedSession passwordLogin(String username, String password, String clientId) {
        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            throw new BusinessException("用户名和密码不能为空");
        }
        if (!requestRateLimitService.tryAcquire(
                "auth-login",
                clientId + ":" + username.trim().toLowerCase(),
                securityProperties.getAuthRateLimitPerMinute(),
                Duration.ofMinutes(1))) {
            throw new BusinessException(ErrorCode.RATE_LIMITED, "请求过于频繁，请稍后再试");
        }
        loginProtectionService.assertAllowed(username, clientId);
        SysUser user = userService.findByUsername(username)
                .orElse(null);
        if (user == null || !UserLifecycle.isActive(user.getStatus())) {
            loginProtectionService.recordFailure(username, clientId);
            throw new BusinessException("用户名或密码错误");
        }
        return TenantContext.withTenant(user.getTenantId(), () -> {
            if (!matchesAndUpgradeLegacyPassword(user, password)) {
                loginProtectionService.recordFailure(username, clientId);
                throw new BusinessException("用户名或密码错误");
            }
            loginProtectionService.clearFailures(username, clientId);
            return session(user, clientId);
        });
    }

    // 消费刷新令牌并创建新会话。
    public AuthenticatedSession refresh(String refreshToken, String clientId) {
        NsoPrincipal refreshPrincipal;
        try {
            refreshPrincipal = tokenService.parseRefreshPrincipal(refreshToken, clientId);
        } catch (JWTVerificationException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        Long userId;
        try {
            userId = tokenService.consumeRefreshToken(refreshToken, clientId);
        } catch (JWTVerificationException exception) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return TenantContext.withTenant(refreshPrincipal.tenantId(), () -> {
            SysUser user = userService.findById(userId)
                    .filter(item -> UserLifecycle.isActive(item.getStatus()))
                    .orElseThrow(() -> new BusinessException("登录会话已失效"));
            if (!refreshPrincipal.tenantId().equals(user.getTenantId())) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED);
            }
            return session(user, clientId);
        });
    }

    // 查询当前认证会话信息。
    public AuthenticatedSession current(NsoPrincipal principal) {
        return TenantContext.withTenant(principal.tenantId(), () -> {
            SysUser user = userService.findById(principal.userId())
                    .filter(item -> UserLifecycle.isActive(item.getStatus()))
                    .orElseThrow(() -> new BusinessException("用户已禁用或不存在"));
            return describe(user, principal.clientId(), null);
        });
    }

    // 校验当前密码并设置新密码。
    public void changePassword(NsoPrincipal principal, String currentPassword, String newPassword) {
        if (principal == null || currentPassword == null || currentPassword.isBlank()
                || newPassword == null || newPassword.length() < 8) {
            throw new BusinessException("当前密码和至少 8 位的新密码不能为空");
        }
        TenantContext.withTenant(principal.tenantId(), () -> {
            SysUser user = userService.findById(principal.userId())
                    .filter(item -> UserLifecycle.isActive(item.getStatus()))
                    .orElseThrow(() -> new BusinessException("用户已禁用或不存在"));
            if (!matchesPassword(user, currentPassword)) {
                throw new BusinessException("当前密码不正确");
            }
            userService.upgradePassword(user.getId(), passwordEncoder.encode(newPassword));
            return null;
        });
    }

    // 注销访问令牌和刷新令牌。
    public void logout(String accessToken, String refreshToken) {
        tokenService.revoke(accessToken, refreshToken);
    }

    private AuthenticatedSession session(SysUser user, String clientId) {
        if (!"INTERNAL".equals(user.getUserType())) throw new BusinessException("外部客户身份不能进入内部系统");
        // 令牌携带当前授权快照，后续通过授权版本使旧会话即时失效。
        List<String> roles = userService.roleCodes(user.getId());
        List<String> permissions = userService.permissionCodes(user.getId());
        int authVersion = userService.authVersion(user.getId());
        NsoTokenService.TokenPair pair = tokenService.issue(new NsoPrincipal(user.getTenantId(), user.getId(), user.getUsername(), clientId, roles, permissions, authVersion));
        return describe(user, clientId, pair);
    }

    private AuthenticatedSession describe(SysUser user, String clientId, NsoTokenService.TokenPair pair) {
        List<String> roles = userService.roleCodes(user.getId());
        List<String> permissions = userService.permissionCodes(user.getId());
        return new AuthenticatedSession(user.getTenantId(), user.getId(), user.getUsername(), user.getNickname(),
                Integer.valueOf(1).equals(user.getForceChangePassword()), clientId, roles,
                permissions, userService.menuRoutes(user.getId()),
                pair == null ? null : pair.accessToken(), pair == null ? null : pair.refreshToken());
    }

    private boolean matchesAndUpgradeLegacyPassword(SysUser user, String rawPassword) {
        // 首次成功登录后升级历史明文散列，避免继续保留兼容格式。
        if (user.getPasswordHash() != null && user.getPasswordHash().startsWith("{noop}")) {
            boolean matched = matchesPassword(user, rawPassword);
            if (matched) {
                userService.upgradePassword(user.getId(), passwordEncoder.encode(rawPassword));
            }
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

    // 按配置初始化系统引导管理员。
    @Override
    public void run(ApplicationArguments args) {
        if (bootstrap.isEnabled() && bootstrap.getAdminUsername() != null && !bootstrap.getAdminUsername().isBlank()
                && bootstrap.getAdminPassword() != null && !bootstrap.getAdminPassword().isBlank()) {
            userService.ensureBootstrapAdmin(
                    bootstrap.getAdminUsername(), passwordEncoder.encode(bootstrap.getAdminPassword()));
        }
    }

    public record AuthenticatedSession(
        // 租户编号
        Long tenantId,
        // 用户编号
        Long userId,
        // 用户名
        String username,
        // 用户昵称
        String nickname,
        // 是否强制修改密码
        boolean forceChangePassword,
        // 客户端编号
        String clientId,
        // 角色列表
        List<String> roles,
        // 权限列表
        List<String> permissions,
        // 菜单路由列表
        List<String> menus,
        // 访问令牌
        String accessToken,
        // 刷新令牌
        String refreshToken
    ) {
    }
}
