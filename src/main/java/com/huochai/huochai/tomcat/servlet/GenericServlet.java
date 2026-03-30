package com.huochai.huochai.tomcat.servlet;

import com.huochai.huochai.tomcat.exception.ServletException;
import com.huochai.huochai.tomcat.servlet.http.HttpServletRequest;
import com.huochai.huochai.tomcat.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * 通用Servlet抽象类
 *
 * @Description 提供Servlet接口的抽象实现，屏蔽了与协议相关的方法
 * @DesignPattern 模板方法模式 - 提供基础实现，子类只需关注具体业务
 */
public abstract class GenericServlet implements Servlet {

    /** Servlet配置 */
    private ServletConfig config;

    /**
     * 初始化Servlet
     *
     * @Description 调用init(ServletConfig)方法，子类可重写此方法
     * @Lifecycle 这是生命周期方法之一
     * @param config Servlet配置对象
     * @throws ServletException 如果初始化失败
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        this.config = config;
        init();
    }

    /**
     * 无参初始化方法
     *
     * @Description 子类可重写此方法进行初始化，无需调用super.init()
     * @throws ServletException 如果初始化失败
     */
    protected void init() throws ServletException {
        // 子类可重写此方法
    }


    /**
     * 获取Servlet配置
     *
     * @return ServletConfig对象
     */
    @Override
    public ServletConfig getServletConfig() {
        return config;
    }

    /**
     * 获取Servlet上下文
     *
     * @Description 通过ServletConfig获取ServletContext
     * @return ServletContext对象
     */
    public ServletContext getServletContext() {
        return config.getServletContext();
    }

    /**
     * 获取初始化参数
     *
     * @Description 获取指定名称的初始化参数
     * @param name 参数名称
     * @return 参数值，如果不存在返回null
     */
    public String getInitParameter(String name) {
        return config.getInitParameter(name);
    }

    /**
     * 获取所有初始化参数名称
     *
     * @return 初始化参数名称枚举
     */
    public java.util.Enumeration<String> getInitParameterNames() {
        return config.getInitParameterNames();
    }

    /**
     * 获取Servlet信息
     *
     * @return 空字符串，子类可重写
     */
    @Override
    public String getServletInfo() {
        return "";
    }

    /**
     * 销毁Servlet
     *
     * @Description 默认实现为空，子类可重写此方法释放资源
     * @Lifecycle 这是生命周期方法之一
     */
    @Override
    public void destroy() {
        // 默认实现为空
    }

    /**
     * 记录日志信息
     *
     * @Description 简单的日志记录方法
     * @param message 日志消息
     */
    public void log(String message) {
        System.out.println("[GenericServlet] " + message);
    }

    /**
     * 记录异常信息
     *
     * @Description 记录异常和附加消息
     * @param message 附加消息
     * @param throwable 异常
     */
    public void log(String message, Throwable throwable) {
        System.err.println("[GenericServlet] " + message);
        throwable.printStackTrace();
    }


    /**
     * 处理请求 - 抽象方法
     *
     * @Description 具体的请求处理逻辑由子类实现
     * @Important 核心方法 - 子类必须实现
     * @param request 请求对象
     * @param response 响应对象
     * @throws ServletException 如果处理请求时发生错误
     * @throws IOException 如果发生IO错误
     */
    @Override
    public void service(ServletRequest request, ServletResponse response)
            throws ServletException, IOException {
        // 默认实现 - 子类应该覆盖
    }
}
