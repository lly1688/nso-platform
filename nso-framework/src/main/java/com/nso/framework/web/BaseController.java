package com.nso.framework.web;

import com.nso.shared.core.domain.AjaxResult;

// 控制器响应封装基类。
public abstract class BaseController {

    // 返回无数据的成功响应。
    protected AjaxResult success() {
        return AjaxResult.success();
    }

    // 返回携带数据的成功响应。
    protected <T> AjaxResult<T> success(T data) {
        return AjaxResult.success(data);
    }

    // 返回默认错误响应。
    protected AjaxResult error(String message) {
        return AjaxResult.error(message);
    }
}
