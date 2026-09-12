package com.nso.shared.exception;

import com.nso.shared.exception.enums.ErrorCode;

import java.util.LinkedHashMap;
import java.util.Map;

// 业务异常。
public class BusinessException extends RuntimeException {

    // 错误码
    private final int code;

    // 规则编码
    private final String ruleCode;

    // 当前值
    private final String currentValue;

    // 期望值
    private final String expectedValue;

    // 建议操作
    private final String action;

    public BusinessException(String message) {
        super(message);
        this.code = ErrorCode.BUSINESS_ERROR.getCode();
        this.ruleCode = null;
        this.currentValue = null;
        this.expectedValue = null;
        this.action = null;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
        this.ruleCode = null;
        this.currentValue = null;
        this.expectedValue = null;
        this.action = null;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
        this.ruleCode = null;
        this.currentValue = null;
        this.expectedValue = null;
        this.action = null;
    }

    // 构造包含规则详情的业务异常。
    private BusinessException(int code, String message, String ruleCode, String currentValue, String expectedValue, String action) {
        super(message);
        this.code = code;
        this.ruleCode = ruleCode;
        this.currentValue = currentValue;
        this.expectedValue = expectedValue;
        this.action = action;
    }

    // 创建规则阻断异常
    public static BusinessException ruleBlock(
            String ruleCode,
            String message,
            String currentValue,
            String expectedValue,
            String action) {
        return new BusinessException(ErrorCode.BUSINESS_ERROR.getCode(), message, ruleCode, currentValue, expectedValue, action);
    }

    // 创建访问拒绝异常
    public static BusinessException accessDenied(String ruleCode, String message, String currentValue, String expectedValue, String action) {
        return new BusinessException(ErrorCode.FORBIDDEN.getCode(), message, ruleCode, currentValue, expectedValue, action);
    }

    public int getCode() {
        return code;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    // 判断异常是否包含规则阻断详情。
    public boolean hasRuleDetail() {
        return ruleCode != null && !ruleCode.isBlank();
    }

    // 转换规则阻断详情。
    public Map<String, Object> toRuleDetail() {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("ruleCode", ruleCode);
        detail.put("reason", getMessage());
        detail.put("currentValue", currentValue);
        detail.put("expectedValue", expectedValue);
        detail.put("action", action);
        return detail;
    }
}
