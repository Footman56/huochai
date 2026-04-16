package com.huochai.tomcat.core;

/**
 * 容器接口
 * 
 * @Description 定义容器的通用接口，Engine、Host、Context、Wrapper都实现此接口
 * @DesignPattern 组合模式 - 形成树形结构
 */
public interface Container extends Lifecycle {
    
    /**
     * 获取容器名称
     * @return 容器名称
     */
    String getName();
    
    /**
     * 设置容器名称
     * @param name 容器名称
     */
    void setName(String name);
    
    /**
     * 获取父容器
     * @return 父容器
     */
    Container getParent();
    
    /**
     * 设置父容器
     * @param parent 父容器
     */
    void setParent(Container parent);
    
    /**
     * 获取父容器类加载器
     * @return 父类加载器
     */
    ClassLoader getParentClassLoader();
    
    /**
     * 设置父容器类加载器
     * @param parent 父类加载器
     */
    void setParentClassLoader(ClassLoader parent);
    
    /**
     * 获取管道
     * @return 管道
     */
    Pipeline getPipeline();
    
    /**
     * 设置管道
     * @param pipeline 管道
     */
    void setPipeline(Pipeline pipeline);
    
    // ==================== 子容器管理 ====================
    
    /**
     * 添加子容器
     * @param child 子容器
     */
    void addChild(Container child);
    
    /**
     * 移除子容器
     * @param child 子容器
     */
    void removeChild(Container child);
    
    /**
     * 查找子容器
     * @param name 子容器名称
     * @return 子容器
     */
    Container findChild(String name);
    
    /**
     * 获取所有子容器
     * @return 子容器数组
     */
    Container[] findChildren();
    
    // ==================== 类加载器 ====================
    
    /**
     * 获取类加载器
     * @return 类加载器
     */
    ClassLoader getClassLoader();
    
    // ==================== 日志 ====================
    
    /**
     * 记录日志
     * @param message 日志消息
     */
    void log(String message);
    
    /**
     * 记录异常日志
     * @param message 日志消息
     * @param throwable 异常
     */
    void log(String message, Throwable throwable);
}
