package com.huochai.auth.domain.service;

import com.huochai.auth.domain.model.LoginSession;
import com.huochai.auth.domain.model.TokenPair;
import com.huochai.common.enums.ClientType;

import java.util.List;

/**
 * Redis Token 服务接口
 *
 * @author huochai
 */
public interface RedisTokenService {

    /**
     * 存储Token（创建会话）
     */
    void storeToken(Long userId, TokenPair tokenPair);

    /**
     * 获取会话
     */
    LoginSession getSession(Long userId, ClientType clientType, String deviceId);

    /**
     * 获取用户所有在线会话
     */
    List<LoginSession> getAllSessions(Long userId);

    /**
     * 验证Token有效性（检查黑名单、过期）
     */
    boolean validateToken(String token);

    /**
     * 将Token加入黑名单
     */
    void addToBlacklist(String token, long expireMillis);

    /**
     * 检查Token是否在黑名单
     */
    boolean isBlacklisted(String token);

    /**
     * 踢出用户指定端
     */
    void kickOut(Long userId, ClientType clientType, String deviceId);

    /**
     * 踢出用户所有端
     */
    void kickOutAll(Long userId);

    /**
     * 检查是否被踢出
     */
    boolean isKickedOut(Long userId, ClientType clientType, String deviceId);

    /**
     * 删除会话
     */
    void removeSession(Long userId, ClientType clientType, String deviceId);

    /**
     * 刷新会话访问时间
     */
    void refreshSessionAccessTime(Long userId, ClientType clientType, String deviceId);
}