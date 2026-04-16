package com.huochai.log.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huochai.log.autoconfigure.LogProperties;
import com.huochai.log.collector.LogCollector;
import com.huochai.log.enums.LogType;
import com.huochai.log.model.RedisLogEntry;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * Redis 操作日志拦截器
 * 通过包装 RedisTemplate 实现日志记录
 */
public class RedisLogInterceptor {

    private final LogProperties logProperties;
    private final LogCollector logCollector;
    private final ObjectMapper objectMapper;

    public RedisLogInterceptor(LogProperties logProperties, LogCollector logCollector) {
        this.logProperties = logProperties;
        this.logCollector = logCollector;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 记录 Redis 操作日志
     */
    public void logRedisOperation(String command, String key, Object value, long duration, boolean success, String error) {
        LogProperties.DatabaseConfig config = logProperties.getDatabase();
        if (!config.isRedisEnabled()) {
            return;
        }

        RedisLogEntry logEntry = RedisLogEntry.builder()
                .logType(LogType.REDIS.getCode())
                .level(success ? "INFO" : "ERROR")
                .command(command)
                .key(truncate(key, 500))
                .duration(duration)
                .status(success ? "SUCCESS" : "FAILURE")
                .errorMessage(error)
                .build();

        if (value != null) {
            try {
                logEntry.setValue(truncate(objectMapper.writeValueAsString(value), 1000));
            } catch (Exception e) {
                logEntry.setValue(truncate(value.toString(), 1000));
            }
        }

        logCollector.collect(logEntry);
    }

    /**
     * 创建带日志记录的 RedisTemplate 包装器
     */
    public <K, V> RedisTemplate<K, V> wrapRedisTemplate(RedisTemplate<K, V> redisTemplate) {
        // 返回一个代理对象，拦截所有方法调用
        return new LoggingRedisTemplate<>(redisTemplate, this);
    }

    private String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }

    /**
     * 带 Redis 日志记录的 RedisTemplate 实现
     */
    public static class LoggingRedisTemplate<K, V> extends RedisTemplate<K, V> {

        private final RedisTemplate<K, V> delegate;
        private final RedisLogInterceptor logInterceptor;

        public LoggingRedisTemplate(RedisTemplate<K, V> delegate, RedisLogInterceptor logInterceptor) {
            this.delegate = delegate;
            this.logInterceptor = logInterceptor;
        }

        @Override
        public ValueOperations<K, V> opsForValue() {
            // 委托给原始模板
            return super.opsForValue();
        }

        // 可以覆盖更多方法来添加日志记录
        // 这里只是一个示例框架
    }
}