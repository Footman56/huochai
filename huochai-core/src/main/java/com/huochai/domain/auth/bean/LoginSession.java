package com.huochai.domain.auth.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录会话实体 - Redis存储
 *
 * @author huochai
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginSession implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 设备信息
     */
    private DeviceInfo deviceInfo;

    /**
     * Token对
     */
    private TokenPair tokenPair;

    /**
     * 创建时间（时间戳）
     */
    private Long createdAt;

    /**
     * 最后访问时间（时间戳）
     */
    private Long lastAccessedAt;

    /**
     * 检查会话是否过期
     */
    public boolean isExpired() {
        if (tokenPair == null || tokenPair.getAccessToken() == null) {
            return true;
        }
        return tokenPair.getAccessToken().isExpired();
    }

    /**
     * 刷新最后访问时间
     */
    public void refreshAccessTime() {
        this.lastAccessedAt = System.currentTimeMillis();
    }

    /**
     * 创建新会话
     */
    public static LoginSession create(Long userId, String username, DeviceInfo deviceInfo, TokenPair tokenPair) {
        long now = System.currentTimeMillis();
        String sessionId = generateSessionId(userId, deviceInfo);
        
        return LoginSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .username(username)
                .deviceInfo(deviceInfo)
                .tokenPair(tokenPair)
                .createdAt(now)
                .lastAccessedAt(now)
                .build();
    }

    /**
     * 生成会话ID
     */
    private static String generateSessionId(Long userId, DeviceInfo deviceInfo) {
        return String.format("%d_%s_%s", 
                userId, 
                deviceInfo.getClientType().getCode(),
                deviceInfo.getDeviceId());
    }
}