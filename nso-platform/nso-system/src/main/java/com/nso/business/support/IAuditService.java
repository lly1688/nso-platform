package com.nso.business.support;
public interface IAuditService { void record(String userName, String clientType, String module, String operation, String requestUri, String requestMethod, String result, String summary, String traceId); }
