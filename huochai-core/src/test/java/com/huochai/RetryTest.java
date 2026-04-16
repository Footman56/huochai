package com.huochai.huochai;

/**
 *
 *@author peilizhi
 *@date 2026/3/26 23:29
 **/

import com.huochai.utils.EnhancedRestTemplateClient;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.MockResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetryTest extends BaseHttpClientTest {

    @Test
    void shouldRetryOnSpecificException() {
        // 模拟三次响应：两次失败，一次成功
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setBody("{\"result\":\"success\"}"));
        mockWebServer.enqueue(new MockResponse().setBody("{\"result\":\"success\"}"));

        EnhancedRestTemplateClient client = new EnhancedRestTemplateClient.Builder()
                .enableRetry(true)
                .retryConfig(4, 10)
                .retryableExceptions(IOException.class, RestClientException.class)
                .build();

        String result = client.get(getBaseUrl() + "/test", String.class, null, null);

        assertThat(result).isEqualTo("{\"result\":\"success\"}");
        // 验证请求次数
        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);
    }

    @Test
    void shouldNotRetryOnNonRetryableException() {
        // 模拟服务端返回400（客户端错误，通常不重试）
        mockWebServer.enqueue(new MockResponse().setResponseCode(400));

        EnhancedRestTemplateClient client = new EnhancedRestTemplateClient.Builder()
                .enableRetry(true)
                .retryConfig(3, 10)
                .retryableExceptions(IOException.class) // 只重试IOException，不重试RestClientException
                .build();

        assertThatThrownBy(() -> client.get(getBaseUrl() + "/bad-request", String.class, null, null))
                .isInstanceOf(RestClientException.class);

        // 验证只调用了一次（未重试）
        // 验证请求次数
        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    void shouldNotRetryWhenSuccess() {
        mockWebServer.enqueue(new MockResponse().setBody("{\"result\":\"success\"}"));

        EnhancedRestTemplateClient client = new EnhancedRestTemplateClient.Builder()
                .enableRetry(true)
                .retryConfig(3, 10)
                .build();

        String result = client.get(getBaseUrl() + "/success", String.class, null, null);

        assertThat(result).isEqualTo("{\"result\":\"success\"}");
        // 验证只调用一次
        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    void shouldRespectMaxRetryAttempts() {
        // 始终返回500
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        EnhancedRestTemplateClient client = new EnhancedRestTemplateClient.Builder()
                .enableRetry(true)
                .retryConfig(2, 10) // 最多重试2次
                .build();

        assertThatThrownBy(() -> client.get(getBaseUrl() + "/always-fail", String.class, null, null))
                .isInstanceOf(RestClientException.class);

        // 总调用次数 = 初始1次 + 重试2次 = 3次
        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);
    }

    @Test
    void shouldRetryWhenReadTimeoutOccurs() throws InterruptedException {
        // 模拟服务端：延迟 5 秒响应（超过客户端读取超时 2 秒）
        MockResponse slowResponse = new MockResponse()
                .setBody("{\"data\":\"slow\"}")
                .setBodyDelay(10, TimeUnit.SECONDS);  // 延迟10秒发送响应体

        // 将慢响应加入队列，队列中有 3 个响应（用于验证重试次数）
        mockWebServer.enqueue(slowResponse);  // 第1次：超时
        mockWebServer.enqueue(slowResponse);  // 第2次：超时
        mockWebServer.enqueue(new MockResponse().setBody("{\"data\":\"success\"}")); // 第3次：成功



        // 创建客户端：读取超时 2 秒，重试 3 次
        EnhancedRestTemplateClient client = new EnhancedRestTemplateClient.Builder()
                .socketTimeout(2000)           // 2秒读取超时
                .enableRetry(true)
                .retryConfig(3, 100)         // 最多重试3次，间隔100ms
                .retryableExceptions(IOException.class, RestClientException.class)
                .build();

        // 发起请求
        String result = client.get(getBaseUrl() + "/slow", String.class, null, null);

        System.out.println("result = " + result);

        // 验证请求次数：前两次超时失败，第三次成功，总共3次请求
        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);


    }
}