package com.nso.framework.web.filter;

import com.nso.business.core.TenantContext;
import com.nso.shared.core.domain.AjaxResult;
import com.nso.shared.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Set;

// 写请求幂等过滤器。
@Component
public class RequestIdempotencyFilter extends OncePerRequestFilter {

    // 需要幂等控制的 HTTP 方法
    private static final Set<String> WRITE_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");

    // JDBC模板
    private final JdbcTemplate jdbc;
    // 对象数据映射
    private final ObjectMapper objectMapper;

    public RequestIdempotencyFilter(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    // 预占请求编号并记录最终处理状态。
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = request.getHeader("X-Request-Id");
        if (!WRITE_METHODS.contains(request.getMethod()) || requestId == null || requestId.isBlank()) {
            chain.doFilter(request, response);
            return;
        }
        if (requestId.length() > 96) {
            writeBusinessError(response, new BusinessException("X-Request-Id 长度不能超过 96 位"));
            return;
        }
        long tenantId = TenantContext.tenantId();
        long userId = TenantContext.userId() == null ? 0L : TenantContext.userId();
        // 先通过唯一键预占请求编号，重复请求在进入业务层前即被拦截。
        try {
            jdbc.update("INSERT INTO nso_idempotency_key (tenant_id,user_id,request_id,request_method,request_path,status) VALUES (?,?,?,?,?,'PROCESSING')",
                    tenantId, userId, requestId, request.getMethod(), request.getRequestURI());
        } catch (DuplicateKeyException ex) {
            writeBusinessError(response, BusinessException.ruleBlock("IDEMPOTENT_REPLAY", "重复提交已被拦截", requestId, "新的 requestId", "请刷新结果后再操作"));
            return;
        }
        // 业务执行结果回写幂等记录，便于后续审计和故障定位。
        try {
            chain.doFilter(request, response);
            jdbc.update("UPDATE nso_idempotency_key SET status='COMPLETED', completed_at=? WHERE tenant_id=? AND user_id=? AND request_id=? AND request_method=? AND request_path=?",
                    LocalDateTime.now(), tenantId, userId, requestId, request.getMethod(), request.getRequestURI());
        } catch (ServletException | IOException | RuntimeException ex) {
            jdbc.update("UPDATE nso_idempotency_key SET status='FAILED', completed_at=? WHERE tenant_id=? AND user_id=? AND request_id=? AND request_method=? AND request_path=?",
                    LocalDateTime.now(), tenantId, userId, requestId, request.getMethod(), request.getRequestURI());
            throw ex;
        }
    }

    private void writeBusinessError(HttpServletResponse response, BusinessException exception) throws IOException {
        response.setStatus(exception.getCode());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Object data = exception.hasRuleDetail() ? exception.toRuleDetail() : null;
        objectMapper.writeValue(response.getWriter(), AjaxResult.error(exception.getCode(), exception.getMessage(), data));
    }
}
