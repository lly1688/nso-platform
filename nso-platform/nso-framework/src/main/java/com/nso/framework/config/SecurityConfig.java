package com.nso.framework.config;

import com.nso.framework.security.NsoTokenAuthenticationFilter;
import com.nso.framework.web.filter.RequestIdempotencyFilter;
import com.nso.framework.web.filter.RequestRateLimitFilter;
import com.nso.framework.web.filter.TraceIdFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// Spring Security 安全过滤链配置。
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    // NSO令牌认证过滤器
    private final NsoTokenAuthenticationFilter tokenAuthenticationFilter;
    // 请求Idempotency过滤器
    private final RequestIdempotencyFilter requestIdempotencyFilter;
    // 请求速率限流过滤器
    private final RequestRateLimitFilter requestRateLimitFilter;
    // 链路追踪标识过滤器
    private final TraceIdFilter traceIdFilter;
    // NSOAPI文档配置
    private final NsoApiDocsProperties apiDocsProperties;

    public SecurityConfig(NsoTokenAuthenticationFilter tokenAuthenticationFilter,
                          RequestIdempotencyFilter requestIdempotencyFilter,
                          RequestRateLimitFilter requestRateLimitFilter,
                          TraceIdFilter traceIdFilter,
                          NsoApiDocsProperties apiDocsProperties) {
        this.tokenAuthenticationFilter = tokenAuthenticationFilter;
        this.requestIdempotencyFilter = requestIdempotencyFilter;
        this.requestRateLimitFilter = requestRateLimitFilter;
        this.traceIdFilter = traceIdFilter;
        this.apiDocsProperties = apiDocsProperties;
    }

    // 构建统一接入认证、限流和幂等控制的无状态过滤链。
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            .requestMatchers(
                                "/api/v1/admin/auth/login",
                                "/api/v1/admin/auth/refresh",
                                "/api/v1/admin/auth/password-recovery-requests",
                                "/api/v1/admin/auth/support-tickets",
                                "/api/v1/public/**",
                                "/actuator/health/**",
                                "/actuator/prometheus").permitAll();
                    if (apiDocsProperties.isEnabled()) {
                        auth.requestMatchers("/doc.html", "/v3/api-docs/**").permitAll();
                    }
                    auth
                        .requestMatchers("/api/v1/admin/**").authenticated()
                            .anyRequest().denyAll();
                })
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                writeSecurityError(response, 401, "登录已过期或尚未登录"))
                        .accessDeniedHandler((request, response, exception) ->
                                writeSecurityError(response, 403, "当前账号没有执行此操作的权限")))
                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(traceIdFilter, NsoTokenAuthenticationFilter.class)
                .addFilterAfter(requestRateLimitFilter, NsoTokenAuthenticationFilter.class)
                .addFilterAfter(requestIdempotencyFilter, RequestRateLimitFilter.class);
        return http.build();
    }

    // 配置管理端跨域访问策略。
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(java.util.List.of(
                "http://localhost:*",
                "https://localhost:*",
                "http://127.0.0.1:*",
                "http://[::1]:*"
        ));
        configuration.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(java.util.List.of("Authorization", "Content-Type", "X-Trace-Id", "X-Request-Id"));
        configuration.setExposedHeaders(java.util.List.of("Authorization", "X-Trace-Id", "X-Request-Id"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // 提供 BCrypt 密码编码器。
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private void writeSecurityError(jakarta.servlet.http.HttpServletResponse response,
                                    int code,
                                    String message) throws IOException {
        response.setStatus(code);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String traceId = TraceIdFilter.currentTraceId();
        if (traceId != null && !traceId.isBlank()) {
            response.setHeader(TraceIdFilter.HEADER, traceId);
        }
        response.getWriter().write("{\"code\":" + code + ",\"message\":\"" + message
                + "\",\"data\":null,\"traceId\":\"" + (traceId == null ? "" : traceId)
                + "\",\"timestamp\":null}");
    }
}
