package com.huochai.log.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huochai.log.collector.LogCollector;
import com.huochai.log.enums.LogType;
import com.huochai.log.trace.TraceContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 日志记录工具类
 * 提供便捷的业务日志记录方法
 */
@Component
public class LogHelper {
    
    private static final Logger log = LoggerFactory.getLogger(LogHelper.class);
    
    private static LogCollector logCollector;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    public void setLogCollector(LogCollector logCollector) {
        LogHelper.logCollector = logCollector;
    }
    
    /**
     * 记录业务日志
     */
    public static void business(String message) {
        business(message, null);
    }
    
    /**
     * 记录业务日志（带额外信息）
     */
    public static void business(String message, Object extra) {
        if (logCollector == null) {
            log.info(message);
            return;
        }
        
        LogEntry logEntry = LogEntry.builder()
                .timestamp(Instant.now())
                .logType(LogType.BUSINESS.getCode())
                .level("INFO")
                .traceId(TraceContextHolder.getTraceId())
                .spanId(TraceContextHolder.getSpanId())
                .message(message)
                .extra(extra)
                .status("SUCCESS")
                .build();
        
        logCollector.collect(logEntry);
    }
    
    /**
     * 记录异常日志
     */
    public static void error(String message, Throwable e) {
        if (logCollector == null) {
            log.error(message, e);
            return;
        }
        
        LogEntry logEntry = LogEntry.builder()
                .timestamp(Instant.now())
                .logType(LogType.EXCEPTION.getCode())
                .level("ERROR")
                .traceId(TraceContextHolder.getTraceId())
                .spanId(TraceContextHolder.getSpanId())
                .message(message)
                .errorMessage(e.getMessage())
                .stackTrace(getStackTrace(e))
                .status("FAILURE")
                .build();
        
        logCollector.collect(logEntry);
    }
    
    /**
     * 记录警告日志
     */
    public static void warn(String message) {
        warn(message, null);
    }
    
    /**
     * 记录警告日志（带额外信息）
     */
    public static void warn(String message, Object extra) {
        if (logCollector == null) {
            log.warn(message);
            return;
        }
        
        LogEntry logEntry = LogEntry.builder()
                .timestamp(Instant.now())
                .logType(LogType.BUSINESS.getCode())
                .level("WARN")
                .traceId(TraceContextHolder.getTraceId())
                .spanId(TraceContextHolder.getSpanId())
                .message(message)
                .extra(extra)
                .status("WARNING")
                .build();
        
        logCollector.collect(logEntry);
    }
    
    /**
     * 记录操作日志
     */
    public static void operation(String operation, String target, String result) {
        LogEntry logEntry = LogEntry.builder()
                .timestamp(Instant.now())
                .logType(LogType.BUSINESS.getCode())
                .level("INFO")
                .traceId(TraceContextHolder.getTraceId())
                .spanId(TraceContextHolder.getSpanId())
                .message(String.format("Operation: %s, Target: %s, Result: %s", operation, target, result))
                .status("SUCCESS")
                .build();
        
        logCollector.collect(logEntry);
    }
    
    private static String getStackTrace(Throwable e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString()).append("\n");
            if (sb.length() > 3000) {
                sb.append("...");
                break;
            }
        }
        return sb.toString();
    }
}