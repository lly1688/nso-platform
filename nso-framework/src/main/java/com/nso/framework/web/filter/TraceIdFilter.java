package com.nso.framework.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

// 链路追踪编号过滤器。
@Component
public class TraceIdFilter extends OncePerRequestFilter {

    // 链路追踪请求头
    public static final String HEADER = "X-Trace-Id";

    // 可接受的外部追踪编号格式
    private static final Pattern SAFE_TRACE_ID = Pattern.compile("^[A-Za-z0-9_-]{8,64}$");

    // 复用合法追踪编号或生成新编号，并写入日志上下文。
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requested = request.getHeader(HEADER);
        String traceId = requested != null && SAFE_TRACE_ID.matcher(requested).matches()
                ? requested
                : UUID.randomUUID().toString().replace("-", "");
        response.setHeader(HEADER, traceId);
        try (MDC.MDCCloseable ignored = MDC.putCloseable("traceId", traceId)) {
            chain.doFilter(request, response);
        }
    }

    // 获取当前线程的链路追踪编号。
    public static String currentTraceId() {
        return MDC.get("traceId");
    }
}
