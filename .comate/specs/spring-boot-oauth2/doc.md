# Spring Boot 3.5.11 OAuth2 实战配置规范

## 1. 需求概述

在 Spring Boot 3.5.11 项目中实现完整的 OAuth2 认证授权体系，包含：
- Authorization Server（授权服务器）
- Resource Server（资源服务器）
- Client Application（客户端应用）
- 完整的配置代码和步骤
- 源码调用链分析
- 生产环境扩展点
- 测试模块
- 采用多模块的项目结构，每个模块中补充readme.md文件,详细说明每个模块的功能和配置。

## 2. 技术架构

### 2.1 OAuth2 流程

```
┌─────────┐                               ┌─────────┐
│  User   │                               │ Resource│
│         │──(1) Login with OAuth2───────→│ Server  │
└─────────┘                               └─────────┘
     ↑                                          │
     │                                          │
     │(2) Authorization Request                 │
     ↓                                          │
┌─────────┐                               ┌─────────┐
│   Auth  │←─(3) User Consent─────────────│         │
│ Server  │                               │         │
│         │──(4) Authorization Code───────→│         │
│         │                               │         │
│         │──(5) Token Request────────────→│         │
│         │                               │         │
│         │──(6) Access Token─────────────→│         │
└─────────┘                               └─────────┘
```

### 2.2 模块划分

| 模块 | 说明 | 端口 |
|------|------|------|
| auth-server | 授权服务器 | 9000 |
| resource-server | 资源服务器 | 8081 |
| client-app | 客户端应用 | 8082 |

## 3. 配置代码

### 3.1 Maven 依赖配置 (pom.xml)

需要添加的 OAuth2 相关依赖：

```xml
<!-- Spring Authorization Server -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-oauth2-authorization-server</artifactId>
    <version>1.4.1</version>
</dependency>

<!-- Resource Server -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>

<!-- OAuth2 Client -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-oauth2-client</artifactId>
</dependency>

<!-- JWT Support -->
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-oauth2-jose</artifactId>
</dependency>
```

### 3.2 Authorization Server 配置

**文件位置**: `src/main/java/com/huochai/auth/AuthorizationServerConfig.java`

```java
package com.huochai.auth;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.UUID;

@Configuration
@EnableWebSecurity
public class AuthorizationServerConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            .oidc(Customizer.withDefaults());  // Enable OIDC

        http.exceptionHandling(exceptions -> exceptions
                .defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/login"),
                    new MediaTypeRequestMatcher(MediaType.TEXT_HTML)
                ))
            .oauth2ResourceServer(resourceServer -> resourceServer
                .jwt(Customizer.withDefaults()));

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/oauth2/**", "/login/**", "/error").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        // Registered Client: client-app
        RegisteredClient clientApp = RegisteredClient.withId(UUID.randomUUID().toString())
            .clientId("client-app")
            .clientSecret(passwordEncoder().encode("secret"))
            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
            .redirectUri("http://127.0.0.1:8082/login/oauth2/code/client-app")
            .redirectUri("http://127.0.0.1:8082")
            .scope(OidcScopes.OPENID)
            .scope("message.read")
            .scope("message.write")
            .clientSettings(ClientSettings.builder()
                .requireAuthorizationConsent(true)
                .requireProofKey(false)
                .build())
            .tokenSettings(TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofHours(1))
                .refreshTokenTimeToLive(Duration.ofDays(1))
                .reuseRefreshTokens(false)
                .build())
            .build();

        return new InMemoryRegisteredClientRepository(clientApp);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
            .privateKey(privateKey)
            .keyID(UUID.randomUUID().toString())
            .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    private static KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate RSA key pair", ex);
        }
        return keyPair;
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder()
            .issuer("http://127.0.0.1:9000")
            .authorizationEndpoint("/oauth2/authorize")
            .tokenEndpoint("/oauth2/token")
            .jwkSetEndpoint("/oauth2/jwks")
            .oidcUserInfoEndpoint("/userinfo")
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### 3.3 Resource Server 配置

**文件位置**: `src/main/java/com/huochai/resource/ResourceServerConfig.java`

```java
package com.huochai.resource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class ResourceServerConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/public/**", "/actuator/**").permitAll()
                .requestMatchers("/admin/**").hasAuthority("SCOPE_admin")
                .requestMatchers("/user/**").hasAnyAuthority("SCOPE_user", "SCOPE_admin")
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");
        grantedAuthoritiesConverter.setAuthorityPrefix("SCOPE_");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }
}
```

### 3.4 Client Application 配置

**文件位置**: `src/main/java/com/huochai/client/ClientApplicationConfig.java`

```java
package com.huochai.client;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class ClientApplicationConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/login").permitAll()
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .defaultSuccessUrl("/oauth2/loginSuccess", true)
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("client-app")
            .clientId("client-app")
            .clientSecret("secret")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://127.0.0.1:8082/login/oauth2/code/client-app")
            .scope("openid", "message.read", "message.write")
            .authorizationUri("http://127.0.0.1:9000/oauth2/authorize")
            .tokenUri("http://127.0.0.1:9000/oauth2/token")
            .userInfoUri("http://127.0.0.1:9000/userinfo")
            .jwkSetUri("http://127.0.0.1:9000/oauth2/jwks")
            .userNameAttributeName("sub")
            .clientName("Client App")
            .build();

        return new InMemoryClientRegistrationRepository(clientRegistration);
    }
}
```

### 3.5 User Details Service 配置

**文件位置**: `src/main/java/com/huochai/auth/CustomUserDetailsService.java`

```java
package com.huochai.auth;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;

    public CustomUserDetailsService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 从数据库加载用户信息
        // 这里使用内存模拟
        if ("admin".equals(username)) {
            return User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin123"))
                .authorities(List.of(
                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                    new SimpleGrantedAuthority("SCOPE_message.read"),
                    new SimpleGrantedAuthority("SCOPE_message.write")
                ))
                .build();
        }

        if ("user".equals(username)) {
            return User.builder()
                .username("user")
                .password(passwordEncoder.encode("user123"))
                .authorities(List.of(
                    new SimpleGrantedAuthority("ROLE_USER"),
                    new SimpleGrantedAuthority("SCOPE_message.read")
                ))
                .build();
        }

        throw new UsernameNotFoundException("User not found: " + username);
    }
}
```

## 4. 源码调用链分析

### 4.1 Authorization Code 授权流程

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Authorization Server 流程                          │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  1. Client App                                                             │
│     └─→ OAuth2AuthorizationRequestRedirectFilter                          │
│         └─→ OAuth2AuthorizationRequestResolver                           │
│             └─→ 生成授权请求重定向到 Authorization Server                  │
│                                                                             │
│  2. Authorization Server - /oauth2/authorize                              │
│     └─→ AuthorizationEndpoint                                              │
│         └─→ OAuth2AuthorizationConsentEndpoint                            │
│             └─→ 用户登录 + 授权                                            │
│                                                                             │
│  3. 授权成功后                                                              │
│     └─→ OAuth2AuthorizationCodeRequestAuthenticationProvider             │
│         └─→ 生成 Authorization Code                                       │
│         └─→ 重定向回 Client App                                           │
│                                                                             │
│  4. Client App - /oauth2/code/{registrationId}                           │
│     └─→ OAuth2LoginAuthenticationFilter                                    │
│         └─→ OAuth2AuthorizationCodeAuthenticationProvider                  │
│             └─→ 交换 Authorization Code → Access Token                   │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 4.2 核心组件调用链

#### 4.2.1 授权请求处理

```
请求: GET /oauth2/authorize?response_type=code&client_id=client-app&scope=openid message.read&redirect_uri=...

Spring Security Filter Chain:
├── SecurityContextPersistenceFilter
├── OAuth2AuthorizationRequestRedirectFilter (处理授权请求重定向)
├── OAuth2AuthorizationEndpointFilter (处理授权端点)
├── UsernamePasswordAuthenticationFilter (用户登录)
├── OAuth2AuthorizationCodeRequestAuthenticationProvider (验证授权请求)
└── AuthorizationEndpoint (生成授权码)

关键类:
- OAuth2AuthorizationRequest: 授权请求对象
- OAuth2AuthorizationConsent: 用户授权同意信息
- RegisteredClient: 注册的客户端信息
```

#### 4.2.2 Token 交换流程

```
请求: POST /oauth2/token
      grant_type=authorization_code
      code=AUTHORIZATION_CODE
      redirect_uri=...
      client_id=client-app
      client_secret=secret

OAuth2TokenEndpointFilter
└── OAuth2AuthorizationCodeAuthenticationProvider
    ├── OAuth2ClientAuthenticationVerificationService (验证客户端)
    ├── OAuth2AuthorizationCodeAuthenticationProvider (验证授权码)
    └── DefaultTokenGenerator (生成 Token)
        ├── JwtGenerator (生成 JWT)
        └── OAuth2AccessToken (访问令牌)
```

### 4.3 Resource Server 验证 Token

```
请求: GET /api/user
      Authorization: Bearer <ACCESS_TOKEN>

Filter Chain:
├── SecurityContextPersistenceFilter
├── JwtAuthenticationFilter (或 OAuth2ResourceServerFilter)
├── JwtDecoder (验证 JWT 签名)
├── JwtAuthenticationProvider
│   └── JwtGrantedAuthoritiesConverter (提取权限)
└── AuthorizationFilter (权限校验)

关键类:
- Jwt: JWT 令牌对象
- JwtDecoder: JWT 解码器
- JwtAuthenticationToken: JWT 认证令牌
- JwtGrantedAuthoritiesConverter: JWT 权限转换器
```

## 5. 生产环境扩展点

### 5.1 自定义 Token 生成器

```java
@Component
public class CustomTokenGenerator implements OAuth2TokenGenerator<OAuth2Token> {

    @Override
    public OAuth2Token generate(OAuth2TokenContext context) {
        if (context.getTokenType() == OAuth2TokenType.ACCESS_TOKEN) {
            // 自定义 Access Token 生成逻辑
            return new CustomOAuth2AccessToken(
                context.getTokenType().getValue(),
                Instant.now(),
                Instant.now().plusHours(1),
                context.getAuthorizedScopes(),
                customClaims(context)
            );
        }
        return null;
    }

    private Map<String, Object> customClaims(OAuth2TokenContext context) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("custom_claim", "value");
        claims.put("user_id", getUserId(context));
        return claims;
    }
}
```

### 5.2 自定义 UserDetailsService

```java
@Service
public class DatabaseUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return User.builder()
            .username(user.getUsername())
            .password(user.getPassword())
            .authorities(user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName()))
                .toList())
            .accountLocked(!user.isEnabled())
            .build();
    }
}
```

### 5.3 自定义 JWT 转换器

```java
@Component
public class CustomJwtAuthenticationConverter extends JwtAuthenticationConverter {

    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
        // 从 JWT 中提取自定义字段
        String userId = jwt.getClaimAsString("user_id");
        List<String> roles = jwt.getClaimAsStringList("roles");

        Collection<GrantedAuthority> authorities = new ArrayList<>();
        if (roles != null) {
            authorities.addAll(roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList()));
        }

        // 添加自定义属性到 Principal
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("user_id", userId);

        JwtAuthenticationToken token = super.convert(jwt);
        return new JwtAuthenticationToken(jwt, authorities, additionalInfo);
    }
}
```

### 5.4 自定义授权同意页面

```java
@Component
public class CustomAuthorizationConsentService {

    @Autowired
    private RegisteredClientRepository clientRepository;

    public void saveConsent(RegisteredClient registeredClient,
                           Principal principal,
                           Set<String> authorizedScopes,
                           Map<String, GrantedAuthority> authorities) {
        // 保存用户授权同意信息到数据库
        // 支持"记住我的选择"功能
    }

    public OAuth2AuthorizationConsent loadConsent(String clientId, String principalName) {
        // 从数据库加载已保存的授权同意信息
        return null;
    }
}
```

### 5.5 Token 持久化（Redis/JPA）

```java
@Component
public class RedisOAuth2TokenService implements OAuth2TokenService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public OAuth2AccessToken saveAccessToken(OAuth2Authorization authorization) {
        String key = "oauth2:token:" + authorization.getAccessToken().getTokenValue();
        redisTemplate.opsForValue().set(key, authorization, 
            authorization.getAccessToken().getExpiresAt().toEpochMilli() - System.currentTimeMillis(),
            TimeUnit.MILLISECONDS);
        return authorization.getAccessToken();
    }

    @Override
    public OAuth2Authorization loadAuthorization(String tokenValue) {
        String key = "oauth2:token:" + tokenValue;
        return (OAuth2Authorization) redisTemplate.opsForValue().get(key);
    }
}
```

### 5.6 Client 详情服务自定义

```java
@Service
public class DatabaseClientDetailsService implements RegisteredClientRepository {

    @Autowired
    private ClientRepository clientRepository;

    @Override
    public RegisteredClient findById(String id) {
        ClientEntity entity = clientRepository.findById(id);
        return convertToRegisteredClient(entity);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        ClientEntity entity = clientRepository.findByClientId(clientId);
        return convertToRegisteredClient(entity);
    }

    private RegisteredClient convertToRegisteredClient(ClientEntity entity) {
        return RegisteredClient.withId(entity.getId())
            .clientId(entity.getClientId())
            .clientSecret(entity.getClientSecret())
            // ... 其他配置
            .build();
    }
}
```

## 6. 配置步骤总结

1. **添加 Maven 依赖**到 pom.xml
2. **创建 Authorization Server 配置类**
   - 配置 RegisteredClientRepository
   - 配置 JWKSource (RSA 密钥对)
   - 配置 AuthorizationServerSettings
3. **创建 Resource Server 配置类**
   - 配置 JWT 解码器
   - 配置权限转换器
   - 配置 API 访问规则
4. **创建 Client Application 配置类**
   - 配置 ClientRegistration
   - 配置 OAuth2 Login
5. **创建 UserDetailsService 实现**
   - 实现用户加载逻辑
   - 配置密码编码器
6. **配置启动类**添加必要注解

## 7. 测试验证

### 7.1 授权端点测试

```bash
# 1. 获取授权码
curl -v "http://127.0.0.1:9000/oauth2/authorize?response_type=code&client_id=client-app&scope=openid message.read&redirect_uri=http://127.0.0.1:8082"

# 2. 交换 Token
curl -X POST "http://127.0.0.1:9000/oauth2/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -u "client-app:secret" \
  -d "grant_type=authorization_code" \
  -d "code=AUTHORIZATION_CODE" \
  -d "redirect_uri=http://127.0.0.1:8082"

# 3. 访问资源服务器
curl -H "Authorization: Bearer ACCESS_TOKEN" \
  "http://127.0.0.1:8081/api/user"
```
