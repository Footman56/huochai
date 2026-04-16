package com.huochai.tomcat.servlet.http;

import com.huochai.tomcat.servlet.ServletRequest;

import java.util.Map;

/**
 * HTTP请求接口
 * 
 * @Description 扩展ServletRequest，添加HTTP协议特定的方法
 */
public interface HttpServletRequest extends ServletRequest {
    
    /**
     * 获取请求方法
     * @return HTTP方法 (GET, POST, etc.)
     */
    String getMethod();
    
    /**
     * 获取请求URI
     * @return 请求URI
     */
    String getRequestURI();
    
    /**
     * 获取查询字符串
     * @return 查询字符串，不包含?
     */
    String getQueryString();
    
    /**
     * 获取协议版本
     * @return 协议版本 (HTTP/1.1)
     */
    String getProtocol();
    
    /**
     * 获取请求URL
     * @return 请求URL
     */
    StringBuffer getRequestURL();
    
    /**
     * 获取上下文路径
     * @return 上下文路径
     */
    String getContextPath();
    
    /**
     * 获取Servlet路径
     * @return Servlet路径
     */
    String getServletPath();
    
    /**
     * 获取路径信息
     * @return 路径信息
     */
    String getPathInfo();
    
    /**
     * 获取指定请求头的值
     * @param name 请求头名称
     * @return 请求头的值
     */
    String getHeader(String name);
    
    /**
     * 获取所有请求头名称
     * @return 请求头名称枚举
     */
    java.util.Enumeration<String> getHeaderNames();
    
    /**
     * 获取指定请求头的整数值
     * @param name 请求头名称
     * @return 整数值
     */
    int getIntHeader(String name);
    
    /**
     * 获取指定请求头的日期值
     * @param name 请求头名称
     * @return 日期值（毫秒）
     */
    long getDateHeader(String name);
    
    /**
     * 获取Cookie数组
     * @return Cookie数组
     */
    Cookie[] getCookies();
    
    /**
     * 获取请求参数Map
     * @return 参数Map
     */
    Map<String, String[]> getParameterMap();
    
    /**
     * 获取指定参数的值
     * @param name 参数名称
     * @return 参数值
     */
    String getParameter(String name);
    
    /**
     * 获取所有参数名称
     * @return 参数名称枚举
     */
    java.util.Enumeration<String> getParameterNames();
    
    /**
     * 获取指定参数的所有值
     * @param name 参数名称
     * @return 参数值数组
     */
    String[] getParameterValues(String name);
    
    /**
     * 获取请求体
     * @return 请求体字节数组
     */
    byte[] getBody();
    
    /**
     * 获取字符编码
     * @return 字符编码
     */
    String getCharacterEncoding();
    
    /**
     * 获取内容长度
     * @return 内容长度
     */
    int getContentLength();
    
    /**
     * 获取内容类型
     * @return 内容类型
     */
    String getContentType();
    
    /**
     * 获取远程地址
     * @return 远程地址
     */
    String getRemoteAddr();
    
    /**
     * 获取远程主机名
     * @return 远程主机名
     */
    String getRemoteHost();
}
