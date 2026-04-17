package com.huochai.domain.auth.service.impl;

import com.huochai.common.enums.ClientType;
import com.huochai.common.exception.AuthErrorCode;
import com.huochai.common.exception.AuthException;
import com.huochai.domain.auth.bean.AuthUser;
import com.huochai.domain.auth.bean.DeviceInfo;
import com.huochai.domain.auth.bean.LoginLog;
import com.huochai.domain.auth.bean.LoginRequest;
import com.huochai.domain.auth.bean.LoginResponse;
import com.huochai.domain.auth.bean.RefreshTokenRequest;
import com.huochai.domain.auth.bean.TokenPair;
import com.huochai.domain.auth.bean.UserInfoResponse;
import com.huochai.domain.auth.repository.LoginLogRepository;
import com.huochai.domain.auth.service.AuthDomainService;
import com.huochai.domain.auth.service.AuthService;
import com.huochai.domain.auth.service.CaptchaService;
import com.huochai.domain.auth.service.RateLimitService;
import com.huochai.domain.auth.service.RedisTokenService;
import com.huochai.domain.auth.service.TokenDomainService;
import com.huochai.domain.permission.bean.LoginUser;
import com.huochai.domain.permission.service.RbacDomainService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 认证应用服务实现
 *
 * @author huochai
 */
@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private AuthDomainService authDomainService;
    
    @Autowired
    private TokenDomainService tokenDomainService;
    
    @Autowired
    private RedisTokenService redisTokenService;
    
    @Autowired
    private CaptchaService captchaService;
    
    @Autowired
    private RateLimitService rateLimitService;
    
    @Autowired
    private LoginLogRepository loginLogRepository;
    
    @Autowired
    private RbacDomainService rbacDomainService;

    @Override
    public LoginResponse login(LoginRequest request) {
        String username = request.getUsername();
        String ip = getClientIp();

        // 限流检查
        if (!rateLimitService.tryAcquireLoginByIp(ip)) {
            log.warn("登录限流（IP）: username={}, ip={}", username, ip);
            throw new AuthException(AuthErrorCode.LOGIN_RATE_LIMIT, "登录尝试次数过多，请稍后重试");
        }
        if (!rateLimitService.tryAcquireLoginByUsername(username)) {
            log.warn("登录限流（用户名）: username={}", username);
            throw new AuthException(AuthErrorCode.LOGIN_RATE_LIMIT, "登录尝试次数过多，请稍后重试");
        }

        // 验证码校验
        if (StrUtil.isNotBlank(request.getCaptchaUuid())) {
            if (!captchaService.verifyCaptcha(request.getCaptchaUuid(), request.getCaptchaCode())) {
                log.warn("验证码错误: username={}", username);
                throw new AuthException(AuthErrorCode.CAPTCHA_ERROR);
            }
        }

        // 构建设备信息
        DeviceInfo deviceInfo = buildDeviceInfo(request);

        try {
            // 用户认证
            AuthUser user = authDomainService.authenticate(username, request.getPassword());

            // 生成Token
            TokenPair tokenPair = authDomainService.generateTokenPair(user, deviceInfo);

            // 记录登录日志
            LoginLog loginLog = LoginLog.success(user.getId(), user.getUsername(), deviceInfo);
            loginLogRepository.save(loginLog);

            // 返回响应
            return LoginResponse.builder()
                    .accessToken(tokenPair.getAccessTokenValue())
                    .refreshToken(tokenPair.getRefreshTokenValue())
                    .expiresIn(tokenPair.getAccessTokenExpiresIn())
                    .tokenType("Bearer")
                    .sessionId(tokenPair.getSessionId())
                    .userId(user.getId())
                    .username(user.getUsername())
                    .build();

        } catch (AuthException e) {
            // 记录失败日志
            LoginLog failLog = LoginLog.fail(username, deviceInfo, e.getMessage());
            loginLogRepository.save(failLog);
            throw e;
        }
    }

    @Override
    public void logout(String token) {
        if (StrUtil.isBlank(token)) {
            return;
        }
        authDomainService.logout(token);
    }

    @Override
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        DeviceInfo deviceInfo = DeviceInfo.builder()
                .clientType(ClientType.fromCode(request.getClientType()))
                .deviceId(StrUtil.blankToDefault(request.getDeviceId(), "default"))
                .build();

        TokenPair tokenPair = authDomainService.refreshToken(request.getRefreshToken(), deviceInfo);

        return LoginResponse.builder()
                .accessToken(tokenPair.getAccessTokenValue())
                .refreshToken(tokenPair.getRefreshTokenValue())
                .expiresIn(tokenPair.getAccessTokenExpiresIn())
                .tokenType("Bearer")
                .sessionId(tokenPair.getSessionId())
                .build();
    }

    @Override
    public UserInfoResponse getCurrentUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser)) {
            throw new AuthException(AuthErrorCode.UNAUTHORIZED);
        }

        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        return buildUserInfoResponse(loginUser);
    }

    @Override
    public UserInfoResponse getUserInfoByToken(String token) {
        if (StrUtil.isBlank(token)) {
            throw new AuthException(AuthErrorCode.TOKEN_MISSING);
        }

        Long userId = tokenDomainService.getUserId(token);
        String username = tokenDomainService.getUsername(token);

        List<String> permissions = rbacDomainService.loadUserPermissions(userId);
        List<String> roles = rbacDomainService.getUserRoleCodes(userId);

        return UserInfoResponse.builder()
                .userId(userId)
                .username(username)
                .roles(roles)
                .permissions(permissions)
                .build();
    }

    /**
     * 构建设备信息
     */
    private DeviceInfo buildDeviceInfo(LoginRequest request) {
        HttpServletRequest httpRequest = getHttpRequest();
        
        return DeviceInfo.builder()
                .deviceId(StrUtil.blankToDefault(request.getDeviceId(), "default"))
                .clientType(ClientType.fromCode(request.getClientType()))
                .ip(getClientIp())
                .os(httpRequest != null ? httpRequest.getHeader("User-Agent") : null)
                .build();
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp() {
        HttpServletRequest request = getHttpRequest();
        if (request == null) {
            return "unknown";
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 多个代理时取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        
        return ip;
    }

    /**
     * 获取当前请求
     */
    private HttpServletRequest getHttpRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 构建用户信息响应
     */
    private UserInfoResponse buildUserInfoResponse(LoginUser loginUser) {
        return UserInfoResponse.builder()
                .userId(loginUser.getUserId())
                .username(loginUser.getUsername())
                .roles(loginUser.getRoles())
                .permissions(loginUser.getPermissions())
                .build();
    }
}