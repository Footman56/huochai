package com.huochai.auth.domain.model;

import com.huochai.common.enums.TokenType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Token 值对象
 *
 * @author huochai
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Token implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Token值
     */
    private String value;

    /**
     * Token类型
     */
    private TokenType type;

    /**
     * 过期时间（时间戳）
     */
    private Long expiresAt;

    /**
     * 唯一标识（用于黑名单）
     */
    private String jti;

    /**
     * 检查是否过期
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    /**
     * 获取剩余有效时间（毫秒）
     */
    public long getRemainingTime() {
        long remaining = expiresAt - System.currentTimeMillis();
        return Math.max(0, remaining);
    }

    /**
     * 创建访问Token
     */
    public static Token accessToken(String value, Long expiresAt, String jti) {
        return new Token(value, TokenType.ACCESS, expiresAt, jti);
    }

    /**
     * 创建刷新Token
     */
    public static Token refreshToken(String value, Long expiresAt, String jti) {
        return new Token(value, TokenType.REFRESH, expiresAt, jti);
    }
}