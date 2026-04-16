package com.huochai.auth.domain.service;

import com.huochai.auth.domain.model.AuthUser;
import com.huochai.auth.domain.model.DeviceInfo;
import com.huochai.auth.domain.model.Token;
import com.huochai.auth.domain.model.TokenPair;
import io.jsonwebtoken.Claims;

/**
 * Token 领域服务接口
 *
 * @author huochai
 */
public interface TokenDomainService {

    /**
     * 生成Token对
     */
    TokenPair generateTokenPair(AuthUser user, DeviceInfo deviceInfo);

    /**
     * 解析Token
     */
    Claims parseToken(String token);

    /**
     * 验证Token
     */
    boolean validateToken(String token);

    /**
     * 从Token中获取用户ID
     */
    Long getUserId(String token);

    /**
     * 从Token中获取用户名
     */
    String getUsername(String token);

    /**
     * 从Token中获取JTI
     */
    String getJti(String token);

    /**
     * 从Token中获取客户端类型
     */
    String getClientType(String token);

    /**
     * 从Token中获取设备ID
     */
    String getDeviceId(String token);

    /**
     * 检查Token是否过期
     */
    boolean isTokenExpired(String token);

    /**
     * 获取Token剩余有效时间（毫秒）
     */
    long getTokenRemainingTime(String token);
}