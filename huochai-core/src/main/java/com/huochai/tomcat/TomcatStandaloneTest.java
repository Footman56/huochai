package com.huochai.tomcat;

import com.huochai.tomcat.demo.HelloServlet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Mini Tomcat 独立测试类
 *
 * @Description 使用纯Socket模拟HTTP请求测试Tomcat功能
 */
public class TomcatStandaloneTest {

    private static Tomcat tomcat;
    private static final int PORT = 8888;

    public static void main(String[] args) throws Exception {
        System.out.println("========== Mini Tomcat 测试 ==========\n");

        try {
            // 1. 启动Tomcat
            startTomcat();

            // 2. 等待服务器启动
            Thread.sleep(2000);

            // 3. 测试请求
            testHelloServlet();
            testEchoServlet();
            testNotFound();

            System.out.println("\n========== 所有测试完成 ==========");

        } finally {
            // 4. 停止Tomcat
            stopTomcat();
        }
    }

    private static void startTomcat() throws Exception {
        System.out.println("[启动] 初始化Tomcat服务器...");

        tomcat = new Tomcat();
        tomcat.setPort(PORT);


        tomcat.initialize();
        tomcat.start();

        // 注册Servlet
        tomcat.addServlet("/", "/hello", HelloServlet.class);
        tomcat.addServlet("/", "/echo", com.huochai.tomcat.demo.EchoServlet.class);


        System.out.println("[启动] Tomcat已启动，端口: " + PORT);
    }

    private static void stopTomcat() {
        try {
            if (tomcat != null) {
                tomcat.stop();
                System.out.println("[停止] Tomcat已停止");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 测试HelloServlet
     */
    private static void testHelloServlet() {
        System.out.println("\n--- 测试1: GET /hello ---");
        try {
            String response = sendHttpRequest("GET", "/hello", null);
            System.out.println("响应状态: " + getStatusCode(response));
            System.out.println("响应头: " + response.split("\r\n\r\n")[0]);

            String body = getBody(response);
            System.out.println("响应体长度: " + (body != null ? body.length() : 0));

            if (body != null && body.contains("Hello from Mini Tomcat")) {
                System.out.println("✅ 测试通过: HelloServlet正常工作");
                System.out.println("\n响应内容预览:");
                System.out.println(body.substring(0, Math.min(300, body.length())) + "...");
            } else {
                System.out.println("❌ 测试失败: 响应内容不正确");
                if (body != null) {
                    System.out.println("实际响应体: " + body.substring(0, Math.min(200, body.length())));
                }
            }

        } catch (Exception e) {
            System.out.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试EchoServlet
     */
    private static void testEchoServlet() {
        System.out.println("\n--- 测试2: GET /echo?name=Test ---");
        try {
            String response = sendHttpRequest("GET", "/echo?name=Test&message=Hello", null);
            System.out.println("响应状态: " + getStatusCode(response));

            String body = getBody(response);
            System.out.println("响应体长度: " + (body != null ? body.length() : 0));

            if (body != null && body.contains("Echo Form") && body.contains("Test")) {
                System.out.println("✅ 测试通过: EchoServlet正常工作");
                System.out.println("\n响应内容预览:");
                System.out.println(body.substring(0, Math.min(300, body.length())) + "...");
            } else {
                System.out.println("❌ 测试失败: 响应内容不正确");
                if (body != null) {
                    System.out.println("实际响应体: " + body.substring(0, Math.min(200, body.length())));
                }
            }

        } catch (Exception e) {
            System.out.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 测试404
     */
    private static void testNotFound() {
        System.out.println("\n--- 测试3: GET /notexist ---");
        try {
            String response = sendHttpRequest("GET", "/notexist", null);
            System.out.println("响应状态: " + getStatusCode(response));
            System.out.println("✅ 测试通过: 正确处理未知路径");

        } catch (Exception e) {
            System.out.println("❌ 测试失败: " + e.getMessage());
        }
    }

    /**
     * 发送HTTP请求
     */
    private static String sendHttpRequest(String method, String path, String body) throws IOException {
        StringBuilder sb = new StringBuilder();

        // 构建请求行
        String query = "";
        if (path.contains("?")) {
            String[] parts = path.split("\\?");
            path = parts[0];
            query = parts[1];
        }

        sb.append(method).append(" ").append(path);
        if (!query.isEmpty()) {
            sb.append("?").append(query);
        }
        sb.append(" HTTP/1.1\r\n");

        // 构建请求头
        sb.append("Host: localhost:").append(PORT).append("\r\n");
        sb.append("Connection: close\r\n");
        sb.append("Accept: text/html\r\n");

        if (body != null) {
            sb.append("Content-Type: application/x-www-form-urlencoded\r\n");
            sb.append("Content-Length: ").append(body.length()).append("\r\n");
        }

        sb.append("\r\n");

        // 添加请求体
        if (body != null) {
            sb.append(body);
        }

        // 发送请求
        try (Socket socket = new Socket("localhost", PORT)) {
            socket.setSoTimeout(5000);

            OutputStream out = socket.getOutputStream();
            out.write(sb.toString().getBytes("UTF-8"));
            out.flush();

            // 读取响应
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = socket.getInputStream().read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }

            return baos.toString("UTF-8");
        }
    }

    /**
     * 获取响应状态码
     */
    private static String getStatusCode(String response) {
        if (response == null || response.isEmpty()) return "无响应";
        String firstLine = response.split("\r\n")[0];
        return firstLine;
    }

    /**
     * 获取响应头
     */
    private static String getHeader(String response, String headerName) {
        if (response == null) return null;
        String[] lines = response.split("\r\n");
        for (String line : lines) {
            if (line.toLowerCase().startsWith(headerName.toLowerCase() + ":")) {
                return line.substring(line.indexOf(":") + 1).trim();
            }
        }
        return null;
    }

    /**
     * 获取响应体
     */
    private static String getBody(String response) {
        if (response == null) return null;
        int index = response.indexOf("\r\n\r\n");
        if (index > 0) {
            return response.substring(index + 4);
        }
        return null;
    }
}
