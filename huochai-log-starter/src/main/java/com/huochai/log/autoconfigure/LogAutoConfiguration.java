package com.huochai.log.autoconfigure;

import com.huochai.log.collector.LogCollector;
import com.huochai.log.collector.LogCollectorImpl;
import com.huochai.log.handler.LogEventHandler;
import com.huochai.log.interceptor.*;
import com.huochai.log.trace.TraceContextHolder;
import com.huochai.log.trace.TraceFilter;
import com.huochai.log.writer.JsonLogWriter;
import com.lmax.disruptor.dsl.Disruptor;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * 日志模块自动配置类
 */
@AutoConfiguration
@EnableConfigurationProperties(LogProperties.class)
@ConditionalOnProperty(prefix = "huochai.log", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LogAutoConfiguration {

    @Autowired
    private LogProperties logProperties;

    /**
     * JSON 日志写入器
     */
    @Bean
    @ConditionalOnMissingBean
    public JsonLogWriter jsonLogWriter() {
        return new JsonLogWriter();
    }

    /**
     * 日志收集器
     */
    @Bean
    @ConditionalOnMissingBean
    public LogCollector logCollector() {
        return new LogCollectorImpl(logProperties);
    }

    /**
     * Trace 上下文持有者
     */
    @Bean
    @ConditionalOnMissingBean
    public TraceContextHolder traceContextHolder() {
        return new TraceContextHolder();
    }

    /**
     * Trace 过滤器
     */
    @Bean
    @ConditionalOnWebApplication
    @ConditionalOnProperty(prefix = "huochai.log.trace", name = "enabled", havingValue = "true", matchIfMissing = true)
    public TraceFilter traceFilter() {
        return new TraceFilter(logProperties);
    }

    /**
     * Controller 拦截器配置
     */
    @Configuration
    @ConditionalOnWebApplication
    @ConditionalOnProperty(prefix = "huochai.log.controller", name = "enabled", havingValue = "true", matchIfMissing = true)
    public static class ControllerInterceptorConfig {
        
        @Bean
        public ControllerInterceptor controllerInterceptor(LogProperties logProperties, LogCollector logCollector) {
            return new ControllerInterceptor(logProperties, logCollector);
        }
    }

    /**
     * Service 拦截器配置
     */
    @Configuration
    @ConditionalOnProperty(prefix = "huochai.log.service", name = "enabled", havingValue = "true", matchIfMissing = true)
    public static class ServiceInterceptorConfig {
        
        @Bean
        public ServiceInterceptor serviceInterceptor(LogProperties logProperties, LogCollector logCollector) {
            return new ServiceInterceptor(logProperties, logCollector);
        }
    }

    /**
     * Redis 拦截器配置
     */
    @Configuration
    @ConditionalOnClass(RedisTemplate.class)
    @ConditionalOnProperty(prefix = "huochai.log.database", name = "redis-enabled", havingValue = "true", matchIfMissing = true)
    public static class RedisInterceptorConfig {
        
        @Bean
        public RedisLogInterceptor redisLogInterceptor(LogProperties logProperties, LogCollector logCollector) {
            return new RedisLogInterceptor(logProperties, logCollector);
        }
    }

    /**
     * RabbitMQ 拦截器配置
     */
    @Configuration
    @ConditionalOnClass({ConnectionFactory.class, RabbitTemplate.class})
    @ConditionalOnProperty(prefix = "huochai.log.mq", name = "consumer-enabled", havingValue = "true", matchIfMissing = true)
    public static class MqInterceptorConfig {
        
        @Bean
        public MqConsumerInterceptor mqConsumerInterceptor(LogProperties logProperties, LogCollector logCollector) {
            return new MqConsumerInterceptor(logProperties, logCollector);
        }
        
        @Bean
        public MqProducerInterceptor mqProducerInterceptor(LogProperties logProperties, LogCollector logCollector) {
            return new MqProducerInterceptor(logProperties, logCollector);
        }
    }
}