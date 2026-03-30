package com.huochai.huochai.tomcat.filter;

import com.huochai.huochai.tomcat.servlet.ServletContext;

/**
 * 过滤器配置接口
 * 
 * @Description 提供Filter的初始化参数和配置信息
 */
public interface FilterConfig {
    
    /**
     * 获取Filter名称
     * @return Filter名称
     */
    String getFilterName();
    
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
