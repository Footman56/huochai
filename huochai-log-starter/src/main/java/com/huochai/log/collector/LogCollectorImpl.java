package com.huochai.log.collector;

import com.huochai.log.autoconfigure.LogProperties;
import com.huochai.log.handler.LogDisruptorConfig;
import com.huochai.log.handler.LogEventTranslator;
import com.huochai.log.model.LogEntry;
import com.huochai.log.trace.TraceContextHolder;
import com.lmax.disruptor.RingBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;

/**
 * 日志收集器实现
 */
public class LogCollectorImpl implements LogCollector {
    
    private static final Logger log = LoggerFactory.getLogger(LogCollectorImpl.class);
    
    private final LogProperties logProperties;
    
    @Autowired
    private LogDisruptorConfig disruptorConfig;
    
    private final LogEventTranslator translator = new LogEventTranslator();
    
    private String host;
    
    public LogCollectorImpl(LogProperties logProperties) {
        this.logProperties = logProperties;
        try {
            this.host = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            this.host = "unknown";
        }
    }
    
    @Override
    public void collect(LogEntry logEntry) {
        enrichLogEntry(logEntry);
        collectAsync(logEntry);
    }
    
    @Override
    public void collectAsync(LogEntry logEntry) {
        enrichLogEntry(logEntry);
        
        try {
            RingBuffer<com.huochai.log.handler.LogEvent> ringBuffer = disruptorConfig.getDisruptor().getRingBuffer();
            ringBuffer.publishEvent(translator, logEntry);
        } catch (Exception e) {
            // 队列写入失败，降级为同步写入
            log.warn("Failed to publish log to disruptor, fallback to sync write: {}", e.getMessage());
            collectSync(logEntry);
        }
    }
    
    /**
     * 同步收集日志（降级方案）
     */
    private void collectSync(LogEntry logEntry) {
        // 直接使用 SLF4J 输出，由 Logback 处理
        Logger targetLog = LoggerFactory.getLogger("huochai-log");
        targetLog.info("{}", logEntry);
    }
    
    /**
     * 丰富日志实体
     */
    private void enrichLogEntry(LogEntry logEntry) {
        if (logEntry.getTimestamp() == null) {
            logEntry.setTimestamp(Instant.now());
        }
        
        if (logEntry.getServiceName() == null) {
            logEntry.setServiceName(logProperties.getServiceName());
        }
        
        if (logEntry.getHost() == null) {
            logEntry.setHost(host);
        }
        
        if (logEntry.getTraceId() == null) {
            logEntry.setTraceId(TraceContextHolder.getTraceId());
        }
        
        if (logEntry.getSpanId() == null) {
            logEntry.setSpanId(TraceContextHolder.getSpanId());
        }
        
        if (logEntry.getParentSpanId() == null) {
            logEntry.setParentSpanId(TraceContextHolder.getParentSpanId());
        }
        
        logEntry.setThreadName(Thread.currentThread().getName());
    }
}