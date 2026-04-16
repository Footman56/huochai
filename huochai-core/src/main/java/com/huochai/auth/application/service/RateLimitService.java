package com.huochai.auth.application.service;

/**
 * 限流服务接口
 *
 * @author huochai
 */
public interface RateLimitService {

    /**
     * 登录限流检查（IP维度）
     */
    boolean tryAcquireLoginByIp(String ip);

    /**
     * 登录限流检查（用户名维度）
     */
    boolean tryAcquireLoginByUsername(String username);

    /**
     * API限流检查
     */
    boolean tryAcquireApi(String key, int limit, int windowSeconds);

    /**
     * 获取剩余次数
     */
    int getRemainingCount(String key, int limit);
}