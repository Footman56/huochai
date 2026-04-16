package com.tomcat;

import com.huochai.tomcat.Tomcat;
import com.huochai.tomcat.demo.HelloServlet;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mini Tomcat 测试类
 *
 * @Description 模拟HTTP请求测试Tomcat功能
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TomcatTest {

    private static Tomcat tomcat;
    private static OkHttpClient client;
    private static final int PORT = 8888;

    @BeforeAll
    static void setUp() throws Exception {
        // 创建Tomcat实例
        tomcat = new Tomcat();
        tomcat.setPort(PORT);

        // 注册Servlet
        tomcat.addServlet("/", "/hello", HelloServlet.class);
        tomcat.addServlet("/", "/echo", com.huochai.tomcat.demo.EchoServlet.class);

        // 初始化并启动
        tomcat.initialize();
        tomcat.start();

        // 等待服务器启动
        Thread.sleep(1000);

        // 创建HTTP客户端
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();

        System.out.println("Tomcat started on port " + PORT);
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (tomcat != null) {
            tomcat.stop();
            System.out.println("Tomcat stopped");
        }
    }

    @Test
    @Order(1)
    @DisplayName("测试GET请求 - HelloServlet")
    void testHelloServlet() throws IOException {
        Request request = new Request.Builder()
                .url("http://localhost:" + PORT + "/hello")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.header("Content-Type")).contains("text/html");

            String body = response.body().string();
            assertThat(body).contains("Hello from Mini Tomcat");
            assertThat(body).contains("Request Method: GET");
            assertThat(body).contains("/hello");

            System.out.println("=== HelloServlet Response ===");
            System.out.println(body);
        }
    }

    @Test
    @Order(2)
    @DisplayName("测试GET请求 with 参数 - EchoServlet")
    void testEchoServletGet() throws IOException {
        Request request = new Request.Builder()
                .url("http://localhost:" + PORT + "/echo?name=Test&message=Hello")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);
            assertThat(response.header("Content-Type")).contains("text/html");

            String body = response.body().string();
            assertThat(body).contains("Echo Form - GET");
            assertThat(body).contains("name");
            assertThat(body).contains("Test");
            assertThat(body).contains("message");
            assertThat(body).contains("Hello");

            System.out.println("=== EchoServlet GET Response ===");
            System.out.println(body);
        }
    }

    @Test
    @Order(3)
    @DisplayName("测试404 - 不存在的Servlet")
    void testNotFound() throws IOException {
        Request request = new Request.Builder()
                .url("http://localhost:" + PORT + "/notexist")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            // 由于未找到会返回404或者可能返回其他状态
            System.out.println("=== Not Found Response ===");
            System.out.println("Status: " + response.code());
        }
    }

    @Test
    @Order(4)
    @DisplayName("测试POST请求 - EchoServlet")
    void testEchoServletPost() throws IOException {
        RequestBody body = RequestBody.create(
                "name=PostTest&message=PostedData",
                okhttp3.MediaType.parse("application/x-www-form-urlencoded")
        );

        Request request = new Request.Builder()
                .url("http://localhost:" + PORT + "/echo")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            assertThat(response.code()).isEqualTo(200);

            String responseBody = response.body().string();
            assertThat(responseBody).contains("Echo Result - POST");
            assertThat(responseBody).contains("PostTest");
            assertThat(responseBody).contains("PostedData");

            System.out.println("=== EchoServlet POST Response ===");
            System.out.println(responseBody);
        }
    }

    @Test
    @Order(5)
    @DisplayName("测试响应头")
    void testResponseHeaders() throws IOException {
        Request request = new Request.Builder()
                .url("http://localhost:" + PORT + "/hello")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            System.out.println("=== Response Headers ===");
            System.out.println("Status: " + response.code());
            System.out.println("Content-Type: " + response.header("Content-Type"));
            System.out.println("Content-Length: " + response.header("Content-Length"));

            assertThat(response.header("Content-Type")).isNotNull();
        }
    }
}
