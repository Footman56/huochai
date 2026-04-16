package com.huochai.log.trace;

import com.huochai.log.autoconfigure.LogProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Trace 过滤器
 * 自动为 HTTP 请求生成或继承 TraceId
 */
public class TraceFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String SPAN_ID_HEADER = "X-Span-Id";
    private static final String PARENT_SPAN_ID_HEADER = "X-Parent-Span-Id";

    private final LogProperties logProperties;

    public TraceFilter(LogProperties logProperties) {
        this.logProperties = logProperties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 尝试从请求头获取 TraceId
            String traceId = request.getHeader(TRACE_ID_HEADER);
            String parentSpanId = request.getHeader(SPAN_ID_HEADER);

            if (traceId == null || traceId.isEmpty()) {
                // 生成新的 TraceId
                traceId = TraceContextHolder.generateTraceId();
            }

            // 生成新的 SpanId
            String spanId = TraceContextHolder.generateSpanId();

            // 设置 Trace 上下文
            TraceContextHolder.setContext(traceId, spanId, parentSpanId);

            // 设置响应头
            response.setHeader(TRACE_ID_HEADER, traceId);
            response.setHeader(SPAN_ID_HEADER, spanId);

            filterChain.doFilter(request, response);
        } finally {
            // 清除 Trace 上下文
            TraceContextHolder.clearContext();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !logProperties.getTrace().isEnabled();
    }
}