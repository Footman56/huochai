package com.huochai.tomcat.core;

/**
 * 引擎接口
 * 
 * @Description Tomcat引擎是最高层容器，代表整个Servlet引擎
 * @DesignPattern 组合模式 - 顶层容器
 */
public interface Engine extends Container {
    
    /**
     * 获取默认主机名
     * @return 默认主机名
     */
    String getDefaultHost();
    
    /**
     * 设置默认主机名
     * @param defaultHost 默认主机名
     */
    void setDefaultHost(String defaultHost);
    
    /**
     * 根据主机名查找主机
     * @param name 主机名
     * @return 主机
     */
    Host findHost(String name);
    
    /**
     * 获取服务
     * @return 服务
     */
    Service getService();
    
    /**
     * 设置服务
     * @param service 服务
     */
    void setService(Service service);
}
