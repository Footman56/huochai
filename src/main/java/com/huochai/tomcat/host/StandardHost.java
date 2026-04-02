package com.huochai.tomcat.host;

import com.huochai.tomcat.core.Container;
import com.huochai.tomcat.core.ContainerBase;
import com.huochai.tomcat.core.Context;
import com.huochai.tomcat.core.Host;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 标准虚拟主机实现
 * 
 * @Description 虚拟主机代表一个域名对应的Web应用集合
 * @DesignPattern 组合模式 - Engine的子容器
 */
public class StandardHost extends ContainerBase implements Host {
    
    // ==================== 基本属性 ====================
    
    private String hostName = "localhost";
    private final Set<String> aliases = new CopyOnWriteArraySet<>();
    private String appBase = ".";
    
    // ==================== 别名管理 ====================
    
    @Override
    public String getHostName() {
        return hostName;
    }
    
    @Override
    public void setHostName(String hostName) {
        if (hostName == null || hostName.isEmpty()) {
            throw new IllegalArgumentException("Host name cannot be null or empty");
        }
        this.hostName = hostName;
        // 同步更新name
        if (getName() == null || getName().isEmpty()) {
            setName(hostName);
        }
    }
    
    @Override
    public void addAlias(String alias) {
        aliases.add(alias);
    }
    
    @Override
    public void removeAlias(String alias) {
        aliases.remove(alias);
    }
    
    @Override
    public boolean findAlias(String alias) {
        return aliases.contains(alias);
    }
    
    @Override
    public String[] findAliases() {
        return aliases.toArray(new String[0]);
    }
    
    // ==================== 应用管理 ====================
    
    @Override
    public Context findContext(String contextPath) {
        if (contextPath == null) {
            contextPath = "";
        }
        Container container = findChild(contextPath);
        if (container instanceof Context) {
            return (Context) container;
        }
        return null;
    }
    
    /**
     * 获取应用根目录
     * @return 应用根目录
     */
    public String getAppBase() {
        return appBase;
    }
    
    /**
     * 设置应用根目录
     * @param appBase 应用根目录
     */
    public void setAppBase(String appBase) {
        this.appBase = appBase;
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

        // 部署Web应用
        deployApps();

        super.startInternal();
    }
    
    /**
     * 部署应用
     * 
     * @Description 默认实现扫描appBase目录部署WAR应用
     */
    protected void deployApps() {
        log("Deploying web applications in " + appBase);
        // 简化实现：不需要自动部署
    }
    
    public String getInfo() {
        return "Mini Tomcat Host/1.0";
    }
}
