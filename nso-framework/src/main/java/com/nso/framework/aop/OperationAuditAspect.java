package com.nso.framework.aop;

import com.nso.business.support.IAuditService;
import com.nso.framework.security.NsoPrincipal;
import com.nso.framework.web.filter.TraceIdFilter;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

// 控制器操作审计切面。
@Aspect
@Component
public class OperationAuditAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationAuditAspect.class);

    // 审计服务
    private final IAuditService audit;
    // 指标计数器
    private final Counter auditWriteFailures;

    public OperationAuditAspect(IAuditService audit, MeterRegistry meterRegistry) {
        this.audit = audit;
        this.auditWriteFailures = Counter.builder("nso.audit.write.failures")
                .description("Audit write failures that did not block the source request")
                .register(meterRegistry);
    }

    // 执行控制器调用并记录成功或失败审计。
    @Around("within(com.nso.web.controller..*)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attrs == null ? null : attrs.getRequest();
        String uri = request == null ? "" : request.getRequestURI();
        String method = request == null ? "" : request.getMethod();
        String trace = TraceIdFilter.currentTraceId();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String user = "ANONYMOUS";
        String client = uri.contains("/mp/") ? "MP" : uri.contains("/public/") ? "PUBLIC" : "ADMIN";
        if (authentication != null && authentication.getPrincipal() instanceof NsoPrincipal principal) {
            user = principal.username();
        }

        // 审计写入失败不能覆盖原接口结果，统一由 recordSafely 降级处理。
        try {
            Object value = joinPoint.proceed();
            recordSafely(user, client, joinPoint, uri, method, "SUCCESS", "接口调用成功", trace);
            return value;
        } catch (Throwable ex) {
            log.error("Unhandled controller failure traceId={} {} {}", trace, method, uri, ex);
            recordSafely(user, client, joinPoint, uri, method, "FAIL",
                    "接口调用失败: " + ex.getClass().getSimpleName(), trace);
            throw ex;
        }
    }

    private void recordSafely(String user, String client, ProceedingJoinPoint joinPoint, String uri,
                              String method, String result, String summary, String trace) {
        try {
            audit.record(user, client, joinPoint.getTarget().getClass().getSimpleName(),
                    joinPoint.getSignature().getName(), uri, method, result, summary, trace);
        } catch (RuntimeException exception) {
            auditWriteFailures.increment();
            log.warn("Audit persistence failed traceId={} {} {}", trace, method, uri, exception);
        }
    }
}
