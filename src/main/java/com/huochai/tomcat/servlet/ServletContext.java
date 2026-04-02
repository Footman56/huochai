package com.huochai.tomcat.servlet;

import java.io.InputStream;
import java.net.URL;

/**
 * Servlet上下文接口
 * 
 * @Description 提供Web应用的全局信息和资源访问
 * @DesignPattern 单例模式 - 每个Web应用只有一个ServletContext
 */
public interface ServletContext {
    
    /**
     * 获取上下文路径
     * @return 上下文路径
     */
    String getContextPath();
    
    /**
     * 获取Servlet名称
     * @return Servlet名称
     */
    String getServletContextName();
    
    /**
     * 获取服务器信息
     * @return 服务器信息
     */
    String getServerInfo();
    
    /**
     * 获取初始化参数
     * @param name 参数名称
     * @return 参数值
     */
    String getInitParameter(String name);
    
    /**
     * 获取所有初始化参数名称
     * @return 参数名称枚举
     */
    java.util.Enumeration<String> getInitParameterNames();
    
    /**
     * 设置属性
     * @param name 属性名称
     * @param value 属性值
     */
    void setAttribute(String name, Object value);
    
    /**
     * 获取属性
     * @param name 属性名称
     * @return 属性值
     */
    Object getAttribute(String name);
    
    /**
     * 获取所有属性名称
     * @return 属性名称枚举
     */
    java.util.Enumeration<String> getAttributeNames();
    
    /**
     * 移除属性
     * @param name 属性名称
     */
    void removeAttribute(String name);
    
    /**
     * 获取真实路径
     * @param path 虚拟路径
     * @return 真实路径
     */
    String getRealPath(String path);
    
    /**
     * 获取资源输入流
     * @param path 资源路径
     * @return 输入流
     */
    InputStream getResourceAsStream(String path);
    
    /**
     * 获取资源URL
     * @param path 资源路径
     * @return URL
     */
    URL getResource(String path) throws java.net.MalformedURLException;
    
    /**
     * 获取Mime类型
     * @param file 文件名
     * @return Mime类型
     */
    String getMimeType(String file);
    
    /**
     * 记录日志
     * @param message 日志消息
     */
    void log(String message);
    
    /**
     * 记录日志
     * @param message 日志消息
     * @param throwable 异常
     */
    void log(String message, Throwable throwable);
    
    /**
     * 获取主要版本
     * @return 主要版本
     */
    int getMajorVersion();
    
    /**
     * 获取次要版本
     * @return 次要版本
     */
    int getMinorVersion();
    
    /**
     * 获取指定上下文的ServletContext
     * @param uripath 上下文路径
     * @return ServletContext
     */
    ServletContext getContext(String uripath);
}
