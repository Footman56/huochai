package com.huochai.log.model;

import com.huochai.log.context.LogEntry;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * MQ 消息日志实体
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class MqLogEntry extends LogEntry {

    /**
     * 消息类型 (producer/consumer)
     */
    private String mqType;

    /**
     * 交换机
     */
    private String exchange;

    /**
     * 路由键
     */
    private String routingKey;

    /**
     * 队列名
     */
    private String queue;

    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 消息体
     */
    private String messageBody;

    /**
     * 消息头
     */
    private String messageHeaders;

    /**
     * 重试次数
     */
    private Integer retryCount;
}