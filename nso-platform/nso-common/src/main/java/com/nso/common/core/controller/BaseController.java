package com.nso.common.core.controller;

import com.nso.common.core.domain.AjaxResult;

public abstract class BaseController {

    protected AjaxResult success() {
        return AjaxResult.success();
    }

    protected <T> AjaxResult<T> success(T data) {
        return AjaxResult.success(data);
    }

    protected AjaxResult error(String message) {
        return AjaxResult.error(message);
    }
}
