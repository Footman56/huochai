package com.huochai.huochai.tomcat.core;

/**
 * 生命周期基础实现
 * 
 * @Description 提供Lifecycle接口的基类实现
 * @DesignPattern 模板方法模式 - 定义生命周期骨架
 */
public abstract class LifecycleBase implements Lifecycle {
    
    // ==================== 状态管理 ====================
    
    protected volatile State state = State.NEW;
    
    private final Object stateLock = new Object();
    
    @Override
    public State getState() {
        return state;
    }
    
    @Override
    public String getStateName() {
        return state.name();
    }
    
    protected void setState(State state) {
        setState(state, null);
    }
    
    protected void setState(State state, String data) {
        synchronized (stateLock) {
            if (state == this.state) {
                return;
            }
            this.state = state;
            // 通知监听器
            fireLifecycleEvent(getStateName(), data);
        }
    }
    
    // ==================== 监听器管理 ====================
    
    private final java.util.List<LifecycleListener> listeners = 
            new java.util.ArrayList<>();
    
    @Override
    public void addLifecycleListener(LifecycleListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }
    
    @Override
    public void removeLifecycleListener(LifecycleListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
    
    @Override
    public LifecycleListener[] findLifecycleListeners() {
        synchronized (listeners) {
            return listeners.toArray(new LifecycleListener[0]);
        }
    }
    
    protected void fireLifecycleEvent(String type, Object data) {
        LifecycleListener.LifecycleEvent event = new LifecycleListener.LifecycleEvent(this, type, data);
        LifecycleListener[] listeners = findLifecycleListeners();
        for (LifecycleListener listener : listeners) {
            try {
                listener.lifecycleEvent(event);
            } catch (Exception e) {
                log("Lifecycle event listener failed", e);
            }
        }
    }
    
    // ==================== 模板方法 ====================
    
    @Override
    public final void init() throws LifecycleException {
        if (state != State.NEW) {
            return;
        }
        
        try {
            setState(State.INITIALIZED);
            initInternal();
            setState(State.INITIALIZED, AFTER_INIT_EVENT);
        } catch (LifecycleException e) {
            setState(State.FAILED);
            throw e;
        } catch (Throwable e) {
            setState(State.FAILED);
            throw new LifecycleException(e);
        }
    }
    
    @Override
    public final void start() throws LifecycleException {
        if (state == State.STARTING || state == State.STARTED) {
            return;
        }
        
        if (state == State.NEW) {
            init();
        }
        
        if (state != State.INITIALIZED && state != State.STOPPED) {
            throw new LifecycleException("Cannot start, state: " + state);
        }
        
        try {
            setState(State.STARTING);
            startInternal();
            setState(State.STARTED, AFTER_START_EVENT);
        } catch (LifecycleException e) {
            setState(State.FAILED);
            throw e;
        } catch (Throwable e) {
            setState(State.FAILED);
            throw new LifecycleException(e);
        }
    }
    
    @Override
    public final void stop() throws LifecycleException {
        if (state == State.STOPPING || state == State.STOPPED) {
            return;
        }
        
        if (state == State.NEW) {
            state = State.STOPPED;
            return;
        }
        
        if (state != State.STARTING && state != State.STARTED) {
            throw new LifecycleException("Cannot stop, state: " + state);
        }
        
        try {
            setState(State.STOPPING);
            stopInternal();
            setState(State.STOPPED, AFTER_STOP_EVENT);
        } catch (LifecycleException e) {
            setState(State.FAILED);
            throw e;
        } catch (Throwable e) {
            setState(State.FAILED);
            throw new LifecycleException(e);
        }
    }
    
    @Override
    public final void destroy() throws LifecycleException {
        if (state == State.DESTROYING || state == State.DESTROYED) {
            return;
        }
        
        if (state != State.STOPPED && state != State.FAILED) {
            throw new LifecycleException("Cannot destroy, state: " + state);
        }
        
        try {
            setState(State.DESTROYING);
            destroyInternal();
            setState(State.DESTROYED, AFTER_DESTROY_EVENT);
        } catch (LifecycleException e) {
            setState(State.FAILED);
            throw e;
        } catch (Throwable e) {
            setState(State.FAILED);
            throw new LifecycleException(e);
        }
    }
    
    // ==================== 状态查询 ====================
    
    @Override
    public boolean isStarted() {
        return state == State.STARTING || state == State.STARTED;
    }
    
    @Override
    public boolean isStopped() {
        return state == State.STOPPING || state == State.STOPPED;
    }
    
    // ==================== 子类实现 ====================
    
    /**
     * 内部初始化方法 - 子类实现
     * @throws LifecycleException 如果初始化失败
     */
    protected abstract void initInternal() throws LifecycleException;
    
    /**
     * 内部启动方法 - 子类实现
     * @throws LifecycleException 如果启动失败
     */
    protected abstract void startInternal() throws LifecycleException;
    
    /**
     * 内部停止方法 - 子类实现
     * @throws LifecycleException 如果停止失败
     */
    protected abstract void stopInternal() throws LifecycleException;
    
    /**
     * 内部销毁方法 - 子类实现
     * @throws LifecycleException 如果销毁失败
     */
    protected abstract void destroyInternal() throws LifecycleException;
    
    // ==================== 日志 ====================
    
    protected void log(String message) {
        System.out.println("[" + getClass().getSimpleName() + "] " + message);
    }
    
    protected void log(String message, Throwable throwable) {
        System.err.println("[" + getClass().getSimpleName() + "] " + message);
        throwable.printStackTrace();
    }
}
