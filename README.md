# 火柴认证系统 (Huochai Auth System)

<p align="center">
  <b>基于 DDD 架构的生产级完整登录认证体系</b>
</p>

<p align="center">
  <a href="#功能特性">功能特性</a> •
  <a href="#登录认证流程">登录认证流程</a> •
  <a href="#技术架构">技术架构</a> •
  <a href="#快速开始">快速开始</a> •
  <a href="#API文档">API文档</a>
</p>

---

## 功能特性

### 核心认证功能

| 功能 | 说明 |
|------|------|
| 🔐 **用户名/密码登录** | 支持 BCrypt 密码加密，安全性高 |
| 🎫 **双 Token 机制** | AccessToken (2小时) + RefreshToken (7天) |
| 📱 **多端登录** | 支持 Web/App/小程序，Token 独立管理 |
| 🔄 **Token 刷新** | 无感刷新，用户无感知 |
| 🚪 **安全登出** | Token 加入黑名单，防止重放攻击 |
| 👤 **单点登录（踢人）** | 同端新登录自动踢出旧会话 |

### 权限控制

| 功能 | 说明 |
|------|------|
| 🛡️ **RBAC 权限模型** | 用户-角色-权限 三层模型 |
| 🔗 **URL 级权限** | 基于 Spring Security 的 URL 拦截 |
| 📝 **方法级权限** | 支持 `@PreAuthorize` 注解 |
| 📊 **权限缓存** | Redis 缓存用户权限，提升性能 |

### 安全增强

| 功能 | 说明 |
|------|------|
| 🔒 **验证码** | 图形验证码防止暴力破解 |
| ⏱️ **登录限流** | IP + 用户名双维度限流 |
| 📋 **登录日志审计** | 记录所有登录行为 |
| 📝 **操作日志** | `@OperationLog` 注解记录操作 |
| 🔍 **TraceId 链路追踪** | 全链路日志追踪 |

### OAuth2 / SSO

| 功能 | 说明 |
|------|------|
| 🌐 **OAuth2 Authorization Server** | 标准 Spring Authorization Server 1.x |
| 🔑 **授权码模式** | Authorization Code Flow，支持 Web/App 客户端 |
| 🔄 **刷新令牌** | Refresh Token 自动续期 |
| 🤖 **客户端凭证模式** | Client Credentials，服务间调用 |
| 📋 **OIDC 支持** | OpenID Connect 1.0，标准 UserInfo 端点 |
| 🏢 **统一认证中心** | SSO 单点登录 |
| 🔐 **令牌内省/撤销** | Token Introspection & Revocation |

---

## OAuth2 功能详解

### 双认证体系架构

本系统同时支持两种认证方式，互不影响：

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            huochai-core (主应用)                              │
│                              Port: 8443                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────┐    ┌─────────────────────────────────────────┐ │
│  │   自定义 JWT 认证体系      │    │         OAuth2 授权服务器               │ │
│  │   (适用于自有前端)        │    │     (适用于第三方接入、SSO)              │ │
│  │                         │    │                                         │ │
│  │  • /auth/login          │    │  • /oauth2/authorize                    │ │
│  │  • /auth/logout         │    │  • /oauth2/token                        │ │
│  │  • /auth/refresh        │    │  • /oauth2/jwks                         │ │
│  │  • /auth/captcha        │    │  • /userinfo (OIDC)                     │ │
│  │                         │    │  • /oauth2/introspect                   │ │
│  │  使用 jjwt + Redis      │    │  • /oauth2/revoke                       │ │
│  │                         │    │                                         │ │
│  └─────────────────────────┘    └─────────────────────────────────────────┘ │
│                                                                              │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                        共享基础设施                                     │  │
│  │  • AuthUserRepository (共享用户数据)                                    │  │
│  │  • RedisTokenService (共享 Redis)                                      │  │
│  │  • UserDetailsServiceImpl (共享用户详情)                                │  │
│  │  • RbacDomainService (共享权限服务)                                     │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### OAuth2 授权码流程

```
┌──────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────┐
│  用户    │     │ 客户端应用    │     │  授权服务器   │     │ 资源服务 │
│ (User)   │     │ (Client App) │     │ (huochai)    │     │  (API)   │
└────┬─────┘     └──────┬───────┘     └──────┬───────┘     └────┬─────┘
     │                  │                    │                  │
     │  1. 访问受保护资源  │                    │                  │
     │◄─────────────────│                    │                  │
     │                  │                    │                  │
     │  2. 重定向到授权页                    │                  │
     │─────────────────────────────────────►│                  │
     │     GET /oauth2/authorize?           │                  │
     │       response_type=code             │                  │
     │       &client_id=web-app             │                  │
     │       &scope=openid profile          │                  │
     │       &redirect_uri=xxx              │                  │
     │                  │                    │                  │
     │  3. 用户登录并授权                    │                  │
     │─────────────────────────────────────►│                  │
     │                  │                    │                  │
     │  4. 重定向回客户端 (带授权码)          │                  │
     │◄─────────────────────────────────────│                  │
     │     redirect_uri?code=xxx&state=yyy  │                  │
     │                  │                    │                  │
     │                  │  5. 用授权码换令牌  │                  │
     │                  │───────────────────►│                  │
     │                  │  POST /oauth2/token                  │
     │                  │  grant_type=authorization_code       │
     │                  │  code=xxx            │                  │
     │                  │                    │                  │
     │                  │  6. 返回访问令牌    │                  │
     │                  │◄───────────────────│                  │
     │                  │  { access_token, refresh_token }     │
     │                  │                    │                  │
     │                  │  7. 访问资源 API    │                  │
     │                  │─────────────────────────────────────►│
     │                  │  Authorization: Bearer {token}       │
     │                  │                    │                  │
     │                  │  8. 返回资源数据    │                  │
     │                  │◄─────────────────────────────────────│
     │                  │                    │                  │
```

### OAuth2 API 端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `/oauth2/authorize` | GET/POST | 授权端点，获取授权码 |
| `/oauth2/token` | POST | 令牌端点，获取/刷新令牌 |
| `/oauth2/jwks` | GET | JWK Set 端点，公钥信息 |
| `/oauth2/introspect` | POST | 令牌内省端点，验证令牌 |
| `/oauth2/revoke` | POST | 令牌撤销端点，撤销令牌 |
| `/userinfo` | GET | OIDC 用户信息端点 |
| `/.well-known/openid-configuration` | GET | OIDC 发现端点 |

### 预置客户端

| 客户端 ID | 类型 | 授权类型 | 用途 |
|-----------|------|----------|------|
| `web-app` | Web 应用 | authorization_code, refresh_token, client_credentials | Web 前端应用 |
| `mobile-app` | 移动应用 | authorization_code, refresh_token | 移动端 App |
| `service-client` | 服务调用 | client_credentials | 服务间调用 |

### OAuth2 使用示例

#### 1. 授权码模式（Web 应用）

```bash
# Step 1: 跳转授权页面
GET https://localhost:8443/oauth2/authorize?
    response_type=code&
    client_id=web-app&
    scope=openid profile email&
    redirect_uri=https://localhost:8443/callback&
    state=xyz

# Step 2: 用户授权后，回调获取授权码
# https://localhost:8443/callback?code=xxx&state=xyz

# Step 3: 用授权码换令牌
curl -X POST https://localhost:8443/oauth2/token \n  -H "Content-Type: application/x-www-form-urlencoded" \n  -d "grant_type=authorization_code" \n  -d "code=xxx" \n  -d "redirect_uri=https://localhost:8443/callback" \n  -u "web-app:secret"

# 响应
{
  "access_token": "eyJhbGciOiJSUzI1NiJ9...",
  "token_type": "Bearer",
  "expires_in": 7200,
  "refresh_token": "xxx",
  "scope": "openid profile email",
  "id_token": "eyJhbGciOiJSUzI1NiJ9..."
}
```

#### 2. 客户端凭证模式（服务间调用）

```bash
curl -X POST https://localhost:8443/oauth2/token \n  -H "Content-Type: application/x-www-form-urlencoded" \n  -d "grant_type=client_credentials" \n  -d "scope=read write" \n  -u "service-client:secret"

# 响应
{
  "access_token": "eyJhbGciOiJSUzI1NiJ9...",
  "token_type": "Bearer",
  "expires_in": 3600,
  "scope": "read write"
}
```

#### 3. 刷新令牌

```bash
curl -X POST https://localhost:8443/oauth2/token \n  -H "Content-Type: application/x-www-form-urlencoded" \n  -d "grant_type=refresh_token" \n  -d "refresh_token=xxx" \n  -u "web-app:secret"
```

#### 4. 获取用户信息（OIDC）

```bash
curl -X GET https://localhost:8443/userinfo \n  -H "Authorization: Bearer {access_token}"

# 响应
{
  "sub": "1",
  "name": "admin",
  "preferred_username": "admin",
  "email": "admin@huochai.com",
  "roles": ["ROLE_ADMIN"],
  "permissions": ["user:list", "user:create"]
}
```

#### 5. 令牌内省

```bash
curl -X POST https://localhost:8443/oauth2/introspect \n  -H "Content-Type: application/x-www-form-urlencoded" \n  -d "token={access_token}" \n  -u "web-app:secret"

# 响应
{
  "active": true,
  "sub": "admin",
  "aud": "web-app",
  "scope": "openid profile email",
  "exp": 1712345678,
  "iat": 1712338478
}
```

---

## 登录认证流程

### 完整登录流程图

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                              用户登录请求                                              │
│                     POST /auth/login                                                  │
└──────────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌──────────────────────────────────────────────────────────────────────────────────────┐
│  Step 1: 验证码校验                                                                    │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │ CaptchaServiceImpl.verifyCaptcha()                                              │ │
│  │ • 从 Redis 获取验证码：captcha:{uuid}                                            │ │
│  │ • 校验验证码是否正确                                                              │ │
│  │ • 校验后删除验证码（防止重放）                                                     │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
│                                        │                                              │
│                        目的：防止暴力破解和自动化攻击                                    │
└──────────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌──────────────────────────────────────────────────────────────────────────────────────┐
│  Step 2: 登录限流校验                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │ RateLimitService                                                                │ │
│  │ • IP 限流：检查 IP 最近登录失败次数                                               │ │
│  │ • 用户名限流：检查用户名最近登录失败次数                                           │ │
│  │ • 限流策略：5分钟内失败5次则锁定                                                  │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
│                                        │                                              │
│                        目的：防止暴力破解和 DDoS 攻击                                   │
└──────────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌──────────────────────────────────────────────────────────────────────────────────────┐
│  Step 3: 用户认证                                                                      │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │ AuthDomainServiceImpl.authenticate()                                            │ │
│  │ • 查询用户：AuthUserRepository.findByUsername()                                  │ │
│  │ • 验证密码：BCryptPasswordEncoder.matches()                                      │ │
│  │ • 检查账号状态：isDisabled(), isAccountValid()                                   │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
│                                        │                                              │
│                        目的：验证用户身份，确保只有合法用户能登录                        │
└──────────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌──────────────────────────────────────────────────────────────────────────────────────┐
│  Step 4: 生成 Token 对                                                                 │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │ TokenDomainServiceImpl.generateTokenPair()                                      │ │
│  │ • 生成 AccessToken（2小时有效）                                                   │ │
│  │   - 包含：userId, username, roles, jti, exp                                      │ │
│  │ • 生成 RefreshToken（7天有效）                                                   │ │
│  │   - 包含：userId, username, jti, tokenType=REFRESH, exp                         │ │
│  │ • 返回 TokenPair 对象                                                            │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
│                                        │                                              │
│                        目的：无状态认证，减少数据库查询压力                              │
└──────────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌──────────────────────────────────────────────────────────────────────────────────────┐
│  Step 5: 处理旧会话（单点登录踢人）                                                      │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │ AuthDomainServiceImpl.generateTokenPair()                                       │ │
│  │                                                                                 │ │
│  │ if (旧会话存在) {                                                                │ │
│  │     // 1. 将旧 Token 加入黑名单                                                   │ │
│  │     redisTokenService.addToBlacklist(accessToken, remainingTime);              │ │
│  │     redisTokenService.addToBlacklist(refreshToken, remainingTime);             │ │
│  │                                                                                 │ │
│  │     // 2. 删除旧会话                                                             │ │
│  │     redisTokenService.removeSession(userId, clientType, deviceId);             │ │
│  │                                                                                 │ │
│  │     // 3. 发布踢出事件                                                           │ │
│  │     eventPublisher.publishUserKickedOut(...);                                  │ │
│  │ }                                                                               │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
│                                        │                                              │
│                        目的：确保同一设备只能有一个活跃会话，防止账号共享风险            │
└──────────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌──────────────────────────────────────────────────────────────────────────────────────┐
│  Step 6: 存储新会话                                                                    │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │ RedisTokenServiceImpl.storeToken()                                              │ │
│  │                                                                                 │ │
│  │ // 1. 创建会话对象                                                               │ │
│  │ LoginSession session = LoginSession.create(userId, username, deviceInfo, ...); │ │
│  │                                                                                 │ │
│  │ // 2. 存储 Token 会话                                                            │ │
│  │ Key: token:{userId}:{clientType}:{deviceId}                                    │ │
│  │ Value: LoginSession                                                            │ │
│  │ TTL: RefreshToken 剩余时间                                                       │ │
│  │                                                                                 │ │
│  │ // 3. 存储会话映射（用于踢人检测）                                                 │ │
│  │ Key: session:{userId}:{clientType}                                             │ │
│  │ Value: deviceId                                                                 │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
│                                        │                                              │
│                        目的：支持会话管理、踢人功能、Token 黑名单校验                     │
└──────────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌──────────────────────────────────────────────────────────────────────────────────────┐
│  Step 7: 发布登录事件                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │ DomainEventPublisher.publishUserLogin()                                         │ │
│  │                                                                                 │ │
│  │ → UserLoginEventHandler                                                        │ │
│  │   • 记录登录日志到数据库                                                          │ │
│  │   • 包含：登录时间、IP、设备、位置等信息                                           │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
│                                        │                                              │
│                        目的：审计追踪，安全分析，异常登录检测                             │
└──────────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌──────────────────────────────────────────────────────────────────────────────────────┐
│  Step 8: 返回登录结果                                                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────────┐ │
│  │ {                                                                               │ │
│  │   "code": 200,                                                                  │ │
│  │   "message": "登录成功",                                                         │ │
│  │   "data": {                                                                     │ │
│  │     "accessToken": "eyJhbGciOiJIUzI1NiJ9...",                                  │ │
│  │     "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",                                 │ │
│  │     "expiresIn": 7200,                                                          │ │
│  │     "tokenType": "Bearer"                                                       │ │
│  │   }                                                                             │ │
│  │ }                                                                               │ │
│  └─────────────────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

### Token 验证流程

每次请求携带 Token 时，`JwtAuthenticationFilter` 会执行以下验证：

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│                              请求进入                                                  │
│                     Authorization: Bearer {token}                                     │
└──────────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌──────────────────────────────────────────────────────────────────────────────────────┐
│  JwtAuthenticationFilter.doFilterInternal()                                           │
│                                                                                       │
│  1. extractToken()           → 从请求头提取 Token                                      │
│  2. validateToken()          → 验证 Token 有效性                                       │
│     ├─ isBlacklisted()       → 检查是否在黑名单                                        │
│     └─ parseToken()          → 解析 JWT 签名和过期时间                                 │
│  3. isKickedOut()            → 检查是否被踢出                                          │
│  4. loadUserPermissions()    → 加载用户权限                                            │
│  5. setAuthentication()      → 设置 SecurityContext                                    │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

### Token 黑名单机制

#### 为什么需要黑名单？

**JWT 的无状态特性问题：**

```
JWT Token = Header.Payload.Signature
```

JWT 一旦签发，在过期时间之前都是有效的，服务端无法主动"撤销"一个 Token。

```
问题场景：
┌─────────────────────────────────────────────────────────────────────────────────────┐
│ T1: 用户在设备A登录 → 获得 Token-A (有效期30分钟)                                      │
│ T2: 用户在设备A再次登录 → 获得 Token-B (新Token)                                       │
│     ↓                                                                                │
│     如果不加入黑名单：                                                                 │
│     Token-A 在剩余时间内仍然有效！                                                     │
│     攻击者拿到 Token-A 仍可访问系统                                                    │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

#### 黑名单实现原理

```java
// 加入黑名单
public void addToBlacklist(String token, long expireMillis) {
    String blacklistKey = "blacklist:" + token;
    redisTemplate.opsForValue().set(blacklistKey, "1", expireMillis, TimeUnit.MILLISECONDS);
}

// 验证时检查
public boolean validateToken(String token) {
    if (isBlacklisted(token)) {
        return false;  // 黑名单中的 Token 拒绝访问
    }
    return tokenDomainService.validateToken(token);
}
```

#### 黑名单存储策略

```
Key:    blacklist:{token}
Value:  1
TTL:    Token 剩余有效时间

目的：自动过期清理，避免 Redis 内存泄漏
```

### 单点登录（踢人）机制

#### 设计目的

| 场景 | 问题 | 解决方案 |
|------|------|----------|
| 账号共享 | 多人共用账号，安全风险 | 同设备只能一个会话 |
| 设备丢失 | 用户换设备登录，旧设备仍在线 | 新登录踢出旧会话 |
| 异常登录 | 检测到异常登录行为 | 管理员可强制踢出 |

#### 实现原理

```
会话映射存储：
┌─────────────────────────────────────────────────────────────────────────────────────┐
│ Key:   session:{userId}:{clientType}                                                 │
│ Value: 当前活跃的 deviceId                                                            │
│ TTL:   RefreshToken 过期时间                                                          │
└─────────────────────────────────────────────────────────────────────────────────────┘

踢人检测逻辑：
┌─────────────────────────────────────────────────────────────────────────────────────┐
│ isKickedOut(userId, clientType, deviceId):                                          │
│                                                                                      │
│ 1. 获取 session:{userId}:{clientType} 的值                                           │
│ 2. 如果值不存在 → 用户未登录或已过期                                                   │
│ 3. 如果值与当前 deviceId 不匹配 → 被其他设备踢出                                       │
│ 4. 如果值与当前 deviceId 匹配 → 正常会话                                              │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

### Token 刷新机制

#### 设计目的

```
问题：
┌─────────────────────────────────────────────────────────────────────────────────────┐
│ AccessToken 有效期短（2小时），频繁过期影响用户体验                                      │
│ 但如果延长 AccessToken 有效期，又会增加安全风险                                         │
└─────────────────────────────────────────────────────────────────────────────────────┘

解决方案：双 Token 机制
┌─────────────────────────────────────────────────────────────────────────────────────┐
│ AccessToken:  短期有效（2小时），用于接口访问                                          │
│ RefreshToken: 长期有效（7天），用于刷新 AccessToken                                    │
│                                                                                      │
│ 用户无感知：AccessToken 过期前自动刷新，保持登录状态                                    │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

#### 刷新流程

```
┌──────────────────────────────────────────────────────────────────────────────────────┐
│  POST /auth/refresh                                                                   │
│  { "refreshToken": "xxx" }                                                            │
└──────────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌──────────────────────────────────────────────────────────────────────────────────────┐
│  1. 验证 RefreshToken 有效性                                                          │
│     • 检查签名和过期时间                                                               │
│     • 检查 tokenType = "REFRESH"                                                      │
│     • 检查黑名单                                                                      │
└──────────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌──────────────────────────────────────────────────────────────────────────────────────┐
│  2. 将旧 RefreshToken 加入黑名单                                                       │
│     • 防止重复使用同一个 RefreshToken                                                  │
│     • 每个 RefreshToken 只能使用一次                                                  │
└──────────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌──────────────────────────────────────────────────────────────────────────────────────┐
│  3. 生成新的 TokenPair                                                                │
│     • 新 AccessToken                                                                  │
│     • 新 RefreshToken                                                                 │
└──────────────────────────────────────────────────────────────────────────────────────┘
                                        │
                                        ▼
┌──────────────────────────────────────────────────────────────────────────────────────┐
│  4. 存储新会话，返回新 Token                                                           │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

### 安全设计总结

| 安全措施 | 防护目标 | 实现方式 |
|----------|----------|----------|
| 验证码 | 暴力破解 | 图形验证码，一次性使用 |
| 登录限流 | DDoS/暴力破解 | IP + 用户名双维度限流 |
| BCrypt 密码 | 密码泄露 | 单向加密，加盐存储 |
| Token 黑名单 | Token 劫持 | Redis 存储，主动失效 |
| 单点踢人 | 账号共享 | 会话映射，新登录踢旧 |
| Token 刷新 | 长期有效风险 | 双 Token，短期 Access + 长期 Refresh |
| 登录日志 | 安全审计 | 记录所有登录行为 |
| TraceId | 问题追踪 | 全链路日志追踪 |

---

## 技术架构

### DDD 领域驱动设计

```
┌─────────────────────────────────────────────────────────────┐
│                      接口层 (Interfaces)                      │
│  AuthController, RoleController, PermissionController       │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                      应用层 (Application)                     │
│         AuthService, PermissionService, CaptchaService      │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                      领域层 (Domain)                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │ AuthUser    │  │   Role      │  │ Permission  │         │
│  │  (聚合根)   │  │  (聚合根)   │  │  (值对象)   │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │ DomainEvent │  │ DomainSvc   │  │ Repository  │         │
│  │  (领域事件) │  │  (领域服务) │  │  (仓储接口) │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                    基础设施层 (Infrastructure)                 │
│     MyBatis Mapper, Redis, JWT, Spring Security, OAuth2     │
└─────────────────────────────────────────────────────────────┘
```

### 技术栈

#### 后端
| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.5.x | 核心框架 |
| Spring Security | 6.x | 安全框架 |
| Spring Authorization Server | 1.x | OAuth2 授权服务器 |
| MyBatis-Plus | 3.5.x | ORM 框架 |
| Redis | 7.x | 缓存/会话存储 |
| MySQL | 8.x | 关系数据库 |
| jjwt | 0.11.5 | JWT 处理 |
| Hutool | 5.8.x | Java 工具库 |

#### 前端
| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4.x | 前端框架 |
| TypeScript | 5.x | 类型支持 |
| Element Plus | 2.5.x | UI 组件库 |
| Pinia | 2.1.x | 状态管理 |
| Vue Router | 4.2.x | 路由管理 |
| Axios | 1.6.x | HTTP 客户端 |
| Vite | 5.x | 构建工具 |

#### 日志
| 技术 | 版本 | 说明 |
|------|------|------|
| Filebeat | 8.11 | 日志采集 |
| Logstash | 8.11 | 日志处理 |
| Elasticsearch | 8.11 | 日志存储 |
| Kibana | 8.11 | 日志可视化 |

---

## 项目结构

```
huochai/
├── huochai-core/                    # 后端核心模块
│   ├── src/main/java/com/huochai/
│   │   ├── auth/                    # 认证领域
│   │   │   ├── domain/              # 领域层
│   │   │   │   ├── model/           # 领域模型
│   │   │   │   │   ├── AuthUser.java       # 用户聚合根
│   │   │   │   │   ├── Token.java          # Token 值对象
│   │   │   │   │   ├── TokenPair.java      # Token 对
│   │   │   │   │   ├── DeviceInfo.java     # 设备信息
│   │   │   │   │   ├── LoginSession.java   # 登录会话
│   │   │   │   │   └── LoginLog.java       # 登录日志
│   │   │   │   ├── event/           # 领域事件
│   │   │   │   │   ├── DomainEvent.java
│   │   │   │   │   ├── UserLoginEvent.java
│   │   │   │   │   ├── UserLogoutEvent.java
│   │   │   │   │   └── TokenRefreshedEvent.java
│   │   │   │   ├── service/         # 领域服务
│   │   │   │   │   ├── AuthDomainService.java
│   │   │   │   │   ├── TokenDomainService.java
│   │   │   │   │   └── RedisTokenService.java
│   │   │   │   └── repository/      # 仓储接口
│   │   │   ├── application/         # 应用层
│   │   │   │   ├── service/         # 应用服务
│   │   │   │   │   ├── AuthService.java
│   │   │   │   │   ├── CaptchaService.java
│   │   │   │   │   └── RateLimitService.java
│   │   │   │   └── dto/             # 数据传输对象
│   │   │   ├── infrastructure/      # 基础设施层
│   │   │   │   ├── filter/          # 过滤器
│   │   │   │   ├── mapper/          # MyBatis Mapper
│   │   │   │   ├── repository/      # 仓储实现
│   │   │   │   └── security/        # Security 相关
│   │   │   └── interfaces/          # 接口层
│   │   │       ├── controller/      # 控制器
│   │   │       └── dto/             # DTO
│   │   ├── permission/              # 权限领域
│   │   │   ├── domain/
│   │   │   ├── application/
│   │   │   └── interfaces/
│   │   ├── sso/                     # SSO 模块
│   │   ├── common/                  # 公共模块
│   │   │   ├── enums/               # 枚举
│   │   │   ├── exception/           # 异常
│   │   │   ├── result/              # 响应
│   │   │   ├── log/                 # 日志
│   │   │   └── trace/               # 链路追踪
│   │   └── config/                  # 配置
│   └── src/main/resources/
│       ├── db/                      # 数据库脚本
│       ├── elk/                     # ELK 配置
│       ├── logback-spring.xml       # 日志配置
│       └── application.properties   # 应用配置
│
└── huochai-admin-ui/                # 前端管理模块
    ├── src/
    │   ├── api/                     # API 接口
    │   ├── stores/                  # 状态管理
    │   ├── router/                  # 路由
    │   ├── views/                   # 页面
    │   └── utils/                   # 工具
    └── package.json
```

---

## 快速开始

### 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Redis 7.0+
- Docker & Docker Compose (用于 ELK)

### 1. 初始化数据库

```bash
# 创建数据库并导入初始数据
mysql -u root -p < huochai-core/src/main/resources/db/init-auth.sql
```

### 2. 配置应用

编辑 `huochai-core/src/main/resources/application.properties`：

```properties
# MySQL 配置
spring.datasource.url=jdbc:mysql://localhost:3306/huochai_auth
spring.datasource.username=root
spring.datasource.password=your_password

# Redis 配置
spring.data.redis.host=localhost
spring.data.redis.port=6379
```

### 3. 启动后端

```bash
cd huochai-core
mvn spring-boot:run
```

### 4. 启动前端

```bash
cd huochai-admin-ui
npm install
npm run dev
```

### 5. 启动 ELK (可选)

```bash
cd huochai-core/src/main/resources/elk
docker-compose up -d
```

### 6. 访问系统

| 服务 | 地址 |
|------|------|
| 后端 Swagger | https://localhost:8443/swagger-ui/index.html |
| 前端管理 | http://localhost:3000 |
| Kibana 日志 | http://localhost:5601 |

---

## API 文档

### 认证接口

#### 获取验证码
```http
GET /auth/captcha
```

**响应：**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "uuid": "abc-123-def",
    "image": "data:image/png;base64,..."
  }
}
```

#### 用户登录
```http
POST /auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123",
  "clientType": "WEB",
  "deviceId": "device-001",
  "captchaUuid": "abc-123-def",
  "captchaCode": "1234"
}
```

**响应：**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "expiresIn": 7200,
    "tokenType": "Bearer",
    "sessionId": "1_WEB_device-001",
    "userId": 1,
    "username": "admin"
  }
}
```

#### 刷新 Token
```http
POST /auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "clientType": "WEB",
  "deviceId": "device-001"
}
```

#### 用户登出
```http
POST /auth/logout
Authorization: Bearer {accessToken}
```

#### 获取用户信息
```http
GET /auth/user-info
Authorization: Bearer {accessToken}
```

**响应：**
```json
{
  "code": 200,
  "data": {
    "userId": 1,
    "username": "admin",
    "email": "admin@huochai.com",
    "roles": ["ROLE_ADMIN"],
    "permissions": ["user:list", "user:create", "role:list"]
  }
}
```

### 管理接口

#### 用户管理
```http
GET    /api/admin/users           # 用户列表
POST   /api/admin/users           # 创建用户
PUT    /api/admin/users/{id}      # 更新用户
DELETE /api/admin/users/{id}      # 删除用户
POST   /api/admin/users/{id}/roles # 分配角色
```

#### 角色管理
```http
GET    /api/admin/roles                    # 角色列表
POST   /api/admin/roles                    # 创建角色
PUT    /api/admin/roles/{id}               # 更新角色
DELETE /api/admin/roles/{id}               # 删除角色
POST   /api/admin/roles/{id}/permissions   # 分配权限
```

#### 权限管理
```http
GET    /api/admin/permissions        # 权限列表
POST   /api/admin/permissions        # 创建权限
PUT    /api/admin/permissions/{id}   # 更新权限
DELETE /api/admin/permissions/{id}   # 删除权限
```

#### 登录日志
```http
GET /api/admin/login-logs            # 日志列表
GET /api/admin/login-logs/user/{id}  # 用户日志
```

---

## 配置说明

### JWT 配置

```properties
# JWT 密钥（至少256位）
jwt.secret=huochai-jwt-secret-key-must-be-at-least-256-bits-long

# AccessToken 有效期（毫秒）默认 2 小时
jwt.access-token-expire=7200000

# RefreshToken 有效期（毫秒）默认 7 天
jwt.refresh-token-expire=604800000

# 发行者
jwt.issuer=huochai-auth
```

### 权限注解使用

```java
// 方法级权限控制
@PreAuthorize("hasAuthority('user:create')")
@PostMapping("/users")
public Result<User> createUser(@RequestBody User user) {
    // ...
}

// 多权限校验（任意一个）
@PreAuthorize("hasAnyAuthority('user:create', 'user:update')")
@PutMapping("/users/{id}")
public Result<User> updateUser(@PathVariable Long id, @RequestBody User user) {
    // ...
}

// 角色校验
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/users/{id}")
public Result<Void> deleteUser(@PathVariable Long id) {
    // ...
}
```

### 操作日志注解

```java
@OperationLog(module = "用户管理", type = OperationType.CREATE, desc = "创建用户")
@PostMapping("/users")
public Result<User> createUser(@RequestBody User user) {
    // 自动记录操作日志
}
```

---

## 默认账号

| 用户名 | 密码 | 角色 | 权限 |
|--------|------|------|------|
| admin | admin123 | ROLE_ADMIN | 所有权限 |

---

## 日志体系

### 日志格式

```
2026-04-16 10:30:00.123 [trace-id] [thread-name] LEVEL logger - message
```

### 日志分类

| 分类 | 标记 | 说明 |
|------|------|------|
| 领域行为 | `[领域行为]` | 聚合根的业务操作 |
| 领域验证 | `[领域验证]` | 业务规则验证 |
| 认证服务 | `[认证服务]` | 认证流程日志 |
| Token服务 | `[Token服务]` | Token 相关操作 |
| 权限服务 | `[权限服务]` | 权限加载和校验 |

### ELK 查询示例

```kibana
# 按 TraceId 查询
traceId: "abc-123-def"

# 按用户查询
username: "admin"

# 查询登录日志
log_message: "登录"

# 查询错误日志
level: "ERROR"
```

---

## 安全建议

### 生产环境配置

1. **修改默认密码**：立即修改 admin 账号密码
2. **JWT 密钥**：使用强随机密钥，至少 256 位
3. **HTTPS**：生产环境必须使用 HTTPS
4. **Redis 密码**：配置 Redis 访问密码
5. **数据库密码**：使用强密码

### 安全检查清单

- [ ] 修改默认管理员密码
- [ ] 配置 JWT 强密钥
- [ ] 启用 HTTPS
- [ ] 配置 Redis 密码
- [ ] 数据库访问控制
- [ ] 启用登录限流
- [ ] 配置验证码

---

## 常见问题

### Q: Token 过期如何处理？
A: 使用 `/auth/refresh` 接口刷新 Token。

### Q: 如何实现多端登录？
A: 登录时传递不同的 `clientType` (WEB/APP/MINI_PROGRAM) 和 `deviceId`。

### Q: 如何添加新的权限？
A: 在数据库 `sys_permission` 表添加权限记录，然后通过接口分配给角色。

### Q: 如何自定义日志？
A: 使用 `@OperationLog` 注解标记需要记录的方法。

---

## License

MIT License

---

## 联系方式

如有问题或建议，请提交 Issue 或 Pull Request。