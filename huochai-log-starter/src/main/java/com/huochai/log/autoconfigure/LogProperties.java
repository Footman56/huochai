package com.huochai.log.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 日志模块配置属性
 */
@Data
@ConfigurationProperties(prefix = "huochai.log")
public class LogProperties {

    /**
     * 是否启用日志模块
     */
    private boolean enabled = true;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * Controller 日志配置
     */
    private ControllerConfig controller = new ControllerConfig();

    /**
     * Service 日志配置
     */
    private ServiceConfig service = new ServiceConfig();

    /**
     * 数据库日志配置
     */
    private DatabaseConfig database = new DatabaseConfig();

    /**
     * MQ 日志配置
     */
    private MqConfig mq = new MqConfig();

    /**
     * Dubbo 日志配置
     */
    private DubboConfig dubbo = new DubboConfig();

    /**
     * Disruptor 配置
     */
    private DisruptorConfig disruptor = new DisruptorConfig();

    /**
     * Trace 配置
     */
    private TraceConfig trace = new TraceConfig();

    @Data
    public static class ControllerConfig {
        /**
         * 是否启用
         */
        private boolean enabled = true;
        /**
         * 是否记录请求体
         */
        private boolean logRequestBody = true;
        /**
         * 是否记录响应体
         */
        private boolean logResponseBody = true;
        /**
         * 是否记录请求头
         */
        private boolean logHeaders = false;
        /**
         * 排除路径
         */
        private String[] excludePaths = {};
        /**
         * 敏感字段 (不记录)
         */
        private String[] sensitiveFields = {"password", "token", "secret"};
    }

    @Data
    public static class ServiceConfig {
        /**
         * 是否启用
         */
        private boolean enabled = true;
        /**
         * 是否记录参数
         */
        private boolean logParams = true;
        /**
         * 是否记录返回值
         */
        private boolean logResult = true;
    }

    @Data
    public static class DatabaseConfig {
        /**
         * 是否启用 MySQL 日志
         */
        private boolean mysqlEnabled = true;
        /**
         * 是否启用 Redis 日志
         */
        private boolean redisEnabled = true;
        /**
         * 是否启用 ES 日志
         */
        private boolean esEnabled = true;
        /**
         * 慢查询阈值 (毫秒)
         */
        private long slowQueryThreshold = 1000;
    }

    @Data
    public static class MqConfig {
        /**
         * 是否启用消费者日志
         */
        private boolean consumerEnabled = true;
        /**
         * 是否启用生产者日志
         */
        private boolean producerEnabled = true;
        /**
         * 是否记录消息体
         */
        private boolean logMessageBody = true;
    }

    @Data
    public static class DubboConfig {
        /**
         * 是否启用
         */
        private boolean enabled = true;
        /**
         * 是否记录参数
         */
        private boolean logArguments = true;
        /**
         * 是否记录结果
         */
        private boolean logResult = true;
    }

    @Data
    public static class DisruptorConfig {
        /**
         * Ring Buffer 大小 (必须是2的幂)
         */
        private int ringBufferSize = 1024 * 16;
        /**
         * 消费者线程数
         */
        private int consumerThreads = 1;
        /**
         * 是否等待所有日志写入完成后再关闭
         */
        private boolean waitForShutdown = true;
    }

    @Data
    public static class TraceConfig {
        /**
         * 是否启用链路追踪
         */
        private boolean enabled = true;
        /**
         * Zipkin 服务地址
         */
        private String zipkinEndpoint = "http://localhost:9411/api/v2/spans";
        /**
         * 采样率 (0.0 - 1.0)
         */
        private float sampleRate = 1.0f;
    }
}