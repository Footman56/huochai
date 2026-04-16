# 完整登录体系（Redis + RBAC + OAuth2 + SSO）技术设计文档

## 一、需求概述

### 1.1 核心功能
- 用户名/密码登录（BCrypt 加密）
- AccessToken + RefreshToken 双 Token 机制
- Redis 会话存储
- 单点登录（踢人机制）
- Token 黑名单（退出登录）
- RBAC 权限控制（URL级/方法级）
- Swagger 可带 Token 调试

### 1.2 扩展功能
- 多端登录（web/app 分开 token）
- 设备标识（deviceId）
- 登录日志审计
- 验证码（防暴力破解）
- 限流（Guava / Redis）
- OAuth2 / OIDC 支持
- 统一认证中心（SSO）
- 统一权限异常处理
- 前端管理页面（独立模块）

### 1.3 日志与链路追踪
- 系统操作日志（操作审计）
- TraceId 链路追踪（全链路日志关联）
- ELK 日志基础设施（Elasticsearch + Kibana + Beats + Logstash）
- 日志分级存储（按级别、按模块）
- 日志可视化查询

## 二、架构设计

### 2.1 整体架构
```
┌─────────────────────────────────────────────────────────────┐
│                        前端管理模块                           │
│  (Vue3 + Element Plus，独立部署)                              │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                     统一认证中心 (SSO)                        │
│  OAuth2 Authorization Server + OIDC                          │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────┐
│                     huochai-core 核心                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │ Auth Context│  │ User Context│  │ Perm Context│         │
│  │  (认证领域)  │  │  (用户领域)  │  │  (权限领域)  │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │
│  │ Log Context │  │ Captcha     │  │ RateLimiter │         │
│  │  (日志领域)  │  │  (验证码)    │  │  (限流)     │         │
│  └─────────────┘  └─────────────┘  └─────────────┘         │
└─────────────────────────────────────────────────────────────┘
                              ↓
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│    Redis     │  │    MySQL     │  │   OAuth2     │
│  (会话/缓存)  │  │  (持久化)     │  │  (第三方)    │
└──────────────┘  └──────────────┘  └──────────────┘
```

### 2.2 认证流程
```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│  Client  │───→│ Captcha  │───→│  Auth    │───→│  Redis   │
│  (前端)   │    │  验证码   │    │  认证    │    │  存储    │
└──────────┘    └──────────┘    └──────────┘    └──────────┘
                                     │
                                     ↓
                               ┌──────────┐
                               │  MySQL   │
                               │  用户数据 │
                               └──────────┘
```

### 2.3 多端登录流程
```
用户登录 → 携带 clientType(web/app) + deviceId
    ↓
生成 Token 时绑定 clientType + deviceId
    ↓
Redis Key: auth:token:{userId}:{clientType}:{deviceId}
    ↓
同一端新登录踢掉旧 Token（单端单设备）
```

## 三、DDD 领域模型设计

### 3.1 领域划分
```
com.huochai
├── auth                          # 认证上下文 (Auth Context)
│   ├── domain
│   │   ├── model
│   │   │   ├── AuthUser.java           # 用户聚合根
│   │   │   ├── Token.java              # Token 值对象
│   │   │   ├── TokenPair.java          # Token对 值对象
│   │   │   ├── DeviceInfo.java         # 设备信息 值对象
│   │   │   ├── LoginSession.java       # 登录会话 实体
│   │   │   └── LoginLog.java           # 登录日志 实体
│   │   ├── repository
│   │   │   ├── AuthUserRepository.java
│   │   │   └── LoginLogRepository.java
│   │   └── service
│   │       ├── AuthDomainService.java  # 认证领域服务
│   │       └── TokenService.java       # Token 领域服务
│   ├── application
│   │   ├── AuthService.java            # 应用服务
│   │   ├── CaptchaService.java         # 验证码服务
│   │   └── RateLimitService.java       # 限流服务
│   └── interfaces
│       ├── dto
│       │   ├── LoginRequest.java
│       │   ├── LoginResponse.java
│       │   └── RefreshTokenRequest.java
│       └── facade
│           └── AuthController.java
│
├── permission                    # 权限上下文 (Permission Context)
│   ├── domain
│   │   ├── model
│   │   │   ├── Role.java               # 角色 聚合
│   │   │   ├── Permission.java         # 权限 值对象
│   │   │   ├── UserRole.java           # 用户角色关联
│   │   │   └── RolePermission.java     # 角色权限关联
│   │   ├── repository
│   │   │   ├── RoleRepository.java
│   │   │   └── PermissionRepository.java
│   │   └── service
│   │       └── RbacDomainService.java  # RBAC 领域服务
│   ├── application
│   │   └── PermissionService.java
│   └── interfaces
│       └── PermissionController.java
│
├── sso                           # SSO 上下文
│   ├── config
│   │   └── OAuth2ServerConfig.java
│   └── service
│       └── SsoService.java
│
├── common                        # 公共模块
│   ├── exception
│   │   ├── AuthException.java
│   │   ├── PermissionDeniedException.java
│   │   └── GlobalExceptionHandler.java
│   ├── result
│   │   └── Result.java
│   └── enums
│       ├── ClientType.java             # 客户端类型枚举
│       └── DeviceType.java
│
└── config                        # 配置模块
    ├── RedisConfig.java
    ├── SecurityConfig.java
    └── RateLimiterConfig.java
```

### 3.2 核心领域模型

#### 3.2.1 AuthUser（用户聚合根）
```java
// 用户聚合根 - 认证上下文
@Entity
@Table(name = "sys_auth_user")
public class AuthUser {
    @Id
    private Long id;
    
    private String username;           // 用户名
    private String password;           // BCrypt加密密码
    private String email;
    private String phone;
    private Integer status;            // 状态: 1正常 0禁用
    
    // 领域行为
    public boolean verifyPassword(String rawPassword, PasswordEncoder encoder);
    public void disable();
    public void enable();
    public boolean isAccountValid();
}

// 设备信息 值对象
public class DeviceInfo {
    private String deviceId;           // 设备唯一标识
    private ClientType clientType;     // WEB/APP/MINI_PROGRAM
    private String deviceName;         // 设备名称
    private String os;                 // 操作系统
    private String browser;            // 浏览器
    private String ip;                 // IP地址
}

// Token 值对象
public class Token {
    private String value;              // Token值
    private TokenType type;            // ACCESS/REFRESH
    private Long expiresAt;            // 过期时间
    private String jti;                // 唯一标识
}

// TokenPair 值对象
public class TokenPair {
    private Token accessToken;
    private Token refreshToken;
    private DeviceInfo deviceInfo;
}
```

#### 3.2.2 LoginSession（登录会话）
```java
// 登录会话实体 - Redis存储
public class LoginSession {
    private String sessionId;
    private Long userId;
    private String username;
    private DeviceInfo deviceInfo;
    private TokenPair tokenPair;
    private Long createdAt;
    private Long lastAccessedAt;
    
    // 领域行为
    public boolean isExpired();
    public void refresh();
}
```

#### 3.2.3 LoginLog（登录日志）
```java
// 登录日志实体 - 审计
@Entity
@Table(name = "sys_login_log")
public class LoginLog {
    @Id
    private Long id;
    private Long userId;
    private String username;
    private DeviceInfo deviceInfo;
    private LoginResult result;        // SUCCESS/FAIL
    private String failReason;
    private Long loginTime;
    
    // 静态工厂方法
    public static LoginLog success(Long userId, DeviceInfo device);
    public static LoginLog fail(String username, DeviceInfo device, String reason);
}
```

#### 3.2.4 Role & Permission（权限模型）
```java
// 角色 聚合
@Entity
@Table(name = "sys_role")
public class Role {
    @Id
    private Long id;
    private String roleName;
    private String roleCode;
    private String description;
    
    // 权限集合（聚合内部）
    @Transient
    private List<Permission> permissions = new ArrayList<>();
}

// 权限 值对象
public class Permission {
    private String code;               // 权限码: user:create
    private String name;               // 权限名: 创建用户
    private ResourceType type;         // API/MENU/BUTTON
    private String url;                // 资源URL
    private String method;             // HTTP方法
}
```

## 四、数据库设计

### 4.1 核心表结构
```sql
-- 用户表 (认证上下文)
CREATE TABLE sys_auth_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL COMMENT 'BCrypt加密',
    email VARCHAR(100),
    phone VARCHAR(20),
    status TINYINT DEFAULT 1 COMMENT '1:正常 0:禁用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 角色表
CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_name VARCHAR(50) NOT NULL UNIQUE,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 权限表
CREATE TABLE sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    permission_name VARCHAR(100) NOT NULL,
    permission_code VARCHAR(100) NOT NULL UNIQUE,
    resource_type VARCHAR(20) COMMENT 'MENU/BUTTON/API',
    resource_url VARCHAR(255),
    method VARCHAR(10) COMMENT 'GET/POST/PUT/DELETE',
    parent_id BIGINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 用户角色关联表
CREATE TABLE sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    UNIQUE KEY uk_user_role (user_id, role_id)
);

-- 角色权限关联表
CREATE TABLE sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    UNIQUE KEY uk_role_permission (role_id, permission_id)
);

-- 登录日志表 (审计)
CREATE TABLE sys_login_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT,
    username VARCHAR(50),
    client_type VARCHAR(20) COMMENT 'WEB/APP/MINI_PROGRAM',
    device_id VARCHAR(100),
    device_name VARCHAR(255),
    os VARCHAR(100),
    browser VARCHAR(100),
    ip VARCHAR(50),
    login_result TINYINT COMMENT '1:成功 0:失败',
    fail_reason VARCHAR(255),
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_time (user_id, login_time),
    INDEX idx_time (login_time)
);
```

### 4.2 Redis 存储设计
```
# Token存储 (支持多端)
auth:token:{userId}:{clientType}:{deviceId} 
  -> {accessToken, refreshToken, expiresAt, sessionId}

# Token黑名单
auth:blacklist:{token} -> 1 (TTL = 剩余有效期)

# 用户在线会话 (用于踢人)
auth:session:{userId}:{clientType} -> deviceId

# 用户权限缓存
auth:permissions:{userId} -> JSON[permissionCodes]

# 验证码
captcha:{uuid} -> {code, createdAt} (TTL = 5分钟)

# 登录限流
ratelimit:login:{ip} -> count (TTL = 1分钟)
ratelimit:login:{username} -> count (TTL = 1分钟)
```

## 五、核心服务设计

### 5.1 认证领域服务 (AuthDomainService)
```java
public interface AuthDomainService {
    // 用户认证
    AuthUser authenticate(String username, String password);
    
    // 生成Token对
    TokenPair generateTokenPair(AuthUser user, DeviceInfo device);
    
    // 验证Token
    Claims validateToken(String token);
    
    // 刷新Token
    TokenPair refreshToken(String refreshToken, DeviceInfo device);
    
    // 登出（加入黑名单）
    void logout(String token);
    
    // 踢出用户（单点登录）
    void kickOut(Long userId, ClientType clientType, String deviceId);
}
```

### 5.2 验证码服务 (CaptchaService)
```java
public interface CaptchaService {
    // 生成验证码
    CaptchaResult generateCaptcha();
    
    // 验证验证码
    boolean verifyCaptcha(String uuid, String code);
}
```

### 5.3 限流服务 (RateLimitService)
```java
public interface RateLimitService {
    // 登录限流检查
    boolean tryAcquireLogin(String ip, String username);
    
    // API限流检查
    boolean tryAcquireApi(String key, int limit, int window);
}
```

### 5.4 RBAC 领域服务 (RbacDomainService)
```java
public interface RbacDomainService {
    // 加载用户权限
    List<String> loadUserPermissions(Long userId);
    
    // 检查权限
    boolean hasPermission(Long userId, String permissionCode);
    
    // 检查URL权限
    boolean hasUrlPermission(Long userId, String url, String method);
}
```

## 六、接口设计

### 6.1 认证接口
```yaml
# 获取验证码
GET /auth/captcha
Response: { uuid, imageBase64 }

# 登录
POST /auth/login
Request: { 
  username, 
  password, 
  clientType,    # WEB/APP/MINI_PROGRAM
  deviceId, 
  captchaUuid, 
  captchaCode 
}
Response: { 
  accessToken, 
  refreshToken, 
  expiresIn, 
  tokenType: "Bearer",
  sessionId 
}

# 登出
POST /auth/logout
Header: Authorization: Bearer {token}
Response: { success: true }

# 刷新Token
POST /auth/refresh
Request: { refreshToken, clientType, deviceId }
Response: { accessToken, refreshToken, expiresIn }

# 获取当前用户信息
GET /auth/user-info
Response: { userId, username, roles, permissions }

# 踢出指定设备
POST /auth/kickout
Request: { clientType, deviceId }
```

### 6.2 权限管理接口
```yaml
# 角色管理
GET    /admin/roles
POST   /admin/roles
PUT    /admin/roles/{id}
DELETE /admin/roles/{id}

# 权限管理
GET    /admin/permissions
POST   /admin/permissions
PUT    /admin/permissions/{id}
DELETE /admin/permissions/{id}

# 用户管理
GET    /admin/users
POST   /admin/users
PUT    /admin/users/{id}
DELETE /admin/users/{id}

# 分配角色
POST /admin/users/{userId}/roles
Request: { roleIds: [1, 2, 3] }

# 分配权限
POST /admin/roles/{roleId}/permissions
Request: { permissionIds: [1, 2, 3] }

# 登录日志
GET /admin/login-logs
```

## 七、前端管理模块设计

### 7.1 技术栈
- Vue 3 + TypeScript
- Element Plus UI
- Pinia 状态管理
- Axios 请求

### 7.2 功能模块
```
huochai-admin-ui/
├── src/
│   ├── views/
│   │   ├── login/           # 登录页
│   │   ├── dashboard/       # 仪表盘
│   │   ├── user/            # 用户管理
│   │   ├── role/            # 角色管理
│   │   ├── permission/      # 权限管理
│   │   ├── login-log/       # 登录日志
│   │   └── online-user/     # 在线用户
│   ├── components/
│   ├── api/
│   └── store/
└── package.json
```

## 八、OAuth2 / OIDC 支持

### 8.1 授权码流程
```
1. 客户端重定向到 /oauth2/authorize
2. 用户登录并授权
3. 重定向回客户端，带上 code
4. 客户端用 code 换取 token
POST /oauth2/token
```

### 8.2 配置
```yaml
spring:
  security:
    oauth2:
      authorizationserver:
        client:
          web-client:
            registration:
              client-id: web-app
              client-secret: {encoded}
              scopes: openid, profile, email
              redirect-uris: https://localhost:8443/callback
```

## 九、异常处理

### 9.1 异常体系
```java
// 基础异常
public class AuthException extends RuntimeException {
    private AuthErrorCode code;
}

// 权限异常
public class PermissionDeniedException extends AuthException { }

// Token异常
public class TokenExpiredException extends AuthException { }
public class TokenInvalidException extends AuthException { }

// 限流异常
public class RateLimitExceededException extends AuthException { }

// 验证码异常
public class CaptchaException extends AuthException { }
```

### 9.2 统一异常处理
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(TokenExpiredException.class)
    public Result<Void> handleTokenExpired() {
        return Result.error(401, "Token已过期，请刷新");
    }
    
    @ExceptionHandler(PermissionDeniedException.class)
    public Result<Void> handlePermissionDenied() {
        return Result.error(403, "权限不足");
    }
    
    @ExceptionHandler(RateLimitExceededException.class)
    public Result<Void> handleRateLimit() {
        return Result.error(429, "请求过于频繁，请稍后重试");
    }
}
```

## 十、需要新增/修改的文件

### 10.1 新增文件清单

| 模块 | 文件路径 | 说明 |
|-----|---------|------|
| **认证领域** | `auth/domain/model/AuthUser.java` | 用户聚合根 |
| | `auth/domain/model/Token.java` | Token值对象 |
| | `auth/domain/model/TokenPair.java` | Token对值对象 |
| | `auth/domain/model/DeviceInfo.java` | 设备信息值对象 |
| | `auth/domain/model/LoginSession.java` | 登录会话实体 |
| | `auth/domain/model/LoginLog.java` | 登录日志实体 |
| | `auth/domain/repository/AuthUserRepository.java` | 用户仓储 |
| | `auth/domain/repository/LoginLogRepository.java` | 日志仓储 |
| | `auth/domain/service/AuthDomainService.java` | 认证领域服务 |
| | `auth/domain/service/TokenDomainService.java` | Token领域服务 |
| | `auth/application/AuthService.java` | 认证应用服务 |
| | `auth/application/CaptchaService.java` | 验证码服务 |
| | `auth/application/RateLimitService.java` | 限流服务 |
| | `auth/application/impl/AuthServiceImpl.java` | 认证服务实现 |
| | `auth/interfaces/dto/LoginRequest.java` | 登录请求DTO |
| | `auth/interfaces/dto/LoginResponse.java` | 登录响应DTO |
| **权限领域** | `permission/domain/model/Role.java` | 角色聚合 |
| | `permission/domain/model/Permission.java` | 权限值对象 |
| | `permission/domain/model/UserRole.java` | 用户角色关联 |
| | `permission/domain/model/RolePermission.java` | 角色权限关联 |
| | `permission/domain/repository/RoleRepository.java` | 角色仓储 |
| | `permission/domain/repository/PermissionRepository.java` | 权限仓储 |
| | `permission/domain/service/RbacDomainService.java` | RBAC领域服务 |
| | `permission/application/PermissionService.java` | 权限应用服务 |
| **SSO** | `sso/config/OAuth2ServerConfig.java` | OAuth2配置 |
| | `sso/service/SsoService.java` | SSO服务 |
| **公共** | `common/exception/AuthException.java` | 基础异常 |
| | `common/exception/PermissionDeniedException.java` | 权限异常 |
| | `common/exception/GlobalExceptionHandler.java` | 全局异常处理 |
| | `common/result/Result.java` | 统一响应 |
| | `common/enums/ClientType.java` | 客户端类型枚举 |
| | `common/enums/LoginResult.java` | 登录结果枚举 |
| **配置** | `config/RedisConfig.java` | Redis配置 |
| | `config/RateLimiterConfig.java` | 限流配置 |
| **前端** | `huochai-admin-ui/` | 前端管理模块 |

### 10.2 修改文件（不影响现有功能）

| 文件路径 | 修改内容 |
|---------|---------|
| `pom.xml` | 新增依赖（保留现有） |
| `application.properties` | 新增配置（保留现有） |
| `utils/JwtUtil.java` | 重构增强（保留原方法签名兼容） |
| `filters/JwtAuthenticationFilter.java` | 增强功能（兼容现有） |
| `config/SecurityConfig.java` | 新增配置（保留现有规则） |
| `web/AuthController.java` | 迁移到新架构（保持接口兼容） |

## 十一、实现优先级

1. **P0 - 核心认证**：用户登录、Token管理、Redis存储
2. **P0 - RBAC权限**：角色、权限、URL级控制
3. **P1 - 安全增强**：验证码、限流、异常处理
4. **P1 - 多端支持**：clientType、deviceId、踢人
5. **P2 - 审计日志**：登录日志、操作日志
6. **P2 - OAuth2/SSO**：统一认证中心
7. **P3 - 前端管理**：管理页面

## 十二、安全考虑

1. **密码安全**：BCrypt 加密，强度 10
2. **Token安全**：
   - AccessToken 2小时，RefreshToken 7天
   - Token 包含 jti 防重放
   - 黑名单机制
3. **限流保护**：
   - IP 级别限流（防暴力破解）
   - 用户名级别限流
   - API 级别限流
4. **验证码**：防机器人攻击

## 十三、预期成果

完成后系统将具备：
1. ✅ 安全的用户认证机制（BCrypt）
2. ✅ 双 Token 机制（Access + Refresh）
3. ✅ Redis 高效会话管理
4. ✅ 多端登录支持（Web/App 分开）
5. ✅ 设备标识管理
6. ✅ 单点登录踢人机制
7. ✅ Token 黑名单（安全登出）
8. ✅ RBAC 灵活权限控制
9. ✅ 验证码防暴力破解
10. ✅ 登录限流保护
11. ✅ 登录审计日志
12. ✅ OAuth2/OIDC 支持
13. ✅ 统一异常处理
14. ✅ 前端管理界面