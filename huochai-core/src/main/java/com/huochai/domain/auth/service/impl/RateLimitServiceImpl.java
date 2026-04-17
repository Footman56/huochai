package com.huochai.domain.auth.service.impl;

import com.huochai.domain.auth.service.RateLimitService;
import com.huochai.common.enums.AuthConstants;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 限流服务实现
 * 使用 Guava 本地缓存 + Redis 分布式限流
 *
 * @author huochai
 */
@Slf4j
@Service
public class RateLimitServiceImpl implements RateLimitService {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 本地缓存（Guava）- 用于单机限流
     */
    private final Cache<String, AtomicInteger> localCache = CacheBuilder.newBuilder()
            .expireAfterWrite(AuthConstants.LOGIN_RATE_LIMIT_WINDOW, TimeUnit.SECONDS)
            .maximumSize(10000)
            .build();

    /**
     * Lua脚本：令牌桶限流
     */
    private static final String RATE_LIMIT_SCRIPT = 
            "local key = KEYS[1]\n" +
            "local limit = tonumber(ARGV[1])\n" +
            "local window = tonumber(ARGV[2])\n" +
            "local current = redis.call('GET', key)\n" +
            "if current == false then\n" +
            "    redis.call('SET', key, 1, 'EX', window)\n" +
            "    return 1\n" +
            "else\n" +
            "    local count = tonumber(current)\n" +
            "    if count < limit then\n" +
            "        redis.call('INCR', key)\n" +
            "        return count + 1\n" +
            "    else\n" +
            "        return 0\n" +
            "    end\n" +
            "end";

    @Override
    public boolean tryAcquireLoginByIp(String ip) {
        String key = AuthConstants.LOGIN_RATE_LIMIT_PREFIX + "ip:" + ip;
        return tryAcquire(key, AuthConstants.LOGIN_RATE_LIMIT_MAX, 
                (int) AuthConstants.LOGIN_RATE_LIMIT_WINDOW);
    }

    @Override
    public boolean tryAcquireLoginByUsername(String username) {
        String key = AuthConstants.LOGIN_RATE_LIMIT_PREFIX + "user:" + username;
        return tryAcquire(key, AuthConstants.LOGIN_RATE_LIMIT_MAX, 
                (int) AuthConstants.LOGIN_RATE_LIMIT_WINDOW);
    }

    @Override
    public boolean tryAcquireApi(String key, int limit, int windowSeconds) {
        return tryAcquire(key, limit, windowSeconds);
    }

    @Override
    public int getRemainingCount(String key, int limit) {
        if (redisTemplate != null) {
            Object count = redisTemplate.opsForValue().get(key);
            if (count != null) {
                return Math.max(0, limit - Integer.parseInt(count.toString()));
            }
        }
        
        AtomicInteger localCount = localCache.getIfPresent(key);
        if (localCount != null) {
            return Math.max(0, limit - localCount.get());
        }
        
        return limit;
    }

    /**
     * 尝试获取许可
     */
    private boolean tryAcquire(String key, int limit, int windowSeconds) {
        // 优先使用 Redis（分布式环境）
        if (redisTemplate != null) {
            return tryAcquireByRedis(key, limit, windowSeconds);
        }
        
        // 降级使用本地缓存
        return tryAcquireByLocal(key, limit);
    }

    /**
     * Redis 分布式限流
     */
    private boolean tryAcquireByRedis(String key, int limit, int windowSeconds) {
        try {
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(RATE_LIMIT_SCRIPT, Long.class);
            Long result = redisTemplate.execute(script, 
                    Collections.singletonList(key), 
                    String.valueOf(limit), 
                    String.valueOf(windowSeconds));
            
            boolean allowed = result != null && result > 0;
            log.debug("Redis限流检查: key={}, limit={}, result={}, allowed={}", key, limit, result, allowed);
            return allowed;
        } catch (Exception e) {
            log.warn("Redis限流异常，降级使用本地限流: {}", e.getMessage());
            return tryAcquireByLocal(key, limit);
        }
    }

    /**
     * 本地限流（单机）
     */
    private boolean tryAcquireByLocal(String key, int limit) {
        try {
            AtomicInteger counter = localCache.get(key, AtomicInteger::new);
            int count = counter.incrementAndGet();
            boolean allowed = count <= limit;
            log.debug("本地限流检查: key={}, count={}, limit={}, allowed={}", key, count, limit, allowed);
            return allowed;
        } catch (Exception e) {
            log.warn("本地限流异常: {}", e.getMessage());
            return true; // 异常时放行
        }
    }
}