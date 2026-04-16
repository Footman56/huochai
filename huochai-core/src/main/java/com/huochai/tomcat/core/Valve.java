package com.huochai.tomcat.core;

/**
 * 阀门接口
 * 
 * @Description 处理请求的组件，是Pipeline中的处理节点
 * @DesignPattern 责任链模式 - 责任链中的处理节点
 */
public interface Valve {
    
    /**
     * 获取阀门名称
     * @return 名称
     */
    String getName();
    
    /**
     * 设置阀门名称
     * @param name 名称
     */
    void setName(String name);
    
    /**
     * 获取下一个阀门
     * @return 下一个阀门
     */
    Valve getNext();
    
    /**
     * 设置下一个阀门
     * @param next 下一个阀门
     */
    void setNext(Valve next);
    
    /**
     * 调用阀门
     * 
     * @Description 处理请求并将控制权传递给下一个阀门
     * @param request 请求对象
     * @param response 响应对象
     * @param valveContext 阀门上下文
     */
    void invoke(Request request, Response response, ValveContext valveContext);
    
    /**
     * 阀门上下文接口
     * 
     * @Description 用于在阀门之间传递控制权
     */
    interface ValveContext {
        /**
         * 调用下一个阀门
         * @param request 请求
         * @param response 响应
         */
        void invokeNext(Request request, Response response);
    }
    
    /**
     * 初始化阀门
     */
    void init();
    
    /**
     * 启动阀门
     */
    void start();
    
    /**
     * 停止阀门
     */
    void stop();
    
    /**
     * 销毁阀门
     */
    void destroy();
    
    /**
     * 是否已启动
     */
    boolean isStarted();
}
