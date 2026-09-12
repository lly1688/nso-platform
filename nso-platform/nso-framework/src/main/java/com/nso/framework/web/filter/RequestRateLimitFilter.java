package com.nso.framework.web.filter;

import com.nso.shared.core.domain.AjaxResult;
import com.nso.framework.config.NsoSecurityProperties;
import com.nso.framework.security.NsoPrincipal;
import com.nso.framework.security.RequestRateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

// 请求频率限制过滤器。
@Component
public class RequestRateLimitFilter extends OncePerRequestFilter {

    // 请求速率限流服务
    private final RequestRateLimitService rateLimitService;
    // NSO安全配置
    private final NsoSecurityProperties properties;
    // 对象数据映射
    private final ObjectMapper objectMapper;

    public RequestRateLimitFilter(RequestRateLimitService rateLimitService,
                                  NsoSecurityProperties properties,
                                  ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    // 按接口场景执行分钟级限流。
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Policy policy = policy(request);
        if (policy != null && !rateLimitService.tryAcquire(
                policy.scope(), identity(request), policy.limit(), Duration.ofMinutes(1))) {
            response.setStatus(429);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");
            objectMapper.writeValue(response.getWriter(), AjaxResult.error(429, "请求过于频繁，请稍后再试"));
            return;
        }
        chain.doFilter(request, response);
    }

    private Policy policy(HttpServletRequest request) {
        String path = request.getRequestURI();
        if ("POST".equals(request.getMethod())
                && path.endsWith("/auth/refresh")) {
            return new Policy("auth", properties.getAuthRateLimitPerMinute());
        }
        if ("GET".equals(request.getMethod()) && ((path.contains("/files/") && path.endsWith("/download"))
                || (path.contains("/exports/") && path.endsWith("/download")))) {
            return new Policy("download", properties.getDownloadRateLimitPerMinute());
        }
        return null;
    }

    private String identity(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof NsoPrincipal principal) {
            return principal.tenantId() + ":" + principal.userId();
        }
        return request.getRemoteAddr();
    }

    // 限流策略。
    private record Policy(String scope, int limit) {
    }
}
