# Client App (OAuth2 Client Application)

## 模块说明

本模块实现 OAuth2 客户端应用，基于 Spring Boot 3.5.11 和 spring-boot-starter-oauth2-client 实现。

## 功能特性

- OAuth2 登录集成
- 自动 Token 管理
- 用户信息获取
- 登出处理
- Thymeleaf 页面模板

## 核心配置

### ClientApplicationConfig

主要配置：

1. **SecurityFilterChain** - 安全过滤链
   - 公开路径: /, /login
   - OAuth2 登录配置
   - 登出配置

2. **ClientRegistrationRepository** - 客户端注册
   - 内存存储 (InMemoryClientRegistrationRepository)
   - 配置授权服务器信息

### 客户端配置

| 配置项 | 值 |
|--------|-----|
| client-id | client-app |
| client-secret | secret |
| 授权类型 | authorization_code |
| 回调地址 | http://127.0.0.1:8082/login/oauth2/code/client-app |
| Scopes | openid, message.read, message.write |
| 授权服务器 | http://127.0.0.1:9000 |

## 页面说明

| 路径 | 说明 |
|------|------|
| / | 首页 |
| /login | 登录页面 |
| /oauth2/loginSuccess | 登录成功页面 |
| /user | 用户信息页面 |

## 启动方式

```bash
cd client-app
mvn spring-boot:run
```

服务启动后访问: http://127.0.0.1:8082

## OAuth2 登录流程

```
1. 用户访问 http://127.0.0.1:8082
2. 点击 "Login with OAuth2"
3. 跳转到授权服务器 http://127.0.0.1:9000/oauth2/authorize
4. 用户输入用户名密码登录
5. 用户授权同意
6. 授权服务器跳转回 http://127.0.0.1:8082/login/oauth2/code/client-app
7. 客户端交换授权码获取 Access Token
8. 使用 Access Token 获取用户信息
9. 显示登录成功页面
```

## 源码调用链

### OAuth2 登录流程

```
OAuth2AuthorizationRequestRedirectFilter
    → 生成授权请求 → 重定向到授权服务器

OAuth2LoginAuthenticationFilter
    → OAuth2AuthorizationCodeAuthenticationProvider
    → 交换授权码 → 获取 Access Token
    → 获取用户信息 → 创建认证

SecurityContextHolder
    → 存储 Authentication 对象
```

### 关键类

- **OAuth2LoginAuthenticationFilter**: 处理 OAuth2 登录
- **OAuth2AuthorizationCodeAuthenticationProvider**: 授权码交换
- **InMemoryClientRegistrationRepository**: 客户端注册存储
- **OAuth2User**: OAuth2 用户信息

## 生产环境扩展点

1. **自定义 ClientRegistrationRepository** - 从数据库加载客户端配置
2. **自定义 OAuth2User** - 扩展用户信息
3. **自定义登录页面** - 定制 UI
4. **Token 持久化** - 使用 Redis 存储 Token
5. **单点登录** - 集成企业 SSO