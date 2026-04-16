package com.huochai.common.trace;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.huochai.common.enums.AuthConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * TraceId 过滤器
 * 为每个请求生成唯一的链路追踪ID
 *
 * @author huochai
 */
@Slf4j
@Component
@Order(1)
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 从请求头获取 TraceId，如果没有则生成新的
            String traceId = request.getHeader(AuthConstants.TRACE_ID_HEADER);
            if (StrUtil.isBlank(traceId)) {
                traceId = IdUtil.fastSimpleUUID();
            }

            // 设置到 MDC
            MDC.put("traceId", traceId);
            
            // 设置响应头
            response.setHeader(AuthConstants.TRACE_ID_HEADER, traceId);

            log.debug("请求开始: traceId={}, uri={}", traceId, request.getRequestURI());

            filterChain.doFilter(request, response);

        } finally {
            // 清除 MDC
            MDC.clear();
        }
    }
}