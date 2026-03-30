package com.huochai.huochai.tomcat.pipeline;

import com.huochai.huochai.tomcat.core.*;

/**
 * Host层的基础阀门 - 用于调用Context
 */
public class StandardHostValve extends StandardValve {

    private final com.huochai.huochai.tomcat.core.Host host;

    public StandardHostValve(com.huochai.huochai.tomcat.core.Host host) {
        this.host = host;
        setName("host-valve");
    }

    @Override
    protected void invokeInternal(Request request, Response response) throws Exception {
        // 映射到Context
        String contextPath = request.getContextPath();

        // 如果contextPath为空，尝试使用默认context
        if (contextPath == null || contextPath.isEmpty()) {
            contextPath = "/";
        }

        com.huochai.huochai.tomcat.core.Context context = null;

        if (host instanceof com.huochai.huochai.tomcat.host.StandardHost) {
            com.huochai.huochai.tomcat.host.StandardHost h =
                (com.huochai.huochai.tomcat.host.StandardHost) host;
            context = h.findContext(contextPath);

            // 如果找不到，尝试根路径
            if (context == null) {
                context = h.findContext("/");
            }
        }

        if (context == null) {
            // 未找到Context，使用默认
            for (Container child : host.findChildren()) {
                if (child instanceof Context) {
                    context = (Context) child;
                    break;
                }
            }
        }

        if (context == null) {
            response.setStatus(404);
            response.getWriter().write("Context Not Found: " + contextPath);
            return;
        }

        // 设置请求的容器
        request.setContainer(context);

        // 调用Context管道
        context.getPipeline().invoke(request, response);
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
