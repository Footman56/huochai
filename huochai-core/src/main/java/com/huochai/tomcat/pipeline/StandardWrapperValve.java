package com.huochai.tomcat.pipeline;

import com.huochai.tomcat.core.*;

/**
 * 基础阀门 - 用于调用容器
 *
 * @Description Wrapper层的基础阀门，调用Servlet
 */
public class StandardWrapperValve extends StandardValve {

    private final com.huochai.tomcat.core.Wrapper wrapper;

    public StandardWrapperValve(com.huochai.tomcat.core.Wrapper wrapper) {
        this.wrapper = wrapper;
        setName("wrapper-valve");
    }

    @Override
    protected void invokeInternal(Request request, Response response) throws Exception {
        // 获取Servlet并调用
        Object servlet = wrapper.allocate();
        System.out.println("[Debug] Allocated servlet: " + servlet);

        if (servlet instanceof com.huochai.tomcat.servlet.Servlet) {
            com.huochai.tomcat.servlet.Servlet s = (com.huochai.tomcat.servlet.Servlet) servlet;

            System.out.println("[Debug] Calling servlet: " + s.getClass().getName());

            // 创建HTTP请求/响应适配器
            s.service(
                new com.huochai.tomcat.servlet.http.HttpServletRequestAdapter(request),
                new com.huochai.tomcat.servlet.http.HttpServletResponseAdapter(response)
            );

            System.out.println("[Debug] Servlet service completed");
        } else {
            System.out.println("[Debug] Servlet is not instance: " + (servlet != null ? servlet.getClass().getName() : "null"));
        }

        // 释放Servlet
        wrapper.deallocate(servlet);
    }

    @Override
    public String getStateName() {
        return isStarted() ? State.STARTED.name() : State.STOPPED.name();
    }

    @Override
    public State getState() {
        return isStarted() ? State.STARTED : State.STOPPED;
    }
}
