package com.huochai.common.log;

import com.huochai.auth.infrastructure.security.LoginUser;
import com.huochai.common.log.mapper.OperationLogMapper;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 操作日志切面
 *
 * @author huochai
 */
@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    @Autowired
    private OperationLogMapper operationLogMapper;

    /**
     * 线程局部变量：存储开始时间
     */
    private static final ThreadLocal<Long> START_TIME = new ThreadLocal<>();

    /**
     * 切点：所有带 @OperationLog 注解的方法
     */
    @Pointcut("@annotation(com.huochai.common.log.OperationLog)")
    public void operationLogPointcut() {
    }

    /**
     * 方法执行后记录日志
     */
    @AfterReturning(pointcut = "operationLogPointcut()", returning = "result")
    public void doAfterReturning(JoinPoint joinPoint, Object result) {
        handleLog(joinPoint, null, result);
    }

    /**
     * 方法抛出异常后记录日志
     */
    @AfterThrowing(pointcut = "operationLogPointcut()", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Exception e) {
        handleLog(joinPoint, e, null);
    }

    /**
     * 处理日志记录
     */
    private void handleLog(JoinPoint joinPoint, Exception e, Object result) {
        try {
            // 获取注解
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            OperationLog operationLog = signature.getMethod().getAnnotation(OperationLog.class);
            if (operationLog == null) {
                return;
            }

            // 构建日志实体
            OperationLogEntity logEntity = new OperationLogEntity();
            
            // 设置 TraceId
            logEntity.setTraceId(MDC.get("traceId"));
            
            // 设置用户信息
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
                LoginUser loginUser = (LoginUser) authentication.getPrincipal();
                logEntity.setUserId(loginUser.getUserId());
                logEntity.setUsername(loginUser.getUsername());
            }

            // 设置操作信息
            logEntity.setModule(operationLog.module());
            logEntity.setOperationType(operationLog.type().getDesc());
            logEntity.setOperationDesc(operationLog.desc());

            // 设置请求信息
            HttpServletRequest request = getRequest();
            if (request != null) {
                logEntity.setRequestMethod(request.getMethod());
                logEntity.setRequestUrl(request.getRequestURI());
                logEntity.setIp(getClientIp(request));
            }

            // 设置请求参数
            if (operationLog.saveParams()) {
                String params = getRequestParams(joinPoint);
                logEntity.setRequestParams(StrUtil.sub(params, 0, 2000));
            }

            // 设置响应结果
            if (operationLog.saveResult() && result != null) {
                String resultStr = JSONUtil.toJsonStr(result);
                logEntity.setResponseResult(StrUtil.sub(resultStr, 0, 2000));
            }

            // 设置状态
            logEntity.setStatus(e == null ? 1 : 0);
            if (e != null) {
                logEntity.setErrorMsg(StrUtil.sub(e.getMessage(), 0, 500));
            }

            // 设置创建时间
            logEntity.setCreatedAt(LocalDateTime.now());

            // 保存日志
            operationLogMapper.insert(logEntity);

        } catch (Exception ex) {
            log.error("记录操作日志异常", ex);
        }
    }

    /**
     * 获取请求参数
     */
    private String getRequestParams(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] paramValues = joinPoint.getArgs();

        Map<String, Object> params = new HashMap<>();
        for (int i = 0; i < paramNames.length; i++) {
            Object value = paramValues[i];
            // 过滤掉文件、请求、响应对象
            if (value instanceof MultipartFile || 
                value instanceof ServletRequest || 
                value instanceof ServletResponse) {
                continue;
            }
            params.put(paramNames[i], value);
        }

        return JSONUtil.toJsonStr(params);
    }

    /**
     * 获取当前请求
     */
    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}