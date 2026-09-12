package com.nso.framework.security;

import com.nso.business.core.TenantContext;
import com.nso.system.service.ISysUserService;
import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

// 令牌认证过滤器。
@org.springframework.stereotype.Component
public class NsoTokenAuthenticationFilter extends OncePerRequestFilter {

    // NSO令牌服务
    private final NsoTokenService tokenService;
    // 系统用户服务
    private final ISysUserService userService;

    public NsoTokenAuthenticationFilter(NsoTokenService tokenService, ISysUserService userService) {
        this.tokenService = tokenService;
        this.userService = userService;
    }

    // 解析 Bearer 令牌并建立安全上下文和租户上下文。
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith("Bearer ")) {
                String token = authorization.substring("Bearer ".length());
                try {
                    NsoPrincipal principal = tokenService.parseAccessToken(token);
                    if (!NsoClientBoundary.allows(request.getRequestURI(), principal.clientId())) {
                        throw new IllegalStateException("client route mismatch");
                    }
                    java.util.List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
                    principal.roles().forEach(role ->
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
                    principal.permissions().forEach(permission ->
                            authorities.add(new SimpleGrantedAuthority(permission)));
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    TenantContext.set(new TenantContext.Actor(
                            principal.tenantId(), principal.userId(), principal.username(), principal.roles()));
                    if (requiresPasswordChange(request, principal)) {
                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
                        response.setContentType("application/json");
                        response.getWriter().write("{\"code\":403,\"message\":\"首次登录必须修改密码\",\"data\":null}");
                        return;
                    }
                } catch (RuntimeException ignored) {
                    SecurityContextHolder.clearContext();
                    TenantContext.clear();
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private boolean requiresPasswordChange(HttpServletRequest request, NsoPrincipal principal) {
        String path = request.getRequestURI();
        int marker = path.indexOf("/api/v1/");
        if (marker >= 0) path = path.substring(marker);
        if (!path.startsWith("/api/v1/admin/"))
            return false;
        if (path.equals("/api/v1/admin/profile/password")
                || (path.equals("/api/v1/admin/profile") && "GET".equals(request.getMethod()))
                || path.equals("/api/v1/admin/auth/me") || path.equals("/api/v1/admin/auth/logout"))
            return false;
        return userService.findById(principal.userId())
                .map(user -> Integer.valueOf(1).equals(user.getForceChangePassword()))
                .orElse(false);
    }
}
