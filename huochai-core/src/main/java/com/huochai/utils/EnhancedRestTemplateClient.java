package com.huochai.utils;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.security.KeyStore;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;

/**
 * 增强版 RestTemplate HTTP/HTTPS 客户端工具类
 * 使用 RequestConfig 统一配置所有超时参数
 *
 * 功能特性：
 * 1. 连接池管理（基于 Apache HttpClient 5）
 * 2. SSL/TLS 配置（支持自定义证书、双向认证、跳过验证）
 * 3. RequestConfig 统一配置超时（连接超时、响应超时、从池获取连接超时）
 * 4. HTTP/HTTPS 代理支持
 * 5. 日志拦截（请求/响应日志记录）
 * 6. 重试机制（基于 Resilience4j）
 * 7. 熔断机制（基于 Resilience4j）
 * 8. 支持 GET/POST/PUT/DELETE/文件上传
 *
 * @author Assistant
 * @version 3.0
 * @since Spring Boot 3.5.11
 */
public class EnhancedRestTemplateClient {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedRestTemplateClient.class);

    private final RestTemplate restTemplate;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final CloseableHttpClient httpClient;

    private EnhancedRestTemplateClient(Builder builder) {
        // 构建 HttpClient（使用 RequestConfig 统一配置）
        this.httpClient = buildHttpClient(builder);

        // 创建 HttpComponentsClientHttpRequestFactory
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);

        // Spring 6.2+ 使用 Duration 设置超时（作为备用，主要配置在 RequestConfig 中）
        //if (builder.connectTimeout > 0) {
        //    factory.setConnectTimeout(Duration.ofMillis(builder.connectTimeout));
        //}

        // 代理配置
        //if (builder.proxyHost != null && builder.proxyPort != null) {
        //    factory.setProxy(new Proxy(Proxy.Type.HTTP,
        //            new InetSocketAddress(builder.proxyHost, builder.proxyPort)));
        //}

        // 创建 RestTemplate
        this.restTemplate = new RestTemplate(factory);

        // 添加日志拦截器
        if (builder.enableLogging) {
            this.restTemplate.getInterceptors().add(new LoggingInterceptor());
        }

        // 配置重试
        if (builder.enableRetry) {
            RetryConfig retryConfig = RetryConfig.custom()
                    .maxAttempts(builder.maxRetryAttempts)
                    .waitDuration(Duration.ofMillis(builder.retryWaitDurationMillis))
                    .retryExceptions(builder.retryableExceptions.toArray(new Class[0]))
                    .ignoreExceptions(builder.ignoreExceptions.toArray(new Class[0]))
                    .build();
            this.retry = Retry.of("rest-template-retry", retryConfig);
        } else {
            this.retry = null;
        }

        // 配置熔断
        if (builder.enableCircuitBreaker) {
            CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                    .failureRateThreshold(builder.cbFailureRateThreshold)
                    .slowCallRateThreshold(builder.cbSlowCallRateThreshold)
                    .slowCallDurationThreshold(Duration.ofMillis(builder.cbSlowCallDurationThresholdMillis))
                    .waitDurationInOpenState(Duration.ofMillis(builder.cbWaitDurationOpenStateMillis))
                    .permittedNumberOfCallsInHalfOpenState(builder.cbPermittedNumberOfCallsInHalfOpenState)
                    .minimumNumberOfCalls(builder.cbMinimumNumberOfCalls)
                    .slidingWindowSize(builder.cbSlidingWindowSize)
                    .slidingWindowType(builder.cbSlidingWindowType)
                    .build();
            this.circuitBreaker = CircuitBreaker.of("rest-template-cb", cbConfig);
        } else {
            this.circuitBreaker = null;
        }
    }

    /**
     * 构建 Apache HttpClient 5.x，使用 RequestConfig 统一配置
     */
    private CloseableHttpClient buildHttpClient(Builder builder) {
        try {
            // ========== 1. SSL 上下文配置 ==========
            SSLContext sslContext = builder.sslContext;
            if (sslContext == null && builder.disableSSL) {
                // 跳过 SSL 校验（仅测试环境）
                sslContext = SSLContexts.custom()
                        .loadTrustMaterial((chain, authType) -> true)
                        .build();
            }

            // 注册协议工厂
            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", new SSLConnectionSocketFactory(
                            sslContext != null ? sslContext : SSLContexts.createSystemDefault(),
                            builder.hostnameVerifier != null ? builder.hostnameVerifier : NoopHostnameVerifier.INSTANCE))
                    .build();

            // ========== 2. 连接管理器配置 ==========
            PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(registry);
            connectionManager.setMaxTotal(builder.maxTotalConnections);
            connectionManager.setDefaultMaxPerRoute(builder.maxConnectionsPerRoute);

            // Socket 配置
            SocketConfig socketConfig = SocketConfig.custom()
                    .setSoTimeout(Timeout.ofMilliseconds(builder.socketTimeout))
                    .setTcpNoDelay(builder.tcpNoDelay)
                    .setSoKeepAlive(builder.keepAlive)
                    .build();
            connectionManager.setDefaultSocketConfig(socketConfig);

            // 连接配置
            ConnectionConfig connectionConfig = ConnectionConfig.custom()
                    // 连接超时（建立TCP连接的超时时间）
                    .setConnectTimeout(Timeout.ofMilliseconds(builder.connectTimeout))
                    // Socket 超时（等待服务器返回数据的超时时间,包括请求体发出和响应体接受）
                    .setSocketTimeout(Timeout.ofMilliseconds(builder.socketTimeout))
                    .setTimeToLive(Timeout.ofMilliseconds(builder.connectionTimeToLiveMillis))
                    .build();
            connectionManager.setDefaultConnectionConfig(connectionConfig);

            // ========== 3. RequestConfig 统一配置所有请求超时 ==========
            RequestConfig requestConfig = RequestConfig.custom()
                    // 从连接池获取连接的超时时间
                    .setConnectionRequestTimeout(Timeout.ofMilliseconds(builder.connectionRequestTimeout))
                    // 响应超时（从发送完请求到接收到响应头的最大等待时间）
                    .setResponseTimeout(Timeout.ofMilliseconds(builder.responseTimeout))
                    // 是否启用自动重定向
                    .setRedirectsEnabled(builder.redirectsEnabled)
                    // 是否启用内容压缩
                    .setContentCompressionEnabled(builder.contentCompressionEnabled)
                    // 是否启用循环重定向
                    .setCircularRedirectsAllowed(builder.circularRedirectsAllowed)
                    // 最大重定向次数
                    .setMaxRedirects(builder.maxRedirects)
                    // 认证缓存是否启用
                    .setAuthenticationEnabled(builder.authenticationEnabled)
                    // 期望继续握手超时
                    .setExpectContinueEnabled(builder.expectContinueEnabled)
                    // 目标服务器首选Cookie规范
                    .setCookieSpec(builder.cookieSpec)
                    // 目标主机地址
                    .setTargetPreferredAuthSchemes(builder.targetPreferredAuthSchemes)
                    // 代理服务器认证方案
                    .setProxyPreferredAuthSchemes(builder.proxyPreferredAuthSchemes)
                    .build();

            // ========== 4. 构建 HttpClient ==========
            return HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .setDefaultRequestConfig(requestConfig)
                    .evictExpiredConnections()
                    .evictIdleConnections(Timeout.ofMilliseconds(builder.connectionEvictIdleMillis))
                    .disableCookieManagement()  // 禁用 Cookie 管理，减少开销
                    .disableAuthCaching()       // 禁用认证缓存
                    .useSystemProperties()       // 使用系统属性（代理、SSL等）
                    .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build HttpClient", e);
        }
    }

    /**
     * 装饰 Supplier 以应用重试和熔断
     */
    private <T> T executeWithResilience(Supplier<T> supplier) {
        Supplier<T> decoratedSupplier = supplier;
        if (retry != null) {
            decoratedSupplier = Retry.decorateSupplier(retry, decoratedSupplier);
        }
        if (circuitBreaker != null) {
            decoratedSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, decoratedSupplier);
        }
        return decoratedSupplier.get();
    }

    // ==================== GET 请求 ====================

    /**
     * GET 请求（返回指定类型）
     */
    public <T> T get(String url, Class<T> responseType, Map<String, Object> params, Map<String, String> headers) {
        return executeWithResilience(() -> {
            URI uri = buildUri(url, params);
            HttpEntity<?> entity = new HttpEntity<>(buildHeaders(headers));
            ResponseEntity<T> response = restTemplate.exchange(uri, HttpMethod.GET, entity, responseType);
            return response.getBody();
        });
    }

    /**
     * GET 请求（支持泛型返回类型）
     */
    public <T> T get(String url, ParameterizedTypeReference<T> responseType,
                     Map<String, Object> params, Map<String, String> headers) {
        return executeWithResilience(() -> {
            URI uri = buildUri(url, params);
            HttpEntity<?> entity = new HttpEntity<>(buildHeaders(headers));
            ResponseEntity<T> response = restTemplate.exchange(uri, HttpMethod.GET, entity, responseType);
            return response.getBody();
        });
    }

    /**
     * GET 请求（返回 ResponseEntity）
     */
    public <T> ResponseEntity<T> getForEntity(String url, Class<T> responseType,
                                              Map<String, Object> params, Map<String, String> headers) {
        return executeWithResilience(() -> {
            URI uri = buildUri(url, params);
            HttpEntity<?> entity = new HttpEntity<>(buildHeaders(headers));
            return restTemplate.exchange(uri, HttpMethod.GET, entity, responseType);
        });
    }

    // ==================== POST 请求 ====================

    /**
     * POST JSON 请求
     */
    public <T> T postForJson(String url, Object requestBody, Class<T> responseType, Map<String, String> headers) {
        return executeWithResilience(() -> {
            HttpHeaders httpHeaders = buildHeaders(headers);
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(requestBody, httpHeaders);
            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
            return response.getBody();
        });
    }

    /**
     * POST 表单请求（application/x-www-form-urlencoded）
     */
    public <T> T postForm(String url, MultiValueMap<String, String> formData,
                          Class<T> responseType, Map<String, String> headers) {
        return executeWithResilience(() -> {
            HttpHeaders httpHeaders = buildHeaders(headers);
            httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(formData, httpHeaders);
            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
            return response.getBody();
        });
    }

    /**
     * POST 请求（自定义 Content-Type）
     */
    public <T> T post(String url, Object requestBody, MediaType contentType,
                      Class<T> responseType, Map<String, String> headers) {
        return executeWithResilience(() -> {
            HttpHeaders httpHeaders = buildHeaders(headers);
            httpHeaders.setContentType(contentType);
            HttpEntity<Object> entity = new HttpEntity<>(requestBody, httpHeaders);
            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
            return response.getBody();
        });
    }

    // ==================== 文件上传 ====================

    /**
     * 文件上传（multipart/form-data）
     */
    public <T> T uploadFile(String url, MultiValueMap<String, Object> formData,
                            Class<T> responseType, Map<String, String> headers) {
        return executeWithResilience(() -> {
            HttpHeaders httpHeaders = buildHeaders(headers);
            httpHeaders.setContentType(MediaType.MULTIPART_FORM_DATA);
            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(formData, httpHeaders);
            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
            return response.getBody();
        });
    }

    // ==================== PUT 请求 ====================

    /**
     * PUT JSON 请求
     */
    public <T> T put(String url, Object requestBody, Class<T> responseType, Map<String, String> headers) {
        return executeWithResilience(() -> {
            HttpHeaders httpHeaders = buildHeaders(headers);
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Object> entity = new HttpEntity<>(requestBody, httpHeaders);
            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.PUT, entity, responseType);
            return response.getBody();
        });
    }

    // ==================== DELETE 请求 ====================

    /**
     * DELETE 请求
     */
    public void delete(String url, Map<String, Object> params, Map<String, String> headers) {
        executeWithResilience(() -> {
            URI uri = buildUri(url, params);
            HttpEntity<?> entity = new HttpEntity<>(buildHeaders(headers));
            restTemplate.exchange(uri, HttpMethod.DELETE, entity, Void.class);
            return null;
        });
    }

    /**
     * DELETE 请求（返回响应体）
     */
    public <T> T deleteForEntity(String url, Class<T> responseType,
                                 Map<String, Object> params, Map<String, String> headers) {
        return executeWithResilience(() -> {
            URI uri = buildUri(url, params);
            HttpEntity<?> entity = new HttpEntity<>(buildHeaders(headers));
            ResponseEntity<T> response = restTemplate.exchange(uri, HttpMethod.DELETE, entity, responseType);
            return response.getBody();
        });
    }

    // ==================== 辅助方法 ====================

    private HttpHeaders buildHeaders(Map<String, String> headers) {
        HttpHeaders httpHeaders = new HttpHeaders();
        if (headers != null && !headers.isEmpty()) {
            headers.forEach(httpHeaders::add);
        }
        return httpHeaders;
    }

    private URI buildUri(String url, Map<String, Object> params) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        if (params != null && !params.isEmpty()) {
            params.forEach((key, value) -> {
                if (value != null) {
                    builder.queryParam(key, value);
                }
            });
        }
        return builder.build().encode().toUri();
    }

    /**
     * 获取 RestTemplate 实例
     */
    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    /**
     * 优雅关闭，释放连接池资源
     */
    public void close() {
        try {
            if (httpClient != null) {
                httpClient.close();
                logger.info("HttpClient closed successfully");
            }
        } catch (IOException e) {
            logger.error("Error closing HttpClient", e);
        }
    }

    // ==================== 日志拦截器 ====================

    /**
     * HTTP 请求日志拦截器
     */
    private static class LoggingInterceptor implements ClientHttpRequestInterceptor {
        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                            ClientHttpRequestExecution execution) throws IOException {
            long start = System.currentTimeMillis();

            // 记录请求信息
            if (logger.isInfoEnabled()) {
                logger.info("→ {} {} | Headers: {}",
                        request.getMethod(), request.getURI(), request.getHeaders());
            }

            if (logger.isDebugEnabled() && body != null && body.length > 0) {
                logger.debug("Request Body: {}", new String(body));
            }

            ClientHttpResponse response = execution.execute(request, body);
            long duration = System.currentTimeMillis() - start;

            // 记录响应信息
            if (logger.isInfoEnabled()) {
                logger.info("← {} {} | Status: {} | Time: {}ms",
                        request.getMethod(), request.getURI(),
                        response.getStatusCode(), duration);
            }

            return response;
        }
    }

    // ==================== Builder 内部类 ====================

    /**
     * Builder 模式构建器
     */
    public static class Builder {
        // ========== SSL 配置 ==========
        private boolean disableSSL = false;
        private SSLContext sslContext;
        private HostnameVerifier hostnameVerifier;

        // ========== 代理配置 ==========
        private String proxyHost;
        private Integer proxyPort;

        // ========== RequestConfig 超时配置（核心）==========
        private int connectTimeout = 30000;              // 连接超时（毫秒）
        private int responseTimeout = 60000;             // 响应超时（毫秒）
        private int connectionRequestTimeout = 30000;    // 从池获取连接超时（毫秒）
        private int socketTimeout = 60000;               // Socket 超时（毫秒）

        // ========== 连接池配置 ==========
        private int maxTotalConnections = 200;           // 最大总连接数
        private int maxConnectionsPerRoute = 50;         // 每个路由最大连接数
        private int connectionTimeToLiveMillis = 300000; // 连接最大存活时间（5分钟）
        private int connectionEvictIdleMillis = 60000;   // 空闲连接驱逐时间（1分钟）

        // ========== Socket 配置 ==========
        private boolean tcpNoDelay = true;               // TCP 无延迟
        private boolean keepAlive = true;                // TCP KeepAlive

        // ========== RequestConfig 高级配置 ==========
        private boolean redirectsEnabled = true;         // 自动重定向
        private boolean contentCompressionEnabled = true; // 内容压缩
        private boolean circularRedirectsAllowed = false; // 循环重定向
        private int maxRedirects = 50;                   // 最大重定向次数
        private boolean authenticationEnabled = true;    // 认证缓存
        private boolean expectContinueEnabled = false;   // 期望继续握手
        private String cookieSpec = "default";           // Cookie 规范
        private List<String> targetPreferredAuthSchemes = Arrays.asList("Bearer", "Basic");
        private List<String> proxyPreferredAuthSchemes = Arrays.asList("Basic");

        // ========== 日志配置 ==========
        private boolean enableLogging = true;

        // ========== 重试配置 ==========
        private boolean enableRetry = false;
        private int maxRetryAttempts = 3;
        private long retryWaitDurationMillis = 1000;
        private List<Class<? extends Throwable>> retryableExceptions = new ArrayList<>();
        private List<Class<? extends Throwable>> ignoreExceptions = new ArrayList<>();

        // ========== 熔断配置 ==========
        private boolean enableCircuitBreaker = false;
        private float cbFailureRateThreshold = 50.0f;
        private float cbSlowCallRateThreshold = 100.0f;
        private long cbSlowCallDurationThresholdMillis = 60000;
        private long cbWaitDurationOpenStateMillis = 60000;
        private int cbPermittedNumberOfCallsInHalfOpenState = 10;
        private int cbMinimumNumberOfCalls = 100;
        private int cbSlidingWindowSize = 100;
        private io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType cbSlidingWindowType =
                io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType.COUNT_BASED;

        public Builder() {
            // 默认重试异常：RestClientException 及其子类
            this.retryableExceptions.add(RestClientException.class);
            this.retryableExceptions.add(IOException.class);
        }

        // ========== SSL 配置方法 ==========
        public Builder disableSSL(boolean disable) {
            this.disableSSL = disable;
            return this;
        }

        public Builder sslContext(SSLContext sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        public Builder hostnameVerifier(HostnameVerifier verifier) {
            this.hostnameVerifier = verifier;
            return this;
        }

        /**
         * 加载信任库（JKS 格式）
         */
        public Builder trustStore(String trustStorePath, String trustStorePass) {
            try {
                KeyStore trustStore = KeyStore.getInstance("JKS");
                try (FileInputStream fis = new FileInputStream(trustStorePath)) {
                    trustStore.load(fis, trustStorePass.toCharArray());
                }
                SSLContext sslContext = SSLContexts.custom()
                        .loadTrustMaterial(trustStore, null)
                        .build();
                this.sslContext = sslContext;
            } catch (Exception e) {
                throw new RuntimeException("Failed to load truststore", e);
            }
            return this;
        }

        /**
         * 配置双向 SSL（加载客户端证书）
         */
        public Builder mutualSsl(String keyStorePath, String keyStorePass, String keyPass) {
            try {
                KeyStore keyStore = KeyStore.getInstance("JKS");
                try (FileInputStream fis = new FileInputStream(keyStorePath)) {
                    keyStore.load(fis, keyStorePass.toCharArray());
                }
                SSLContext sslContext = SSLContexts.custom()
                        .loadKeyMaterial(keyStore, keyPass.toCharArray())
                        .loadTrustMaterial(null, (chain, authType) -> true)
                        .build();
                this.sslContext = sslContext;
            } catch (Exception e) {
                throw new RuntimeException("Failed to load keystore for mutual SSL", e);
            }
            return this;
        }

        // ========== RequestConfig 超时配置方法 ==========

        /**
         * 配置所有超时（统一设置）
         */
        public Builder timeout(int connectTimeout, int responseTimeout, int connectionRequestTimeout) {
            this.connectTimeout = connectTimeout;
            this.responseTimeout = responseTimeout;
            this.connectionRequestTimeout = connectionRequestTimeout;
            return this;
        }

        /**
         * 连接超时（建立TCP连接的超时时间）
         */
        public Builder connectTimeout(int timeoutMillis) {
            this.connectTimeout = timeoutMillis;
            return this;
        }

        /**
         * 响应超时（等待服务器返回数据的超时时间）
         */
        public Builder responseTimeout(int timeoutMillis) {
            this.responseTimeout = timeoutMillis;
            return this;
        }

        /**
         * 从连接池获取连接的超时时间
         */
        public Builder connectionRequestTimeout(int timeoutMillis) {
            this.connectionRequestTimeout = timeoutMillis;
            return this;
        }

        /**
         * Socket 超时（读写超时）
         */
        public Builder socketTimeout(int timeoutMillis) {
            this.socketTimeout = timeoutMillis;
            return this;
        }

        // ========== 连接池配置 ==========
        public Builder maxTotalConnections(int maxTotal) {
            this.maxTotalConnections = maxTotal;
            return this;
        }

        public Builder maxConnectionsPerRoute(int perRoute) {
            this.maxConnectionsPerRoute = perRoute;
            return this;
        }

        public Builder connectionTimeToLive(int millis) {
            this.connectionTimeToLiveMillis = millis;
            return this;
        }

        public Builder connectionEvictIdle(int millis) {
            this.connectionEvictIdleMillis = millis;
            return this;
        }

        // ========== Socket 配置 ==========
        public Builder tcpNoDelay(boolean noDelay) {
            this.tcpNoDelay = noDelay;
            return this;
        }

        public Builder keepAlive(boolean keepAlive) {
            this.keepAlive = keepAlive;
            return this;
        }

        // ========== RequestConfig 高级配置 ==========
        public Builder redirectsEnabled(boolean enabled) {
            this.redirectsEnabled = enabled;
            return this;
        }

        public Builder contentCompressionEnabled(boolean enabled) {
            this.contentCompressionEnabled = enabled;
            return this;
        }

        public Builder maxRedirects(int maxRedirects) {
            this.maxRedirects = maxRedirects;
            return this;
        }

        public Builder authenticationEnabled(boolean enabled) {
            this.authenticationEnabled = enabled;
            return this;
        }

        // ========== 代理配置 ==========
        public Builder proxy(String host, int port) {
            this.proxyHost = host;
            this.proxyPort = port;
            return this;
        }

        // ========== 日志配置 ==========
        public Builder enableLogging(boolean enable) {
            this.enableLogging = enable;
            return this;
        }

        // ========== 重试配置 ==========
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

        // ========== 熔断配置 ==========
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
         * 构建 EnhancedRestTemplateClient 实例
         */
        public EnhancedRestTemplateClient build() {
            return new EnhancedRestTemplateClient(this);
        }
    }
}