package com.huochai.tomcat;

import com.huochai.tomcat.config.ServerConfig;
import com.huochai.tomcat.connector.Connector;
import com.huochai.tomcat.context.StandardContext;
import com.huochai.tomcat.core.Engine;
import com.huochai.tomcat.core.Host;
import com.huochai.tomcat.engine.StandardEngine;
import com.huochai.tomcat.host.StandardHost;
import com.huochai.tomcat.pipeline.StandardPipeline;
import com.huochai.tomcat.pipeline.StandardEngineValve;
import com.huochai.tomcat.pipeline.StandardHostValve;
import com.huochai.tomcat.wrapper.StandardWrapper;

/**
 * Mini Tomcat主类 (简化版)
 */
public class Tomcat {

    private ServerConfig config;
    private Engine engine;
    private Host host;
    private Connector connector;

    public Tomcat() {
        this.config = new ServerConfig();
    }

    public Tomcat setPort(int port) {
        config.setPort(port);
        return this;
    }

    public Tomcat setHost(String hostName) {
        config.setHostName(hostName);
        return this;
    }

    public Tomcat setBaseDir(String appBase) {
        config.setAppBase(appBase);
        return this;
    }

    public StandardContext addWebapp(String contextPath, String docBase) {
        if (engine == null) {
            initEngine();
        }

        if (host == null) {
            initHost();
        }

        StandardContext context = new StandardContext(contextPath, docBase);

        // 设置Context的管道基础阀门
        com.huochai.tomcat.pipeline.StandardPipeline contextPipeline =
            (com.huochai.tomcat.pipeline.StandardPipeline) context.getPipeline();
        contextPipeline.setBasic(new com.huochai.tomcat.pipeline.StandardContextValve(context));

        try {
            host.addChild(context);
        } catch (Exception e) {
            throw new RuntimeException("Failed to add webapp: " + contextPath, e);
        }

        return context;
    }

    public Tomcat addServlet(String contextPath, String urlPattern,
                             Class<? extends com.huochai.tomcat.servlet.Servlet> servletClass) {
        if (engine == null) {
            initEngine();
        }

        if (host == null) {
            initHost();
        }

        StandardContext context = (StandardContext) host.findChild(contextPath);
        if (context == null) {
            context = addWebapp(contextPath, contextPath);
        }

        StandardWrapper wrapper = new StandardWrapper();
        wrapper.setServletName(urlPattern);
        wrapper.setServletClass(servletClass.getName());
        wrapper.setLoadOnStartup(1);

        // 设置Wrapper的管道基础阀门
        com.huochai.tomcat.pipeline.StandardPipeline wrapperPipeline =
            (com.huochai.tomcat.pipeline.StandardPipeline) wrapper.getPipeline();
        wrapperPipeline.setBasic(new com.huochai.tomcat.pipeline.StandardWrapperValve(wrapper));

        context.addServlet(urlPattern, wrapper);

        return this;
    }

    public Engine getEngine() {
        if (engine == null) {
            initEngine();
        }
        return engine;
    }

    public Host getHost() {
        if (host == null) {
            initHost();
        }
        return host;
    }

    public void initialize() throws Exception {
        if (engine == null) {
            initEngine();
        }
        engine.init();
    }

    public void start() throws Exception {

        if (connector == null) {
            connector = new Connector(config.getPort(), getEngine());
        }

        if (!engine.isStarted()) {
            engine.start();
        }

        connector.start();

        System.out.println("Tomcat started on port " + config.getPort());
    }

    public void stop() throws Exception {
        if (connector != null) {
            connector.stop();
        }

        if (engine != null && engine.isStarted()) {
            engine.stop();
        }

        System.out.println("Tomcat stopped");
    }

    private void initEngine() {
        engine = new StandardEngine();
        engine.setName("Tomcat");
        engine.setDefaultHost(config.getHostName() != null ? config.getHostName() : "localhost");

        StandardPipeline enginePipeline = (StandardPipeline) engine.getPipeline();
        enginePipeline.setBasic(new StandardEngineValve(engine));
    }

    private void initHost() {
        String hostName = config.getHostName() != null ? config.getHostName() : "localhost";
        host = new StandardHost();
        host.setHostName(hostName);
        host.setAppBase(config.getAppBase() != null ? config.getAppBase() : ".");

        StandardPipeline hostPipeline = (StandardPipeline) host.getPipeline();
        hostPipeline.setBasic(new StandardHostValve(host));

        try {
            engine.addChild(host);
        } catch (Exception e) {
            throw new RuntimeException("Failed to add host", e);
        }
    }

    public static void main(String[] args) throws Exception {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8421);
        tomcat.start();
        tomcat.addServlet("/", "/hello", com.huochai.tomcat.demo.HelloServlet.class);
        System.out.println("Press Ctrl+C to stop...");
        Thread.currentThread().join();
    }
}
