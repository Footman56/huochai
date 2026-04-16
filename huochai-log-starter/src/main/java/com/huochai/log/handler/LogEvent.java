package com.huochai.log.handler;

import com.huochai.log.model.LogEntry;
import lombok.Data;

/**
 * Disruptor 日志事件
 */
@Data
public class LogEvent {
    
    private LogEntry logEntry;
    
    public void clear() {
        this.logEntry = null;
    }
}