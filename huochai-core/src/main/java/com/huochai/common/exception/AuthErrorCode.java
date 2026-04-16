package com.huochai.common.exception;

import lombok.Getter;

/**
 * 认证错误码枚举
 *
 * @author huochai
 */
@Getter
public enum AuthErrorCode {

    // ========== 通用错误 1000-1999 ==========
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "方法不允许"),
    INTERNAL_ERROR(500, "服务器内部错误"),

    // ========== 认证相关 2000-2999 ==========
    USER_NOT_FOUND(2001, "用户不存在"),
    PASSWORD_ERROR(2002, "密码错误"),
    ACCOUNT_DISABLED(2003, "账号已被禁用"),
    ACCOUNT_LOCKED(2004, "账号已被锁定"),
    LOGIN_FAILED(2005, "登录失败"),
    LOGOUT_FAILED(2006, "登出失败"),
    
    // ========== Token相关 2100-2199 ==========
    TOKEN_INVALID(2100, "Token无效"),
    TOKEN_EXPIRED(2101, "Token已过期"),
    TOKEN_BLACKLISTED(2102, "Token已被禁用"),
    TOKEN_MISSING(2103, "Token缺失"),
    TOKEN_PARSE_ERROR(2104, "Token解析失败"),
    REFRESH_TOKEN_INVALID(2105, "刷新Token无效"),
    REFRESH_TOKEN_EXPIRED(2106, "刷新Token已过期"),

    // ========== 权限相关 3000-3999 ==========
    PERMISSION_DENIED(3001, "权限不足"),
    ROLE_NOT_FOUND(3002, "角色不存在"),
    PERMISSION_NOT_FOUND(3003, "权限不存在"),
    ROLE_ALREADY_EXISTS(3004, "角色已存在"),
    PERMISSION_ALREADY_EXISTS(3005, "权限已存在"),

    // ========== 验证码相关 4000-4999 ==========
    CAPTCHA_INVALID(4001, "验证码无效"),
    CAPTCHA_EXPIRED(4002, "验证码已过期"),
    CAPTCHA_ERROR(4003, "验证码错误"),

    // ========== 限流相关 5000-5999 ==========
    RATE_LIMIT_EXCEEDED(5001, "请求过于频繁，请稍后重试"),
    LOGIN_RATE_LIMIT(5002, "登录尝试次数过多，请稍后重试"),

    // ========== 会话相关 6000-6999 ==========
    SESSION_EXPIRED(6001, "会话已过期"),
    SESSION_KICKED(6002, "账号在其他设备登录"),
    DEVICE_NOT_ALLOWED(6003, "该设备不允许登录");

    private final Integer code;
    private final String message;

    AuthErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}