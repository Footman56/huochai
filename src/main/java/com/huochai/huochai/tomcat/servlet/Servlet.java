package com.huochai.huochai.tomcat.servlet;

import com.huochai.huochai.tomcat.exception.ServletException;

import java.io.IOException;

/**
 * Servlet接口
 *
 * @Description 定义Servlet的生命周期方法，所有Servlet必须实现此接口
 * @DesignPattern 模板方法模式的基接口
 */
public interface Servlet {

    /**
     * 初始化Servlet
     *
     * @Description 在Servlet实例创建后调用一次，用于初始化资源
     * @Important 生命周期方法 - 只在初始化时调用一次
     * @param config Servlet配置对象
     * @throws ServletException 如果初始化失败
     */
    void init(ServletConfig config) throws ServletException;

    /**
     * 获取Servlet配置
     *
     * @Description 返回ServletConfig对象，包含Servlet的初始化参数
     * @return ServletConfig对象
     */
    ServletConfig getServletConfig();

    /**
     * 处理请求
     *
     * @Description 每次请求都会调用此方法，是Servlet的核心处理逻辑
     * @Important 生命周期方法 - 每次请求都会调用
     * @param request 请求对象
     * @param response 响应对象
     * @throws ServletException 如果处理请求时发生错误
     * @throws IOException 如果发生IO错误
     */
    void service(ServletRequest request, ServletResponse response)
            throws ServletException, IOException;

    /**
     * 获取Servlet信息
     *
     * @Description 返回Servlet的名称、版本等信息
     * @return Servlet描述字符串
     */
    String getServletInfo();

    /**
     * 销毁Servlet
     *
     * @Description 在Servlet实例销毁前调用一次，用于释放资源
     * @Important 生命周期方法 - 只在销毁时调用一次
     */
    void destroy();
}
