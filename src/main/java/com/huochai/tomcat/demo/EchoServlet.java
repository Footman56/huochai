package com.huochai.tomcat.demo;


import com.huochai.tomcat.servlet.HttpServlet;
import com.huochai.tomcat.servlet.http.HttpServletRequest;
import com.huochai.tomcat.servlet.http.HttpServletResponse;
import com.huochai.tomcat.util.Constants;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Map;

/**
 * Echo Servlet示例
 * 
 * @Description 演示参数接收和表单处理
 */
public class EchoServlet extends HttpServlet {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 处理GET请求 - 回显查询参数
     * 
     * @Description 将请求参数以表单形式回显
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        // 设置响应内容类型
        response.setContentType(Constants.CONTENT_TYPE_HTML + ";charset=UTF-8");
        
        PrintWriter out = response.getWriter();
        
        // 打印HTML表单页面
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Echo Form</title>");
        out.println("<style>");
        out.println("body { font-family: Arial, sans-serif; margin: 40px; }");
        out.println("form { background: #f5f5f5; padding: 20px; border-radius: 5px; max-width: 500px; }");
        out.println("input[type='text'] { width: 100%; padding: 8px; margin: 5px 0; }");
        out.println("input[type='submit'] { background: #4CAF50; color: white; padding: 10px 20px; border: none; cursor: pointer; }");
        out.println("input[type='submit']:hover { background: #45a049; }");
        out.println("table { border-collapse: collapse; width: 100%; margin-top: 20px; }");
        out.println("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        out.println("th { background-color: #4CAF50; color: white; }");
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h1>Echo Form - GET</h1>");
        
        // 显示已提交的参数
        Map<String, String[]> params = request.getParameterMap();
        if (!params.isEmpty()) {
            out.println("<h2>Submitted Parameters:</h2>");
            out.println("<table>");
            out.println("<tr><th>Parameter</th><th>Value</th></tr>");
            for (Map.Entry<String, String[]> entry : params.entrySet()) {
                String[] values = entry.getValue();
                for (String value : values) {
                    out.println("<tr><td>" + escapeHtml(entry.getKey()) + "</td><td>" + escapeHtml(value) + "</td></tr>");
                }
            }
            out.println("</table>");
        }
        
        // 显示表单
        out.println("<h2>Submit Form:</h2>");
        out.println("<form method='get' action='/echo'>");
        out.println("<label>Name: <input type='text' name='name'></label><br>");
        out.println("<label>Message: <input type='text' name='message'></label><br>");
        out.println("<input type='submit' value='Submit GET'>");
        out.println("</form>");
        
        out.println("</body>");
        out.println("</html>");
    }
    
    /**
     * 处理POST请求 - 回显表单数据
     * 
     * @Description 将POST提交的表单数据以HTML形式回显
     * @param request HTTP请求对象
     * @param response HTTP响应对象
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws IOException {
        
        // 设置响应内容类型
        response.setContentType(Constants.CONTENT_TYPE_HTML + ";charset=UTF-8");
        
        PrintWriter out = response.getWriter();
        
        // 打印HTML响应页面
        out.println("<!DOCTYPE html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<title>Echo Result</title>");
        out.println("<style>");
        out.println("body { font-family: Arial, sans-serif; margin: 40px; }");
        out.println("table { border-collapse: collapse; width: 100%; max-width: 500px; }");
        out.println("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }");
        out.println("th { background-color: #2196F3; color: white; }");
        out.println("</style>");
        out.println("</head>");
        out.println("<body>");
        out.println("<h1>Echo Result - POST</h1>");
        
        // 显示表单参数
        Map<String, String[]> params = request.getParameterMap();
        if (!params.isEmpty()) {
            out.println("<table>");
            out.println("<tr><th>Parameter</th><th>Value</th></tr>");
            for (Map.Entry<String, String[]> entry : params.entrySet()) {
                String[] values = entry.getValue();
                for (String value : values) {
                    out.println("<tr><td>" + escapeHtml(entry.getKey()) + "</td><td>" + escapeHtml(value) + "</td></tr>");
                }
            }
            out.println("</table>");
        } else {
            out.println("<p>No parameters submitted</p>");
        }
        
        // 显示请求头
        out.println("<h2>Request Headers:</h2>");
        out.println("<table>");
        out.println("<tr><th>Header</th><th>Value</th></tr>");
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            out.println("<tr><td>" + escapeHtml(name) + "</td><td>" + escapeHtml(request.getHeader(name)) + "</td></tr>");
        }
        out.println("</table>");
        
        out.println("<p><a href='/echo'>Back to Form</a></p>");
        out.println("</body>");
        out.println("</html>");
    }
    
    /**
     * HTML转义
     *
     * @Description 防止XSS攻击
     * @param input 输入字符串
     * @return 转义后的字符串
     */
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#x27;");
    }
}
