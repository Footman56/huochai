package com.huochai.auth.domain.service;

import com.huochai.auth.domain.model.AuthUser;
import com.huochai.auth.domain.model.DeviceInfo;
import com.huochai.auth.domain.model.TokenPair;

/**
 * 认证领域服务接口
 *
 * @author huochai
 */
public interface AuthDomainService {

    /**
     * 用户认证
     */
    AuthUser authenticate(String username, String password);

    /**
     * 生成Token对
     */
    TokenPair generateTokenPair(AuthUser user, DeviceInfo deviceInfo);

    /**
     * 刷新Token
     */
    TokenPair refreshToken(String refreshToken, DeviceInfo deviceInfo);

    /**
     * 登出
     */
    void logout(String token);

    /**
     * 踢出用户
     */
    void kickOut(Long userId, String clientType, String deviceId);
}