package com.huochai.common.exception;

import com.huochai.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 *
 * @author huochai
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 认证异常处理
     */
    @ExceptionHandler(AuthException.class)
    public Result<Void> handleAuthException(AuthException e, HttpServletRequest request) {
        log.warn("认证异常: {} - {}", e.getCode(), e.getMessage());
        Result<Void> result = Result.error(e.getCode(), e.getMessage());
        result.setTraceId(MDC.get("traceId"));
        return result;
    }

    /**
     * Token异常处理
     */
    @ExceptionHandler(TokenException.class)
    public Result<Void> handleTokenException(TokenException e, HttpServletRequest request) {
        log.warn("Token异常: {}", e.getMessage());
        Result<Void> result = Result.error(e.getCode(), e.getMessage());
        result.setTraceId(MDC.get("traceId"));
        return result;
    }

    /**
     * 权限拒绝异常处理
     */
    @ExceptionHandler(PermissionDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handlePermissionDeniedException(PermissionDeniedException e, HttpServletRequest request) {
        log.warn("权限拒绝: {}", e.getMessage());
        Result<Void> result = Result.error(e.getCode(), e.getMessage());
        result.setTraceId(MDC.get("traceId"));
        return result;
    }

    /**
     * Spring Security 访问拒绝异常
     */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result<Void> handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request) {
        log.warn("访问拒绝: {}", e.getMessage());
        Result<Void> result = Result.error(AuthErrorCode.PERMISSION_DENIED.getCode(), "权限不足");
        result.setTraceId(MDC.get("traceId"));
        return result;
    }

    /**
     * Spring Security 认证异常
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<Void> handleAuthenticationException(AuthenticationException e, HttpServletRequest request) {
        log.warn("认证失败: {}", e.getMessage());
        String message = "认证失败";
        if (e instanceof BadCredentialsException) {
            message = "用户名或密码错误";
        }
        Result<Void> result = Result.error(AuthErrorCode.LOGIN_FAILED.getCode(), message);
        result.setTraceId(MDC.get("traceId"));
        return result;
    }

    /**
     * 参数校验异常 - @RequestBody
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", message);
        Result<Void> result = Result.error(AuthErrorCode.BAD_REQUEST.getCode(), message);
        result.setTraceId(MDC.get("traceId"));
        return result;
    }

    /**
     * 参数校验异常 - @RequestParam
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        String message = e.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining(", "));
        log.warn("参数校验失败: {}", message);
        Result<Void> result = Result.error(AuthErrorCode.BAD_REQUEST.getCode(), message);
        result.setTraceId(MDC.get("traceId"));
        return result;
    }

    /**
     * 参数非法异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("参数非法: {}", e.getMessage());
        Result<Void> result = Result.error(AuthErrorCode.BAD_REQUEST.getCode(), e.getMessage());
        result.setTraceId(MDC.get("traceId"));
        return result;
    }

    /**
     * 其他未捕获异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常: ", e);
        Result<Void> result = Result.error(AuthErrorCode.INTERNAL_ERROR.getCode(), "系统繁忙，请稍后重试");
        result.setTraceId(MDC.get("traceId"));
        return result;
    }
}