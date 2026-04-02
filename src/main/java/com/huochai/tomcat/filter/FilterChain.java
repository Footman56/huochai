package com.huochai.tomcat.filter;

import com.huochai.tomcat.servlet.ServletRequest;
import com.huochai.tomcat.servlet.ServletResponse;

/**
 * 过滤器链接口
 *
 * @Description 管理Filter的执行顺序，依次调用每个Filter
 * @DesignPattern 责任链模式 - 维护Filter列表，按顺序执行
 */
public interface FilterChain {

    /**
     * 执行下一个Filter
     *
     * @Description 调用链中的下一个Filter，最终调用目标Servlet
     * @DesignPattern 责任链模式 - 递归调用直到链尾
     * @param request 请求对象
     * @param response 响应对象
     */
    void doFilter(ServletRequest request, ServletResponse response);

    /**
     * 添加Filter到链
     *
     * @Description 将Filter添加到处理链中
     * @param filter 要添加的Filter
     */
    void addFilter(Filter filter);

    /**
     * 获取所有Filter
     * @return Filter列表
     */
    java.util.List<Filter> getFilters();

    /**
     * 获取当前Filter索引
     * @return 当前索引
     */
    int getPosition();

    /**
     * 设置当前Filter索引
     * @param position 索引
     */
    void setPosition(int position);
}
