package com.huochai.log.context;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.huochai.log.enums.LogType;
import lombok.Data;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * 统一日志实体 - 基础日志模型
 * 所有日志类型必须包含这些公共字段
 */
@Data
@SuperBuilder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogEntry {
    
    /** 日志时间戳 (ISO 8601 格式) */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;
    
    /** 链路追踪ID */
    private String traceId;
    
    /** Span ID */
    private String spanId;
    
    /** 父 Span ID */
    private String parentSpanId;
    
    /** 日志类型 */
    private String logType;
    
    /** 日志级别 (INFO/WARN/ERROR) */
    private String level;
    
    /** 服务名称 */
    private String serviceName;
    
    /** 服务实例IP */
    private String host;
    
    /** 应用端口 */
    private Integer port;
    
    /** 线程名 */
    private String threadName;
    
    /** 类名 */
    private String className;
    
    /** 方法名 */
    private String methodName;
    
    /** 日志消息 */
    private String message;
    
    /** 执行耗时 (毫秒) */
    private Long duration;
    
    /** 结果状态 (SUCCESS/FAILURE) */
    private String status;
    
    /** 错误信息 */
    private String errorMessage;
    
    /** 错误堆栈 */
    private String stackTrace;
    
    /** 扩展字段 (JSON格式) */
    private Object extra;
    
    /**
     * 创建基础日志条目
     */
    public static LogEntry create(LogType logType, String level) {
        return LogEntry.builder()
                .timestamp(Instant.now())
                .logType(logType.getCode())
                .level(level)
                .build();
    }
}