package com.huochai.huochai.tomcat.core;

/**
 * 管道接口
 * 
 * @Description 管理一组Valve，按顺序处理请求
 * @DesignPattern 责任链模式 - Valve形成处理链
 */
public interface Pipeline {
    
    /**
     * 获取基础Valve
     * @return 基础Valve
     */
    Valve getBasic();
    
    /**
     * 设置基础Valve
     * @param valve 基础Valve
     */
    void setBasic(Valve valve);
    
    /**
     * 添加Valve
     * @param valve 要添加的Valve
     */
    void addValve(Valve valve);
    
    /**
     * 移除Valve
     * @param valve 要移除的Valve
     */
    void removeValve(Valve valve);
    
    /**
     * 获取所有Valve
     * @return Valve数组
     */
    Valve[] getValves();
    
    /**
     * 调用管道
     * 
     * @Description 依次执行所有Valve，最后执行基础Valve
     * @param request 请求对象
     * @param response 响应对象
     */
    void invoke(Request request, Response response);
    
    /**
     * 清空所有Valve
     */
    void removeAllValves();
    
    /**
     * 初始化管道
     */
    void init();
    
    /**
     * 启动管道
     */
    void start() throws Exception;
    
    /**
     * 停止管道
     */
    void stop() throws Exception;
    
    /**
     * 是否已启动
     */
    boolean isStarted();
}
