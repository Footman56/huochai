# 火柴认证系统 (Huochai Auth System)

<p align="center">
  <b>基于 DDD 架构的生产级完整登录认证体系</b>
</p>

<p align="center">
  <a href="#功能特性">功能特性</a> •
  <a href="#技术架构">技术架构</a> •
  <a href="#快速开始">快速开始</a> •
  <a href="#API文档">API文档</a> •
  <a href="#配置说明">配置说明</a>
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
| 🌐 **OAuth2 Authorization Server** | 标准授权服务器 |
| 🔑 **授权码模式** | 支持 Web/App 客户端 |
| 📋 **OIDC 支持** | OpenID Connect 1.0 |
| 🏢 **统一认证中心** | SSO 单点登录 |

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