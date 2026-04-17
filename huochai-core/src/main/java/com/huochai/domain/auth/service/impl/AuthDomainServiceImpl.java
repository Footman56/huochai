package com.huochai.domain.auth.service.impl;

import com.huochai.domain.auth.bean.event.DomainEventPublisher;
import com.huochai.domain.auth.bean.AuthUser;
import com.huochai.domain.auth.bean.DeviceInfo;
import com.huochai.domain.auth.bean.TokenPair;
import com.huochai.domain.auth.repository.AuthUserRepository;
import com.huochai.domain.auth.service.AuthDomainService;
import com.huochai.domain.auth.service.RedisTokenService;
import com.huochai.domain.auth.service.TokenDomainService;
import com.huochai.common.enums.ClientType;
import com.huochai.common.exception.AuthErrorCode;
import com.huochai.common.exception.AuthException;
import com.huochai.common.exception.TokenException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import cn.hutool.core.util.StrUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;

/**
 * 认证领域服务实现
 * 
 * 职责：
 * 1. 用户认证（用户名密码验证）
 * 2. Token 生成和管理
 * 3. 登出和踢人管理
 *
 * @author huochai
 */
@Slf4j
@Service
public class AuthDomainServiceImpl implements AuthDomainService {

    /**
     * 用户仓储
     */
    @Autowired
    private AuthUserRepository authUserRepository;

    /**
     * Token 领域服务
     */
    @Autowired
    private TokenDomainService tokenDomainService;

    /**
     * Redis Token 服务
     */
    @Autowired
    private RedisTokenService redisTokenService;

    /**
     * 密码编码器
     */
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 领域事件发布器
     */
    @Autowired
    private DomainEventPublisher eventPublisher;

    @Override
    public AuthUser authenticate(String username, String password) {
        log.info("[认证服务] 开始认证: username={}", username);
        
        // ========== 参数校验 ==========
        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            log.warn("[认证服务] 认证失败-参数为空: username={}", username);
            throw new AuthException(AuthErrorCode.BAD_REQUEST, "用户名或密码不能为空");
        }

        // ========== 查询用户 ==========
        log.debug("[认证服务] 查询用户: username={}", username);
        AuthUser user = authUserRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("[认证服务] 认证失败-用户不存在: username={}", username);
                    return new AuthException(AuthErrorCode.USER_NOT_FOUND, "用户名或密码错误");
                });
        
        log.info("[认证服务] 用户查询成功: userId={}, username={}", user.getId(), user.getUsername());

        // ========== 验证密码 ==========
        log.info("[认证服务] 验证密码: userId={}", user.getId());
        if (!user.verifyPassword(password, passwordEncoder)) {
            log.warn("[认证服务] 认证失败-密码错误: userId={}, username={}", user.getId(), username);
            throw new AuthException(AuthErrorCode.PASSWORD_ERROR, "用户名或密码错误");
        }
        log.info("[认证服务] 密码验证通过: userId={}", user.getId());

        // ========== 验证账号状态 ==========
        log.info("[认证服务] 验证账号状态: userId={}", user.getId());
        
        // 状态分支：检查是否被禁用
        if (user.isDisabled()) {
            log.warn("[认证服务] 认证失败-账号被禁用: userId={}, username={}", user.getId(), username);
            throw new AuthException(AuthErrorCode.ACCOUNT_DISABLED, "账号已被禁用");
        }
        
        // 状态分支：检查是否有效
        if (!user.isAccountValid()) {
            log.warn("[认证服务] 认证失败-账号无效: userId={}, username={}", user.getId(), username);
            throw new AuthException(AuthErrorCode.ACCOUNT_LOCKED, "账号状态异常");
        }
        
        log.info("[认证服务] 认证成功: userId={}, username={}", user.getId(), user.getUsername());
        return user;
    }

    @Override
    public TokenPair generateTokenPair(AuthUser user, DeviceInfo deviceInfo) {
        log.info("[认证服务] 生成Token: userId={}, username={}, clientType={}, deviceId={}", 
                user.getId(), user.getUsername(), 
                deviceInfo.getClientType(), deviceInfo.getDeviceId());
        
        // ========== 生成Token对 ==========
        TokenPair tokenPair = tokenDomainService.generateTokenPair(user, deviceInfo);
        log.debug("[认证服务] Token生成成功: userId={}, accessJti={}, refreshJti={}", 
                user.getId(), 
                tokenPair.getAccessToken().getJti(),
                tokenPair.getRefreshToken().getJti());

        // ========== 处理旧会话（单点登录踢人）==========
        ClientType clientType = deviceInfo.getClientType();
        String deviceId = deviceInfo.getDeviceId();
        
        // 检查是否存在旧会话
        var oldSession = redisTokenService.getSession(user.getId(), clientType, deviceId);
        if (oldSession != null) {
            log.info("[认证服务] 发现旧会话，准备踢出: userId={}, clientType={}, oldDeviceId={}", 
                    user.getId(), clientType, deviceId);
            
            // 踢出旧会话
            redisTokenService.kickOut(user.getId(), clientType, deviceId);
            
            // 发布踢出事件
            eventPublisher.publishUserKickedOut(user.getId(), clientType.getCode(), deviceId, "新设备登录");
        }
        
        // ========== 存储新Token ==========
        redisTokenService.storeToken(user.getId(), tokenPair);
        log.info("[认证服务] Token存储成功: userId={}, sessionId={}", user.getId(), tokenPair.getSessionId());

        // ========== 发布登录事件 ==========
        eventPublisher.publishUserLogin(user.getId(), user.getUsername(), deviceInfo);
        
        log.info("[认证服务] 登录流程完成: userId={}, username={}, clientType={}", 
                user.getId(), user.getUsername(), clientType);

        return tokenPair;
    }

    @Override
    public TokenPair refreshToken(String refreshToken, DeviceInfo deviceInfo) {
        log.info("[认证服务] 刷新Token: clientType={}, deviceId={}", 
                deviceInfo.getClientType(), deviceInfo.getDeviceId());
        
        // ========== 验证刷新Token ==========
        log.debug("[认证服务] 解析刷新Token");
        Claims claims;
        try {
            claims = tokenDomainService.parseToken(refreshToken);
        } catch (TokenException e) {
            log.warn("[认证服务] 刷新Token解析失败: {}", e.getMessage());
            throw e;
        }
        
        // ========== 检查Token类型 ==========
        String tokenType = claims.get("tokenType", String.class);
        log.debug("[认证服务] Token类型: {}", tokenType);
        
        if (!"REFRESH".equals(tokenType)) {
            log.warn("[认证服务] Token类型错误: 期望REFRESH, 实际={}", tokenType);
            throw new TokenException(AuthErrorCode.REFRESH_TOKEN_INVALID, "无效的刷新Token");
        }

        // ========== 检查黑名单 ==========
        if (redisTokenService.isBlacklisted(refreshToken)) {
            log.warn("[认证服务] 刷新Token在黑名单中");
            throw new TokenException.BlacklistedException();
        }

        // ========== 获取用户信息 ==========
        Long userId = tokenDomainService.getUserId(refreshToken);
        String username = tokenDomainService.getUsername(refreshToken);
        String oldJti = tokenDomainService.getJti(refreshToken);
        
        log.debug("[认证服务] 解析用户信息: userId={}, username={}, jti={}", userId, username, oldJti);

        // ========== 查询用户 ==========
        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("[认证服务] 用户不存在: userId={}", userId);
                    return new AuthException(AuthErrorCode.USER_NOT_FOUND);
                });

        // ========== 检查用户状态 ==========
        if (!user.isAccountValid()) {
            log.warn("[认证服务] 用户状态异常: userId={}, status={}", userId, user.getStatus());
            throw new AuthException(AuthErrorCode.ACCOUNT_DISABLED);
        }

        // ========== 将旧Token加入黑名单 ==========
        long remainingTime = tokenDomainService.getTokenRemainingTime(refreshToken);
        redisTokenService.addToBlacklist(refreshToken, remainingTime);
        log.info("[认证服务] 旧Token加入黑名单: jti={}, remainingTime={}ms", oldJti, remainingTime);

        // ========== 生成新Token对 ==========
        TokenPair newTokenPair = tokenDomainService.generateTokenPair(user, deviceInfo);
        String newJti = newTokenPair.getAccessToken().getJti();
        
        // ========== 存储新Token ==========
        redisTokenService.storeToken(user.getId(), newTokenPair);
        
        // ========== 发布Token刷新事件 ==========
        eventPublisher.publishTokenRefreshed(user.getId(), user.getUsername(), newJti, oldJti);
        
        log.info("[认证服务] Token刷新成功: userId={}, oldJti={}, newJti={}", userId, oldJti, newJti);

        return newTokenPair;
    }

    @Override
    public void logout(String token) {
        if (StrUtil.isBlank(token)) {
            log.debug("[认证服务] 登出Token为空，跳过");
            return;
        }

        log.info("[认证服务] 开始登出");
        
        try {
            // ========== 解析Token ==========
            Long userId = tokenDomainService.getUserId(token);
            String username = tokenDomainService.getUsername(token);
            String clientType = tokenDomainService.getClientType(token);
            String deviceId = tokenDomainService.getDeviceId(token);
            String jti = tokenDomainService.getJti(token);
            
            log.debug("[认证服务] 解析Token成功: userId={}, username={}, clientType={}, deviceId={}, jti={}", 
                    userId, username, clientType, deviceId, jti);

            // ========== 踢出会话 ==========
            redisTokenService.kickOut(userId, ClientType.fromCode(clientType), deviceId);
            
            // ========== 发布登出事件 ==========
            eventPublisher.publishUserLogout(userId, username, clientType, deviceId);

            log.info("[认证服务] 登出成功: userId={}, username={}, clientType={}", userId, username, clientType);
            
        } catch (Exception e) {
            log.warn("[认证服务] 登出失败: {}", e.getMessage());
        }
    }

    @Override
    public void kickOut(Long userId, String clientType, String deviceId) {
        log.info("[认证服务] 踢出用户: userId={}, clientType={}, deviceId={}", userId, clientType, deviceId);
        
        redisTokenService.kickOut(userId, ClientType.fromCode(clientType), deviceId);
        
        // 发布踢出事件
        eventPublisher.publishUserKickedOut(userId, clientType, deviceId, "管理员操作");
        
        log.info("[认证服务] 踢出成功: userId={}, clientType={}", userId, clientType);
    }
}