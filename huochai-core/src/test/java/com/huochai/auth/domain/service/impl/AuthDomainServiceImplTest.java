package com.huochai.auth.domain.service.impl;

import com.huochai.domain.auth.bean.event.DomainEventPublisher;
import com.huochai.domain.auth.bean.AuthUser;
import com.huochai.domain.auth.bean.DeviceInfo;
import com.huochai.domain.auth.bean.LoginSession;
import com.huochai.domain.auth.bean.Token;
import com.huochai.domain.auth.bean.TokenPair;
import com.huochai.domain.auth.repository.AuthUserRepository;
import com.huochai.domain.auth.service.RedisTokenService;
import com.huochai.domain.auth.service.TokenDomainService;
import com.huochai.common.enums.ClientType;
import com.huochai.common.exception.AuthErrorCode;
import com.huochai.common.exception.AuthException;
import com.huochai.common.exception.TokenException;
import com.huochai.domain.auth.service.impl.AuthDomainServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import io.jsonwebtoken.Claims;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 认证领域服务测试类
 * 
 * 覆盖场景：
 * 1. 用户认证（正常登录、参数校验、用户不存在、密码错误、账号状态）
 * 2. Token生成（正常生成、多端登录、旧会话踢人）
 * 3. Token刷新（正常刷新、类型校验、黑名单校验）
 * 4. 登出（正常登出、空Token）
 * 5. 踢人（管理员踢人）
 *
 * @author huochai
 */
@ExtendWith(MockitoExtension.class)
class AuthDomainServiceImplTest {

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private TokenDomainService tokenDomainService;

    @Mock
    private RedisTokenService redisTokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private DomainEventPublisher eventPublisher;

    @InjectMocks
    private AuthDomainServiceImpl authDomainService;

    // 测试数据
    private AuthUser normalUser;
    private AuthUser disabledUser;
    private AuthUser invalidUser;
    private DeviceInfo webDevice;
    private DeviceInfo appDevice;
    private TokenPair tokenPair;

    @BeforeEach
    void setUp() {
        // 创建正常用户
        normalUser = new AuthUser();
        normalUser.setId(1L);
        normalUser.setUsername("testuser");
        normalUser.setPassword("$2a$10$encodedpassword"); // BCrypt加密后的密码
        normalUser.setEmail("test@example.com");
        normalUser.setPhone("13800138000");
        normalUser.setStatus(1);
        normalUser.setDeleted(0);

        // 创建禁用用户
        disabledUser = new AuthUser();
        disabledUser.setId(2L);
        disabledUser.setUsername("disableduser");
        disabledUser.setPassword("$2a$10$encodedpassword");
        disabledUser.setStatus(0); // 禁用状态
        disabledUser.setDeleted(0);

        // 创建无效用户（状态异常）
        invalidUser = new AuthUser();
        invalidUser.setId(3L);
        invalidUser.setUsername("invaliduser");
        invalidUser.setPassword("$2a$10$encodedpassword");
        invalidUser.setStatus(null); // 状态异常
        invalidUser.setDeleted(0);

        // 创建设备信息
        webDevice = DeviceInfo.builder()
                .deviceId("web-device-001")
                .clientType(ClientType.WEB)
                .ip("192.168.1.100")
                .deviceName("Chrome Browser")
                .build();

        appDevice = DeviceInfo.builder()
                .deviceId("app-device-001")
                .clientType(ClientType.APP)
                .deviceName("iPhone 15")
                .build();

        // 创建Token对
        Token accessToken = new Token();
        accessToken.setValue("access-token-value");
        accessToken.setJti("access-jti-123");
        accessToken.setExpiresAt(System.currentTimeMillis() + 7200000);

        Token refreshToken = new Token();
        refreshToken.setValue("refresh-token-value");
        refreshToken.setJti("refresh-jti-456");
        refreshToken.setExpiresAt(System.currentTimeMillis() + 604800000);

        tokenPair = new TokenPair();
        tokenPair.setAccessToken(accessToken);
        tokenPair.setRefreshToken(refreshToken);
        tokenPair.setDeviceInfo(webDevice);
        tokenPair.setSessionId("1_WEB_web-device-001");
    }

    // ==================== 用户认证测试 ====================

    @Nested
    @DisplayName("用户认证测试")
    class AuthenticateTest {

        @Test
        @DisplayName("正常登录 - 用户名密码正确")
        void authenticate_Success() {
            // Given
            when(authUserRepository.findByUsername("testuser")).thenReturn(Optional.of(normalUser));
            when(passwordEncoder.matches("password123", "$2a$10$encodedpassword")).thenReturn(true);

            // When
            AuthUser result = authDomainService.authenticate("testuser", "password123");

            // Then
            assertNotNull(result);
            assertEquals("testuser", result.getUsername());
            assertEquals(1L, result.getId());

            verify(authUserRepository).findByUsername("testuser");
            verify(passwordEncoder).matches("password123", "$2a$10$encodedpassword");
        }

        @Test
        @DisplayName("登录失败 - 用户名为空")
        void authenticate_EmptyUsername() {
            // When & Then
            AuthException exception = assertThrows(AuthException.class, 
                    () -> authDomainService.authenticate("", "password123"));

            assertEquals(AuthErrorCode.BAD_REQUEST, exception.getErrorCode());
            assertEquals("用户名或密码不能为空", exception.getMessage());

            verify(authUserRepository, never()).findByUsername(any());
        }

        @Test
        @DisplayName("登录失败 - 密码为空")
        void authenticate_EmptyPassword() {
            // When & Then
            AuthException exception = assertThrows(AuthException.class, 
                    () -> authDomainService.authenticate("testuser", ""));

            assertEquals(AuthErrorCode.BAD_REQUEST, exception.getErrorCode());
            assertEquals("用户名或密码不能为空", exception.getMessage());

            verify(authUserRepository, never()).findByUsername(any());
        }

        @Test
        @DisplayName("登录失败 - 用户不存在")
        void authenticate_UserNotFound() {
            // Given
            when(authUserRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

            // When & Then
            AuthException exception = assertThrows(AuthException.class, 
                    () -> authDomainService.authenticate("nonexistent", "password123"));

            assertEquals(AuthErrorCode.USER_NOT_FOUND, exception.getErrorCode());
            assertEquals("用户名或密码错误", exception.getMessage());

            verify(authUserRepository).findByUsername("nonexistent");
        }

        @Test
        @DisplayName("登录失败 - 密码错误")
        void authenticate_WrongPassword() {
            // Given
            when(authUserRepository.findByUsername("testuser")).thenReturn(Optional.of(normalUser));
            when(passwordEncoder.matches("wrongpassword", "$2a$10$encodedpassword")).thenReturn(false);

            // When & Then
            AuthException exception = assertThrows(AuthException.class, 
                    () -> authDomainService.authenticate("testuser", "wrongpassword"));

            assertEquals(AuthErrorCode.PASSWORD_ERROR, exception.getErrorCode());
            assertEquals("用户名或密码错误", exception.getMessage());

            verify(passwordEncoder).matches("wrongpassword", "$2a$10$encodedpassword");
        }

        @Test
        @DisplayName("登录失败 - 账号被禁用")
        void authenticate_AccountDisabled() {
            // Given
            when(authUserRepository.findByUsername("disableduser")).thenReturn(Optional.of(disabledUser));
            when(passwordEncoder.matches("password123", "$2a$10$encodedpassword")).thenReturn(true);

            // When & Then
            AuthException exception = assertThrows(AuthException.class, 
                    () -> authDomainService.authenticate("disableduser", "password123"));

            assertEquals(AuthErrorCode.ACCOUNT_DISABLED, exception.getErrorCode());
            assertEquals("账号已被禁用", exception.getMessage());
        }

        @Test
        @DisplayName("登录失败 - 账号状态异常")
        void authenticate_AccountStatusInvalid() {
            // Given
            when(authUserRepository.findByUsername("invaliduser")).thenReturn(Optional.of(invalidUser));
            when(passwordEncoder.matches("password123", "$2a$10$encodedpassword")).thenReturn(true);

            // When & Then
            AuthException exception = assertThrows(AuthException.class, 
                    () -> authDomainService.authenticate("invaliduser", "password123"));

            assertEquals(AuthErrorCode.ACCOUNT_LOCKED, exception.getErrorCode());
            assertEquals("账号状态异常", exception.getMessage());
        }

        @Test
        @DisplayName("登录失败 - 用户已删除")
        void authenticate_UserDeleted() {
            // Given
            AuthUser deletedUser = new AuthUser();
            deletedUser.setId(4L);
            deletedUser.setUsername("deleteduser");
            deletedUser.setPassword("$2a$10$encodedpassword");
            deletedUser.setStatus(1);
            deletedUser.setDeleted(1); // 已删除

            when(authUserRepository.findByUsername("deleteduser")).thenReturn(Optional.of(deletedUser));
            when(passwordEncoder.matches("password123", "$2a$10$encodedpassword")).thenReturn(true);

            // When & Then
            AuthException exception = assertThrows(AuthException.class, 
                    () -> authDomainService.authenticate("deleteduser", "password123"));

            assertEquals(AuthErrorCode.ACCOUNT_LOCKED, exception.getErrorCode());
        }
    }

    // ==================== Token生成测试 ====================

    @Nested
    @DisplayName("Token生成测试")
    class GenerateTokenPairTest {

        @Test
        @DisplayName("正常生成Token")
        void generateTokenPair_Success() {
            // Given
            when(tokenDomainService.generateTokenPair(normalUser, webDevice)).thenReturn(tokenPair);
            when(redisTokenService.getSession(1L, ClientType.WEB, "web-device-001")).thenReturn(null);

            // When
            TokenPair result = authDomainService.generateTokenPair(normalUser, webDevice);

            // Then
            assertNotNull(result);
            assertEquals("access-token-value", result.getAccessTokenValue());
            assertEquals("refresh-token-value", result.getRefreshTokenValue());

            verify(redisTokenService).storeToken(1L, tokenPair);
            verify(eventPublisher).publishUserLogin(1L, "testuser", webDevice);
        }

        @Test
        @DisplayName("多端登录 - 同一用户不同端")
        void generateTokenPair_MultiClient() {
            // Given
            TokenPair appTokenPair = createTokenPair(appDevice, "app-session-id");
            
            when(tokenDomainService.generateTokenPair(normalUser, appDevice)).thenReturn(appTokenPair);
            when(redisTokenService.getSession(1L, ClientType.APP, "app-device-001")).thenReturn(null);

            // When
            TokenPair result = authDomainService.generateTokenPair(normalUser, appDevice);

            // Then
            assertNotNull(result);
            verify(redisTokenService).storeToken(1L, appTokenPair);
        }

        @Test
        @DisplayName("旧会话踢人 - 同一设备重复登录")
        void generateTokenPair_KickOldSession() {
            // Given - 模拟存在旧会话
            LoginSession oldSession = mock(LoginSession.class);
            TokenPair oldTokenPair = mock(TokenPair.class);
            Token oldAccessToken = mock(Token.class);
            Token oldRefreshToken = mock(Token.class);

            when(oldTokenPair.getAccessTokenValue()).thenReturn("old-access-token");
            when(oldTokenPair.getRefreshTokenValue()).thenReturn("old-refresh-token");
            when(oldTokenPair.getAccessToken()).thenReturn(oldAccessToken);
            when(oldTokenPair.getRefreshToken()).thenReturn(oldRefreshToken);
            when(oldAccessToken.getRemainingTime()).thenReturn(3600000L);
            when(oldRefreshToken.getRemainingTime()).thenReturn(86400000L);
            when(oldSession.getTokenPair()).thenReturn(oldTokenPair);

            when(redisTokenService.getSession(1L, ClientType.WEB, "web-device-001")).thenReturn(oldSession);
            when(tokenDomainService.generateTokenPair(normalUser, webDevice)).thenReturn(tokenPair);

            // When
            TokenPair result = authDomainService.generateTokenPair(normalUser, webDevice);

            // Then
            assertNotNull(result);
            // 验证踢出旧会话
            verify(redisTokenService).kickOut(1L, ClientType.WEB, "web-device-001");
            // 验证发布踢出事件
            verify(eventPublisher).publishUserKickedOut(1L, "WEB", "web-device-001", "新设备登录");
            // 验证存储新Token
            verify(redisTokenService).storeToken(1L, tokenPair);
        }

        @Test
        @DisplayName("无旧会话 - 直接生成新Token")
        void generateTokenPair_NoOldSession() {
            // Given
            when(tokenDomainService.generateTokenPair(normalUser, webDevice)).thenReturn(tokenPair);
            when(redisTokenService.getSession(1L, ClientType.WEB, "web-device-001")).thenReturn(null);

            // When
            TokenPair result = authDomainService.generateTokenPair(normalUser, webDevice);

            // Then
            assertNotNull(result);
            // 不应该调用kickOut
            verify(redisTokenService, never()).kickOut(anyLong(), any(), any());
            // 验证存储Token
            verify(redisTokenService).storeToken(1L, tokenPair);
            // 验证发布登录事件
            verify(eventPublisher).publishUserLogin(1L, "testuser", webDevice);
        }
    }

    // ==================== Token刷新测试 ====================

    @Nested
    @DisplayName("Token刷新测试")
    class RefreshTokenTest {

        @Test
        @DisplayName("正常刷新Token")
        void refreshToken_Success() {
            // Given
            Claims claims = mock(Claims.class);
            when(claims.get("tokenType", String.class)).thenReturn("REFRESH");
            
            when(tokenDomainService.parseToken("refresh-token-value")).thenReturn(claims);
            when(redisTokenService.isBlacklisted("refresh-token-value")).thenReturn(false);
            when(tokenDomainService.getUserId("refresh-token-value")).thenReturn(1L);
            when(tokenDomainService.getUsername("refresh-token-value")).thenReturn("testuser");
            when(tokenDomainService.getJti("refresh-token-value")).thenReturn("old-jti");
            when(tokenDomainService.getTokenRemainingTime("refresh-token-value")).thenReturn(86400000L);
            when(authUserRepository.findById(1L)).thenReturn(Optional.of(normalUser));
            when(tokenDomainService.generateTokenPair(eq(normalUser), eq(webDevice))).thenReturn(tokenPair);

            // When
            TokenPair result = authDomainService.refreshToken("refresh-token-value", webDevice);

            // Then
            assertNotNull(result);
            // 验证旧Token加入黑名单
            verify(redisTokenService).addToBlacklist("refresh-token-value", 86400000L);
            // 验证存储新Token
            verify(redisTokenService).storeToken(1L, tokenPair);
            // 验证发布刷新事件
            verify(eventPublisher).publishTokenRefreshed(1L, "testuser", "access-jti-123", "old-jti");
        }

        @Test
        @DisplayName("刷新失败 - Token类型错误（使用AccessToken刷新）")
        void refreshToken_WrongTokenType() {
            // Given
            Claims claims = mock(Claims.class);
            when(claims.get("tokenType", String.class)).thenReturn("ACCESS");
            when(tokenDomainService.parseToken("access-token-value")).thenReturn(claims);

            // When & Then
            TokenException exception = assertThrows(TokenException.class, 
                    () -> authDomainService.refreshToken("access-token-value", webDevice));

            assertEquals(AuthErrorCode.REFRESH_TOKEN_INVALID, exception.getErrorCode());
            assertEquals("无效的刷新Token", exception.getMessage());
        }

        @Test
        @DisplayName("刷新失败 - Token在黑名单中")
        void refreshToken_TokenBlacklisted() {
            // Given
            Claims claims = mock(Claims.class);
            when(claims.get("tokenType", String.class)).thenReturn("REFRESH");
            when(tokenDomainService.parseToken("refresh-token-value")).thenReturn(claims);
            when(redisTokenService.isBlacklisted("refresh-token-value")).thenReturn(true);

            // When & Then
            assertThrows(TokenException.BlacklistedException.class, 
                    () -> authDomainService.refreshToken("refresh-token-value", webDevice));
        }

        @Test
        @DisplayName("刷新失败 - Token解析失败")
        void refreshToken_TokenParseFailed() {
            // Given
            when(tokenDomainService.parseToken("invalid-token")).thenThrow(new TokenException.InvalidException());

            // When & Then
            assertThrows(TokenException.class, 
                    () -> authDomainService.refreshToken("invalid-token", webDevice));
        }

        @Test
        @DisplayName("刷新失败 - 用户不存在")
        void refreshToken_UserNotFound() {
            // Given
            Claims claims = mock(Claims.class);
            when(claims.get("tokenType", String.class)).thenReturn("REFRESH");
            when(tokenDomainService.parseToken("refresh-token-value")).thenReturn(claims);
            when(redisTokenService.isBlacklisted("refresh-token-value")).thenReturn(false);
            when(tokenDomainService.getUserId("refresh-token-value")).thenReturn(999L);
            when(authUserRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThrows(AuthException.class, 
                    () -> authDomainService.refreshToken("refresh-token-value", webDevice));
        }

        @Test
        @DisplayName("刷新失败 - 用户状态异常")
        void refreshToken_UserStatusInvalid() {
            // Given
            Claims claims = mock(Claims.class);
            when(claims.get("tokenType", String.class)).thenReturn("REFRESH");
            when(tokenDomainService.parseToken("refresh-token-value")).thenReturn(claims);
            when(redisTokenService.isBlacklisted("refresh-token-value")).thenReturn(false);
            when(tokenDomainService.getUserId("refresh-token-value")).thenReturn(2L);
            when(authUserRepository.findById(2L)).thenReturn(Optional.of(disabledUser));

            // When & Then
            AuthException exception = assertThrows(AuthException.class, 
                    () -> authDomainService.refreshToken("refresh-token-value", webDevice));

            assertEquals(AuthErrorCode.ACCOUNT_DISABLED, exception.getErrorCode());
        }
    }

    // ==================== 登出测试 ====================

    @Nested
    @DisplayName("登出测试")
    class LogoutTest {

        @Test
        @DisplayName("正常登出")
        void logout_Success() {
            // Given
            when(tokenDomainService.getUserId("access-token-value")).thenReturn(1L);
            when(tokenDomainService.getUsername("access-token-value")).thenReturn("testuser");
            when(tokenDomainService.getClientType("access-token-value")).thenReturn("WEB");
            when(tokenDomainService.getDeviceId("access-token-value")).thenReturn("web-device-001");
            when(tokenDomainService.getJti("access-token-value")).thenReturn("access-jti-123");

            // When
            authDomainService.logout("access-token-value");

            // Then
            verify(redisTokenService).kickOut(1L, ClientType.WEB, "web-device-001");
            verify(eventPublisher).publishUserLogout(1L, "testuser", "WEB", "web-device-001");
        }

        @Test
        @DisplayName("登出 - Token为空")
        void logout_EmptyToken() {
            // When
            authDomainService.logout("");

            // Then - 不应该调用任何服务
            verify(tokenDomainService, never()).getUserId(any());
            verify(redisTokenService, never()).kickOut(anyLong(), any(), any());
        }

        @Test
        @DisplayName("登出 - Token为null")
        void logout_NullToken() {
            // When
            authDomainService.logout(null);

            // Then - 不应该调用任何服务
            verify(tokenDomainService, never()).getUserId(any());
            verify(redisTokenService, never()).kickOut(anyLong(), any(), any());
        }

        @Test
        @DisplayName("登出 - Token解析异常时静默失败")
        void logout_TokenParseError() {
            // Given
            when(tokenDomainService.getUserId("invalid-token")).thenThrow(new TokenException.InvalidException());

            // When - 不应该抛出异常
            assertDoesNotThrow(() -> authDomainService.logout("invalid-token"));

            // Then - 不应该调用kickOut
            verify(redisTokenService, never()).kickOut(anyLong(), any(), any());
        }
    }

    // ==================== 踢人测试 ====================

    @Nested
    @DisplayName("踢人测试")
    class KickOutTest {

        @Test
        @DisplayName("管理员踢人 - 正常踢出")
        void kickOut_Success() {
            // When
            authDomainService.kickOut(1L, "WEB", "web-device-001");

            // Then
            verify(redisTokenService).kickOut(1L, ClientType.WEB, "web-device-001");
            verify(eventPublisher).publishUserKickedOut(1L, "WEB", "web-device-001", "管理员操作");
        }

        @Test
        @DisplayName("踢人 - 不同客户端类型")
        void kickOut_DifferentClientTypes() {
            // When & Then - 踢出APP端
            authDomainService.kickOut(1L, "APP", "app-device-001");
            verify(redisTokenService).kickOut(1L, ClientType.APP, "app-device-001");

            // When & Then - 踢出小程序端
            authDomainService.kickOut(1L, "MINI_PROGRAM", "mini-device-001");
            verify(redisTokenService).kickOut(1L, ClientType.MINI_PROGRAM, "mini-device-001");
        }
    }

    // ==================== 边界场景测试 ====================

    @Nested
    @DisplayName("边界场景测试")
    class EdgeCaseTest {

        @Test
        @DisplayName("并发登录 - 模拟同一用户同时在不同设备登录")
        void concurrentLogin_DifferentDevices() {
            // Given
            when(tokenDomainService.generateTokenPair(normalUser, webDevice)).thenReturn(tokenPair);
            when(tokenDomainService.generateTokenPair(normalUser, appDevice)).thenReturn(createTokenPair(appDevice, "app-session"));
            when(redisTokenService.getSession(anyLong(), any(), any())).thenReturn(null);

            // When - 同时在两个设备登录
            TokenPair webResult = authDomainService.generateTokenPair(normalUser, webDevice);
            TokenPair appResult = authDomainService.generateTokenPair(normalUser, appDevice);

            // Then - 两个会话都应该成功创建
            assertNotNull(webResult);
            assertNotNull(appResult);
            verify(redisTokenService, times(2)).storeToken(anyLong(), any());
        }

        @Test
        @DisplayName("特殊字符用户名")
        void authenticate_SpecialCharacters() {
            // Given
            AuthUser specialUser = new AuthUser();
            specialUser.setId(10L);
            specialUser.setUsername("user@#$%");
            specialUser.setPassword("$2a$10$encodedpassword");
            specialUser.setStatus(1);
            specialUser.setDeleted(0);

            when(authUserRepository.findByUsername("user@#$%")).thenReturn(Optional.of(specialUser));
            when(passwordEncoder.matches("password123", "$2a$10$encodedpassword")).thenReturn(true);

            // When
            AuthUser result = authDomainService.authenticate("user@#$%", "password123");

            // Then
            assertNotNull(result);
            assertEquals("user@#$%", result.getUsername());
        }

        @Test
        @DisplayName("超长用户名")
        void authenticate_LongUsername() {
            // Given
            String longUsername = "a".repeat(1000);
            when(authUserRepository.findByUsername(longUsername)).thenReturn(Optional.empty());

            // When & Then
            AuthException exception = assertThrows(AuthException.class, 
                    () -> authDomainService.authenticate(longUsername, "password123"));

            assertEquals(AuthErrorCode.USER_NOT_FOUND, exception.getErrorCode());
        }

        @Test
        @DisplayName("Token刷新 - 验证RefreshToken只能使用一次")
        void refreshToken_RefreshTokenCanBeUsedOnlyOnce() {
            // Given
            Claims claims = mock(Claims.class);
            when(claims.get("tokenType", String.class)).thenReturn("REFRESH");
            when(tokenDomainService.parseToken("refresh-token-value")).thenReturn(claims);
            when(redisTokenService.isBlacklisted("refresh-token-value")).thenReturn(false);
            when(tokenDomainService.getUserId("refresh-token-value")).thenReturn(1L);
            when(tokenDomainService.getUsername("refresh-token-value")).thenReturn("testuser");
            when(tokenDomainService.getJti("refresh-token-value")).thenReturn("old-jti");
            when(tokenDomainService.getTokenRemainingTime("refresh-token-value")).thenReturn(86400000L);
            when(authUserRepository.findById(1L)).thenReturn(Optional.of(normalUser));
            when(tokenDomainService.generateTokenPair(eq(normalUser), eq(webDevice))).thenReturn(tokenPair);

            // When
            authDomainService.refreshToken("refresh-token-value", webDevice);

            // Then - 验证旧Token被加入黑名单
            verify(redisTokenService).addToBlacklist("refresh-token-value", 86400000L);
        }
    }

    // ==================== 辅助方法 ====================

    private TokenPair createTokenPair(DeviceInfo deviceInfo, String sessionId) {
        Token accessToken = new Token();
        accessToken.setValue("access-token-" + deviceInfo.getDeviceId());
        accessToken.setJti("access-jti-" + deviceInfo.getDeviceId());
        accessToken.setExpiresAt(System.currentTimeMillis() + 7200000);

        Token refreshToken = new Token();
        refreshToken.setValue("refresh-token-" + deviceInfo.getDeviceId());
        refreshToken.setJti("refresh-jti-" + deviceInfo.getDeviceId());
        refreshToken.setExpiresAt(System.currentTimeMillis() + 604800000);

        TokenPair pair = new TokenPair();
        pair.setAccessToken(accessToken);
        pair.setRefreshToken(refreshToken);
        pair.setDeviceInfo(deviceInfo);
        pair.setSessionId(sessionId);
        return pair;
    }
}