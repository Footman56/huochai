package com.huochai.tomcat.core;

/**
 * 生命周期接口
 * 
 * @Description 定义容器生命周期方法
 * @DesignPattern 模板方法模式 - 定义生命周期骨架
 */
public interface Lifecycle {
    
    /**
     * 生命周期状态枚举
     */
    enum State {
        /** 新创建 */
        NEW,
        /** 已初始化 */
        INITIALIZED,
        /** 启动中 */
        STARTING,
        /** 已启动 */
        STARTED,
        /** 停止中 */
        STOPPING,
        /** 已停止 */
        STOPPED,
        /** 销毁中 */
        DESTROYING,
        /** 已销毁 */
        DESTROYED,
        /** 失败 */
        FAILED
    }
    
    // ==================== 生命周期事件类型 ====================
    
    String BEFORE_INIT_EVENT = "before_init";
    String AFTER_INIT_EVENT = "after_init";
    String BEFORE_START_EVENT = "before_start";
    String AFTER_START_EVENT = "after_start";
    String BEFORE_STOP_EVENT = "before_stop";
    String AFTER_STOP_EVENT = "after_stop";
    String BEFORE_DESTROY_EVENT = "before_destroy";
    String AFTER_DESTROY_EVENT = "after_destroy";
    
    // ==================== 生命周期方法 ====================
    
    /**
     * 初始化
     * 
     * @Description 初始化组件
     * @throws LifecycleException 如果初始化失败
     */
    void init() throws LifecycleException;
    
    /**
     * 启动
     * 
     * @Description 启动组件
     * @throws LifecycleException 如果启动失败
     */
    void start() throws LifecycleException;
    
    /**
     * 停止
     * 
     * @Description 停止组件
     * @throws LifecycleException 如果停止失败
     */
    void stop() throws LifecycleException;
    
    /**
     * 销毁
     * 
     * @Description 销毁组件
     * @throws LifecycleException 如果销毁失败
     */
    void destroy() throws LifecycleException;
    
    // ==================== 监听器管理 ====================
    
    /**
     * 添加生命周期监听器
     * @param listener 监听器
     */
    void addLifecycleListener(LifecycleListener listener);
    
    /**
     * 移除生命周期监听器
     * @param listener 监听器
     */
    void removeLifecycleListener(LifecycleListener listener);
    
    /**
     * 获取所有生命周期监听器
     * @return 监听器数组
     */
    LifecycleListener[] findLifecycleListeners();
    
    // ==================== 状态查询 ====================
    
    /**
     * 获取当前状态
     * @return 状态
     */
    State getState();
    
    /**
     * 获取状态名称
     * @return 状态名称
     */
    String getStateName();
    
    /**
     * 是否已启动
     * @return 是否已启动
     */
    boolean isStarted();
    
    /**
     * 是否已停止
     * @return 是否已停止
     */
    boolean isStopped();
    
    // ==================== 异常定义 ====================
    
    /**
     * 生命周期异常
     */
    class LifecycleException extends Exception {
        private static final long serialVersionUID = 1L;
        
        public LifecycleException(String message) {
            super(message);
        }
        
        public LifecycleException(String message, Throwable cause) {
            super(message, cause);
        }
        
        public LifecycleException(Throwable cause) {
            super(cause);
        }
    }
}
