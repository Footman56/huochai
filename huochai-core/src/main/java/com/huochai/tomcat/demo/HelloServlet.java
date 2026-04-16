package com.huochai.tomcat.demo;

import com.huochai.tomcat.servlet.HttpServlet;
import com.huochai.tomcat.servlet.http.HttpServletRequest;
import com.huochai.tomcat.servlet.http.HttpServletResponse;
import com.huochai.tomcat.util.Constants;

import java.io.IOException;

/**
 * Hello Servlet示例
 *
 * @Description 演示基本的GET请求处理
 */
public class HelloServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
     * 处理GET请求
     *
     * @Description 返回简单的问候信息
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        // 设置响应内容类型
        response.setContentType(Constants.CONTENT_TYPE_HTML + ";charset=UTF-8");

        // 获取PrintWriter
        java.io.PrintWriter out = response.getWriter();

        // 写入HTML响应
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Hello Mini Tomcat</title>");
        out.println("<style>");
        out.println("body { font-family: Arial, sans-serif; margin: 40px; }");
        out.println("h1 { color: #333; }");
        out.println(".info { background: #f5f5f5; padding: 20px; border-radius: 5px; }");
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h1>Hello from Mini Tomcat!</h1>");
        out.println("<div class='info'>");
        out.println("<p><strong>Request Method:</strong> " + request.getMethod() + "</p>");
        out.println("<p><strong>Request URI:</strong> " + request.getRequestURI() + "</p>");
        out.println("<p><strong>Protocol:</strong> " + request.getProtocol() + "</p>");

        // 显示请求参数
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            out.println("<p><strong>Query String:</strong> " + queryString + "</p>");
        }

        out.println("<p><strong>Remote Address:</strong> " + request.getRemoteAddr() + "</p>");
        out.println("</div>");
        out.println("</body>");
        out.println("</html>");
    }
}
