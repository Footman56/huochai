package com.huochai.common.enums;

import lombok.Getter;

/**
 * 客户端类型枚举
 *
 * @author huochai
 */
@Getter
public enum ClientType {

    WEB("WEB", "Web端"),
    APP("APP", "移动App"),
    MINI_PROGRAM("MINI_PROGRAM", "小程序"),
    ADMIN("ADMIN", "管理后台");

    private final String code;
    private final String desc;

    ClientType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ClientType fromCode(String code) {
        for (ClientType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return WEB; // 默认返回Web端
    }
}