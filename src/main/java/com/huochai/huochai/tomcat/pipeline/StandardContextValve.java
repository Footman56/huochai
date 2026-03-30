package com.huochai.huochai.tomcat.pipeline;

import com.huochai.huochai.tomcat.core.*;

/**
 * Context层的基础阀门 - 用于调用Wrapper
 */
public class StandardContextValve extends StandardValve {

    private final com.huochai.huochai.tomcat.core.Context context;

    public StandardContextValve(com.huochai.huochai.tomcat.core.Context context) {
        this.context = context;
        setName("context-valve");
    }

    @Override
    protected void invokeInternal(Request request, Response response) throws Exception {
        // 映射到Wrapper
        com.huochai.huochai.tomcat.core.Wrapper wrapper = null;

        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();

        // 去掉context path，只保留servlet path
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            uri = uri.substring(contextPath.length());
        }
        if (uri.isEmpty()) {
            uri = "/";
        }

        if (context instanceof com.huochai.huochai.tomcat.context.StandardContext) {
            com.huochai.huochai.tomcat.context.StandardContext ctx =
                (com.huochai.huochai.tomcat.context.StandardContext) context;
            wrapper = ctx.findServlet(uri);

            // 如果找不到，尝试根路径
            if (wrapper == null) {
                wrapper = ctx.findServlet("/");
            }
        }

        if (wrapper == null) {
            // 未找到Servlet
            response.setStatus(404);
            response.getWriter().write("Not Found: " + uri);
            return;
        }

        // 设置请求的Wrapper
        request.setWrapper(wrapper);

        // 调用Wrapper管道
        wrapper.getPipeline().invoke(request, response);
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
