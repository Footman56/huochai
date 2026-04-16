package com.huochai.auth.application.service;

import com.huochai.auth.interfaces.dto.LoginRequest;
import com.huochai.auth.interfaces.dto.LoginResponse;
import com.huochai.auth.interfaces.dto.RefreshTokenRequest;
import com.huochai.auth.interfaces.dto.UserInfoResponse;

/**
 * 认证应用服务接口
 *
 * @author huochai
 */
public interface AuthService {

    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest request);

    /**
     * 用户登出
     */
    void logout(String token);

    /**
     * 刷新Token
     */
    LoginResponse refreshToken(RefreshTokenRequest request);

    /**
     * 获取当前用户信息
     */
    UserInfoResponse getCurrentUserInfo();

    /**
     * 根据Token获取用户信息
     */
    UserInfoResponse getUserInfoByToken(String token);
}