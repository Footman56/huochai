package com.huochai.tomcat.demo;

import com.huochai.tomcat.Tomcat;
import com.huochai.tomcat.core.LifecycleListener;

/**
 * Mini Tomcat 使用示例 (V2)
 *
 * @Description 演示如何启动和使用Mini Tomcat服务器
 *
 * 使用方法:
 * 1. 运行此类启动服务器
 * 2. 浏览器访问 http://localhost:8080/hello
 * 3. 浏览器访问 http://localhost:8080/echo
 * 4. 按Enter停止服务器
 *
 * 本示例展示了:
 * - 使用Tomcat主类创建服务器
 * - 注册Servlet
 * - 使用LifecycleListener监听生命周期事件
 * - 优雅关闭服务器
 */
public class TomcatDemo {

    /** 默认端口 */
    private static final int PORT = 8080;

    public static void main(String[] args) {
        try {
            System.out.println("===========================================");
            System.out.println("  Mini Tomcat Demo V2");
            System.out.println("===========================================");

            // 使用Tomcat主类创建服务器
            Tomcat tomcat = new Tomcat();
            tomcat.setPort(PORT);

            // 添加生命周期监听器
            tomcat.getEngine().addLifecycleListener(new LifecycleListener() {
                @Override
                public void lifecycleEvent(LifecycleListener.LifecycleEvent event) {
                    System.out.println("[Listener] " + event.getType() + ": " + event.getData());
                }
            });

            // 注册Servlet
            tomcat.addServlet("/", "/hello", HelloServlet.class);
            tomcat.addServlet("/", "/echo", EchoServlet.class);

            // 启动服务器
            tomcat.start();

            System.out.println("\n===========================================");
            System.out.println("  Server is running. Press Enter to stop.");
            System.out.println("===========================================\n");

            // 等待用户输入以保持服务器运行
            System.in.read();

            // 停止服务器
            tomcat.stop();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 快速启动方式
     */
    public static void quickStart() throws Exception {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(PORT);
        tomcat.addServlet("/", "/hello", HelloServlet.class);
        tomcat.start();

        System.out.println("Server running. Press Enter to stop...");
        System.in.read();

        tomcat.stop();
    }
}
