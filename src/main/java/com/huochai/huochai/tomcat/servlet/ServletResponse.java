package com.huochai.huochai.tomcat.servlet;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * Servlet响应接口
 * 
 * @Description 定义Servlet响应对象的基本接口
 */
public interface ServletResponse {
    
    /**
     * 获取Servlet上下文
     * @return ServletContext
     */
    ServletContext getServletContext();
    
    /**
     * 设置响应缓冲区大小
     * @param size 缓冲区大小
     */
    void setBufferSize(int size);
    
    /**
     * 获取响应缓冲区大小
     * @return 缓冲区大小
     */
    int getBufferSize();
    
    /**
     * 获取输出流
     * @return OutputStream
     * @throws IOException 如果获取失败
     */
    OutputStream getOutputStream() throws IOException;
    
    /**
     * 获取PrintWriter
     * @return PrintWriter
     * @throws IOException 如果获取失败
     */
    PrintWriter getWriter() throws IOException;
    
    /**
     * 设置Content-Type
     * @param type 内容类型
     */
    void setContentType(String type);
    
    /**
     * 获取Content-Type
     * @return 内容类型
     */
    String getContentType();
    
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
     * 设置内容长度
     * @param length 内容长度
     */
    void setContentLength(int length);
    
    /**
     * 刷新缓冲区
     * @throws IOException 如果刷新失败
     */
    void flushBuffer() throws IOException;
    
    /**
     * 重置缓冲区
     */
    void reset();
    
    /**
     * 标记是否已提交
     * @return 是否已提交
     */
    boolean isCommitted();
    
    /**
     * 重置缓冲区（包含头）
     */
    void resetBuffer();
}
