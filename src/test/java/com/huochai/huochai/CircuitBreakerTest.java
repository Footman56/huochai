package com.huochai.huochai;

import com.huochai.utils.EnhancedRestTemplateClient;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClientException;

import java.util.concurrent.TimeUnit;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import okhttp3.mockwebserver.MockResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CircuitBreakerTest extends BaseHttpClientTest {

    /**
     * 测试：失败率达到阈值后熔断器打开
     */
    @Test
    void shouldOpenCircuitBreakerWhenFailureRateExceedsThreshold() {
        // 配置熔断器：失败率阈值50%，滑动窗口5次，最小调用次数5，熔断开启时间2秒
        EnhancedRestTemplateClient client = new EnhancedRestTemplateClient.Builder()
                .enableCircuitBreaker(true)
                .circuitBreakerConfig(50.0f, 2000, 5)
                .circuitBreakerMinimumCalls(5)
                .build();

        // 模拟服务端：始终返回500
        for (int i = 0; i < 5; i++) {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        }

        // 发送5个请求，全部失败（失败率100% > 50%）
        for (int i = 0; i < 5; i++) {
            try {
                client.get(getBaseUrl() + "/test", String.class, null, null);
            } catch (Exception ignored) {
            }
        }

        // 第6个请求，熔断器应该打开，直接抛出 CallNotPermittedException
        assertThatThrownBy(() -> client.get(getBaseUrl() + "/test", String.class, null, null))
                .isInstanceOf(CallNotPermittedException.class);

        // 验证只有前5次请求真正发送到服务器（第6次被熔断拦截）
        assertThat(mockWebServer.getRequestCount()).isEqualTo(5);
    }

    /**
     * 测试：熔断器打开后，经过等待时间进入半开状态，成功请求可关闭熔断器
     */
    @Test
    void shouldCloseCircuitBreakerAfterSuccessfulHalfOpenRequest() throws InterruptedException {
        // 配置熔断器：失败率阈值50%，窗口5次，熔断开启时间2秒
        EnhancedRestTemplateClient client = new EnhancedRestTemplateClient.Builder()
                .enableCircuitBreaker(true)
                .circuitBreakerConfig(50.0f, 2000, 5)
                .circuitBreakerMinimumCalls(5)
                .build();

        // 前5个请求失败
        for (int i = 0; i < 5; i++) {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        }
        // 后续成功响应（用于半开状态）
        mockWebServer.enqueue(new MockResponse().setBody("success"));

        // 发送5次失败请求，触发熔断
        for (int i = 0; i < 5; i++) {
            try {
                client.get(getBaseUrl() + "/test", String.class, null, null);
            } catch (Exception ignored) {
            }
        }

        // 确认熔断器已打开
        assertThatThrownBy(() -> client.get(getBaseUrl() + "/test", String.class, null, null))
                .isInstanceOf(CallNotPermittedException.class);

        // 等待熔断器进入半开状态（2秒）
        Thread.sleep(2100);

        // 半开状态下发送一次请求，成功（返回200）
        String result = client.get(getBaseUrl() + "/test", String.class, null, null);
        assertThat(result).isEqualTo("success");

        // 熔断器应已关闭，后续请求正常
        String result2 = client.get(getBaseUrl() + "/test", String.class, null, null);
        assertThat(result2).isEqualTo("success");

        // 验证请求次数：5次失败 + 1次半开成功 + 1次正常 = 7次
        assertThat(mockWebServer.getRequestCount()).isEqualTo(7);
    }

    /**
     * 测试：半开状态下请求失败，熔断器重新打开
     */
    @Test
    void shouldReopenCircuitBreakerWhenHalfOpenRequestFails() throws InterruptedException {
        EnhancedRestTemplateClient client = new EnhancedRestTemplateClient.Builder()
                .enableCircuitBreaker(true)
                .circuitBreakerConfig(50.0f, 2000, 5)
                .circuitBreakerMinimumCalls(5)
                .build();

        // 前5个请求失败
        for (int i = 0; i < 5; i++) {
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));
        }
        // 半开状态下的请求也失败
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        // 触发熔断
        for (int i = 0; i < 5; i++) {
            try {
                client.get(getBaseUrl() + "/test", String.class, null, null);
            } catch (Exception ignored) {
            }
        }

        // 等待进入半开
        Thread.sleep(2100);

        // 半开状态下的请求失败
        assertThatThrownBy(() -> client.get(getBaseUrl() + "/test", String.class, null, null))
                .isInstanceOf(RestClientException.class);

        // 熔断器应重新打开，再次请求应被拒绝
        assertThatThrownBy(() -> client.get(getBaseUrl() + "/test", String.class, null, null))
                .isInstanceOf(CallNotPermittedException.class);

        // 验证请求次数：5次失败 + 1次半开失败 = 6次
        assertThat(mockWebServer.getRequestCount()).isEqualTo(6);
    }

    /**
     * 测试：慢调用超过阈值触发熔断
     */
    @Test
    void shouldOpenCircuitBreakerOnSlowCalls() throws InterruptedException {
        // 配置熔断器：慢调用阈值2秒，慢调用失败率50%，窗口5次，最小调用次数5
        EnhancedRestTemplateClient client = new EnhancedRestTemplateClient.Builder()
                .socketTimeout(3000)  // 客户端超时3秒，大于慢调用阈值2秒
                .enableCircuitBreaker(true)
                .circuitBreakerConfig(50.0f, 2000, 5)
                .circuitBreakerSlowCallConfig(50.0f, 2000) // 耗时>2秒视为慢调用
                .circuitBreakerMinimumCalls(5)
                .build();

        // 模拟慢响应：每个响应延迟2.5秒（超过2秒）
        MockResponse slowResponse = new MockResponse()
                .setBody("slow")
                .setBodyDelay(2500, TimeUnit.MILLISECONDS);

        // 准备5个慢响应
        for (int i = 0; i < 5; i++) {
            mockWebServer.enqueue(slowResponse);
        }

        // 发送5个慢请求，每个都超过2秒（慢调用），失败率100% > 50%
        for (int i = 0; i < 5; i++) {
            try {
                client.get(getBaseUrl() + "/test", String.class, null, null);
            } catch (Exception ignored) {
            }
        }

        // 第6个请求，熔断器应该打开，直接抛出 CallNotPermittedException
        assertThatThrownBy(() -> client.get(getBaseUrl() + "/test", String.class, null, null))
                .isInstanceOf(CallNotPermittedException.class);

        // 验证只有前5次请求真正发送到服务器（第6次被熔断拦截）
        assertThat(mockWebServer.getRequestCount()).isEqualTo(5);
    }

    /**
     * 测试：熔断器关闭时，请求正常处理
     */
    @Test
    void shouldNotTriggerCircuitBreakerWhenAllSuccess() {
        EnhancedRestTemplateClient client = new EnhancedRestTemplateClient.Builder()
                .enableCircuitBreaker(true)
                .circuitBreakerConfig(50.0f, 2000, 5)
                .circuitBreakerMinimumCalls(5)
                .build();

        // 模拟5个成功响应
        for (int i = 0; i < 5; i++) {
            mockWebServer.enqueue(new MockResponse().setBody("success"));
        }

        for (int i = 0; i < 5; i++) {
            String result = client.get(getBaseUrl() + "/test", String.class, null, null);
            assertThat(result).isEqualTo("success");
        }

        // 熔断器未打开，继续请求
        String result = client.get(getBaseUrl() + "/test", String.class, null, null);
        assertThat(result).isEqualTo("success");

        assertThat(mockWebServer.getRequestCount()).isEqualTo(6);
    }
}