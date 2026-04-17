package com.huochai.domain.auth.service.impl;

import com.huochai.domain.auth.bean.AuthUser;
import com.huochai.domain.auth.bean.DeviceInfo;
import com.huochai.domain.auth.bean.Token;
import com.huochai.domain.auth.bean.TokenPair;
import com.huochai.domain.auth.service.TokenDomainService;
import com.huochai.common.enums.AuthConstants;
import com.huochai.common.enums.TokenType;
import com.huochai.common.exception.TokenException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import cn.hutool.core.util.IdUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Token 领域服务实现
 * 
 * 职责：
 * 1. Token 生成（Access + Refresh）
 * 2. Token 解析
 * 3. Token 验证
 *
 * @author huochai
 */
@Slf4j
@Service
public class TokenDomainServiceImpl implements TokenDomainService {

    @Value("${jwt.secret:huochai-jwt-secret-key-must-be-at-least-256-bits-long-for-security-2024}")
    private String secret;

    @Value("${jwt.access-token-expire:" + AuthConstants.DEFAULT_ACCESS_TOKEN_EXPIRE + "}")
    private long accessTokenExpire;

    @Value("${jwt.refresh-token-expire:" + AuthConstants.DEFAULT_REFRESH_TOKEN_EXPIRE + "}")
    private long refreshTokenExpire;

    @Value("${jwt.issuer:huochai-auth}")
    private String issuer;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        log.info("[Token服务] 初始化: issuer={}, accessTokenExpire={}ms, refreshTokenExpire={}ms", 
                issuer, accessTokenExpire, refreshTokenExpire);
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        log.info("[Token服务] 初始化完成");
    }

    @Override
    public TokenPair generateTokenPair(AuthUser user, DeviceInfo deviceInfo) {
        log.info("[Token服务] 开始生成Token对: userId={}, username={}, clientType={}",
                user.getId(), user.getUsername(), deviceInfo.getClientType());
        
        long now = System.currentTimeMillis();
        
        // ========== 生成 AccessToken ==========
        String accessJti = IdUtil.fastSimpleUUID();
        Date accessExpiresAt = new Date(now + accessTokenExpire);
        log.debug("[Token服务] 生成AccessToken: jti={}, expiresAt={}", accessJti, accessExpiresAt);
        
        String accessTokenValue = buildToken(user, deviceInfo, TokenType.ACCESS, accessJti, accessExpiresAt);
        Token accessToken = Token.accessToken(accessTokenValue, accessExpiresAt.getTime(), accessJti);

        // ========== 生成 RefreshToken ==========
        String refreshJti = IdUtil.fastSimpleUUID();
        Date refreshExpiresAt = new Date(now + refreshTokenExpire);
        log.debug("[Token服务] 生成RefreshToken: jti={}, expiresAt={}", refreshJti, refreshExpiresAt);
        
        String refreshTokenValue = buildToken(user, deviceInfo, TokenType.REFRESH, refreshJti, refreshExpiresAt);
        Token refreshToken = Token.refreshToken(refreshTokenValue, refreshExpiresAt.getTime(), refreshJti);

        // ========== 构建会话ID ==========
        String sessionId = String.format("%d_%s_%s", 
                user.getId(),
                deviceInfo.getClientType().getCode(),
                deviceInfo.getDeviceId());

        log.info("[Token服务] Token对生成成功: userId={}, sessionId={}, accessJti={}, refreshJti={}", 
                user.getId(), sessionId, accessJti, refreshJti);

        return new TokenPair(accessToken, refreshToken, deviceInfo, sessionId);
    }

    /**
     * 构建 Token
     */
    private String buildToken(AuthUser user, DeviceInfo deviceInfo, TokenType tokenType, String jti, Date expiresAt) {
        log.debug("[Token服务] 构建Token: type={}, jti={}, userId={}", tokenType, jti, user.getId());
        
        return Jwts.builder()
                .setSubject(user.getUsername())
                .setIssuer(issuer)
                .setIssuedAt(new Date())
                .setExpiration(expiresAt)
                .setId(jti)
                .claim(AuthConstants.USER_ID_CLAIM, user.getId())
                .claim(AuthConstants.USERNAME_CLAIM, user.getUsername())
                .claim(AuthConstants.TOKEN_TYPE_CLAIM, tokenType.getCode())
                .claim(AuthConstants.CLIENT_TYPE_CLAIM, deviceInfo.getClientType().getCode())
                .claim(AuthConstants.DEVICE_ID_CLAIM, deviceInfo.getDeviceId())
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public Claims parseToken(String token) {
        log.debug("[Token服务] 解析Token");
        
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            log.debug("[Token服务] Token解析成功: subject={}, jti={}", claims.getSubject(), claims.getId());
            return claims;
            
        } catch (ExpiredJwtException e) {
            // 状态分支：Token已过期
            log.warn("[Token服务] Token已过期: jti={}, expiredAt={}", 
                    e.getClaims().getId(), e.getClaims().getExpiration());
            throw new TokenException.ExpiredException();
            
        } catch (UnsupportedJwtException e) {
            // 状态分支：不支持的Token格式
            log.warn("[Token服务] 不支持的Token格式: {}", e.getMessage());
            throw new TokenException.InvalidException("不支持的Token格式");
            
        } catch (MalformedJwtException e) {
            // 状态分支：Token格式错误
            log.warn("[Token服务] Token格式错误: {}", e.getMessage());
            throw new TokenException.InvalidException("Token格式错误");
            
        } catch (io.jsonwebtoken.security.SignatureException e) {
            // 状态分支：签名验证失败
            log.warn("[Token服务] Token签名验证失败: {}", e.getMessage());
            throw new TokenException.InvalidException("Token签名验证失败");
            
        } catch (IllegalArgumentException e) {
            // 状态分支：Token参数非法
            log.warn("[Token服务] Token参数非法: {}", e.getMessage());
            throw new TokenException.InvalidException("Token参数非法");
        }
    }

    @Override
    public boolean validateToken(String token) {
        log.debug("[Token服务] 验证Token");
        
        try {
            parseToken(token);
            log.debug("[Token服务] Token验证通过");
            return true;
        } catch (TokenException e) {
            log.debug("[Token服务] Token验证失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        Object userId = claims.get(AuthConstants.USER_ID_CLAIM);
        
        // 处理不同类型的userId
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }
        return (Long) userId;
    }

    @Override
    public String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims.get(AuthConstants.USERNAME_CLAIM, String.class);
    }

    @Override
    public String getJti(String token) {
        Claims claims = parseToken(token);
        return claims.getId();
    }

    @Override
    public String getClientType(String token) {
        Claims claims = parseToken(token);
        return claims.get(AuthConstants.CLIENT_TYPE_CLAIM, String.class);
    }

    @Override
    public String getDeviceId(String token) {
        Claims claims = parseToken(token);
        return claims.get(AuthConstants.DEVICE_ID_CLAIM, String.class);
    }

    @Override
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            boolean expired = claims.getExpiration().before(new Date());
            log.debug("[Token服务] Token过期检查: jti={}, expired={}", claims.getId(), expired);
            return expired;
        } catch (TokenException.ExpiredException e) {
            return true;
        }
    }

    @Override
    public long getTokenRemainingTime(String token) {
        try {
            Claims claims = parseToken(token);
            long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();
            long result = Math.max(0, remaining);
            log.debug("[Token服务] Token剩余时间: jti={}, remaining={}ms", claims.getId(), result);
            return result;
        } catch (TokenException.ExpiredException e) {
            return 0;
        }
    }
}