package com.huochai.log.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huochai.log.autoconfigure.LogProperties;
import com.huochai.log.collector.LogCollector;
import com.huochai.log.enums.LogType;
import com.huochai.log.model.MqLogEntry;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * MQ 生产者日志拦截器
 * 通过 RabbitTemplate 回调机制记录消息发送日志
 */
public class MqProducerInterceptor implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnCallback {
    
    private final LogProperties logProperties;
    private final LogCollector logCollector;
    private final ObjectMapper objectMapper;
    
    public MqProducerInterceptor(LogProperties logProperties, LogCollector logCollector) {
        this.logProperties = logProperties;
        this.logCollector = logCollector;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 消息确认回调
     */
    @Override
    public void confirm(org.springframework.amqp.rabbit.support.CorrelationData correlationData, boolean ack, String cause) {
        LogProperties.MqConfig config = logProperties.getMq();
        if (!config.isProducerEnabled()) {
            return;
        }
        
        MqLogEntry logEntry = MqLogEntry.builder()
                .logType(LogType.MQ_PRODUCER.getCode())
                .level(ack ? "INFO" : "ERROR")
                .mqType("producer")
                .messageId(correlationData != null ? correlationData.getId() : null)
                .status(ack ? "SUCCESS" : "FAILURE")
                .errorMessage(ack ? null : cause)
                .build();
        
        logCollector.collect(logEntry);
    }
    
    /**
     * 消息退回回调
     */
    @Override
    public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        LogProperties.MqConfig config = logProperties.getMq();
        if (!config.isProducerEnabled()) {
            return;
        }
        
        MqLogEntry logEntry = MqLogEntry.builder()
                .logType(LogType.MQ_PRODUCER.getCode())
                .level("WARN")
                .mqType("producer")
                .exchange(exchange)
                .routingKey(routingKey)
                .status("RETURNED")
                .errorMessage(replyText)
                .build();
        
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