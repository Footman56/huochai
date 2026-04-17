package com.huochai.auth.domain.service.impl;

import com.huochai.domain.auth.bean.DeviceInfo;
import com.huochai.domain.auth.bean.LoginSession;
import com.huochai.domain.auth.bean.Token;
import com.huochai.domain.auth.bean.TokenPair;
import com.huochai.domain.auth.service.TokenDomainService;
import com.huochai.common.enums.AuthConstants;
import com.huochai.common.enums.ClientType;
import com.huochai.domain.auth.service.impl.RedisTokenServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Redis Token 服务测试类
 *
 * 覆盖场景：
 * 1. Token存储和获取
 * 2. Token验证（黑名单检查）
 * 3. 黑名单管理
 * 4. 会话踢出
 * 5. 踢出状态检测
 *
 * @author huochai
 */
@ExtendWith(MockitoExtension.class)
class RedisTokenServiceImplTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private TokenDomainService tokenDomainService;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RedisTokenServiceImpl redisTokenService;

    private TokenPair tokenPair;
    private LoginSession session;
    private DeviceInfo deviceInfo;

    @BeforeEach
    void setUp() {
        // 创建设备信息
        deviceInfo = DeviceInfo.builder()
                .deviceId("test-device-001")
                .clientType(ClientType.WEB)
                .ip("192.168.1.100")
                .build();

        // 创建Token
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
        tokenPair.setDeviceInfo(deviceInfo);
        tokenPair.setSessionId("1_WEB_test-device-001");

        // 创建会话
        session = mock(LoginSession.class);
        // 使用 lenient 避免未使用的 stub 报错
        lenient().when(session.getTokenPair()).thenReturn(tokenPair);
        lenient().when(session.getDeviceInfo()).thenReturn(deviceInfo);
    }

    // ==================== Token存储测试 ====================

    @Nested
    @DisplayName("Token存储测试")
    class StoreTokenTest {

        @Test
        @DisplayName("正常存储Token")
        void storeToken_Success() {
            // Given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(tokenDomainService.getUsername("access-token-value")).thenReturn("testuser");

            // When
            redisTokenService.storeToken(1L, tokenPair);

            // Then
            verify(valueOperations, times(2)).set(anyString(), any(), anyLong(), any());
        }

        @Test
        @DisplayName("存储失败 - TokenPair为null")
        void storeToken_NullTokenPair() {
            // When
            redisTokenService.storeToken(1L, null);

            // Then - 不应该调用任何Redis操作
            verify(redisTemplate, never()).opsForValue();
        }

        @Test
        @DisplayName("存储失败 - DeviceInfo为null")
        void storeToken_NullDeviceInfo() {
            // Given
            TokenPair invalidTokenPair = new TokenPair();
            invalidTokenPair.setDeviceInfo(null);

            // When
            redisTokenService.storeToken(1L, invalidTokenPair);

            // Then - 不应该调用任何Redis操作
            verify(redisTemplate, never()).opsForValue();
        }
    }

    // ==================== Token验证测试 ====================

    @Nested
    @DisplayName("Token验证测试")
    class ValidateTokenTest {

        @Test
        @DisplayName("验证成功 - Token有效且不在黑名单")
        void validateToken_Success() {
            // Given
            when(redisTemplate.hasKey(anyString())).thenReturn(false);
            when(tokenDomainService.validateToken("valid-token")).thenReturn(true);

            // When
            boolean result = redisTokenService.validateToken("valid-token");

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("验证失败 - Token为空")
        void validateToken_EmptyToken() {
            // When
            boolean result = redisTokenService.validateToken("");

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("验证失败 - Token为null")
        void validateToken_NullToken() {
            // When
            boolean result = redisTokenService.validateToken(null);

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("验证失败 - Token在黑名单中")
        void validateToken_Blacklisted() {
            // Given
            when(redisTemplate.hasKey(contains("blacklist"))).thenReturn(true);
            when(tokenDomainService.getJti("blacklisted-token")).thenReturn("jti-123");

            // When
            boolean result = redisTokenService.validateToken("blacklisted-token");

            // Then
            assertFalse(result);
            verify(tokenDomainService, never()).validateToken(any());
        }

        @Test
        @DisplayName("验证失败 - Token本身无效")
        void validateToken_InvalidToken() {
            // Given
            when(redisTemplate.hasKey(anyString())).thenReturn(false);
            when(tokenDomainService.validateToken("invalid-token")).thenReturn(false);

            // When
            boolean result = redisTokenService.validateToken("invalid-token");

            // Then
            assertFalse(result);
        }
    }

    // ==================== 黑名单测试 ====================

    @Nested
    @DisplayName("黑名单测试")
    class BlacklistTest {

        @Test
        @DisplayName("加入黑名单成功")
        void addToBlacklist_Success() {
            // Given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(tokenDomainService.getJti("token-to-blacklist")).thenReturn("jti-123");

            // When
            redisTokenService.addToBlacklist("token-to-blacklist", 3600000L);

            // Then
            verify(valueOperations).set(contains("blacklist"), eq("1"), eq(3600000L), any());
        }

        @Test
        @DisplayName("检查黑名单 - Token在黑名单中")
        void isBlacklisted_True() {
            // Given
            when(redisTemplate.hasKey(anyString())).thenReturn(true);
            when(tokenDomainService.getJti("blacklisted-token")).thenReturn("jti-123");

            // When
            boolean result = redisTokenService.isBlacklisted("blacklisted-token");

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("检查黑名单 - Token不在黑名单中")
        void isBlacklisted_False() {
            // Given
            when(redisTemplate.hasKey(anyString())).thenReturn(false);

            // When
            boolean result = redisTokenService.isBlacklisted("clean-token");

            // Then
            assertFalse(result);
        }
    }

    // ==================== 会话踢出测试 ====================

    @Nested
    @DisplayName("会话踢出测试")
    class KickOutTest {

        @Test
        @DisplayName("踢出成功 - 会话存在")
        void kickOut_Success() {
            // Given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(session);
            when(redisTemplate.hasKey(anyString())).thenReturn(false);
            when(session.getTokenPair()).thenReturn(tokenPair);

            Token accessToken = mock(Token.class);
            Token refreshToken = mock(Token.class);
            when(accessToken.getRemainingTime()).thenReturn(3600000L);
            when(refreshToken.getRemainingTime()).thenReturn(86400000L);
            when(tokenPair.getAccessToken()).thenReturn(accessToken);
            when(tokenPair.getRefreshToken()).thenReturn(refreshToken);
            when(tokenPair.getAccessTokenValue()).thenReturn("access-token");
            when(tokenPair.getRefreshTokenValue()).thenReturn("refresh-token");

            // When
            redisTokenService.kickOut(1L, ClientType.WEB, "test-device-001");

            // Then
            verify(redisTemplate, times(3)).delete(anyString()); // token key + session key
            verify(valueOperations, times(2)).set(contains("blacklist"), eq("1"), anyLong(), any());
        }

        @Test
        @DisplayName("踢出 - 会话不存在")
        void kickOut_NoSession() {
            // Given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null);

            // When
            redisTokenService.kickOut(1L, ClientType.WEB, "non-existent-device");

            // Then
            verify(redisTemplate, times(2)).delete(anyString());
        }

        @Test
        @DisplayName("踢出所有会话")
        void kickOutAll_Success() {
            // Given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(redisTemplate.keys(anyString())).thenReturn(Set.of(
                    AuthConstants.TOKEN_KEY_PREFIX + "1:WEB:device-001",
                    AuthConstants.TOKEN_KEY_PREFIX + "1:APP:device-002"
            ));
            when(valueOperations.get(anyString())).thenReturn(session);

            Token accessToken = mock(Token.class);
            Token refreshToken = mock(Token.class);
            when(accessToken.getRemainingTime()).thenReturn(3600000L);
            when(refreshToken.getRemainingTime()).thenReturn(86400000L);
            when(tokenPair.getAccessToken()).thenReturn(accessToken);
            when(tokenPair.getRefreshToken()).thenReturn(refreshToken);
            when(tokenPair.getAccessTokenValue()).thenReturn("access-token");
            when(tokenPair.getRefreshTokenValue()).thenReturn("refresh-token");

            when(session.getTokenPair()).thenReturn(tokenPair);
            when(session.getDeviceInfo()).thenReturn(deviceInfo);

            // When
            redisTokenService.kickOutAll(1L);

            // Then - 验证踢出被调用
            verify(redisTemplate, atLeastOnce()).delete(anyString());
        }
    }

    // ==================== 踢出状态检测测试 ====================

    @Nested
    @DisplayName("踢出状态检测测试")
    class IsKickedOutTest {

        @Test
        @DisplayName("未被踢出 - 当前设备ID匹配")
        void isKickedOut_NotKickedOut() {
            // Given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn("test-device-001");

            // When
            boolean result = redisTokenService.isKickedOut(1L, ClientType.WEB, "test-device-001");

            // Then
            assertFalse(result);
        }

        @Test
        @DisplayName("已被踢出 - 会话不存在")
        void isKickedOut_SessionNotFound() {
            // Given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null);

            // When
            boolean result = redisTokenService.isKickedOut(1L, ClientType.WEB, "test-device-001");

            // Then
            assertTrue(result);
        }

        @Test
        @DisplayName("已被踢出 - 设备ID不匹配")
        void isKickedOut_DeviceIdMismatch() {
            // Given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn("another-device-002");

            // When
            boolean result = redisTokenService.isKickedOut(1L, ClientType.WEB, "test-device-001");

            // Then
            assertTrue(result);
        }
    }

    // ==================== 会话获取测试 ====================

    @Nested
    @DisplayName("会话获取测试")
    class GetSessionTest {

        @Test
        @DisplayName("获取会话成功")
        void getSession_Success() {
            // Given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(session);

            // When
            LoginSession result = redisTokenService.getSession(1L, ClientType.WEB, "test-device-001");

            // Then
            assertNotNull(result);
        }

        @Test
        @DisplayName("获取会话 - 会话不存在")
        void getSession_NotFound() {
            // Given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null);

            // When
            LoginSession result = redisTokenService.getSession(1L, ClientType.WEB, "non-existent");

            // Then
            assertNull(result);
        }

        @Test
        @DisplayName("获取所有会话")
        void getAllSessions_Success() {
            // Given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(redisTemplate.keys(anyString())).thenReturn(Set.of(
                    AuthConstants.TOKEN_KEY_PREFIX + "1:WEB:device-001",
                    AuthConstants.TOKEN_KEY_PREFIX + "1:APP:device-002"
            ));
            when(valueOperations.get(anyString())).thenReturn(session);

            // When
            List<LoginSession> result = redisTokenService.getAllSessions(1L);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("获取所有会话 - 无会话")
        void getAllSessions_Empty() {
            // Given
            when(redisTemplate.keys(anyString())).thenReturn(null);

            // When
            List<LoginSession> result = redisTokenService.getAllSessions(1L);

            // Then
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }
}