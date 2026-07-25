package com.nso.common.core.domain;

import java.io.Serializable;

public class AjaxResult<T> implements Serializable {

    public static final int SUCCESS_CODE = 200;
    public static final int ERROR_CODE = 500;

    private int code;
    private String message;
    private T data;

    public AjaxResult() {
    }

    public AjaxResult(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
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
}
