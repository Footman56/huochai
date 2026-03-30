package com.huochai.huochai.tomcat.pipeline;

import com.huochai.huochai.tomcat.core.*;

/**
 * Engine层的基础阀门 - 用于调用Host
 */
public class StandardEngineValve extends StandardValve {

    private final com.huochai.huochai.tomcat.core.Engine engine;

    public StandardEngineValve(com.huochai.huochai.tomcat.core.Engine engine) {
        this.engine = engine;
        setName("engine-valve");
    }

    @Override
    protected void invokeInternal(Request request, Response response) throws Exception {
        // 映射到Host
        String hostName = request.getServerName();
        if (hostName == null) {
            hostName = engine.getDefaultHost();
        }

        com.huochai.huochai.tomcat.core.Host host = engine.findHost(hostName);

        if (host == null) {
            // 未找到Host，使用默认
            host = engine.findHost(engine.getDefaultHost());
        }

        if (host == null) {
            response.setStatus(502);
            response.getWriter().write("No Host Found: " + hostName);
            return;
        }

        // 设置请求的容器
        request.setContainer(host);

        // 调用Host管道
        host.getPipeline().invoke(request, response);
    }

    @Override
    public String getStateName() {
        return isStarted() ? State.STARTED.name() : State.STOPPED.name();
    }

    @Override
    public State getState() {
        return isStarted() ? State.STARTED : State.STOPPED;
    }
}
