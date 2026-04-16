package com.huochai.common.enums;

import lombok.Getter;

/**
 * Token类型枚举
 *
 * @author huochai
 */
@Getter
public enum TokenType {

    ACCESS("ACCESS", "访问Token"),
    REFRESH("REFRESH", "刷新Token");

    private final String code;
    private final String desc;

    TokenType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}