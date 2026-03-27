package com.huochai.huochai.utils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.net.ssl.SSLException;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.ProxyProvider;

/**
 * WebClient 增强版 HTTP/HTTPS 客户端工具类
 *
 * 功能特性：
 * 1. 支持同步阻塞和响应式异步两种调用方式
 * 2. 基于 Reactor Netty 的高性能连接池
 * 3. 完整的超时控制（连接超时、读取超时、响应超时）
 * 4. SSL/TLS 配置（支持自定义证书、跳过验证）
 * 5. HTTP/HTTPS 代理支持
 * 6. 请求/响应日志拦截
 * 7. 基于 Resilience4j 的重试机制
 * 8. 基于 Resilience4j 的熔断机制
 * 9. 支持 GET/POST/PUT/DELETE/文件上传
 * 10. 支持泛型返回类型
 *
 * @author Assistant
 * @version 1.0
 * @since Spring Boot 3.5.11
 */
@Slf4j
public class EnhancedWebClientClient {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedWebClientClient.class);

    // WebClient 实例，所有请求共享
    private final WebClient webClient;

    // Resilience4j 重试器实例
    private final Retry retry;

    // Resilience4j 熔断器实例
    private final CircuitBreaker circuitBreaker;

    /**
     * 私有构造函数，通过 Builder 模式创建实例
     */
    private EnhancedWebClientClient(Builder builder) {
        // 1. 构建底层 HttpClient（包含连接池、超时、SSL、代理等配置）
        HttpClient httpClient = buildHttpClient(builder);

        // 2. 创建 ReactorClientHttpConnector
        ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);

        // 3. 构建 WebClient 实例
        WebClient.Builder webClientBuilder = WebClient.builder()
                .clientConnector(connector);

        // 4. 添加日志过滤器（如果启用）
        if (builder.enableLogging) {
            webClientBuilder.filter(loggingFilter());
        }

        // 5. 设置默认请求头（如果配置了）
        if (builder.defaultHeaders != null && !builder.defaultHeaders.isEmpty()) {
            webClientBuilder.defaultHeaders(headers -> headers.setAll(builder.defaultHeaders));
        }

        this.webClient = webClientBuilder.build();

        // 6. 配置重试器（如果启用）
        if (builder.enableRetry) {
            RetryConfig retryConfig = RetryConfig.custom()
                    .maxAttempts(builder.maxRetryAttempts)                          // 最大重试次数
                    .waitDuration(Duration.ofMillis(builder.retryWaitDurationMillis)) // 重试间隔
                    .retryExceptions(builder.retryableExceptions.toArray(new Class[0]))  // 触发重试的异常
                    .ignoreExceptions(builder.ignoreExceptions.toArray(new Class[0]))    // 忽略重试的异常
                    .build();
            this.retry = Retry.of("webclient-retry", retryConfig);
        } else {
            this.retry = null;
        }

        // 7. 配置熔断器（如果启用）
        if (builder.enableCircuitBreaker) {
            CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                    // 失败率阈值：当失败率达到此值时熔断器打开
                    .failureRateThreshold(builder.cbFailureRateThreshold)
                    // 慢调用率阈值：当慢调用率达到此值时熔断器打开
                    .slowCallRateThreshold(builder.cbSlowCallRateThreshold)
                    // 慢调用时间阈值：超过此时间视为慢调用
                    .slowCallDurationThreshold(Duration.ofMillis(builder.cbSlowCallDurationThresholdMillis))
                    // 熔断器打开持续时间：在此时间内所有请求直接失败
                    .waitDurationInOpenState(Duration.ofMillis(builder.cbWaitDurationOpenStateMillis))
                    // 半开状态允许的请求数：用于探测服务是否恢复
                    .permittedNumberOfCallsInHalfOpenState(builder.cbPermittedNumberOfCallsInHalfOpenState)
                    // 最小调用次数：达到此次数才开始统计
                    .minimumNumberOfCalls(builder.cbMinimumNumberOfCalls)
                    // 滑动窗口大小：统计最近 N 次调用
                    .slidingWindowSize(builder.cbSlidingWindowSize)
                    // 滑动窗口类型：基于次数或基于时间
                    .slidingWindowType(builder.cbSlidingWindowType)
                    .build();
            this.circuitBreaker = CircuitBreaker.of("webclient-cb", cbConfig);
        } else {
            this.circuitBreaker = null;
        }
    }

    /**
     * 构建底层 HttpClient（Reactor Netty）
     *
     * @param builder 配置构建器
     * @return 配置好的 HttpClient
     */
    private HttpClient buildHttpClient(Builder builder) {
        // ========== 1. 连接池配置 ==========
        ConnectionProvider connectionProvider = ConnectionProvider.builder("webclient-pool")
                .maxConnections(builder.maxConnections)                              // 最大连接数
                .pendingAcquireTimeout(Duration.ofMillis(builder.pendingAcquireTimeoutMillis)) // 获取连接超时
                .maxIdleTime(Duration.ofMillis(builder.maxIdleTimeMillis))          // 空闲连接最大存活时间
                .maxLifeTime(Duration.ofMillis(builder.maxLifeTimeMillis))          // 连接最大生命周期
                .evictInBackground(Duration.ofMillis(builder.evictInBackgroundMillis)) // 后台驱逐检查间隔
                .build();

        // ========== 2. 创建 HttpClient 实例 ==========
        HttpClient httpClient = HttpClient.create(connectionProvider)
                // 连接超时：建立 TCP 连接的最大等待时间
                .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, builder.connectTimeout)
                // 响应超时：等待服务器响应的最大时间
                .responseTimeout(Duration.ofMillis(builder.responseTimeout))
                // 是否跟随重定向
                .followRedirect(builder.followRedirect);

        // ========== 3. 代理配置 ==========
        if (builder.proxyHost != null && builder.proxyPort != null) {
            httpClient = httpClient.proxy(proxy -> proxy
                    .type(ProxyProvider.Proxy.HTTP)          // 代理类型
                    .host(builder.proxyHost)                 // 代理主机
                    .port(builder.proxyPort)                 // 代理端口
                    .username(builder.proxyUsername)         // 代理用户名（可选）
                    .password(builder.proxyPassword));       // 代理密码（可选）
        }

        // ========== 4. SSL/TLS 配置 ==========
        if (builder.sslContext != null) {
            // 使用自定义 SSL 上下文
            httpClient = httpClient.secure(spec -> spec.sslContext(builder.sslContext));
        } else if (builder.disableSSL) {
            // 测试环境：跳过 SSL 证书校验（生产环境请勿使用）
            httpClient = httpClient.secure(spec -> {
                try {
                    spec.sslContext(
                            SslContextBuilder.forClient()
                                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                    .build());
                } catch (SSLException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        return httpClient;
    }

    /**
     * 日志过滤器：记录请求和响应的详细信息
     *
     * @return ExchangeFilterFunction 过滤器
     */
    private ExchangeFilterFunction loggingFilter() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            // 记录请求信息
            if (logger.isInfoEnabled()) {
                logger.info("→ {} {} | Headers: {}",
                        request.method(), request.url(), request.headers());
            }
            return Mono.just(request);
        }).andThen(ExchangeFilterFunction.ofResponseProcessor(response -> {
            // 记录响应信息
            if (logger.isInfoEnabled()) {
                logger.info("← {} {} | Status: {}",
                        response.request().getMethod(),
                        response.request().getURI(),
                        response.statusCode());
            }
            return Mono.just(response);
        }));
    }

    /**
     * 装饰 Mono，应用重试和熔断机制
     *
     * @param mono 原始 Mono
     * @param <T> 返回类型
     * @return 装饰后的 Mono
     */
    private <T> Mono<T> decorateMono(Mono<T> mono) {
        Mono<T> decorated = mono;
        // 先应用重试（内部），再应用熔断（外部）
        if (retry != null) {
            decorated = decorated.transformDeferred(RetryOperator.of(retry));
        }
        if (circuitBreaker != null) {
            decorated = decorated.transformDeferred(CircuitBreakerOperator.of(circuitBreaker));
        }
        return decorated;
    }

    // ==================== 辅助方法 ====================

    /**
     * 构建请求 URI（支持查询参数）
     */
    private URI buildUri(String url, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return URI.create(url);
        }
        StringBuilder sb = new StringBuilder(url);
        if (!url.contains("?")) {
            sb.append("?");
        } else if (!url.endsWith("&")) {
            sb.append("&");
        }
        params.forEach((key, value) -> {
            if (value != null) {
                sb.append(key).append("=").append(value).append("&");
            }
        });
        // 移除最后一个 &
        String finalUrl = sb.substring(0, sb.length() - 1);
        return URI.create(finalUrl);
    }

    /**
     * 构建请求头
     */
    private Consumer<HttpHeaders> buildHeaders(Map<String, String> headers) {
        return httpHeaders -> {
            if (headers != null && !headers.isEmpty()) {
                headers.forEach(httpHeaders::add);
            }
        };
    }

    // ==================== GET 请求 ====================

    /**
     * GET 请求（同步阻塞方式）
     *
     * @param url 请求地址
     * @param responseType 响应类型
     * @param params 查询参数（可选）
     * @param headers 请求头（可选）
     * @param <T> 响应类型
     * @return 响应结果
     */
    public <T> T get(String url, Class<T> responseType,
                     Map<String, Object> params, Map<String, String> headers) {
        return getMono(url, responseType, params, headers).block();
    }

    /**
     * GET 请求（同步阻塞方式，支持泛型）
     */
    public <T> T get(String url, ParameterizedTypeReference<T> responseType,
                     Map<String, Object> params, Map<String, String> headers) {
        return getMono(url, responseType, params, headers).block();
    }

    /**
     * GET 请求（异步响应式方式）
     *
     * @param url 请求地址
     * @param responseType 响应类型
     * @param params 查询参数（可选）
     * @param headers 请求头（可选）
     * @param <T> 响应类型
     * @return Mono 响应流
     */
    public <T> Mono<T> getMono(String url, Class<T> responseType,
                               Map<String, Object> params, Map<String, String> headers) {
        URI uri = buildUri(url, params);
        return decorateMono(webClient.get()
                .uri(uri)
                .headers(buildHeaders(headers))
                .retrieve()
                .bodyToMono(responseType))
                .onErrorResume( e -> {
                    log.error("GET 请求失败，url: {}, params: {}, headers: {}, error: {}",
                            url, params, headers, e.getMessage());
                    return Mono.empty();
                });
    }

    /**
     * GET 请求（异步响应式方式，支持泛型）
     */
    public <T> Mono<T> getMono(String url, ParameterizedTypeReference<T> responseType,
                               Map<String, Object> params, Map<String, String> headers) {
        URI uri = buildUri(url, params);
        return decorateMono(webClient.get()
                .uri(uri)
                .headers(buildHeaders(headers))
                .retrieve()
                .bodyToMono(responseType));
    }

    // ==================== POST JSON 请求 ====================

    /**
     * POST JSON 请求（同步阻塞方式）
     */
    public <T> T postForJson(String url, Object requestBody, Class<T> responseType,
                             Map<String, String> headers) {
        return postForJsonMono(url, requestBody, responseType, headers).block();
    }

    /**
     * POST JSON 请求（异步响应式方式）
     */
    public <T> Mono<T> postForJsonMono(String url, Object requestBody, Class<T> responseType,
                                       Map<String, String> headers) {
        return decorateMono(webClient.post()
                .uri(url)
                .headers(buildHeaders(headers))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(responseType));
    }

    // ==================== POST 表单请求 ====================

    /**
     * POST 表单请求（同步阻塞方式）
     *
     * @param url 请求地址
     * @param formData 表单数据
     * @param responseType 响应类型
     * @param headers 请求头（可选）
     * @param <T> 响应类型
     * @return 响应结果
     */
    public <T> T postForm(String url, MultiValueMap<String, String> formData,
                          Class<T> responseType, Map<String, String> headers) {
        return postFormMono(url, formData, responseType, headers).block();
    }

    /**
     * POST 表单请求（异步响应式方式）
     */
    public <T> Mono<T> postFormMono(String url, MultiValueMap<String, String> formData,
                                    Class<T> responseType, Map<String, String> headers) {
        return decorateMono(webClient.post()
                .uri(url)
                .headers(buildHeaders(headers))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(responseType));
    }

    // ==================== 文件上传 ====================

    /**
     * 文件上传（同步阻塞方式）
     *
     * @param url 上传地址
     * @param formData 多部分表单数据（包含文件）
     * @param responseType 响应类型
     * @param headers 请求头（可选）
     * @param <T> 响应类型
     * @return 响应结果
     */
    public <T> T uploadFile(String url, MultiValueMap<String, Object> formData,
                            Class<T> responseType, Map<String, String> headers) {
        return uploadFileMono(url, formData, responseType, headers).block();
    }

    /**
     * 文件上传（异步响应式方式）
     */
    public <T> Mono<T> uploadFileMono(String url, MultiValueMap<String, Object> formData,
                                      Class<T> responseType, Map<String, String> headers) {
        return decorateMono(webClient.post()
                .uri(url)
                .headers(buildHeaders(headers))
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(formData))
                .retrieve()
                .bodyToMono(responseType));
    }

    // ==================== PUT 请求 ====================

    /**
     * PUT JSON 请求（同步阻塞方式）
     */
    public <T> T put(String url, Object requestBody, Class<T> responseType,
                     Map<String, String> headers) {
        return putMono(url, requestBody, responseType, headers).block();
    }

    /**
     * PUT JSON 请求（异步响应式方式）
     */
    public <T> Mono<T> putMono(String url, Object requestBody, Class<T> responseType,
                               Map<String, String> headers) {
        return decorateMono(webClient.put()
                .uri(url)
                .headers(buildHeaders(headers))
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(responseType));
    }

    // ==================== DELETE 请求 ====================

    /**
     * DELETE 请求（同步阻塞方式）
     */
    public void delete(String url, Map<String, Object> params, Map<String, String> headers) {
        deleteMono(url, params, headers).block();
    }

    /**
     * DELETE 请求（异步响应式方式）
     */
    public Mono<Void> deleteMono(String url, Map<String, Object> params, Map<String, String> headers) {
        URI uri = buildUri(url, params);
        return decorateMono(webClient.delete()
                .uri(uri)
                .headers(buildHeaders(headers))
                .retrieve()
                .bodyToMono(Void.class));
    }

    /**
     * DELETE 请求并返回响应体（同步阻塞方式）
     */
    public <T> T deleteForEntity(String url, Class<T> responseType,
                                 Map<String, Object> params, Map<String, String> headers) {
        return deleteForEntityMono(url, responseType, params, headers).block();
    }

    /**
     * DELETE 请求并返回响应体（异步响应式方式）
     */
    public <T> Mono<T> deleteForEntityMono(String url, Class<T> responseType,
                                           Map<String, Object> params, Map<String, String> headers) {
        URI uri = buildUri(url, params);
        return decorateMono(webClient.method(HttpMethod.DELETE)
                .uri(uri)
                .headers(buildHeaders(headers))
                .retrieve()
                .bodyToMono(responseType));
    }

    // ==================== 高级方法 ====================

    /**
     * 自定义请求方法（支持完全自定义）
     *
     * @param method HTTP 方法
     * @param url 请求地址
     * @param requestBody 请求体（可选）
     * @param contentType 内容类型（可选）
     * @param responseType 响应类型
     * @param params 查询参数（可选）
     * @param headers 请求头（可选）
     * @param <T> 响应类型
     * @return 响应结果（同步阻塞）
     */
    public <T> T exchange(HttpMethod method, String url, Object requestBody,
                          MediaType contentType, Class<T> responseType,
                          Map<String, Object> params, Map<String, String> headers) {
        return exchangeMono(method, url, requestBody, contentType, responseType, params, headers).block();
    }

    /**
     * 自定义请求方法（异步响应式）
     */
    public <T> Mono<T> exchangeMono(HttpMethod method, String url, Object requestBody,
                                    MediaType contentType, Class<T> responseType,
                                    Map<String, Object> params, Map<String, String> headers) {
        URI uri = buildUri(url, params);
        WebClient.RequestBodySpec requestSpec = webClient.method(method).uri(uri);

        if (headers != null && !headers.isEmpty()) {
            requestSpec.headers(httpHeaders -> headers.forEach(httpHeaders::add));
        }
        if (contentType != null) {
            requestSpec.contentType(contentType);
        }
        if (requestBody != null) {
            requestSpec.bodyValue(requestBody);
        }

        return decorateMono(requestSpec.retrieve().bodyToMono(responseType));
    }

    /**
     * 获取原始 WebClient 实例（用于更复杂的操作）
     */
    public WebClient getWebClient() {
        return webClient;
    }

    // ==================== Builder 内部类 ====================

    /**
     * Builder 模式构建器
     */
    public static class Builder {
        // ========== 连接池配置 ==========
        private int maxConnections = 200;                              // 最大连接数
        private long pendingAcquireTimeoutMillis = 30000;              // 获取连接超时
        private long maxIdleTimeMillis = 60000;                        // 空闲连接最大存活时间
        private long maxLifeTimeMillis = 300000;                       // 连接最大生命周期
        private long evictInBackgroundMillis = 30000;                  // 后台驱逐检查间隔

        // ========== 超时配置 ==========
        private int connectTimeout = 30000;                            // 连接超时（毫秒）
        private long responseTimeout = 60000;                          // 响应超时（毫秒）

        // ========== 代理配置 ==========
        private String proxyHost;                                       // 代理主机
        private Integer proxyPort;                                     // 代理端口
        private String proxyUsername;                                  // 代理用户名（可选）
        private Function<? super String, ? extends String> proxyPassword;                                  // 代理密码（可选）

        // ========== SSL 配置 ==========
        private boolean disableSSL = false;                            // 是否跳过 SSL 校验（仅测试）
        private io.netty.handler.ssl.SslContext sslContext;            // 自定义 SSL 上下文

        // ========== 请求配置 ==========
        private boolean followRedirect = true;                         // 是否跟随重定向
        private Map<String, String> defaultHeaders;                    // 默认请求头

        // ========== 日志配置 ==========
        private boolean enableLogging = true;                          // 是否启用日志

        // ========== 重试配置 ==========
        private boolean enableRetry = false;                           // 是否启用重试
        private int maxRetryAttempts = 3;                              // 最大重试次数
        private long retryWaitDurationMillis = 1000;                   // 重试间隔（毫秒）
        private List<Class<? extends Throwable>> retryableExceptions = new ArrayList<>();  // 触发重试的异常
        private List<Class<? extends Throwable>> ignoreExceptions = new ArrayList<>();     // 忽略重试的异常

        // ========== 熔断配置 ==========
        private boolean enableCircuitBreaker = false;                  // 是否启用熔断
        private float cbFailureRateThreshold = 50.0f;                  // 失败率阈值（%）
        private float cbSlowCallRateThreshold = 100.0f;                // 慢调用率阈值（%）
        private long cbSlowCallDurationThresholdMillis = 60000;        // 慢调用时间阈值（毫秒）
        private long cbWaitDurationOpenStateMillis = 60000;            // 熔断器打开持续时间（毫秒）
        private int cbPermittedNumberOfCallsInHalfOpenState = 10;      // 半开状态允许的请求数
        private int cbMinimumNumberOfCalls = 100;                      // 最小调用次数
        private int cbSlidingWindowSize = 100;                         // 滑动窗口大小
        private io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType cbSlidingWindowType =
                io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.COUNT_BASED;

        public Builder() {
            // 默认触发重试的异常
            retryableExceptions.add(java.io.IOException.class);
            retryableExceptions.add(java.net.SocketTimeoutException.class);
            retryableExceptions.add(org.springframework.web.reactive.function.client.WebClientRequestException.class);
        }

        // ========== 连接池配置方法 ==========
        public Builder maxConnections(int maxConnections) {
            this.maxConnections = maxConnections;
            return this;
        }

        public Builder pendingAcquireTimeout(long millis) {
            this.pendingAcquireTimeoutMillis = millis;
            return this;
        }

        public Builder maxIdleTime(long millis) {
            this.maxIdleTimeMillis = millis;
            return this;
        }

        public Builder maxLifeTime(long millis) {
            this.maxLifeTimeMillis = millis;
            return this;
        }

        public Builder evictInBackground(long millis) {
            this.evictInBackgroundMillis = millis;
            return this;
        }

        // ========== 超时配置方法 ==========
        public Builder connectTimeout(int timeoutMillis) {
            this.connectTimeout = timeoutMillis;
            return this;
        }

        public Builder responseTimeout(long timeoutMillis) {
            this.responseTimeout = timeoutMillis;
            return this;
        }

        // ========== 代理配置方法 ==========
        public Builder proxy(String host, int port) {
            this.proxyHost = host;
            this.proxyPort = port;
            return this;
        }

        public Builder proxy(String host, int port, String username, Function<? super String, ? extends String> password) {
            this.proxyHost = host;
            this.proxyPort = port;
            this.proxyUsername = username;
            this.proxyPassword = password;
            return this;
        }

        // ========== SSL 配置方法 ==========
        public Builder disableSSL(boolean disable) {
            this.disableSSL = disable;
            return this;
        }

        public Builder sslContext(io.netty.handler.ssl.SslContext sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        // ========== 请求配置方法 ==========
        public Builder followRedirect(boolean follow) {
            this.followRedirect = follow;
            return this;
        }

        public Builder defaultHeaders(Map<String, String> headers) {
            this.defaultHeaders = headers;
            return this;
        }

        // ========== 日志配置方法 ==========
        public Builder enableLogging(boolean enable) {
            this.enableLogging = enable;
            return this;
        }

        // ========== 重试配置方法 ==========
        public Builder enableRetry(boolean enable) {
            this.enableRetry = enable;
            return this;
        }

        public Builder retryConfig(int maxAttempts, long waitMillis) {
            this.maxRetryAttempts = maxAttempts;
            this.retryWaitDurationMillis = waitMillis;
            return this;
        }

        @SafeVarargs
        public final Builder retryableExceptions(Class<? extends Throwable>... exceptions) {
            this.retryableExceptions = Arrays.asList(exceptions);
            return this;
        }

        @SafeVarargs
        public final Builder ignoreExceptions(Class<? extends Throwable>... exceptions) {
            this.ignoreExceptions = Arrays.asList(exceptions);
            return this;
        }

        // ========== 熔断配置方法 ==========
        public Builder enableCircuitBreaker(boolean enable) {
            this.enableCircuitBreaker = enable;
            return this;
        }

        public Builder circuitBreakerConfig(float failureRateThreshold,
                                            long waitDurationOpenStateMillis,
                                            int slidingWindowSize) {
            this.cbFailureRateThreshold = failureRateThreshold;
            this.cbWaitDurationOpenStateMillis = waitDurationOpenStateMillis;
            this.cbSlidingWindowSize = slidingWindowSize;
            return this;
        }

        public Builder circuitBreakerSlowCallConfig(float slowCallRateThreshold,
                                                    long slowCallDurationThresholdMillis) {
            this.cbSlowCallRateThreshold = slowCallRateThreshold;
            this.cbSlowCallDurationThresholdMillis = slowCallDurationThresholdMillis;
            return this;
        }

        public Builder circuitBreakerHalfOpenConfig(int permittedNumberOfCalls) {
            this.cbPermittedNumberOfCallsInHalfOpenState = permittedNumberOfCalls;
            return this;
        }

        public Builder circuitBreakerMinimumCalls(int minimumNumberOfCalls) {
            this.cbMinimumNumberOfCalls = minimumNumberOfCalls;
            return this;
        }

        /**
         * 构建 EnhancedWebClientClient 实例
         */
        public EnhancedWebClientClient build() {
            return new EnhancedWebClientClient(this);
        }
    }
}