package com.nso.common.exception;

import com.nso.common.exception.enums.ErrorCode;

import java.util.LinkedHashMap;
import java.util.Map;

public class BusinessException extends RuntimeException {

    private final int code;
    private final String ruleCode;
    private final String currentValue;
    private final String expectedValue;
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

    private BusinessException(int code, String message, String ruleCode, String currentValue, String expectedValue, String action) {
        super(message);
        this.code = code;
        this.ruleCode = ruleCode;
        this.currentValue = currentValue;
        this.expectedValue = expectedValue;
        this.action = action;
    }

    public static BusinessException ruleBlock(
            String ruleCode,
            String message,
            String currentValue,
            String expectedValue,
            String action) {
        return new BusinessException(ErrorCode.BUSINESS_ERROR.getCode(), message, ruleCode, currentValue, expectedValue, action);
    }

    public static BusinessException accessDenied(String ruleCode, String message, String currentValue, String expectedValue, String action) {
        return new BusinessException(ErrorCode.FORBIDDEN.getCode(), message, ruleCode, currentValue, expectedValue, action);
    }

    public int getCode() {
        return code;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public boolean hasRuleDetail() {
        return ruleCode != null && !ruleCode.isBlank();
    }

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
