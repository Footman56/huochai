package com.huochai.tomcat.core;

import java.util.EventListener;

/**
 * 生命周期事件监听器
 * 
 * @Description 监听容器生命周期事件
 * @DesignPattern 观察者模式 - 事件监听
 */
public interface LifecycleListener extends EventListener {
    
    /**
     * 生命周期事件
     */
    static class LifecycleEvent extends java.util.EventObject {
        private final String type;
        private final Object data;
        
        public LifecycleEvent(Object source, String type, Object data) {
            super(source);
            this.type = type;
            this.data = data;
        }
        
        public String getType() { return type; }
        public Object getData() { return data; }
    }
    
    /**
     * 处理生命周期事件
     * @param event 生命周期事件
     */
    void lifecycleEvent(LifecycleEvent event);
}
