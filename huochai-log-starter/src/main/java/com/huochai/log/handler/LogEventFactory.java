package com.huochai.log.handler;

import com.lmax.disruptor.EventFactory;

/**
 * Disruptor 事件工厂
 */
public class LogEventFactory implements EventFactory<LogEvent> {
    
    @Override
    public LogEvent newInstance() {
        return new LogEvent();
    }
}