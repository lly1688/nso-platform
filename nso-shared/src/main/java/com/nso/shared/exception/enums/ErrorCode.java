package com.nso.shared.exception.enums;

// 业务错误码枚举。
public enum ErrorCode {

    // 操作成功
    SUCCESS(0, "success"),
    // 业务错误
    BUSINESS_ERROR(400, "business error"),
    // 未授权
    UNAUTHORIZED(401, "unauthorized"),
    // 无权限
    FORBIDDEN(403, "forbidden"),
    // 未找到
    NOT_FOUND(404, "not found"),
    // 登录锁定
    LOGIN_LOCKED(423, "login locked"),
    // 请求限流
    RATE_LIMITED(429, "rate limited"),
    // 系统错误
    SYSTEM_ERROR(500, "system error");

    // 错误码
    private final int code;
    // 错误消息
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
