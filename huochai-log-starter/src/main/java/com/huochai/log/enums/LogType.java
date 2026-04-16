package com.huochai.log.enums;

/**
 * 日志类型枚举
 */
public enum LogType {
    
    /** Controller 层日志 */
    CONTROLLER("CONTROLLER", "HTTP请求日志"),
    
    /** Service 层日志 */
    SERVICE("SERVICE", "服务层日志"),
    
    /** Dubbo RPC 日志 */
    DUBBO("DUBBO", "Dubbo RPC日志"),
    
    /** MQ 消费者日志 */
    MQ_CONSUMER("MQ_CONSUMER", "消息消费日志"),
    
    /** MQ 生产者日志 */
    MQ_PRODUCER("MQ_PRODUCER", "消息生产日志"),
    
    /** MySQL 操作日志 */
    MYSQL("MYSQL", "MySQL操作日志"),
    
    /** Redis 操作日志 */
    REDIS("REDIS", "Redis操作日志"),
    
    /** Elasticsearch 操作日志 */
    ES("ES", "Elasticsearch操作日志"),
    
    /** 定时任务日志 */
    SCHEDULED("SCHEDULED", "定时任务日志"),
    
    /** 异常日志 */
    EXCEPTION("EXCEPTION", "异常日志"),
    
    /** 业务日志 */
    BUSINESS("BUSINESS", "业务日志");
    
    private final String code;
    private final String description;
    
    LogType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
}