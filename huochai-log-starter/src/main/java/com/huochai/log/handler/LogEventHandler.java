package com.huochai.log.handler;

import com.huochai.log.model.LogEntry;
import com.huochai.log.writer.JsonLogWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Disruptor 事件处理器
 * 负责将日志事件写入本地文件
 */
public class LogEventHandler implements com.lmax.disruptor.EventHandler<LogEvent> {
    
    private static final Logger log = LoggerFactory.getLogger(LogEventHandler.class);
    
    private final JsonLogWriter jsonLogWriter;
    
    public LogEventHandler(JsonLogWriter jsonLogWriter) {
        this.jsonLogWriter = jsonLogWriter;
    }
    
    @Override
    public void onEvent(LogEvent event, long sequence, boolean endOfBatch) {
        try {
            LogEntry logEntry = event.getLogEntry();
            if (logEntry != null) {
                jsonLogWriter.write(logEntry);
            }
        } catch (Exception e) {
            log.error("Failed to write log entry: {}", e.getMessage(), e);
        } finally {
            // 清空事件数据，便于GC
            event.clear();
        }
    }
}