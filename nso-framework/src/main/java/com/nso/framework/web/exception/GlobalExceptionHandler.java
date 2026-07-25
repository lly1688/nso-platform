package com.nso.framework.web.exception;

import com.nso.common.core.domain.AjaxResult;
import com.nso.common.exception.BusinessException;

import jakarta.validation.ConstraintViolationException;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public AjaxResult<Void> handleBusinessException(BusinessException exception) {
        return AjaxResult.error(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public AjaxResult<Void> handleValidationException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("参数校验失败");
        return AjaxResult.error(400, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public AjaxResult<Void> handleConstraintViolationException(ConstraintViolationException exception) {
        return AjaxResult.error(400, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public AjaxResult<Void> handleException(Exception exception) {
        return AjaxResult.error(500, exception.getMessage());
    }
}
