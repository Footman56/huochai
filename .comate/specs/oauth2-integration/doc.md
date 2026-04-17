# OAuth2 功能集成设计文档

## 1. 需求背景

在现有自定义 JWT 认证体系基础上，集成标准 OAuth2/OIDC 功能，实现：
- 授权码模式 (Authorization Code Flow)
- 客户端凭证模式 (Client Credentials Flow)
- 刷新令牌模式 (Refresh Token Flow)
- OIDC 用户信息端点

**设计原则：对现有代码影响最小，通过新增模块和配置实现。**

---

## 2. 架构设计

### 2.1 整体架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            huochai-core (主应用)                              │
│                              Port: 8443                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────┐    ┌─────────────────────────────────────────┐ │
│  │   现有 JWT 认证体系      │    │         OAuth2 授权服务器               │ │
│  │   (保持不变)             │    │     (新增/增强，最小侵入)                │ │
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
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 OAuth2 授权流程

```
┌──────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────┐
│  用户    │     │ 客户端应用    │     │  授权服务器   │     │ 资源服务 │
│ (User)   │     │ (Client App) │     │ (huochai)    │     │  (API)   │
└────┬─────┘     └──────┬───────┘     └──────┬───────┘     └────┬─────┘
     │                  │                    │                  │
     │  1. 访问资源     │                    │                  │
     │◄─────────────────│                    │                  │
     │                  │                    │                  │
     │  2. 重定向到授权页                    │                  │
     │─────────────────────────────────────►│                  │
     │                  │                    │                  │
     │  3. 用户登录并授权                    │                  │
     │─────────────────────────────────────►│                  │
     │                  │                    │                  │
     │  4. 重定向回客户端 (带code)            │                  │
     │◄─────────────────────────────────────│                  │
     │                  │                    │                  │
     │                  │  5. 用 code 换 token                  │
     │                  │───────────────────►│                  │
     │                  │                    │                  │
     │                  │  6. 返回 access_token                 │
     │                  │◄───────────────────│                  │
     │                  │                    │                  │
     │                  │  7. 访问资源 (Bearer Token)            │
     │                  │─────────────────────────────────────►│
     │                  │                    │                  │
     │                  │  8. 返回资源数据                      │
     │                  │◄─────────────────────────────────────│
     │                  │                    │                  │
```

---

## 3. 模块设计

### 3.1 基于现有结构改造

```
huochai-core/src/main/java/com/huochai/
├── auth/                          # 现有认证领域（扩展）
│   ├── domain/
│   │   ├── model/
│   │   │   ├── AuthUser.java               # [不变] 用户聚合根
│   │   │   └── OAuth2Client.java           # [新增] OAuth2客户端值对象
│   │   ├── repository/
│   │   │   └── OAuth2ClientRepository.java # [新增] 客户端仓储接口
│   │   └── service/
│   │       ├── AuthDomainService.java      # [不变] 认证领域服务
│   │       └── OAuth2ClientService.java    # [新增] 客户端服务
│   ├── infrastructure/
│   │   ├── mapper/
│   │   │   └── OAuth2ClientMapper.java     # [新增] MyBatis Mapper
│   │   └── repository/
│   │       └── OAuth2ClientRepositoryImpl.java # [新增] 仓储实现
│   └── interfaces/
│       └── controller/
│           ├── AuthController.java         # [不变] 认证控制器
│           └── OAuth2Controller.java       # [新增] OAuth2页面控制器
│
├── sso/                           # 现有SSO模块（增强）
│   └── config/
│       ├── OAuth2ServerConfig.java         # [改造] 增强授权服务器配置
│       └── OAuth2SecurityConfig.java       # [新增] OAuth2安全配置
│
├── config/
│   └── SecurityConfig.java                 # [改造] 添加OAuth2过滤链
│
└── resources/
    └── db/
        └── oauth2_tables.sql               # [新增] 数据库脚本
```

### 3.2 改造策略

| 改造点 | 说明 |
|--------|------|
| `OAuth2ServerConfig.java` | 增强现有配置，添加完整的授权服务器功能 |
| `SecurityConfig.java` | 添加 OAuth2 SecurityFilterChain（Order=1） |
| 新增 `OAuth2Client.java` | 在 auth.domain.model 下添加客户端模型 |
| 新增 `OAuth2ClientRepository` | 遵循现有仓储模式 |
| 新增 `OAuth2Controller` | 提供授权页面和同意页面 |

### 3.3 完全不变的文件

| 文件 | 说明 |
|------|------|
| `AuthDomainService.java` | 现有认证逻辑完全不变 |
| `TokenDomainService.java` | JWT 生成解析不变 |
| `JwtAuthenticationFilter.java` | JWT 过滤器不变 |
| `RedisTokenService.java` | Redis Token 服务不变 |
| `AuthUserRepository.java` | 用户仓储不变 |
| `UserDetailsServiceImpl.java` | 用户详情服务不变 |
| `RbacDomainService.java` | 权限服务不变 |
| `AuthController.java` | 现有认证接口不变 |

---

## 4. 详细设计

### 4.1 OAuth2 授权服务器配置

**核心配置类：`OAuth2AuthorizationServerConfig.java`**

```java
@Configuration
public class OAuth2AuthorizationServerConfig {

    /**
     * 注册客户端仓库
     * 支持从数据库读取客户端配置
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository(
            OAuth2ClientRepository clientRepository,
            PasswordEncoder passwordEncoder) {
        return new JdbcRegisteredClientRepository(...);
    }

    /**
     * 授权服务
     * 存储授权码、访问令牌、刷新令牌
     */
    @Bean
    public OAuth2AuthorizationService authorizationService(
            OAuth2AuthorizationRepository authorizationRepository) {
        return new JpaOAuth2AuthorizationService(...);
    }

    /**
     * 授权同意服务
     * 记录用户的授权同意选择
     */
    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService() {
        return new JdbcOAuth2AuthorizationConsentService(...);
    }

    /**
     * JWK Source
     * 用于签名 JWT 的密钥源
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        // RSA 密钥对，可配置到 KeyStore
    }

    /**
     * 授权服务器安全过滤链
     */
    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfigurer.applyDefaultSecurity(http);
        // 配置 OIDC
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
            .oidc(oidc -> oidc.userInfoEndpoint(userInfo -> 
                userInfo.userInfoMapper(customUserInfoMapper())));
        return http.build();
    }
}
```

### 4.2 支持的授权类型

| 授权类型 | 用途 | 端点 |
|----------|------|------|
| `authorization_code` | Web 应用、SPA | `/oauth2/authorize` |
| `refresh_token` | 刷新令牌 | `/oauth2/token` |
| `client_credentials` | 服务间调用 | `/oauth2/token` |

### 4.3 客户端配置

**预置客户端：**

| 客户端 ID | 类型 | 授权类型 | 回调地址 |
|-----------|------|----------|----------|
| `web-app` | Web 应用 | authorization_code, refresh_token | https://localhost:8443/login/oauth2/code/web-app |
| `mobile-app` | 移动应用 | authorization_code, refresh_token | myapp://callback |
| `service-client` | 服务调用 | client_credentials | - |

### 4.4 OIDC 支持

**UserInfo 端点 (`/userinfo`)**

返回标准 OIDC 用户信息：

```json
{
  "sub": "1",
  "name": "admin",
  "email": "admin@huochai.com",
  "phone_number": "13800138000",
  "preferred_username": "admin",
  "roles": ["ROLE_ADMIN"],
  "permissions": ["user:list", "user:create"]
}
```

### 4.5 与现有认证体系共存

**双认证体系架构：**

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           请求入口                                       │
│                        huochai-core:8443                                │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       SecurityFilterChain                               │
│                                                                         │
│  @Order(1)  OAuth2AuthorizationServerSecurityFilterChain               │
│             匹配: /oauth2/**, /.well-known/**, /userinfo                │
│             使用: OAuth2 Token 验证                                     │
│                                                                         │
│  @Order(2)  DefaultSecurityFilterChain                                 │
│             匹配: /**                                                   │
│             使用: 自定义 JWT Filter (JwtAuthenticationFilter)           │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 5. 数据库设计

### 5.1 OAuth2 客户端表

```sql
CREATE TABLE oauth2_registered_client (
    id VARCHAR(100) PRIMARY KEY,
    client_id VARCHAR(100) NOT NULL UNIQUE,
    client_id_issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    client_secret VARCHAR(200),
    client_secret_expires_at TIMESTAMP,
    client_name VARCHAR(200),
    client_authentication_methods VARCHAR(500),
    authorization_grant_types VARCHAR(500),
    redirect_uris VARCHAR(1000),
    post_logout_redirect_uris VARCHAR(1000),
    scopes VARCHAR(500),
    client_settings VARCHAR(2000),
    token_settings VARCHAR(2000)
);
```

### 5.2 OAuth2 授权记录表

```sql
CREATE TABLE oauth2_authorization (
    id VARCHAR(100) PRIMARY KEY,
    registered_client_id VARCHAR(100) NOT NULL,
    principal_name VARCHAR(200) NOT NULL,
    authorization_grant_type VARCHAR(100),
    authorized_scopes VARCHAR(1000),
    attributes TEXT,
    state VARCHAR(500),
    authorization_code_value TEXT,
    authorization_code_issued_at TIMESTAMP,
    authorization_code_expires_at TIMESTAMP,
    authorization_code_metadata VARCHAR(2000),
    access_token_value TEXT,
    access_token_issued_at TIMESTAMP,
    access_token_expires_at TIMESTAMP,
    access_token_metadata VARCHAR(2000),
    access_token_type VARCHAR(100),
    access_token_scopes VARCHAR(500),
    oidc_id_token_value TEXT,
    oidc_id_token_issued_at TIMESTAMP,
    oidc_id_token_expires_at TIMESTAMP,
    oidc_id_token_metadata VARCHAR(2000),
    refresh_token_value TEXT,
    refresh_token_issued_at TIMESTAMP,
    refresh_token_expires_at TIMESTAMP,
    refresh_token_metadata VARCHAR(2000),
    user_code_value TEXT,
    user_code_issued_at TIMESTAMP,
    user_code_expires_at TIMESTAMP,
    user_code_metadata VARCHAR(2000),
    device_code_value TEXT,
    device_code_issued_at TIMESTAMP,
    device_code_expires_at TIMESTAMP,
    device_code_metadata VARCHAR(2000)
);
```

### 5.3 OAuth2 授权同意表

```sql
CREATE TABLE oauth2_authorization_consent (
    registered_client_id VARCHAR(100) NOT NULL,
    principal_name VARCHAR(200) NOT NULL,
    authorities VARCHAR(1000),
    PRIMARY KEY (registered_client_id, principal_name)
);
```

---

## 6. API 端点

### 6.1 OAuth2 标准端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `/oauth2/authorize` | GET/POST | 授权端点 |
| `/oauth2/token` | POST | 令牌端点 |
| `/oauth2/jwks` | GET | JWK Set 端点 |
| `/oauth2/introspect` | POST | 令牌内省端点 |
| `/oauth2/revoke` | POST | 令牌撤销端点 |
| `/userinfo` | GET | OIDC 用户信息端点 |
| `/.well-known/openid-configuration` | GET | OIDC 发现端点 |

### 6.2 授权码模式示例

**Step 1: 获取授权码**

```
GET /oauth2/authorize?
    response_type=code&
    client_id=web-app&
    scope=openid profile email&
    redirect_uri=https://localhost:8443/login/oauth2/code/web-app&
    state=xyz

→ 重定向到登录页面（如未登录）
→ 显示授权同意页面
→ 用户同意后重定向回：
https://localhost:8443/login/oauth2/code/web-app?code=xxx&state=xyz
```

**Step 2: 用授权码换令牌**

```bash
curl -X POST https://localhost:8443/oauth2/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=authorization_code" \
  -d "code=xxx" \
  -d "redirect_uri=https://localhost:8443/login/oauth2/code/web-app" \
  -u "web-app:secret"
```

**响应：**

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiJ9...",
  "token_type": "Bearer",
  "expires_in": 7200,
  "refresh_token": "xxx",
  "scope": "openid profile email",
  "id_token": "eyJhbGciOiJSUzI1NiJ9..."
}
```

---

## 7. 配置说明

### 7.1 application.properties

```properties
# OAuth2 Authorization Server 配置
oauth2.server.issuer=https://localhost:8443
oauth2.server.token.access-ttl=7200
oauth2.server.token.refresh-ttl=604800

# JWK 密钥配置（生产环境建议使用 KeyStore）
oauth2.jwk.key-id=huochai-key-1
# 或使用 KeyStore
oauth2.jwk.keystore.path=classpath:keystore.jks
oauth2.jwk.keystore.password=changeit
oauth2.jwk.keystore.alias=huochai

# 预置客户端（可选，也可从数据库读取）
oauth2.client.web-app.client-id=web-app
oauth2.client.web-app.client-secret={bcrypt}$2a$10$xxx
oauth2.client.web-app.redirect-uri=https://localhost:8443/login/oauth2/code/web-app
```

---

## 8. 影响分析

### 8.1 对现有代码的影响

| 影响级别 | 说明 |
|----------|------|
| **无影响** | 现有 JWT 认证流程完全不变 |
| **无影响** | 现有登录、登出、刷新 Token 接口不变 |
| **无影响** | 现有权限控制逻辑不变 |
| **最小修改** | SecurityConfig 添加 OAuth2 端点放行 |
| **新增配置** | application.properties 添加 OAuth2 配置 |

### 8.2 共享组件

- `UserDetailsServiceImpl` - OAuth2 复用现有用户详情服务
- `AuthUserRepository` - OAuth2 复用现有用户数据访问
- `PasswordEncoder` - OAuth2 复用现有密码编码器
- `RbacDomainService` - OAuth2 复用现有权限服务

---

## 9. 测试场景

### 9.1 功能测试

| 场景 | 验证点 |
|------|--------|
| 授权码模式 | 完整授权流程、授权码一次性使用 |
| 刷新令牌 | Token 刷新成功、旧 Token 失效 |
| 客户端凭证 | 服务间调用认证 |
| OIDC | UserInfo 返回正确、ID Token 有效 |
| 令牌撤销 | 撤销后 Token 失效 |
| 令牌内省 | 内省结果正确 |

### 9.2 安全测试

| 场景 | 验证点 |
|------|--------|
| 无效授权码 | 拒绝访问 |
| 过期令牌 | 拒绝访问 |
| 无效客户端 | 拒绝访问 |
| 重放攻击 | 授权码只能使用一次 |
| CSRF | State 参数验证 |

---

## 10. 预期产出

1. 完整的 OAuth2 授权服务器功能
2. OIDC 1.0 支持
3. 详细的 README 文档
4. 测试用例覆盖核心场景
5. 现有认证体系完全不受影响