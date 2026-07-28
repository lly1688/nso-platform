package com.nso.common.core.domain;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

public class AjaxResult<T> implements Serializable {

    public static final int SUCCESS_CODE = 0;
    public static final int ERROR_CODE = 500;

    private int code;
    private String message;
    private T data;
    private String traceId;
    private OffsetDateTime timestamp;

    public AjaxResult() {
    }

    public AjaxResult(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = UUID.randomUUID().toString().replace("-", "");
        this.timestamp = OffsetDateTime.now();
    }

    public static AjaxResult<Void> success() {
        return new AjaxResult<>(SUCCESS_CODE, "success", null);
    }

    public static <T> AjaxResult<T> success(T data) {
        return new AjaxResult<>(SUCCESS_CODE, "success", data);
    }

    public static AjaxResult<Void> error(String message) {
        return new AjaxResult<>(ERROR_CODE, message, null);
    }

    public static AjaxResult<Void> error(int code, String message) {
        return new AjaxResult<>(code, message, null);
    }

    public static <T> AjaxResult<T> error(int code, String message, T data) {
        return new AjaxResult<>(code, message, data);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
