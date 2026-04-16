package com.huochai.tomcat.core;

/**
 * 服务接口
 *
 * @Description 一个Service可以包含一个Engine和多个Connector
 */
public interface Service extends Lifecycle {
    /**
     * 获取服务名称
     * @return 服务名称
     */
    String getName();

    /**
     * 设置服务名称
     * @param name 服务名称
     */
    void setName(String name);

    /**
     * 获取引擎
     * @return 引擎
     */
    Engine getEngine();

    /**
     * 设置引擎
     * @param engine 引擎
     */
    void setEngine(Engine engine);
}
