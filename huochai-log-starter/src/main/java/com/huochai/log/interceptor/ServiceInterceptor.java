package com.huochai.log.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huochai.log.autoconfigure.LogProperties;
import com.huochai.log.collector.LogCollector;
import com.huochai.log.enums.LogType;
import com.huochai.log.model.LogEntry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * Service 层日志拦截器
 * 自动拦截所有 Service 方法
 */
@Aspect
public class ServiceInterceptor {
    
    private final LogProperties logProperties;
    private final LogCollector logCollector;
    private final ObjectMapper objectMapper;
    
    public ServiceInterceptor(LogProperties logProperties, LogCollector logCollector) {
        this.logProperties = logProperties;
        this.logCollector = logCollector;
        this.objectMapper = new ObjectMapper();
    }
    
    @Pointcut("@within(org.springframework.stereotype.Service)")
    public void servicePointcut() {
    }
    
    @Around("servicePointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        LogProperties.ServiceConfig config = logProperties.getService();
        
        long startTime = System.currentTimeMillis();
        LogEntry logEntry = LogEntry.builder()
                .logType(LogType.SERVICE.getCode())
                .level("INFO")
                .className(joinPoint.getTarget().getClass().getName())
                .methodName(joinPoint.getSignature().getName())
                .build();
        
        // 记录参数
        if (config.isLogParams()) {
            try {
                String args = objectMapper.writeValueAsString(joinPoint.getArgs());
                logEntry.setMessage("Args: " + args);
            } catch (Exception e) {
                logEntry.setMessage("Args: [serialization failed]");
            }
        }
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            logEntry.setDuration(duration);
            logEntry.setStatus("SUCCESS");
            
            // 记录返回值
            if (config.isLogResult() && result != null) {
                try {
                    String resultJson = objectMapper.writeValueAsString(result);
                    logEntry.setExtra(resultJson);
                } catch (Exception e) {
                    // 忽略序列化失败
                }
            }
            
            return result;
        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;
            logEntry.setDuration(duration);
            logEntry.setStatus("FAILURE");
            logEntry.setLevel("ERROR");
            logEntry.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            logCollector.collect(logEntry);
        }
    }
}