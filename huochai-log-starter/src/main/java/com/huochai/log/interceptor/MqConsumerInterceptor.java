package com.huochai.log.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huochai.log.autoconfigure.LogProperties;
import com.huochai.log.collector.LogCollector;
import com.huochai.log.enums.LogType;
import com.huochai.log.model.MqLogEntry;
import com.huochai.log.trace.TraceContextHolder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;

import com.rabbitmq.client.Channel;

/**
 * MQ 消费者日志拦截器
 * 自动记录消息消费日志
 */
public class MqConsumerInterceptor {
    
    private final LogProperties logProperties;
    private final LogCollector logCollector;
    private final ObjectMapper objectMapper;
    
    public MqConsumerInterceptor(LogProperties logProperties, LogCollector logCollector) {
        this.logProperties = logProperties;
        this.logCollector = logCollector;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 记录消息消费日志
     */
    public void logConsume(Message message, String queue, long duration, boolean success, String error) {
        LogProperties.MqConfig config = logProperties.getMq();
        if (!config.isConsumerEnabled()) {
            return;
        }
        
        MessageProperties props = message.getMessageProperties();
        
        MqLogEntry logEntry = MqLogEntry.builder()
                .logType(LogType.MQ_CONSUMER.getCode())
                .level(success ? "INFO" : "ERROR")
                .mqType("consumer")
                .queue(queue)
                .exchange(props.getReceivedExchange())
                .routingKey(props.getReceivedRoutingKey())
                .messageId(props.getMessageId())
                .duration(duration)
                .status(success ? "SUCCESS" : "FAILURE")
                .errorMessage(error)
                .retryCount(props.getRedelivered() ? 1 : 0)
                .build();
        
        // 记录消息体
        if (config.isLogMessageBody()) {
            try {
                String body = new String(message.getBody());
                logEntry.setMessageBody(truncate(body, 2000));
            } catch (Exception e) {
                logEntry.setMessageBody("[serialization failed]");
            }
        }
        
        logCollector.collect(logEntry);
    }
    
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }
}