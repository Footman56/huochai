package com.huochai.huochai.tomcat.core;

/**
 * 虚拟主机接口
 * 
 * @Description 代表一个虚拟主机，拥有自己的域名和Web应用集合
 * @DesignPattern 组合模式 - Engine的子容器
 */
public interface Host extends Container {
    
    /**
     * 获取主机名(域名)
     * @return 主机名
     */
    String getHostName();
    
    /**
     * 设置主机名
     * @param hostName 主机名
     */
    void setHostName(String hostName);
    
    /**
     * 添加别名
     * @param alias 别名
     */
    void addAlias(String alias);
    
    /**
     * 移除别名
     * @param alias 别名
     */
    void removeAlias(String alias);
    
    /**
     * 根据别名查找主机
     * @param alias 别名
     * @return 是否存在
     */
    boolean findAlias(String alias);
    
    /**
     * 获取所有别名
     * @return 别名数组
     */
    String[] findAliases();
    
    /**
     * 根据上下文路径查找Web应用
     * @param contextPath 上下文路径
     * @return Web应用
     */
    Context findContext(String contextPath);
    
    /**
     * 获取应用根目录
     * @return 应用根目录
     */
    String getAppBase();
    
    /**
     * 设置应用根目录
     * @param appBase 应用根目录
     */
    void setAppBase(String appBase);
}
