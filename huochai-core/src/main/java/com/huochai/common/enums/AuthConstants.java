package com.huochai.common.enums;

/**
 * 认证常量
 *
 * @author huochai
 */
public final class AuthConstants {

    private AuthConstants() {}

    // ========== Redis Key 前缀 ==========
    /**
     * Token存储前缀
     * 完整格式: auth:token:{userId}:{clientType}:{deviceId}
     */
    public static final String TOKEN_KEY_PREFIX = "auth:token:";

    /**
     * Token黑名单前缀
     * 完整格式: auth:blacklist:{token}
     */
    public static final String BLACKLIST_KEY_PREFIX = "auth:blacklist:";

    /**
     * 用户会话前缀
     * 完整格式: auth:session:{userId}:{clientType}
     */
    public static final String SESSION_KEY_PREFIX = "auth:session:";

    /**
     * 用户权限缓存前缀
     * 完整格式: auth:permissions:{userId}
     */
    public static final String PERMISSIONS_KEY_PREFIX = "auth:permissions:";

    /**
     * 验证码前缀
     * 完整格式: captcha:{uuid}
     */
    public static final String CAPTCHA_KEY_PREFIX = "captcha:";

    /**
     * 登录限流前缀
     * 完整格式: ratelimit:login:{key}
     */
    public static final String LOGIN_RATE_LIMIT_PREFIX = "ratelimit:login:";

    // ========== Token相关 ==========
    /**
     * Authorization Header 前缀
     */
    public static final String BEARER_PREFIX = "Bearer ";

    /**
     * Authorization Header 名称
     */
    public static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * JTI Claim 名称
     */
    public static final String JTI_CLAIM = "jti";

    /**
     * 用户ID Claim 名称
     */
    public static final String USER_ID_CLAIM = "userId";

    /**
     * 用户名 Claim 名称
     */
    public static final String USERNAME_CLAIM = "username";

    /**
     * Token类型 Claim 名称
     */
    public static final String TOKEN_TYPE_CLAIM = "tokenType";

    /**
     * 客户端类型 Claim 名称
     */
    public static final String CLIENT_TYPE_CLAIM = "clientType";

    /**
     * 设备ID Claim 名称
     */
    public static final String DEVICE_ID_CLAIM = "deviceId";

    // ========== 默认值 ==========
    /**
     * AccessToken 默认有效期（毫秒）: 2小时
     */
    public static final long DEFAULT_ACCESS_TOKEN_EXPIRE = 2 * 60 * 60 * 1000L;

    /**
     * RefreshToken 默认有效期（毫秒）: 7天
     */
    public static final long DEFAULT_REFRESH_TOKEN_EXPIRE = 7 * 24 * 60 * 60 * 1000L;

    /**
     * 验证码有效期（秒）: 5分钟
     */
    public static final long CAPTCHA_EXPIRE_SECONDS = 300;

    /**
     * 登录限流窗口（秒）: 1分钟
     */
    public static final long LOGIN_RATE_LIMIT_WINDOW = 60;

    /**
     * 登录限流最大次数
     */
    public static final int LOGIN_RATE_LIMIT_MAX = 5;

    // ========== 请求头 ==========
    /**
     * 客户端类型请求头
     */
    public static final String CLIENT_TYPE_HEADER = "X-Client-Type";

    /**
     * 设备ID请求头
     */
    public static final String DEVICE_ID_HEADER = "X-Device-Id";

    /**
     * TraceId 请求头
     */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
}