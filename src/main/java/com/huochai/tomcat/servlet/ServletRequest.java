package com.huochai.tomcat.servlet;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;

/**
 * Servlet请求接口
 * 
 * @Description 定义Servlet请求对象的基本接口
 */
public interface ServletRequest {
    
    /**
     * 获取请求的属性
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
     * 设置请求属性
     * @param name 属性名称
     * @param value 属性值
     */
    void setAttribute(String name, Object value);
    
    /**
     * 移除请求属性
     * @param name 属性名称
     */
    void removeAttribute(String name);
    
    /**
     * 获取Servlet上下文
     * @return ServletContext
     */
    ServletContext getServletContext();
    
    /**
     * 获取输入流
     * @return InputStream
     * @throws IOException 如果获取失败
     */
    InputStream getInputStream() throws IOException;
    
    /**
     * 获取读取器
     * @return BufferedReader
     * @throws IOException 如果获取失败
     */
    BufferedReader getReader() throws IOException;
    
    /**
     * 获取请求协议
     * @return 协议版本
     */
    String getProtocol();
    
    /**
     * 获取字符编码
     * @return 字符编码
     */
    String getCharacterEncoding();
    
    /**
     * 设置字符编码
     * @param charset 字符编码
     */
    void setCharacterEncoding(String charset);
    
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
     * 获取请求体
     * @return 请求体字节数组
     */
    byte[] getBody() throws IOException;
    
    /**
     * 获取服务器端口
     * @return 服务器端口
     */
    int getServerPort();
    
    /**
     * 获取服务器名称
     * @return 服务器名称
     */
    String getServerName();
    
    /**
     * 获取远程地址
     * @return 远程地址
     */
    String getRemoteAddr();
    
    /**
     * 获取远程主机
     * @return 远程主机
     */
    String getRemoteHost();
    
    /**
     * 是否是异步模式
     * @return 是否异步
     */
    boolean isAsyncStarted();
    
    /**
     * 是否支持异步
     * @return 是否支持
     */
    boolean isAsyncSupported();
}
