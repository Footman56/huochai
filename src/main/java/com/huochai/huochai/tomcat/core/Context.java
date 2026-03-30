package com.huochai.huochai.tomcat.core;

import java.util.Set;

/**
 * Web应用上下文接口
 * 
 * @Description 代表一个Web应用，管理Servlet、Filter等
 * @DesignPattern 组合模式 - Host的子容器
 */
public interface Context extends Container {
    
    /**
     * 获取上下文路径
     * @return 上下文路径
     */
    String getContextPath();
    
    /**
     * 设置上下文路径
     * @param contextPath 上下文路径
     */
    void setContextPath(String contextPath);
    
    /**
     * 获取文档根目录
     * @return 文档根目录
     */
    String getDocBase();
    
    /**
     * 设置文档根目录
     * @param docBase 文档根目录
     */
    void setDocBase(String docBase);
    
    // ==================== Servlet管理 ====================
    
    /**
     * 添加Servlet
     * @param urlPattern URL模式
     * @param wrapper Servlet包装器
     */
    void addServlet(String urlPattern, Wrapper wrapper);
    
    /**
     * 移除Servlet
     * @param urlPattern URL模式
     */
    void removeServlet(String urlPattern);
    
    /**
     * 根据URL查找Servlet
     * @param uri URI
     * @return Servlet包装器
     */
    Wrapper findServlet(String uri);
    
    // ==================== Filter管理 ====================
    
    /**
     * 添加Filter
     * @param filterName Filter名称
     * @param filter Filter实例
     */
    void addFilter(String filterName, Object filter);
    
    /**
     * 获取Filter链
     * @return Filter链
     */
    Object getFilterChain();
    
    // ==================== 欢迎文件 ====================
    
    /**
     * 获取欢迎文件列表
     * @return 欢迎文件集合
     */
    Set<String> getWelcomeFiles();
    
    /**
     * 添加欢迎文件
     * @param welcomeFile 欢迎文件
     */
    void addWelcomeFile(String welcomeFile);
    
    // ==================== 初始化参数 ====================
    
    /**
     * 获取初始化参数
     * @param name 参数名称
     * @return 参数值
     */
    String getInitParameter(String name);
    
    /**
     * 设置初始化参数
     * @param name 参数名称
     * @param value 参数值
     */
    void setInitParameter(String name, String value);
    
    // ==================== Session ====================
    
    /**
     * 创建或获取Session
     * @param sessionId Session ID
     * @return Session
     */
    Object getSession(String sessionId);
    
    /**
     * 创建新Session
     * @return 新Session
     */
    Object createSession();
}
