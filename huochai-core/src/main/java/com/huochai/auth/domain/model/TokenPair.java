package com.huochai.auth.domain.model;

import com.huochai.common.enums.ClientType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Token对 值对象
 *
 * @author huochai
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenPair implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 访问Token
     */
    private Token accessToken;

    /**
     * 刷新Token
     */
    private Token refreshToken;

    /**
     * 设备信息
     */
    private DeviceInfo deviceInfo;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 获取Access Token值
     */
    public String getAccessTokenValue() {
        return accessToken != null ? accessToken.getValue() : null;
    }

    /**
     * 获取Refresh Token值
     */
    public String getRefreshTokenValue() {
        return refreshToken != null ? refreshToken.getValue() : null;
    }

    /**
     * 获取Access Token过期时间
     */
    public Long getAccessTokenExpiresAt() {
        return accessToken != null ? accessToken.getExpiresAt() : null;
    }

    /**
     * 获取Access Token有效期（秒）
     */
    public Long getAccessTokenExpiresIn() {
        if (accessToken == null) {
            return null;
        }
        long expiresIn = (accessToken.getExpiresAt() - System.currentTimeMillis()) / 1000;
        return Math.max(0, expiresIn);
    }
}