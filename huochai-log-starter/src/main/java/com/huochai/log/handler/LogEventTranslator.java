package com.huochai.log.handler;

import com.huochai.log.model.LogEntry;
import com.lmax.disruptor.EventTranslatorOneArg;

/**
 * Disruptor 事件翻译器
 * 将 LogEntry 转换为 LogEvent
 */
public class LogEventTranslator implements EventTranslatorOneArg<LogEvent, LogEntry> {
    
    @Override
    public void translateTo(LogEvent event, long sequence, LogEntry logEntry) {
        event.setLogEntry(logEntry);
    }
}