# Auth Server (Authorization Server)

## 模块说明

本模块实现 OAuth2 授权服务器，基于 Spring Authorization Server 1.4.1 实现。

## 功能特性

- 支持 Authorization Code 授权流程
- 支持 Refresh Token
- 支持 Client Credentials 授权
- 支持 OIDC (OpenID Connect)
- 自动生成 RSA 密钥对用于 JWT 签名
- 内置用户认证服务 (CustomUserDetailsService)

## 核心配置

### AuthorizationServerConfig

主要 Bean 配置：

1. **SecurityFilterChain** - 安全过滤链配置
   - 授权服务器安全过滤链 (@Order(1))
   - 默认安全过滤链 (@Order(2))

2. **RegisteredClientRepository** - 注册客户端存储
   - 内存存储 (InMemoryRegisteredClientRepository)
   - 支持多个客户端注册

3. **JWKSource** - JWT 密钥源
   - 自动生成 RSA 2048 位密钥对
   - 用于签名 JWT 访问令牌

4. **AuthorizationServerSettings** - 授权服务器设置
   - Issuer: http://127.0.0.1:9000
   - 授权端点: /oauth2/authorize
   - Token 端点: /oauth2/token
   - JWK Set 端点: /oauth2/jwks
   - UserInfo 端点: /userinfo

### 内置测试用户

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | ROLE_ADMIN, SCOPE_message.read, SCOPE_message.write |
| user | user123 | ROLE_USER, SCOPE_message.read |

### 注册客户端

- **client-id**: client-app
- **client-secret**: secret
- **授权类型**: authorization_code, refresh_token, client_credentials
- **回调地址**: http://127.0.0.1:8082/login/oauth2/code/client-app
- **Scopes**: openid, message.read, message.write

## 启动方式

```bash
cd auth-server
mvn spring-boot:run
```

服务启动后访问: http://127.0.0.1:9000

## OAuth2 授权流程

```
1. 用户访问客户端应用 http://127.0.0.1:8082
2. 客户端重定向到授权服务器 http://127.0.0.1:9000/oauth2/authorize
3. 用户登录 (admin/admin123 或 user/user123)
4. 用户授权同意
5. 授权服务器返回授权码给客户端
6. 客户端使用授权码换取访问令牌
7. 客户端使用访问令牌访问资源服务器
```

## 源码调用链

### 授权请求处理

```
OAuth2AuthorizationRequestRedirectFilter
    → OAuth2AuthorizationRequestResolver
    → AuthorizationEndpoint
    → OAuth2AuthorizationConsentEndpoint
    → OAuth2AuthorizationCodeRequestAuthenticationProvider
    → 生成 Authorization Code
```

### Token 交换

```
OAuth2TokenEndpointFilter
    → OAuth2AuthorizationCodeAuthenticationProvider
    → OAuth2ClientAuthenticationVerificationService
    → DefaultTokenGenerator
    → JwtGenerator
```

## 生产环境扩展点

1. **自定义 RegisteredClientRepository** - 使用数据库存储客户端信息
2. **自定义 UserDetailsService** - 从数据库加载用户
3. **自定义 Token 生成器** - 添加自定义 Claims
4. **Token 持久化** - 使用 Redis 存储 Token
5. **自定义授权同意页面** - 定制授权页面 UI