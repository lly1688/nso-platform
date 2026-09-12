package com.nso.business.support;

/**
 * 操作审计服务接口。
 */
public interface IAuditService {

    /**
     * 记录一次接口操作审计。
     *
     * @param userName 操作用户
     * @param clientType 客户端类型
     * @param module 业务模块
     * @param operation 操作名称
     * @param requestUri 请求地址
     * @param requestMethod 请求方法
     * @param result 操作结果
     * @param summary 操作摘要
     * @param traceId 链路编号
     */
    void record(
            String userName,
            String clientType,
            String module,
            String operation,
            String requestUri,
            String requestMethod,
            String result,
            String summary,
            String traceId);
}
