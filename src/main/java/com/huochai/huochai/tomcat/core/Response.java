package com.huochai.huochai.tomcat.core;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * 响应接口
 * 
 * @Description 简化版响应接口，用于容器内部
 */
public interface Response {
    
    /**
     * 设置状态码
     * @param statusCode 状态码
     */
    void setStatus(int statusCode);
    
    /**
     * 获取状态码
     * @return 状态码
     */
    int getStatus();
    
    /**
     * 设置状态消息
     * @param message 状态消息
     */
    void setStatusMessage(String message);
    
    /**
     * 获取状态消息
     * @return 状态消息
     */
    String getStatusMessage();
    
    /**
     * 发送错误
     * @param statusCode 状态码
     * @param message 错误消息
     */
    void sendError(int statusCode, String message) throws IOException;
    
    /**
     * 发送重定向
     * @param location 重定向地址
     */
    void sendRedirect(String location) throws IOException;
    
    /**
     * 设置响应头
     * @param name 头名称
     * @param value 头值
     */
    void setHeader(String name, String value);
    
    /**
     * 添加响应头
     * @param name 头名称
     * @param value 头值
     */
    void addHeader(String name, String value);
    
    /**
     * 获取响应头
     * @param name 头名称
     * @return 头值
     */
    String getHeader(String name);
    
    /**
     * 设置Content-Type
     * @param contentType 内容类型
     */
    void setContentType(String contentType);
    
    /**
     * 获取Content-Type
     * @return 内容类型
     */
    String getContentType();
    
    /**
     * 设置Content-Length
     * @param length 内容长度
     */
    void setContentLength(int length);
    
    /**
     * 获取Content-Length
     * @return 内容长度
     */
    int getContentLength();
    
    /**
     * 设置字符编码
     * @param charset 字符编码
     */
    void setCharacterEncoding(String charset);
    
    /**
     * 获取字符编码
     * @return 字符编码
     */
    String getCharacterEncoding();
    
    /**
     * 获取输出流
     * @return 输出流
     */
    OutputStream getOutputStream();
    
    /**
     * 获取Writer
     * @return PrintWriter
     */
    PrintWriter getWriter() throws IOException;
    
    /**
     * 是否已提交
     * @return 是否已提交
     */
    boolean isCommitted();
    
    /**
     * 重置缓冲区
     */
    void resetBuffer();
    
    /**
     * 刷新缓冲区
     */
    void flushBuffer() throws IOException;
    
    /**
     * 获取响应体字节数组
     * @return 响应体
     */
    byte[] getBody();
    
    /**
     * 添加Cookie
     * @param name Cookie名称
     * @param value Cookie值
     */
    void addCookie(String name, String value);
    
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
     * 构建响应
     */
    void build() throws IOException;
}
