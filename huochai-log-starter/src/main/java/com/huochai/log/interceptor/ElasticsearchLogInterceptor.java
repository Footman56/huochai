package com.huochai.log.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huochai.log.autoconfigure.LogProperties;
import com.huochai.log.collector.LogCollector;
import com.huochai.log.enums.LogType;
import com.huochai.log.model.ElasticsearchLogEntry;

/**
 * Elasticsearch 操作日志拦截器
 * 通过包装 ES Client 实现日志记录
 */
public class ElasticsearchLogInterceptor {
    
    private final LogProperties logProperties;
    private final LogCollector logCollector;
    private final ObjectMapper objectMapper;
    
    public ElasticsearchLogInterceptor(LogProperties logProperties, LogCollector logCollector) {
        this.logProperties = logProperties;
        this.logCollector = logCollector;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * 记录 ES 操作日志
     */
    public void logEsOperation(String operation, String index, String docId, 
                               String query, Object requestBody, long duration,
                               Integer responseStatus, Integer hitCount, 
                               boolean success, String error) {
        LogProperties.DatabaseConfig config = logProperties.getDatabase();
        if (!config.isEsEnabled()) {
            return;
        }
        
        ElasticsearchLogEntry logEntry = ElasticsearchLogEntry.builder()
                .logType(LogType.ES.getCode())
                .level(success ? "INFO" : "ERROR")
                .operation(operation)
                .index(index)
                .docId(docId)
                .query(truncate(query, 1000))
                .duration(duration)
                .responseStatus(responseStatus)
                .hitCount(hitCount)
                .status(success ? "SUCCESS" : "FAILURE")
                .errorMessage(error)
                .build();
        
        if (requestBody != null) {
            try {
                logEntry.setRequestBody(truncate(objectMapper.writeValueAsString(requestBody), 2000));
            } catch (Exception e) {
                logEntry.setRequestBody(truncate(requestBody.toString(), 2000));
            }
        }
        
        logCollector.collect(logEntry);
    }
    
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }
}