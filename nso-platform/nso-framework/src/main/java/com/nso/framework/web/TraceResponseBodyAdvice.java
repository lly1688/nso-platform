package com.nso.framework.web;

import com.nso.framework.web.filter.TraceIdFilter;
import com.nso.shared.core.domain.AjaxResult;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

// 响应体链路追踪增强器。
@ControllerAdvice
public class TraceResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    // 对所有控制器响应启用链路追踪增强。
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    // 将当前追踪编号同步到统一响应体和响应头。
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof AjaxResult<?> result) {
            String traceId = TraceIdFilter.currentTraceId();
            if (traceId != null && !traceId.isBlank()) {
                result.setTraceId(traceId);
                response.getHeaders().set(TraceIdFilter.HEADER, traceId);
            }
        }
        return body;
    }
}
