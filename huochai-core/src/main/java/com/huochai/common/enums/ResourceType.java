package com.huochai.common.enums;

import lombok.Getter;

/**
 * 资源类型枚举
 *
 * @author huochai
 */
@Getter
public enum ResourceType {

    MENU("MENU", "菜单"),
    BUTTON("BUTTON", "按钮"),
    API("API", "接口");

    private final String code;
    private final String desc;

    ResourceType(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static ResourceType fromCode(String code) {
        for (ResourceType type : values()) {
            if (type.getCode().equalsIgnoreCase(code)) {
                return type;
            }
        }
        return API;
    }
}