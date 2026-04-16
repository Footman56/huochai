package com.huochai.tomcat.core;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 容器基类
 * 
 * @Description 提供容器接口的基础实现
 * @DesignPattern 模板方法模式 + 组合模式
 */
public abstract class ContainerBase extends LifecycleBase implements Container {
    
    // ==================== 基本属性 ====================
    
    private String name;
    private Container parent;
    private ClassLoader parentClassLoader;
    private Pipeline pipeline;
    
    // ==================== 子容器 ====================
    
    protected final Map<String, Container> children = new ConcurrentHashMap<>();
    
    // ==================== 构造方法 ====================
    
    public ContainerBase() {
        // 创建默认管道
        pipeline = createPipeline();
    }
    
    // ==================== Container 接口实现 ====================
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public void setName(String name) {
        this.name = name;
    }
    
    @Override
    public Container getParent() {
        return parent;
    }
    
    @Override
    public void setParent(Container parent) {
        Container oldParent = this.parent;
        this.parent = parent;
        
        // 更新类加载器
        if (parent != null) {
            this.parentClassLoader = parent.getParentClassLoader();
        } else {
            this.parentClassLoader = Thread.currentThread().getContextClassLoader();
        }
        
        // 通知父容器变化
        if (oldParent != parent) {
            parentChanged(oldParent, parent);
        }
    }
    
    @Override
    public ClassLoader getParentClassLoader() {
        if (parentClassLoader != null) {
            return parentClassLoader;
        }
        if (parent != null) {
            return parent.getParentClassLoader();
        }
        return Thread.currentThread().getContextClassLoader();
    }
    
    @Override
    public void setParentClassLoader(ClassLoader parent) {
        this.parentClassLoader = parent;
    }
    
    @Override
    public Pipeline getPipeline() {
        return pipeline;
    }
    
    @Override
    public void setPipeline(Pipeline pipeline) {
        this.pipeline = pipeline;
    }
    
    // ==================== 子容器管理 ====================
    
    @Override
    public void addChild(Container child) {
        if (child == null) {
            return;
        }
        
        String childName = child.getName();
        if (childName == null || childName.isEmpty()) {
            throw new IllegalArgumentException("Child container name cannot be null or empty");
        }
        
        synchronized (children) {
            if (children.get(childName) != null) {
                throw new IllegalArgumentException("Child container with name '" + childName + "' already exists");
            }
            
            child.setParent(this);
            children.put(childName, child);
            
            // 如果父容器已启动，自动启动子容器
            if (isStarted()) {
                try {
                    child.start();
                } catch (Exception e) {
                    log("Failed to start child container: " + childName, e);
                }
            }
        }
    }
    
    @Override
    public void removeChild(Container child) {
        if (child == null) {
            return;
        }
        
        synchronized (children) {
            String childName = child.getName();
            children.remove(childName);
            child.setParent(null);
        }
    }
    
    @Override
    public Container findChild(String name) {
        return children.get(name);
    }
    
    @Override
    public Container[] findChildren() {
        synchronized (children) {
            return children.values().toArray(new Container[0]);
        }
    }
    
    // ==================== 模板方法 ====================
    
    /**
     * 创建管道 - 子类可重写
     * @return Pipeline实例
     */
    protected Pipeline createPipeline() {
        return new com.huochai.tomcat.pipeline.StandardPipeline(this);
    }
    
    /**
     * 父容器变化通知 - 子类可重写
     * @param oldParent 旧父容器
     * @param newParent 新父容器
     */
    protected void parentChanged(Container oldParent, Container newParent) {
        // 默认实现为空
    }
    
    // ==================== 生命周期 ====================
    
    @Override
    protected void initInternal() throws LifecycleException {
        // 初始化子容器
        for (Container child : findChildren()) {
            try {
                child.init();
            } catch (Exception e) {
                log("Child container init failed", e);
            }
        }
    }
    
    @Override
    protected void startInternal() throws LifecycleException {
        // 启动子容器
        for (Container child : findChildren()) {
            if (!child.isStarted()) {
                try {
                    child.start();
                } catch (Exception e) {
                    log("Child container start failed: " + child.getName(), e);
                }
            }
        }
        
        // 启动管道
        if (pipeline != null) {
            try {
                pipeline.start();
            } catch (Exception e) {
                throw new LifecycleException("Pipeline start failed", e);
            }
        }
    }
    
    @Override
    protected void stopInternal() throws LifecycleException {
        // 停止管道
        if (pipeline != null) {
            try {
                pipeline.stop();
            } catch (Exception e) {
                log("Pipeline stop failed", e);
            }
        }
        
        // 停止子容器
        for (Container child : findChildren()) {
            if (child.isStarted()) {
                try {
                    child.stop();
                } catch (Exception e) {
                    log("Child container stop failed: " + child.getName(), e);
                }
            }
        }
    }
    
    @Override
    protected void destroyInternal() throws LifecycleException {
        // 销毁子容器
        for (Container child : findChildren()) {
            try {
                child.destroy();
            } catch (Exception e) {
                log("Child container destroy failed: " + child.getName(), e);
            }
        }
        
        // 清除子容器
        synchronized (children) {
            children.clear();
        }
    }
    
    // ==================== 类加载器 ====================
    
    @Override
    public ClassLoader getClassLoader() {
        return getParentClassLoader();
    }
    
    // ==================== 日志 ====================
    
    @Override
    public void log(String message) {
        System.out.println("[" + getClass().getSimpleName() + "[" + name + "]] " + message);
    }
    
    @Override
    public void log(String message, Throwable throwable) {
        System.err.println("[" + getClass().getSimpleName() + "[" + name + "]] " + message);
        throwable.printStackTrace();
    }
}
