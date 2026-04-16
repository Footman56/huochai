package com.huochai.common.log;

/**
 * 操作类型枚举
 *
 * @author huochai
 */
public enum OperationType {

    /**
     * 其他
     */
    OTHER("其他"),

    /**
     * 新增
     */
    INSERT("新增"),

    /**
     * 修改
     */
    UPDATE("修改"),

    /**
     * 删除
     */
    DELETE("删除"),

    /**
     * 查询
     */
    SELECT("查询"),

    /**
     * 登录
     */
    LOGIN("登录"),

    /**
     * 登出
     */
    LOGOUT("登出"),

    /**
     * 导出
     */
    EXPORT("导出"),

    /**
     * 导入
     */
    IMPORT("导入");

    private final String desc;

    OperationType(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}