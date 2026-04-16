package com.huochai.log.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huochai.log.autoconfigure.LogProperties;
import com.huochai.log.collector.LogCollector;
import com.huochai.log.enums.LogType;
import com.huochai.log.model.ControllerLogEntry;
import com.huochai.log.trace.TraceContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller 层日志拦截器
 * 自动拦截所有 Controller 方法
 */
@Aspect
public class ControllerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ControllerInterceptor.class);

    private final LogProperties logProperties;
    private final LogCollector logCollector;
    private final ObjectMapper objectMapper;

    public ControllerInterceptor(LogProperties logProperties, LogCollector logCollector) {
        this.logProperties = logProperties;
        this.logCollector = logCollector;
        this.objectMapper = new ObjectMapper();
    }

    @Pointcut("@within(org.springframework.web.bind.annotation.RestController) || " +
            "@within(org.springframework.stereotype.Controller)")
    public void controllerPointcut() {
    }

    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        LogProperties.ControllerConfig config = logProperties.getController();

        // 检查排除路径
        HttpServletRequest request = getRequest();
        if (request != null && isExcludedPath(request.getRequestURI())) {
            return joinPoint.proceed();
        }

        long startTime = System.currentTimeMillis();
        ControllerLogEntry logEntry = ControllerLogEntry.builder()
                .logType(LogType.CONTROLLER.getCode())
                .level("INFO")
                .className(joinPoint.getTarget().getClass().getName())
                .methodName(joinPoint.getSignature().getName())
                .build();

        // 记录请求信息
        if (request != null) {
            logEntry.setHttpMethod(request.getMethod());
            logEntry.setPath(request.getRequestURI());
            logEntry.setClientIp(getClientIp(request));
            logEntry.setUserAgent(request.getHeader("User-Agent"));
            logEntry.setQueryParams(request.getQueryString());

            // 记录请求体
            if (config.isLogRequestBody() && request instanceof ContentCachingRequestWrapper) {
                ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) request;
                byte[] buf = wrapper.getContentAsByteArray();
                if (buf.length > 0) {
                    String requestBody = new String(buf, StandardCharsets.UTF_8);
                    logEntry.setRequestBody(maskSensitiveFields(requestBody));
                }
            }

            // 记录请求头
            if (config.isLogHeaders()) {
                logEntry.setRequestHeaders(getHeadersAsString(request));
            }
        }

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;

            logEntry.setDuration(duration);
            logEntry.setStatus("SUCCESS");

            // 记录响应信息
            HttpServletResponse response = getResponse();
            if (response != null) {
                logEntry.setResponseStatus(response.getStatus());

                if (config.isLogResponseBody() && response instanceof ContentCachingResponseWrapper) {
                    ContentCachingResponseWrapper wrapper = (ContentCachingResponseWrapper) response;
                    byte[] buf = wrapper.getContentAsByteArray();
                    if (buf.length > 0) {
                        String responseBody = new String(buf, StandardCharsets.UTF_8);
                        logEntry.setResponseBody(maskSensitiveFields(responseBody));
                        wrapper.copyBodyToResponse();
                    }
                }
            }

            return result;
        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;
            logEntry.setDuration(duration);
            logEntry.setStatus("FAILURE");
            logEntry.setLevel("ERROR");
            logEntry.setErrorMessage(e.getMessage());
            logEntry.setStackTrace(getStackTraceAsString(e));
            throw e;
        } finally {
            logCollector.collect(logEntry);
        }
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private HttpServletResponse getResponse() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getResponse() : null;
    }

    private boolean isExcludedPath(String path) {
        String[] excludePaths = logProperties.getController().getExcludePaths();
        if (excludePaths == null || excludePaths.length == 0) {
            return false;
        }
        return Arrays.stream(excludePaths).anyMatch(path::startsWith);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String getHeadersAsString(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headers.put(name, request.getHeader(name));
        }
        try {
            return objectMapper.writeValueAsString(headers);
        } catch (Exception e) {
            return headers.toString();
        }
    }

    private String maskSensitiveFields(String content) {
        String[] sensitiveFields = logProperties.getController().getSensitiveFields();
        if (sensitiveFields == null || sensitiveFields.length == 0) {
            return content;
        }
        String result = content;
        for (String field : sensitiveFields) {
            result = result.replaceAll("(" + field + "\":\\s*\")([^\"]*)(\")", "$1******$3");
        }
        return result;
    }

    private String getStackTraceAsString(Throwable e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString()).append("\n");
            if (sb.length() > 2000) {
                sb.append("...");
                break;
            }
        }
        return sb.toString();
    }
}