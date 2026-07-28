package com.nso.framework.config;

import com.nso.framework.security.NsoTokenAuthenticationFilter;
import com.nso.framework.web.filter.RequestIdempotencyFilter;
import com.nso.framework.web.filter.RequestRateLimitFilter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final NsoTokenAuthenticationFilter tokenAuthenticationFilter;
    private final RequestIdempotencyFilter requestIdempotencyFilter;
    private final RequestRateLimitFilter requestRateLimitFilter;

    public SecurityConfig(NsoTokenAuthenticationFilter tokenAuthenticationFilter, RequestIdempotencyFilter requestIdempotencyFilter,
                          RequestRateLimitFilter requestRateLimitFilter) {
        this.tokenAuthenticationFilter = tokenAuthenticationFilter;
        this.requestIdempotencyFilter = requestIdempotencyFilter;
        this.requestRateLimitFilter = requestRateLimitFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/api/v1/admin/auth/login",
                                "/api/v1/admin/auth/refresh",
                                "/api/v1/mp/auth/wechat-login",
                                "/api/v1/mp/auth/login",
                                "/api/v1/mp/auth/refresh",
                                "/api/v1/public/**",
                                "/actuator/health",
                                "/doc.html",
                                "/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/v1/admin/**", "/api/v1/mp/**").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) ->
                                writeSecurityError(response, 401, "登录已过期或尚未登录"))
                        .accessDeniedHandler((request, response, exception) ->
                                writeSecurityError(response, 403, "当前账号没有执行此操作的权限")))
                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(requestRateLimitFilter, NsoTokenAuthenticationFilter.class)
                .addFilterAfter(requestIdempotencyFilter, RequestRateLimitFilter.class);
        return http.build();
    }

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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private void writeSecurityError(jakarta.servlet.http.HttpServletResponse response, int code, String message) throws IOException {
        response.setStatus(code);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":" + code + ",\"message\":\"" + message
                + "\",\"data\":null,\"traceId\":null,\"timestamp\":null}");
    }
}
