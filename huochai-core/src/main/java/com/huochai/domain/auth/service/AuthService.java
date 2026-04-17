package com.huochai.domain.auth.service;

import com.huochai.domain.auth.bean.LoginRequest;
import com.huochai.domain.auth.bean.LoginResponse;
import com.huochai.domain.auth.bean.RefreshTokenRequest;
import com.huochai.domain.auth.bean.UserInfoResponse;

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