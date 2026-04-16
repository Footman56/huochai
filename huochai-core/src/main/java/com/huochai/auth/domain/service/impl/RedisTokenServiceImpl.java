package com.huochai.auth.domain.service.impl;

import com.huochai.auth.domain.model.LoginSession;
import com.huochai.auth.domain.model.TokenPair;
import com.huochai.auth.domain.service.RedisTokenService;
import com.huochai.auth.domain.service.TokenDomainService;
import com.huochai.common.enums.AuthConstants;
import com.huochai.common.enums.ClientType;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Redis Token 服务实现
 * 
 * 职责：
 * 1. Token 会话存储
 * 2. Token 黑名单管理
 * 3. 会话踢出管理
 *
 * @author huochai
 */
@Slf4j
@Service
public class RedisTokenServiceImpl implements RedisTokenService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private TokenDomainService tokenDomainService;

    @Override
    public void storeToken(Long userId, TokenPair tokenPair) {
        log.debug("[Redis服务] 存储Token: userId={}", userId);
        
        if (tokenPair == null || tokenPair.getDeviceInfo() == null) {
            log.warn("[Redis服务] Token或设备信息为空，跳过存储");
            return;
        }

        ClientType clientType = tokenPair.getDeviceInfo().getClientType();
        String deviceId = tokenPair.getDeviceInfo().getDeviceId();
        
        // ========== 创建会话 ==========
        log.debug("[Redis服务] 创建会话对象: userId={}, clientType={}, deviceId={}", 
                userId, clientType, deviceId);
        
        LoginSession session = LoginSession.create(
                userId,
                tokenDomainService.getUsername(tokenPair.getAccessTokenValue()),
                tokenPair.getDeviceInfo(),
                tokenPair
        );

        // ========== 存储 Token ==========
        String tokenKey = buildTokenKey(userId, clientType, deviceId);
        long expireMillis = tokenPair.getRefreshToken().getExpiresAt() - System.currentTimeMillis();
        
        log.debug("[Redis服务] 写入Redis: key={}, expireMs={}", tokenKey, expireMillis);
        redisTemplate.opsForValue().set(tokenKey, session, expireMillis, TimeUnit.MILLISECONDS);

        // ========== 存储会话映射（用于踢人）==========
        String sessionKey = buildSessionKey(userId, clientType);
        redisTemplate.opsForValue().set(sessionKey, deviceId, expireMillis, TimeUnit.MILLISECONDS);

        log.info("[Redis服务] Token存储成功: userId={}, clientType={}, deviceId={}, expireMs={}", 
                userId, clientType, deviceId, expireMillis);
    }

    @Override
    public LoginSession getSession(Long userId, ClientType clientType, String deviceId) {
        log.debug("[Redis服务] 获取会话: userId={}, clientType={}, deviceId={}", userId, clientType, deviceId);
        
        String tokenKey = buildTokenKey(userId, clientType, deviceId);
        Object value = redisTemplate.opsForValue().get(tokenKey);
        
        if (value instanceof LoginSession session) {
            log.debug("[Redis服务] 会话获取成功: userId={}, sessionId={}", userId, session.getSessionId());
            return session;
        }
        
        log.debug("[Redis服务] 会话不存在: userId={}, clientType={}, deviceId={}", userId, clientType, deviceId);
        return null;
    }

    @Override
    public List<LoginSession> getAllSessions(Long userId) {
        log.debug("[Redis服务] 获取所有会话: userId={}", userId);
        
        List<LoginSession> sessions = new ArrayList<>();
        String pattern = AuthConstants.TOKEN_KEY_PREFIX + userId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        
        if (keys != null && !keys.isEmpty()) {
            log.debug("[Redis服务] 找到{}个会话", keys.size());
            for (String key : keys) {
                Object value = redisTemplate.opsForValue().get(key);
                if (value instanceof LoginSession session) {
                    sessions.add(session);
                }
            }
        } else {
            log.debug("[Redis服务] 未找到任何会话");
        }
        
        return sessions;
    }

    @Override
    public boolean validateToken(String token) {
        log.debug("[Redis服务] 验证Token");
        
        // ========== 检查Token是否为空 ==========
        if (StrUtil.isBlank(token)) {
            log.debug("[Redis服务] Token为空，验证失败");
            return false;
        }

        // ========== 检查黑名单 ==========
        if (isBlacklisted(token)) {
            log.warn("[Redis服务] Token在黑名单中，验证失败: jti={}", 
                    tokenDomainService.getJti(token));
            return false;
        }

        // ========== 检查Token本身有效性 ==========
        boolean valid = tokenDomainService.validateToken(token);
        log.debug("[Redis服务] Token验证结果: {}", valid ? "有效" : "无效");
        
        return valid;
    }

    @Override
    public void addToBlacklist(String token, long expireMillis) {
        String jti = tokenDomainService.getJti(token);
        String blacklistKey = AuthConstants.BLACKLIST_KEY_PREFIX + token;
        
        log.debug("[Redis服务] 加入黑名单: jti={}, expireMs={}", jti, expireMillis);
        redisTemplate.opsForValue().set(blacklistKey, "1", expireMillis, TimeUnit.MILLISECONDS);
        
        log.info("[Redis服务] Token已加入黑名单: jti={}, expireMs={}", jti, expireMillis);
    }

    @Override
    public boolean isBlacklisted(String token) {
        String blacklistKey = AuthConstants.BLACKLIST_KEY_PREFIX + token;
        boolean blacklisted = redisTemplate.hasKey(blacklistKey);
        
        if (blacklisted) {
            log.debug("[Redis服务] Token在黑名单中: jti={}", tokenDomainService.getJti(token));
        }
        
        return blacklisted;
    }

    @Override
    public void kickOut(Long userId, ClientType clientType, String deviceId) {
        log.info("[Redis服务] 开始踢出用户: userId={}, clientType={}, deviceId={}", userId, clientType, deviceId);
        
        // ========== 获取当前会话 ==========
        LoginSession session = getSession(userId, clientType, deviceId);
        
        if (session != null && session.getTokenPair() != null) {
            log.debug("[Redis服务] 找到会话，准备加入黑名单");
            
            // ========== 将Token加入黑名单 ==========
            String accessToken = session.getTokenPair().getAccessTokenValue();
            String refreshToken = session.getTokenPair().getRefreshTokenValue();
            
            // 状态分支：处理AccessToken
            if (StrUtil.isNotBlank(accessToken)) {
                long remainingTime = session.getTokenPair().getAccessToken().getRemainingTime();
                log.debug("[Redis服务] AccessToken加入黑名单: remainingMs={}", remainingTime);
                addToBlacklist(accessToken, remainingTime);
            }
            
            // 状态分支：处理RefreshToken
            if (StrUtil.isNotBlank(refreshToken)) {
                long remainingTime = session.getTokenPair().getRefreshToken().getRemainingTime();
                log.debug("[Redis服务] RefreshToken加入黑名单: remainingMs={}", remainingTime);
                addToBlacklist(refreshToken, remainingTime);
            }
        } else {
            log.debug("[Redis服务] 会话不存在，无需加入黑名单");
        }

        // ========== 删除会话 ==========
        removeSession(userId, clientType, deviceId);
        
        log.info("[Redis服务] 踢出用户完成: userId={}, clientType={}, deviceId={}", userId, clientType, deviceId);
    }

    @Override
    public void kickOutAll(Long userId) {
        log.info("[Redis服务] 踢出所有会话: userId={}", userId);
        
        List<LoginSession> sessions = getAllSessions(userId);
        log.debug("[Redis服务] 找到{}个会话需要踢出", sessions.size());
        
        for (LoginSession session : sessions) {
            if (session.getDeviceInfo() != null) {
                kickOut(userId, session.getDeviceInfo().getClientType(), session.getDeviceInfo().getDeviceId());
            }
        }
        
        log.info("[Redis服务] 所有会话已踢出: userId={}, count={}", userId, sessions.size());
    }

    @Override
    public boolean isKickedOut(Long userId, ClientType clientType, String deviceId) {
        String sessionKey = buildSessionKey(userId, clientType);
        Object currentDeviceId = redisTemplate.opsForValue().get(sessionKey);
        
        // 状态分支：Session不存在
        if (currentDeviceId == null) {
            log.debug("[Redis服务] 会话不存在，用户已被踢出: userId={}, clientType={}", userId, clientType);
            return true;
        }
        
        // 状态分支：DeviceId不匹配
        if (!deviceId.equals(currentDeviceId.toString())) {
            log.debug("[Redis服务] 设备ID不匹配，用户已被踢出: userId={}, expected={}, actual={}", 
                    userId, currentDeviceId, deviceId);
            return true;
        }
        
        return false;
    }

    @Override
    public void removeSession(Long userId, ClientType clientType, String deviceId) {
        log.debug("[Redis服务] 删除会话: userId={}, clientType={}, deviceId={}", userId, clientType, deviceId);
        
        String tokenKey = buildTokenKey(userId, clientType, deviceId);
        String sessionKey = buildSessionKey(userId, clientType);
        
        Boolean tokenDeleted = redisTemplate.delete(tokenKey);
        Boolean sessionDeleted = redisTemplate.delete(sessionKey);
        
        log.debug("[Redis服务] 会话删除结果: tokenKey={}, sessionKey={}", tokenDeleted, sessionDeleted);
    }

    @Override
    public void refreshSessionAccessTime(Long userId, ClientType clientType, String deviceId) {
        log.debug("[Redis服务] 刷新会话访问时间: userId={}, clientType={}, deviceId={}", 
                userId, clientType, deviceId);
        
        LoginSession session = getSession(userId, clientType, deviceId);
        
        if (session != null) {
            session.refreshAccessTime();
            String tokenKey = buildTokenKey(userId, clientType, deviceId);
            long expireMillis = session.getTokenPair().getRefreshToken().getRemainingTime();
            redisTemplate.opsForValue().set(tokenKey, session, expireMillis, TimeUnit.MILLISECONDS);
            
            log.debug("[Redis服务] 会话访问时间已刷新: userId={}, lastAccessTime={}", 
                    userId, session.getLastAccessedAt());
        } else {
            log.warn("[Redis服务] 会话不存在，无法刷新: userId={}, clientType={}, deviceId={}", 
                    userId, clientType, deviceId);
        }
    }

    /**
     * 构建 Token 存储Key
     */
    private String buildTokenKey(Long userId, ClientType clientType, String deviceId) {
        return AuthConstants.TOKEN_KEY_PREFIX + userId + ":" + clientType.getCode() + ":" + deviceId;
    }

    /**
     * 构建会话 Key
     */
    private String buildSessionKey(Long userId, ClientType clientType) {
        return AuthConstants.SESSION_KEY_PREFIX + userId + ":" + clientType.getCode();
    }
}