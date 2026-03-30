package com.huochai.huochai.tomcat.wrapper;

import com.huochai.huochai.tomcat.core.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 标准Servlet包装器实现
 * 
 * @Description 包装单个Servlet，管理其生命周期和实例分配
 * @DesignPattern 装饰器模式 + 单例模式
 */
public class StandardWrapper extends ContainerBase implements Wrapper {
    
    // ==================== 基本属性 ====================
    
    private String servletName;
    private String servletClass;
    private int loadOnStartup = -1;
    private boolean available = true;
    
    // ==================== Servlet实例 ====================
    
    private volatile Object servlet;
    private boolean singleThreadModel;
    private final AtomicInteger servletCount = new AtomicInteger(0);
    private int maxInstances = 20;
    
    // ==================== 初始化参数 ====================
    
    private final Map<String, String> initParameters = new ConcurrentHashMap<>();
    
    // ==================== ServletConfig ====================

    private com.huochai.huochai.tomcat.servlet.ServletConfig servletConfig;
    
    // ==================== 构造方法 ====================
    
    public StandardWrapper() {
        super();
        setName("DefaultServlet");
    }
    
    public StandardWrapper(String servletName) {
        super();
        this.servletName = servletName;
        setName(servletName);
    }
    
    // ==================== Wrapper 接口实现 ====================
    
    @Override
    public String getServletName() {
        return servletName;
    }
    
    @Override
    public void setServletName(String servletName) {
        this.servletName = servletName;
        if (getName() == null || getName().isEmpty()) {
            setName(servletName);
        }
    }
    
    @Override
    public String getServletClass() {
        return servletClass;
    }
    
    @Override
    public void setServletClass(String servletClass) {
        this.servletClass = servletClass;
    }
    
    @Override
    public int getLoadOnStartup() {
        return loadOnStartup;
    }
    
    @Override
    public void setLoadOnStartup(int loadOnStartup) {
        this.loadOnStartup = loadOnStartup;
    }
    
    @Override
    public String getInitParameter(String name) {
        return initParameters.get(name);
    }
    
    @Override
    public void addInitParameter(String name, String value) {
        initParameters.put(name, value);
    }
    
    @Override
    public Map<String, String> getInitParameters() {
        return new ConcurrentHashMap<>(initParameters);
    }
    
    // ==================== Servlet实例管理 ====================
    
    @Override
    public Object getServlet() {
        return servlet;
    }
    
    @Override
    public void loadServlet() throws Exception {
        if (servlet != null) {
            return;
        }
        
        // 创建Servlet实例
        if (servletClass != null && !servletClass.isEmpty()) {
            try {
                Class<?> clazz = Class.forName(servletClass, true, getClass().getClassLoader());
                servlet = clazz.newInstance();
                
                // 初始化Servlet
                if (servletConfig == null) {
                    servletConfig = new StandardWrapperConfig(this);
                }
                if (servlet instanceof com.huochai.huochai.tomcat.servlet.Servlet) {
                    ((com.huochai.huochai.tomcat.servlet.Servlet) servlet).init(servletConfig);
                }
            } catch (Exception e) {
                throw new LifecycleException("Failed to load servlet: " + servletClass, e);
            }
        }
    }
    
    @Override
    public Object allocate() {
        // 单线程模型
        if (singleThreadModel) {
            synchronized (this) {
                if (servlet == null) {
                    try {
                        loadServlet();
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to load servlet", e);
                    }
                }
                servletCount.incrementAndGet();
            }
        } else {
            if (servlet == null) {
                try {
                    loadServlet();
                } catch (Exception e) {
                    throw new RuntimeException("Failed to load servlet", e);
                }
            }
        }
        return servlet;
    }
    
    @Override
    public void deallocate(Object servlet) {
        if (singleThreadModel) {
            servletCount.decrementAndGet();
        }
        // 如果是单线程模型且实例数为0，可以销毁Servlet
    }
    
    /**
     * 是否可用
     * @return 是否可用
     */
    public boolean isAvailable() {
        return available;
    }
    
    /**
     * 设置可用性
     * @param available 是否可用
     */
    public void setAvailable(boolean available) {
        this.available = available;
    }
    
    /**
     * 获取ServletConfig
     * @return ServletConfig
     */
    public com.huochai.huochai.tomcat.servlet.ServletConfig getServletConfig() {
        return servletConfig;
    }
    
    // ==================== 生命周期 ====================
    
    @Override
    protected void initInternal() throws LifecycleException {
        super.initInternal();
        // 不在这里加载Servlet，延迟加载
    }
    
    @Override
    protected void startInternal() throws LifecycleException {
        // 根据loadOnStartup决定是否加载
        if (loadOnStartup >= 0) {
            try {
                loadServlet();
            } catch (Exception e) {
                log("Failed to load servlet on startup", e);
            }
        }
        
        super.startInternal();
    }
    
    @Override
    protected void stopInternal() throws LifecycleException {
        // 销毁Servlet
        if (servlet != null && servlet instanceof com.huochai.huochai.tomcat.servlet.Servlet) {
            try {
                ((com.huochai.huochai.tomcat.servlet.Servlet) servlet).destroy();
            } catch (Exception e) {
                log("Servlet destroy failed", e);
            }
        }
        servlet = null;
        
        super.stopInternal();
    }
    
    public String getInfo() {
        return "Mini Tomcat Wrapper/1.0";
    }
    
    // ==================== ServletConfig实现 ====================
    
    /**
     * 标准Wrapper配置
     */
    private static class StandardWrapperConfig implements com.huochai.huochai.tomcat.servlet.ServletConfig {
        private final StandardWrapper wrapper;

        public StandardWrapperConfig(StandardWrapper wrapper) {
            this.wrapper = wrapper;
        }

        @Override
        public String getServletName() {
            return wrapper.getServletName();
        }

        @Override
        public com.huochai.huochai.tomcat.servlet.ServletContext getServletContext() {
            if (wrapper.getParent() instanceof Context) {
                return (com.huochai.huochai.tomcat.servlet.ServletContext) wrapper.getParent();
            }
            return null;
        }

        @Override
        public String getInitParameter(String name) {
            return wrapper.getInitParameter(name);
        }

        @Override
        public java.util.Enumeration<String> getInitParameterNames() {
            return java.util.Collections.enumeration(wrapper.getInitParameters().keySet());
        }
    }
}
