package com.huochai.huochai.tomcat.servlet;

import java.util.Enumeration;

/**
 * HttpSession接口
 *
 * @Description 提供Session管理的基础接口
 */
public interface HttpSession {

    /**
     * 获取Session ID
     * @return Session ID
     */
    String getId();

    /**
     * 获取Session创建时间
     * @return 创建时间（毫秒）
     */
    long getCreationTime();

    /**
     * 获取最后访问时间
     * @return 最后访问时间（毫秒）
     */
    long getLastAccessedTime();

    /**
     * 设置最大不活跃间隔
     * @param interval 间隔（秒）
     */
    void setMaxInactiveInterval(int interval);

    /**
     * 获取最大不活跃间隔
     * @return 间隔（秒）
     */
    int getMaxInactiveInterval();

    /**
     * 获取Session上下文
     * @return Session上下文
     */
    Object getSessionContext();

    /**
     * 获取属性
     * @param name 属性名
     * @return 属性值
     */
    Object getAttribute(String name);

    /**
     * 获取属性（已废弃）
     * @param name 属性名
     * @return 属性值
     */
    Object getValue(String name);

    /**
     * 获取所有属性名
     * @return 属性名枚举
     */
    Enumeration<String> getAttributeNames();

    /**
     * 获取所有属性名（已废弃）
     * @return 属性名数组
     */
    String[] getValueNames();

    /**
     * 设置属性
     * @param name 属性名
     * @param value 属性值
     */
    void setAttribute(String name, Object value);

    /**
     * 设置属性（已废弃）
     * @param name 属性名
     * @param value 属性值
     */
    void putValue(String name, Object value);

    /**
     * 移除属性
     * @param name 属性名
     */
    void removeAttribute(String name);

    /**
     * 移除属性（已废弃）
     * @param name 属性名
     */
    void removeValue(String name);

    /**
     * 使Session无效
     */
    void invalidate();

    /**
     * 判断是否为新Session
     * @return 是否为新Session
     */
    boolean isNew();
}
