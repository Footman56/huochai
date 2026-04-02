package com.huochai.tomcat.filter;

import com.huochai.tomcat.servlet.ServletRequest;
import com.huochai.tomcat.servlet.ServletResponse;

/**
 * 过滤器接口
 *
 * @Description 定义过滤器的基本接口，用于请求/响应的预处理和后处理
 * @DesignPattern 责任链模式 - 多个Filter形成链式处理
 */
public interface Filter {

    /**
     * 初始化过滤器
     *
     * @Description 在Filter实例创建后调用一次，用于初始化资源
     * @Lifecycle 生命周期方法 - 只在初始化时调用一次
     * @param config Filter配置对象
     */
    void init(FilterConfig config);

    /**
     * 处理请求
     *
     * @Description 核心过滤逻辑，调用chain.doFilter()将请求传递给下一个Filter
     * @DesignPattern 责任链模式 - FilterChain负责调用下一个Filter
     * @param request 请求对象
     * @param response 响应对象
     * @param chain Filter链
     */
    void doFilter(ServletRequest request, ServletResponse response, FilterChain chain);

    /**
     * 销毁过滤器
     *
     * @Description 在Filter实例销毁前调用一次，用于释放资源
     * @Lifecycle 生命周期方法 - 只在销毁时调用一次
     */
    void destroy();
}
