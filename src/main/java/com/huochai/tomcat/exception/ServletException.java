package com.huochai.tomcat.exception;

/**
 * Servlet异常
 * 
 * @Description 定义Servlet相关的异常类型
 * @DesignPattern 异常封装模式
 */
public class ServletException extends Exception {

    private static final long serialVersionUID = 1L;
    
    /** 错误状态码 */
    private int statusCode;
    
    /**
     * 默认构造方法
     */
    public ServletException() {
        super();
        this.statusCode = 500;
    }
    
    /**
     * 带消息的构造方法
     * 
     * @param message 异常消息
     */
    public ServletException(String message) {
        super(message);
        this.statusCode = 500;
    }
    
    /**
     * 带状态码的构造方法
     * 
     * @param statusCode HTTP状态码
     * @param message 异常消息
     */
    public ServletException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }
    
    /**
     * 带原因的构造方法
     * 
     * @param message 异常消息
     * @param cause 原始异常
     */
    public ServletException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 500;
    }
    
    /**
     * 完全参数的构造方法
     * 
     * @param statusCode HTTP状态码
     * @param message 异常消息
     * @param cause 原始异常
     */
    public ServletException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }
    
    /**
     * 获取错误状态码
     * 
     * @return HTTP状态码
     */
    public int getStatusCode() {
        return statusCode;
    }
    
    /**
     * 设置错误状态码
     * 
     * @param statusCode HTTP状态码
     */
    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }
}
