package com.huochai.log.trace;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Trace 上下文持有者
 * 管理链路追踪的 traceId 和 spanId
 */
public class TraceContextHolder {

    private static final String TRACE_ID_KEY = "traceId";
    private static final String SPAN_ID_KEY = "spanId";
    private static final String PARENT_SPAN_ID_KEY = "parentSpanId";

    private static final ThreadLocal<String> traceIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> spanIdHolder = new ThreadLocal<>();
    private static final ThreadLocal<String> parentSpanIdHolder = new ThreadLocal<>();

    /**
     * 生成新的 TraceId
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成新的 SpanId
     */
    public static String generateSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 设置 Trace 上下文
     */
    public static void setContext(String traceId, String spanId, String parentSpanId) {
        traceIdHolder.set(traceId);
        spanIdHolder.set(spanId);
        parentSpanIdHolder.set(parentSpanId);

        // 同步到 MDC，以便 Logback 使用
        MDC.put(TRACE_ID_KEY, traceId);
        MDC.put(SPAN_ID_KEY, spanId);
        MDC.put(PARENT_SPAN_ID_KEY, parentSpanId);
    }

    /**
     * 初始化新的 Trace 上下文
     */
    public static void initContext() {
        String traceId = generateTraceId();
        String spanId = generateSpanId();
        setContext(traceId, spanId, null);
    }

    /**
     * 清除 Trace 上下文
     */
    public static void clearContext() {
        traceIdHolder.remove();
        spanIdHolder.remove();
        parentSpanIdHolder.remove();

        MDC.remove(TRACE_ID_KEY);
        MDC.remove(SPAN_ID_KEY);
        MDC.remove(PARENT_SPAN_ID_KEY);
    }

    /**
     * 获取当前 TraceId
     */
    public static String getTraceId() {
        String traceId = traceIdHolder.get();
        if (traceId == null) {
            traceId = MDC.get(TRACE_ID_KEY);
        }
        return traceId;
    }

    /**
     * 获取当前 SpanId
     */
    public static String getSpanId() {
        String spanId = spanIdHolder.get();
        if (spanId == null) {
            spanId = MDC.get(SPAN_ID_KEY);
        }
        return spanId;
    }

    /**
     * 获取父 SpanId
     */
    public static String getParentSpanId() {
        String parentSpanId = parentSpanIdHolder.get();
        if (parentSpanId == null) {
            parentSpanId = MDC.get(PARENT_SPAN_ID_KEY);
        }
        return parentSpanId;
    }

    /**
     * 设置 TraceId
     */
    public static void setTraceId(String traceId) {
        traceIdHolder.set(traceId);
        MDC.put(TRACE_ID_KEY, traceId);
    }

    /**
     * 设置 SpanId
     */
    public static void setSpanId(String spanId) {
        spanIdHolder.set(spanId);
        MDC.put(SPAN_ID_KEY, spanId);
    }

    /**
     * 设置父 SpanId
     */
    public static void setParentSpanId(String parentSpanId) {
        parentSpanIdHolder.set(parentSpanId);
        MDC.put(PARENT_SPAN_ID_KEY, parentSpanId);
    }

    /**
     * 创建子 Span
     */
    public static String createChildSpan() {
        String currentSpanId = getSpanId();
        String newSpanId = generateSpanId();
        setParentSpanId(currentSpanId);
        setSpanId(newSpanId);
        return newSpanId;
    }
}