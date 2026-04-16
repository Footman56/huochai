package com.huochai.log.context;

import com.huochai.log.enums.LogType;
import com.huochai.log.model.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * 日志上下文
 * 提供额外的上下文信息附加能力
 */
public class LogContext {
    
    private static final ThreadLocal<Map<String, Object>> contextHolder = ThreadLocal.withInitial(HashMap::new);
    
    /**
     * 添加上下文信息
     */
    public static void put(String key, Object value) {
        contextHolder.get().put(key, value);
    }
    
    /**
     * 获取上下文信息
     */
    public static Object get(String key) {
        return contextHolder.get().get(key);
    }
    
    /**
     * 移除上下文信息
     */
    public static void remove(String key) {
        contextHolder.get().remove(key);
    }
    
    /**
     * 获取所有上下文信息
     */
    public static Map<String, Object> getAll() {
        return new HashMap<>(contextHolder.get());
    }
    
    /**
     * 清除所有上下文信息
     */
    public static void clear() {
        contextHolder.remove();
    }
    
    /**
     * 设置用户ID
     */
    public static void setUserId(String userId) {
        put("userId", userId);
        MDC.put("userId", userId);
    }
    
    /**
     * 设置租户ID
     */
    public static void setTenantId(String tenantId) {
        put("tenantId", tenantId);
        MDC.put("tenantId", tenantId);
    }
    
    /**
     * 设置业务类型
     */
    public static void setBizType(String bizType) {
        put("bizType", bizType);
    }
}