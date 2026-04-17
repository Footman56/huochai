package com.huochai.config.filter;

import com.huochai.domain.auth.service.RedisTokenService;
import com.huochai.domain.auth.service.TokenDomainService;
import com.huochai.domain.permission.bean.LoginUser;
import com.huochai.common.enums.AuthConstants;
import com.huochai.common.enums.ClientType;
import com.huochai.common.exception.TokenException;
import com.huochai.domain.permission.service.RbacDomainService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 认证过滤器
 *
 * @author huochai
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private TokenDomainService tokenDomainService;

    @Autowired
    private RedisTokenService redisTokenService;

    @Autowired
    private RbacDomainService rbacDomainService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // 从请求头获取Token
            String token = extractToken(request);

            if (StrUtil.isNotBlank(token)) {
                // 验证Token
                if (redisTokenService.validateToken(token)) {
                    log.debug("Token有效: {}", token);
                    // 解析Token
                    Long userId = tokenDomainService.getUserId(token);
                    String username = tokenDomainService.getUsername(token);
                    String clientType = tokenDomainService.getClientType(token);
                    String deviceId = tokenDomainService.getDeviceId(token);

                    log.debug("解析Token: userId={}, username={}, clientType={}, deviceId={}", userId, username, clientType, deviceId);

                    // 检查是否被踢出
                    if (redisTokenService.isKickedOut(userId, ClientType.fromCode(clientType), deviceId)) {
                        log.warn("用户已被踢出: userId={}", userId);
                        throw new TokenException.BlacklistedException();
                    }

                    // 加载权限
                    List<String> permissions = rbacDomainService.loadUserPermissions(userId);
                    List<String> roles = rbacDomainService.getUserRoleCodes(userId);

                    // 构建登录用户
                    LoginUser loginUser = new LoginUser();
                    loginUser.setUserId(userId);
                    loginUser.setUsername(username);
                    loginUser.setClientType(clientType);
                    loginUser.setDeviceId(deviceId);
                    loginUser.setToken(token);
                    loginUser.setPermissions(permissions);
                    loginUser.setRoles(roles);

                    // 设置认证信息
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // 刷新会话访问时间
                    redisTokenService.refreshSessionAccessTime(userId, ClientType.fromCode(clientType), deviceId);

                    log.debug("JWT认证成功: userId={}, username={}", userId, username);
                }
            }
        } catch (TokenException e) {
            log.warn("Token验证失败: {}", e.getMessage());
            // 清除认证信息
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头提取Token
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(AuthConstants.AUTHORIZATION_HEADER);
        if (StrUtil.isNotBlank(header) && header.startsWith(AuthConstants.BEARER_PREFIX)) {
            return header.substring(AuthConstants.BEARER_PREFIX.length());
        }
        return null;
    }
}