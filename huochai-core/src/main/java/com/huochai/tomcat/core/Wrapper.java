package com.huochai.tomcat.core;

/**
 * Servlet包装器接口
 * 
 * @Description 包装单个Servlet，管理其生命周期
 * @DesignPattern 组合模式 - Context的子容器(叶子节点)
 */
public interface Wrapper extends Container {
    
    /**
     * 获取Servlet名称
     * @return Servlet名称
     */
    String getServletName();
    
    /**
     * 设置Servlet名称
     * @param servletName Servlet名称
     */
    void setServletName(String servletName);
    
    /**
     * 获取Servlet类名
     * @return Servlet类名
     */
    String getServletClass();
    
    /**
     * 设置Servlet类名
     * @param servletClass Servlet类名
     */
    void setServletClass(String servletClass);
    
    /**
     * 获取加载优先级
     * @return 优先级(-1表示不预加载)
     */
    int getLoadOnStartup();
    
    /**
     * 设置加载优先级
     * @param loadOnStartup 优先级
     */
    void setLoadOnStartup(int loadOnStartup);
    
    /**
     * 获取初始化参数
     * @param name 参数名称
     * @return 参数值
     */
    String getInitParameter(String name);
    
    /**
     * 添加初始化参数
     * @param name 参数名称
     * @param value 参数值
     */
    void addInitParameter(String name, String value);
    
    /**
     * 获取所有初始化参数
     * @return 初始化参数Map
     */
    java.util.Map<String, String> getInitParameters();
    
    /**
     * 获取Servlet实例
     * @return Servlet实例
     * @throws Exception 如果获取失败
     */
    Object getServlet() throws Exception;
    
    /**
     * 加载Servlet
     * @throws Exception 如果加载失败
     */
    void loadServlet() throws Exception;
    
    /**
     * 分配Servlet实例(线程安全)
     * @return Servlet实例
     */
    Object allocate();
    
    /**
     * 释放Servlet实例
     * @param servlet Servlet实例
     */
    void deallocate(Object servlet);
}
