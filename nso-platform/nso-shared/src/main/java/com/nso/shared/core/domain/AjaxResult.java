package com.nso.shared.core.domain;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

// 统一 API 响应结果。
public class AjaxResult<T> implements Serializable {

    // 成功响应码
    public static final int SUCCESS_CODE = 0;

    // 默认错误响应码
    public static final int ERROR_CODE = 500;

    // 响应码
    private int code;

    // 响应消息
    private String message;

    // 响应数据
    private T data;

    // 链路追踪编号
    private String traceId;

    // 响应时间
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

    // 创建无数据的成功响应。
    public static AjaxResult<Void> success() {
        return new AjaxResult<>(SUCCESS_CODE, "success", null);
    }

    // 创建携带数据的成功响应。
    public static <T> AjaxResult<T> success(T data) {
        return new AjaxResult<>(SUCCESS_CODE, "success", data);
    }

    // 创建默认错误响应。
    public static AjaxResult<Void> error(String message) {
        return new AjaxResult<>(ERROR_CODE, message, null);
    }

    // 创建指定响应码的错误响应。
    public static AjaxResult<Void> error(int code, String message) {
        return new AjaxResult<>(code, message, null);
    }

    // 创建携带详情的错误响应。
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
