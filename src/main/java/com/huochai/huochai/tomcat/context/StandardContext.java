package com.huochai.huochai.tomcat.context;

import com.huochai.huochai.tomcat.core.*;
import com.huochai.huochai.tomcat.mapper.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 标准Web应用上下文实现
 * 
 * @Description 管理Web应用中的Servlet、Filter、欢迎文件等
 * @DesignPattern 组合模式 - Host的子容器
 */
public class StandardContext extends ContainerBase implements Context {
    
    // ==================== 基本属性 ====================
    
    private String contextPath = "";
    private String docBase = ".";
    private boolean distributed = false;
    
    // ==================== Servlet管理 ====================
    
    private final Map<String, Wrapper> servlets = new ConcurrentHashMap<>();
    private final ServletMapper servletMapper = new ServletMapper();
    
    // ==================== 初始化参数 ====================
    
    private final Map<String, String> initParameters = new ConcurrentHashMap<>();
    
    // ==================== 欢迎文件 ====================
    
    private static final Set<String> welcomeFiles = new LinkedHashSet<>();
    static {
        welcomeFiles.add("index.html");
        welcomeFiles.add("index.htm");
        welcomeFiles.add("index.jsp");
    }
    
    // ==================== 构造方法 ====================
    
    public StandardContext() {
        super();
        setName("");
    }
    
    public StandardContext(String contextPath, String docBase) {
        this();
        this.contextPath = contextPath;
        this.docBase = docBase;
        setName(contextPath);
    }
    
    // ==================== Context 接口实现 ====================
    
    @Override
    public String getContextPath() {
        return contextPath;
    }
    
    @Override
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
        if (contextPath != null && contextPath.isEmpty()) {
            setName("");
        } else {
            setName(contextPath);
        }
    }
    
    @Override
    public String getDocBase() {
        return docBase;
    }
    
    @Override
    public void setDocBase(String docBase) {
        this.docBase = docBase;
    }
    
    // ==================== Servlet管理 ====================

    @Override
    public void addServlet(String urlPattern, Wrapper wrapper) {
        servlets.put(urlPattern, wrapper);
        servletMapper.addMapping(urlPattern, wrapper);
        wrapper.setParent(this);

        // 如果Context已启动，立即加载Servlet
        if (isStarted()) {
            try {
                wrapper.start();
            } catch (Exception e) {
                log("Failed to start servlet: " + urlPattern, e);
            }
        }
    }

    @Override
    public void removeServlet(String urlPattern) {
        servlets.remove(urlPattern);
        servletMapper.removeMapping(urlPattern);
    }

    @Override
    public Wrapper findServlet(String uri) {
        Mapping mapping = servletMapper.getMapping(uri);
        return mapping != null ? mapping.getWrapper() : null;
    }
    
    /**
     * 获取Servlet映射器
     * @return ServletMapper
     */
    public ServletMapper getServletMapper() {
        return servletMapper;
    }
    
    // ==================== Filter管理 ====================
    
    @Override
    public void addFilter(String filterName, Object filter) {
        // Filter管理简化实现
        log("Add filter: " + filterName);
    }
    
    @Override
    public Object getFilterChain() {
        return new com.huochai.huochai.tomcat.filter.StandardFilterChain();
    }
    
    // ==================== 欢迎文件 ====================
    
    @Override
    public Set<String> getWelcomeFiles() {
        return new LinkedHashSet<>(welcomeFiles);
    }
    
    @Override
    public void addWelcomeFile(String welcomeFile) {
        welcomeFiles.add(welcomeFile);
    }
    
    // ==================== 初始化参数 ====================
    
    @Override
    public String getInitParameter(String name) {
        return initParameters.get(name);
    }
    
    @Override
    public void setInitParameter(String name, String value) {
        initParameters.put(name, value);
    }
    
    // ==================== Session管理 ====================
    
    @Override
    public Object getSession(String sessionId) {
        // Session管理简化实现
        return null;
    }
    
    @Override
    public Object createSession() {
        // Session管理简化实现
        return new com.huochai.huochai.tomcat.session.StandardSession(this);
    }
    
    // ==================== 生命周期 ====================
    
    @Override
    protected void initInternal() throws LifecycleException {
        // 初始化Servlet映射器
        for (Wrapper wrapper : servlets.values()) {
            try {
                wrapper.init();
            } catch (Exception e) {
                log("Servlet init failed: " + wrapper.getName(), e);
            }
        }

        // 初始化管道
        getPipeline().init();
    }
    
    @Override
    protected void startInternal() throws LifecycleException {
        // 启动管道
        try {
            getPipeline().start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 加载需要预加载的Servlet
        for (Wrapper wrapper : servlets.values()) {
            if (wrapper.getLoadOnStartup() >= 0) {
                try {
                    wrapper.start();
                } catch (Exception e) {
                    log("Servlet start failed: " + wrapper.getName(), e);
                }
            }
        }
        
        super.startInternal();
    }
    
    // ==================== 请求处理 ====================
    
    /**
     * 处理请求
     *
     * @Description Context级别处理入口
     * @param request 请求
     * @param response 响应
     */
    public void invoke(Request request, Response response) {
        // 管道处理
        getPipeline().invoke(request, response);
    }

    public String getInfo() {
        return "Mini Tomcat Context/1.0";
    }
    
    // ==================== 其他 ====================
    
    public boolean getDistributed() {
        return distributed;
    }
    
    public void setDistributed(boolean distributed) {
        this.distributed = distributed;
    }
}
