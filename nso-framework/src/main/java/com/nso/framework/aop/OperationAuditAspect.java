package com.nso.framework.aop;

import com.nso.business.support.IAuditService;
import com.nso.framework.security.NsoPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.UUID;

@Aspect @Component public class OperationAuditAspect {
 private final IAuditService audit; public OperationAuditAspect(IAuditService audit){this.audit=audit;}
 @Around("within(com.nso.web.controller..*)") public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {ServletRequestAttributes attrs=(ServletRequestAttributes)RequestContextHolder.getRequestAttributes();HttpServletRequest request=attrs==null?null:attrs.getRequest();String uri=request==null?"":request.getRequestURI();String method=request==null?"":request.getMethod();String trace=request==null?null:request.getHeader("X-Trace-Id");if(trace==null||trace.isBlank())trace=UUID.randomUUID().toString().replace("-","");Authentication authentication=SecurityContextHolder.getContext().getAuthentication();String user="ANONYMOUS";String client=uri.contains("/mp/")?"MP":uri.contains("/public/")?"PUBLIC":"ADMIN";if(authentication!=null&&authentication.getPrincipal() instanceof NsoPrincipal principal)user=principal.username();try{Object value=joinPoint.proceed();recordSafely(user,client,joinPoint,uri,method,"SUCCESS","接口调用成功",trace);return value;}catch(Throwable ex){recordSafely(user,client,joinPoint,uri,method,"FAIL","接口调用失败: "+ex.getClass().getSimpleName(),trace);throw ex;}}
 private void recordSafely(String user,String client,ProceedingJoinPoint joinPoint,String uri,String method,String result,String summary,String trace){try{audit.record(user,client,joinPoint.getTarget().getClass().getSimpleName(),joinPoint.getSignature().getName(),uri,method,result,summary,trace);}catch(RuntimeException ignored){}}
}
