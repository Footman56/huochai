package com.huochai.huochai.tomcat.engine;

import com.huochai.huochai.tomcat.core.*;

/**
 * 标准引擎实现
 * 
 * @Description Tomcat引擎是最高层容器，管理所有虚拟主机
 * @DesignPattern 组合模式 - 顶层容器
 */
public class StandardEngine extends ContainerBase implements Engine {
    
    // ==================== 基本属性 ====================
    
    private String defaultHost = "localhost";
    private Service service;
    
    // ==================== 构造方法 ====================
    
    public StandardEngine() {
        super();
        setName("Engine");
        // 引擎是顶层容器，设置空路径
        setPipeline(createPipeline());
    }
    
    public StandardEngine(Service service) {
        this();
        this.service = service;
    }
    
    // ==================== Engine 接口实现 ====================
    
    @Override
    public String getDefaultHost() {
        return defaultHost;
    }
    
    @Override
    public void setDefaultHost(String defaultHost) {
        this.defaultHost = defaultHost;
    }
    
    @Override
    public Host findHost(String name) {
        Container container = findChild(name);
        if (container instanceof Host) {
            return (Host) container;
        }
        // 如果找不到，尝试默认主机
        if (!name.equals(defaultHost)) {
            container = findChild(defaultHost);
            if (container instanceof Host) {
                return (Host) container;
            }
        }
        return null;
    }
    
    @Override
    public Service getService() {
        return service;
    }
    
    @Override
    public void setService(Service service) {
        this.service = service;
    }
    
    // ==================== 生命周期 ====================
    
    @Override
    protected void initInternal() throws LifecycleException {
        // 初始化管道
        getPipeline().init();
        super.initInternal();
    }
    
    @Override
    protected void startInternal() throws LifecycleException {
        // 启动管道
        try {
            getPipeline().start();
        } catch (Exception e) {
            throw new LifecycleException("Failed to start pipeline", e);
        }

        // 确保有默认主机
        if (findChild(defaultHost) == null) {
            log("No default host defined, using localhost");
        }

        super.startInternal();
    }
    
    // ==================== 请求处理 ====================
    
    /**
     * 处理请求
     * 
     * @Description 引擎级别处理入口
     * @param request 请求
     * @param response 响应
     */
    public void invoke(Request request, Response response) {
        // 管道处理
        getPipeline().invoke(request, response);
    }
    
    public String getInfo() {
        return "Mini Tomcat Engine/1.0";
    }
}
