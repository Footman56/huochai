package com.huochai.log.interceptor;

import com.huochai.log.autoconfigure.LogProperties;
import com.huochai.log.collector.LogCollector;
import com.huochai.log.context.LogEntry;
import com.huochai.log.enums.LogType;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * 定时任务日志拦截器
 * 自动拦截所有 @Scheduled 方法
 */
@Aspect
public class ScheduledInterceptor {

    private final LogProperties logProperties;
    private final LogCollector logCollector;

    public ScheduledInterceptor(LogProperties logProperties, LogCollector logCollector) {
        this.logProperties = logProperties;
        this.logCollector = logCollector;
    }

    @Pointcut("@annotation(org.springframework.scheduling.annotation.Scheduled)")
    public void scheduledPointcut() {
    }

    @Around("scheduledPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        LogEntry logEntry = LogEntry.builder()
                .logType(LogType.SCHEDULED.getCode())
                .level("INFO")
                .className(joinPoint.getTarget().getClass().getName())
                .methodName(joinPoint.getSignature().getName())
                .build();

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            logEntry.setDuration(duration);
            logEntry.setStatus("SUCCESS");

            logCollector.collect(logEntry);
            return result;
        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;
            logEntry.setDuration(duration);
            logEntry.setStatus("FAILURE");
            logEntry.setLevel("ERROR");
            logEntry.setErrorMessage(e.getMessage());

            logCollector.collect(logEntry);
            throw e;
        }
    }
}