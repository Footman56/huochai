package com.huochai.common.enums;

import lombok.Getter;

/**
 * 登录结果枚举
 *
 * @author huochai
 */
@Getter
public enum LoginResult {

    SUCCESS(1, "登录成功"),
    FAIL(0, "登录失败");

    private final Integer code;
    private final String desc;

    LoginResult(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}