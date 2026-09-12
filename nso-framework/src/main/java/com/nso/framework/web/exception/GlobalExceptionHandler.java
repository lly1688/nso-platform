package com.nso.framework.web.exception;

import com.nso.business.support.IAuditService;
import com.nso.shared.core.domain.AjaxResult;
import com.nso.shared.exception.BusinessException;
import com.nso.framework.security.NsoPrincipal;

import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

// 全局异常响应处理器。
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 审计服务
    private final IAuditService auditService;

    public GlobalExceptionHandler(IAuditService auditService) {
        this.auditService = auditService;
    }

    // 转换业务异常及规则阻断详情。
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<AjaxResult<?>> handleBusinessException(BusinessException exception) {
        HttpStatus status = switch (exception.getCode()) {
            case 401 -> HttpStatus.UNAUTHORIZED;
            case 403 -> HttpStatus.FORBIDDEN;
            case 404 -> HttpStatus.NOT_FOUND;
            case 423 -> HttpStatus.LOCKED;
            case 429 -> HttpStatus.TOO_MANY_REQUESTS;
            default -> HttpStatus.BAD_REQUEST;
        };
        if (exception.hasRuleDetail()) {
            return ResponseEntity.status(status)
                    .body(AjaxResult.error(exception.getCode(), exception.getMessage(), exception.toRuleDetail()));
        }
        return ResponseEntity.status(status).body(AjaxResult.error(exception.getCode(), exception.getMessage()));
    }

    // 转换方法级权限拒绝异常并记录安全审计。
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<AjaxResult<Void>> handleAccessDenied(AccessDeniedException exception) {
        recordAccessDenied();
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(AjaxResult.error(403, "当前账号没有执行此操作的权限"));
    }

    private void recordAccessDenied() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attributes == null ? null : attributes.getRequest();
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication != null && authentication.getPrincipal() instanceof NsoPrincipal principal
                    ? principal.username() : "ANONYMOUS";
            String uri = request == null ? "" : request.getRequestURI();
            String method = request == null ? "" : request.getMethod();
            String client = uri.contains("/mp/") ? "MP" : uri.contains("/public/") ? "PUBLIC" : "ADMIN";
            String traceId = request == null ? null : request.getHeader("X-Trace-Id");
            auditService.record(username, client, "SECURITY", "ACCESS_DENIED", uri, method,
                    "DENIED", "权限校验拒绝", traceId == null || traceId.isBlank()
                            ? UUID.randomUUID().toString().replace("-", "") : traceId);
        } catch (RuntimeException ignored) {
            // 审计存储暂时不可用时仍需返回原安全响应。
        }
    }

    // 转换请求体字段校验异常。
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public AjaxResult<Void> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("参数校验失败");
        return AjaxResult.error(400, message);
    }

    // 转换约束校验异常。
    @ExceptionHandler(ConstraintViolationException.class)
    public AjaxResult<Void> handleConstraintViolationException(ConstraintViolationException exception) {
        return AjaxResult.error(400, exception.getMessage());
    }

    // 转换未处理的系统异常。
    @ExceptionHandler(Exception.class)
    public AjaxResult<Void> handleException(Exception exception) {
        return AjaxResult.error(500, exception.getMessage());
    }
}
