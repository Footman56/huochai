package com.huochai.domain.auth.service;

import com.huochai.domain.auth.bean.AuthUser;
import com.huochai.domain.auth.bean.DeviceInfo;
import com.huochai.domain.auth.bean.TokenPair;

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