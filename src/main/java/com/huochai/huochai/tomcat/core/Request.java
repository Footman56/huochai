package com.huochai.huochai.tomcat.core;

import java.io.InputStream;

/**
 * 请求接口
 * 
 * @Description 简化版请求接口，用于容器内部
 */
public interface Request {
    
    /**
     * 获取URI
     * @return 请求URI
     */
    String getRequestURI();
    
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
     * 获取查询字符串
     * @return 查询字符串
     */
    String getQueryString();
    
    /**
     * 获取方法
     * @return HTTP方法
     */
    String getMethod();
    
    /**
     * 获取协议
     * @return 协议版本
     */
    String getProtocol();
    
    /**
     * 获取服务器名称
     * @return 服务器名称
     */
    String getServerName();
    
    /**
     * 获取服务器端口
     * @return 服务器端口
     */
    int getServerPort();
    
    /**
     * 获取远程地址
     * @return 远程地址
     */
    String getRemoteAddr();
    
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
    byte[] getBody();
    
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
     * 获取请求参数
     * @param name 参数名称
     * @return 参数值
     */
    String getParameter(String name);
    
    /**
     * 获取所有请求参数Map
     * @return 参数Map
     */
    java.util.Map<String, String[]> getParameterMap();
    
    /**
     * 获取属性
     * @param name 属性名称
     * @return 属性值
     */
    Object getAttribute(String name);
    
    /**
     * 设置属性
     * @param name 属性名称
     * @param value 属性值
     */
    void setAttribute(String name, Object value);
    
    /**
     * 获取输入流
     * @return 输入流
     */
    InputStream getInputStream();
    
    /**
     * 获取关联的容器
     * @return 容器
     */
    Container getContainer();
    
    /**
     * 设置关联的容器
     * @param container 容器
     */
    void setContainer(Container container);
    
    /**
     * 获取已解析的包装器
     * @return Wrapper
     */
    Wrapper getWrapper();
    
    /**
     * 设置已解析的包装器
     * @param wrapper Wrapper
     */
    void setWrapper(Wrapper wrapper);

    /**
     * 设置远程地址
     * @param remoteAddr 远程地址
     */
    void setRemoteAddr(String remoteAddr);

    /**
     * 设置服务器端口
     * @param serverPort 服务器端口
     */
    void setServerPort(int serverPort);

    /**
     * 设置服务器名称
     * @param serverName 服务器名称
     */
    void setServerName(String serverName);

    /**
     * 解析请求
     */
    void parse();
}
