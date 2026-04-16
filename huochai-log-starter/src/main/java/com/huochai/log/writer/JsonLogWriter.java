package com.huochai.log.writer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import com.huochai.log.context.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * JSON 日志写入器
 * 将日志以 JSON 格式写入本地文件（由 Logback 管理）
 */
@Component
public class JsonLogWriter {

    private static final Logger log = LoggerFactory.getLogger("huochai-json-log");

    private final ObjectMapper objectMapper;

    public JsonLogWriter() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * 写入日志
     *
     * @param logEntry 日志实体
     */
    public void write(LogEntry logEntry) {
        try {
            String json = objectMapper.writeValueAsString(logEntry);
            log.info(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize log entry: {}", e.getMessage());
        }
    }

    /**
     * 写入错误日志
     *
     * @param logEntry 日志实体
     */
    public void writeError(LogEntry logEntry) {
        try {
            String json = objectMapper.writeValueAsString(logEntry);
            log.error(json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize error log entry: {}", e.getMessage());
        }
    }
}