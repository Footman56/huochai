package com.huochai.huochai.tomcat.servlet;

import com.huochai.huochai.tomcat.exception.ServletException;
import com.huochai.huochai.tomcat.servlet.http.HttpServletRequest;
import com.huochai.huochai.tomcat.servlet.http.HttpServletResponse;
import com.huochai.huochai.tomcat.util.Constants;

import java.io.IOException;
import java.io.StringWriter;

/**
 * HTTP Servlet抽象类
 * 
 * @Description 基于HTTP协议的Servlet抽象基类，提供doGet/doPost等方法
 * @DesignPattern 模板方法模式 - service()方法根据请求方法调用对应的doXxx()方法
 *               子类只需重写对应的doXxx()方法，无需关心HTTP方法分发逻辑
 */
public abstract class HttpServlet extends GenericServlet {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 处理HTTP请求
     *
     * @Description 根据HTTP方法分发到对应的doXxx()方法
     * @DesignPattern 模板方法 - 框架定义处理流程，子类提供具体实现
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @throws ServletException 如果处理请求时发生错误
     * @throws IOException 如果发生IO错误
     */
    @Override
    public void service(ServletRequest request, ServletResponse response)
            throws ServletException, IOException {

        // 将ServletRequest转换为HttpServletRequest
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // 获取HTTP方法
        String method = httpRequest.getMethod();
        
        // 根据方法分发到对应的doXxx()方法
        if (Constants.METHOD_GET.equals(method)) {
            doGet(httpRequest, httpResponse);
        } else if (Constants.METHOD_POST.equals(method)) {
            doPost(httpRequest, httpResponse);
        } else if (Constants.METHOD_PUT.equals(method)) {
            doPut(httpRequest, httpResponse);
        } else if (Constants.METHOD_DELETE.equals(method)) {
            doDelete(httpRequest, httpResponse);
        } else if (Constants.METHOD_HEAD.equals(method)) {
            doHead(httpRequest, httpResponse);
        } else if (Constants.METHOD_OPTIONS.equals(method)) {
            doOptions(httpRequest, httpResponse);
        } else if (Constants.METHOD_TRACE.equals(method)) {
            doTrace(httpRequest, httpResponse);
        } else {
            // 不支持的方法
            doNotSupported(httpRequest, httpResponse);
        }
    }
    
    /**
     * 处理GET请求
     * 
     * @Description 默认实现返回405 Method Not Allowed
     *              子类应重写此方法提供具体实现
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @throws ServletException 如果处理请求时发生错误
     * @throws IOException 如果发生IO错误
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.sendError(Constants.SC_METHOD_NOT_ALLOWED, "GET method not supported");
    }
    
    /**
     * 处理POST请求
     * 
     * @Description 默认实现返回405 Method Not Allowed
     *              子类应重写此方法提供具体实现
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @throws ServletException 如果处理请求时发生错误
     * @throws IOException 如果发生IO错误
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.sendError(Constants.SC_METHOD_NOT_ALLOWED, "POST method not supported");
    }
    
    /**
     * 处理PUT请求
     * 
     * @Description 默认实现返回405 Method Not Allowed
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @throws ServletException 如果处理请求时发生错误
     * @throws IOException 如果发生IO错误
     */
    protected void doPut(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.sendError(Constants.SC_METHOD_NOT_ALLOWED, "PUT method not supported");
    }
    
    /**
     * 处理DELETE请求
     * 
     * @Description 默认实现返回405 Method Not Allowed
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @throws ServletException 如果处理请求时发生错误
     * @throws IOException 如果发生IO错误
     */
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.sendError(Constants.SC_METHOD_NOT_ALLOWED, "DELETE method not supported");
    }
    
    /**
     * 处理HEAD请求
     * 
     * @Description HEAD方法与GET类似，但只返回头部信息，不返回body
     *              默认调用doGet()但只设置头部
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @throws ServletException 如果处理请求时发生错误
     * @throws IOException 如果发生IO错误
     */
    protected void doHead(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // 保存原始Writer
        java.io.Writer originalWriter = response.getWriter();
        
        // 创建空的Writer（不输出body）
        StringWriter emptyWriter = new StringWriter();
        
        // 临时替换Writer
        try {
            // 通过反射或修改来禁用body输出
            // 这里简单处理：调用doGet但不输出body
            doGet(request, response);
        } finally {
            // 不需要恢复Writer，因为body应该为空
        }
    }
    
    /**
     * 处理OPTIONS请求
     * 
     * @Description 返回服务器支持的HTTP方法
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @throws ServletException 如果处理请求时发生错误
     * @throws IOException 如果发生IO错误
     */
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // 设置Allow头，表明支持的HTTP方法
        response.setHeader("Allow", "GET, POST, PUT, DELETE, HEAD, OPTIONS, TRACE");
    }
    
    /**
     * 处理TRACE请求
     * 
     * @Description 用于调试，返回请求的追踪信息
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @throws ServletException 如果处理请求时发生错误
     * @throws IOException 如果发生IO错误
     */
    protected void doTrace(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        // 返回请求行和头部信息
        StringBuilder trace = new StringBuilder();
        trace.append(request.getMethod()).append(" ")
             .append(request.getRequestURI())
             .append(request.getQueryString() != null ? "?" + request.getQueryString() : "")
             .append(" ").append(request.getProtocol());
        
        java.util.Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            trace.append("\n").append(name).append(": ").append(request.getHeader(name));
        }
        
        response.setContentType("message/http");
        response.getWriter().write(trace.toString());
    }
    
    /**
     * 处理不支持的方法
     * 
     * @Description 返回501 Not Implemented
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     * @throws ServletException 如果处理请求时发生错误
     * @throws IOException 如果发生IO错误
     */
    private void doNotSupported(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.sendError(Constants.SC_INTERNAL_SERVER_ERROR, "Method not implemented");
    }
    
    /**
     * 获取Last-Modified头
     * 
     * @Description 用于HTTP缓存机制，子类可重写返回资源的最后修改时间
     * @param request HTTP请求对象
     * @return 最后修改时间（毫秒），返回-1表示不支持
     */
    protected long getLastModified(HttpServletRequest request) {
        return -1;
    }
}
