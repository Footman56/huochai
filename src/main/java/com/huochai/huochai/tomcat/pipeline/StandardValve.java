package com.huochai.huochai.tomcat.pipeline;

import com.huochai.huochai.tomcat.core.*;

/**
 * 标准阀门实现基类
 *
 * @Description 提供Valve接口的基础实现
 * @DesignPattern 责任链模式 - 处理节点
 */
public abstract class StandardValve implements Valve, Lifecycle {

    // ==================== 基本属性 ====================

    private String name;
    private Valve next;
    private volatile boolean started = false;
    private boolean isBasic = false;

    /**
     * 标记为基础阀门
     */
    public void setBasic(boolean isBasic) {
        this.isBasic = isBasic;
    }

    public boolean isBasic() {
        return isBasic;
    }

    // ==================== Valve 接口实现 ====================

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Valve getNext() {
        return next;
    }

    @Override
    public void setNext(Valve next) {
        this.next = next;
    }

    @Override
    public void invoke(Request request, Response response, ValveContext valveContext) {
        // 模板方法 - 子类实现具体处理逻辑
        try {
            // 调用子类的核心处理方法
            invokeInternal(request, response);
        } catch (Exception e) {
            // 处理异常
            handleException(request, response, e);
            return;
        }

        // 调用下一个阀门 - 但基础阀门不应该调用
        if (valveContext != null && !isBasic) {
            valveContext.invokeNext(request, response);
        }
    }

    /**
     * 内部处理方法 - 子类实现
     *
     * @param request 请求
     * @param response 响应
     * @throws Exception 处理异常
     */
    protected abstract void invokeInternal(Request request, Response response) throws Exception;

    /**
     * 处理异常 - 可重写
     *
     * @param request 请求
     * @param response 响应
     * @param e 异常
     */
    protected void handleException(Request request, Response response, Exception e) {
        e.printStackTrace();
        try {
            response.setStatus(500);
            response.getWriter().write("Error: " + e.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // ==================== Lifecycle 接口实现 ====================

    @Override
    public void init() {
        // 默认空实现
    }

    @Override
    public void start() {
        started = true;
    }

    @Override
    public void stop() {
        started = false;
    }

    @Override
    public void destroy() {
        // 默认空实现
    }

    @Override
    public boolean isStarted() {
        return started;
    }

    @Override
    public boolean isStopped() {
        return !started;
    }

    @Override
    public String getStateName() {
        return started ? State.STARTED.name() : State.STOPPED.name();
    }

    @Override
    public State getState() {
        return started ? State.STARTED : State.STOPPED;
    }

    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        // Default empty implementation
    }

    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        // Default empty implementation
    }

    @Override
    public LifecycleListener[] findLifecycleListeners() {
        return new LifecycleListener[0];
    }
}
