package com.nso.common.exception.enums;

public enum ErrorCode {

    SUCCESS(0, "success"),
    BUSINESS_ERROR(400, "business error"),
    UNAUTHORIZED(401, "unauthorized"),
    FORBIDDEN(403, "forbidden"),
    NOT_FOUND(404, "not found"),
    LOGIN_LOCKED(423, "login locked"),
    RATE_LIMITED(429, "rate limited"),
    SYSTEM_ERROR(500, "system error");

    private final int code;
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
