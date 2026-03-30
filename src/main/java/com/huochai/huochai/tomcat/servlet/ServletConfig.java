package com.huochai.huochai.tomcat.servlet;

/**
 * Servlet配置接口
 * 
 * @Description 提供Servlet的初始化参数和配置信息
 */
public interface ServletConfig {
    
    /**
     * 获取Servlet名称
     * @return Servlet名称
     */
    String getServletName();
    
    /**
     * 获取Servlet上下文
     * @return ServletContext
     */
    ServletContext getServletContext();
    
    /**
     * 获取指定初始化参数的值
     * @param name 参数名称
     * @return 参数值
     */
    String getInitParameter(String name);
    
    /**
     * 获取所有初始化参数名称
     * @return 参数名称枚举
     */
    java.util.Enumeration<String> getInitParameterNames();
}
