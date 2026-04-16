package com.huochai.log.collector;


import com.huochai.log.context.LogEntry;

/**
 * 日志收集器接口
 */
public interface LogCollector {
    
    /**
     * 收集日志
     * @param logEntry 日志实体
     */
    void collect(LogEntry logEntry);
    
    /**
     * 异步收集日志（写入 Disruptor 队列）
     * @param logEntry 日志实体
     */
    void collectAsync(LogEntry logEntry);
}