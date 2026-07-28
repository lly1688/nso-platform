package com.nso.framework.security;

import com.nso.business.core.TenantContext;
import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@org.springframework.stereotype.Component
public class NsoTokenAuthenticationFilter extends OncePerRequestFilter {

    private final NsoTokenService tokenService;

    public NsoTokenAuthenticationFilter(NsoTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith("Bearer ")) {
                String token = authorization.substring("Bearer ".length());
                try {
                    NsoPrincipal principal = tokenService.parseAccessToken(token);
                    java.util.List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
                    principal.roles().forEach(role -> authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())));
                    principal.permissions().forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission)));
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    TenantContext.set(new TenantContext.Actor(principal.tenantId(), principal.userId(), principal.username(), principal.roles()));
                } catch (RuntimeException ignored) {
                    SecurityContextHolder.clearContext();
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

}
