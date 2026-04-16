package com.huochai.log.handler;

import com.huochai.log.autoconfigure.LogProperties;
import com.huochai.log.writer.JsonLogWriter;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Disruptor 日志队列配置
 */
@Component
public class LogDisruptorConfig {
    
    private static final Logger log = LoggerFactory.getLogger(LogDisruptorConfig.class);
    
    private final LogProperties logProperties;
    private final JsonLogWriter jsonLogWriter;
    
    private Disruptor<LogEvent> disruptor;
    
    public LogDisruptorConfig(LogProperties logProperties, JsonLogWriter jsonLogWriter) {
        this.logProperties = logProperties;
        this.jsonLogWriter = jsonLogWriter;
    }
    
    @PostConstruct
    public void init() {
        LogProperties.DisruptorConfig config = logProperties.getDisruptor();
        
        // 创建事件工厂
        LogEventFactory eventFactory = new LogEventFactory();
        
        // 创建线程工厂
        ThreadFactory threadFactory = new LogThreadFactory();
        
        // 创建 Disruptor
        disruptor = new Disruptor<>(
                eventFactory,
                config.getRingBufferSize(),
                threadFactory,
                ProducerType.MULTI,
                new BlockingWaitStrategy()
        );
        
        // 设置事件处理器
        LogEventHandler[] handlers = new LogEventHandler[config.getConsumerThreads()];
        for (int i = 0; i < config.getConsumerThreads(); i++) {
            handlers[i] = new LogEventHandler(jsonLogWriter);
        }
        disruptor.handleEventsWith(handlers);
        
        // 启动 Disruptor
        disruptor.start();
        log.info("Log Disruptor started with ringBufferSize={}, consumerThreads={}",
                config.getRingBufferSize(), config.getConsumerThreads());
    }
    
    @PreDestroy
    public void shutdown() {
        if (disruptor != null) {
            LogProperties.DisruptorConfig config = logProperties.getDisruptor();
            if (config.isWaitForShutdown()) {
                disruptor.shutdown();
                log.info("Log Disruptor shutdown completed");
            } else {
                disruptor.halt();
                log.info("Log Disruptor halted");
            }
        }
    }
    
    public Disruptor<LogEvent> getDisruptor() {
        return disruptor;
    }
    
    /**
     * 日志线程工厂
     */
    private static class LogThreadFactory implements ThreadFactory {
        private static final AtomicInteger counter = new AtomicInteger(1);
        
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "log-disruptor-" + counter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}