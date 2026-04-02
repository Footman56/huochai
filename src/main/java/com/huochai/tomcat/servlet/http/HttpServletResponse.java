package com.huochai.tomcat.servlet.http;

import com.huochai.tomcat.servlet.ServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * HTTP响应接口
 * 
 * @Description 扩展ServletResponse，添加HTTP协议特定的方法
 */
public interface HttpServletResponse extends ServletResponse {
    
    /** 缓冲区大小 */
    int BUFFER_SIZE = 8192;
    
    /**
     * 设置状态码
     * @param statusCode 状态码
     */
    void setStatus(int statusCode);
    
    /**
     * 设置状态码和消息
     * @param statusCode 状态码
     * @param message 状态消息
     */
    void setStatus(int statusCode, String message);
    
    /**
     * 获取状态码
     * @return 状态码
     */
    int getStatus();
    
    /**
     * 发送错误
     * @param statusCode 状态码
     * @throws IOException 如果发送失败
     */
    void sendError(int statusCode) throws IOException;
    
    /**
     * 发送错误
     * @param statusCode 状态码
     * @param message 错误消息
     * @throws IOException 如果发送失败
     */
    void sendError(int statusCode, String message) throws IOException;
    
    /**
     * 发送重定向
     * @param location 重定向URL
     * @throws IOException 如果发送失败
     */
    void sendRedirect(String location) throws IOException;
    
    /**
     * 设置响应头
     * @param name 头名称
     * @param value 头值
     */
    void setHeader(String name, String value);
    
    /**
     * 设置响应头（整数值）
     * @param name 头名称
     * @param value 整数值
     */
    void setIntHeader(String name, int value);
    
    /**
     * 设置响应头（日期值）
     * @param name 头名称
     * @param date 日期值（毫秒）
     */
    void setDateHeader(String name, long date);
    
    /**
     * 添加响应头
     * @param name 头名称
     * @param value 头值
     */
    void addHeader(String name, String value);
    
    /**
     * 添加响应头（整数值）
     * @param name 头名称
     * @param value 整数值
     */
    void addIntHeader(String name, int value);
    
    /**
     * 添加响应头（日期值）
     * @param name 头名称
     * @param date 日期值（毫秒）
     */
    void addDateHeader(String name, long date);
    
    /**
     * 获取指定响应头的值
     * @param name 头名称
     * @return 头值
     */
    String getHeader(String name);
    
    /**
     * 获取所有响应头名称
     * @return 头名称集合
     */
    java.util.Collection<String> getHeaderNames();
    
    /**
     * 获取指定响应头的所有值
     * @param name 头名称
     * @return 头值集合
     */
    java.util.Collection<String> getHeaders(String name);
    
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
     * 添加Cookie
     * @param cookie Cookie对象
     */
    void addCookie(Cookie cookie);
    
    /**
     * 检查是否已提交响应
     * @return 是否已提交
     */
    boolean isCommitted();
    
    /**
     * 重置缓冲区
     */
    void resetBuffer();
    
    /**
     * 设置缓冲区大小
     * @param size 缓冲区大小
     */
    void setBufferSize(int size);
    
    /**
     * 获取缓冲区大小
     * @return 缓冲区大小
     */
    int getBufferSize();
    
    /**
     * 刷新缓冲区
     * @throws IOException 如果刷新失败
     */
    void flushBuffer() throws IOException;
    
    /**
     * 获取输出流
     * @return ServletOutputStream
     */
    com.huochai.tomcat.servlet.ServletOutputStream getOutputStream() throws IOException;

    /**
     * 获取PrintWriter
     * @return PrintWriter
     * @throws IOException 如果获取失败
     */
    PrintWriter getWriter() throws IOException;
}
