# Resource Server

## 模块说明

本模块实现 OAuth2 资源服务器，基于 Spring Boot 3.5.11 和 spring-boot-starter-oauth2-resource-server 实现。

## 功能特性

- JWT 令牌验证
- 基于 Scope 的权限控制
- 无状态 Session 管理
- 方法级安全注解支持

## 核心配置

### ResourceServerConfig

主要配置：

1. **SecurityFilterChain** - 安全过滤链
   - 无状态 Session 管理 (STATELESS)
   - JWT 令牌验证
   - 基于 Scope 的权限控制

2. **JwtAuthenticationConverter** - JWT 认证转换器
   - 从 JWT 的 "roles" Claim 提取权限
   - 权限前缀: SCOPE_

### 访问规则

| 路径 | 权限要求 |
|------|----------|
| /public/** | 公开 |
| /actuator/** | 公开 |
| /admin/** | SCOPE_admin |
| /user/** | SCOPE_user 或 SCOPE_admin |
| /** | 需要认证 |

## 接口说明

| 方法 | 路径 | 权限 | 说明 |
|------|------|------|------|
| GET | /api/public/hello | 公开 | 公开接口 |
| GET | /api/protected/hello | 需认证 | 受保护接口 |
| GET | /api/user/profile | SCOPE_user | 用户信息 |
| GET | /api/message/read | SCOPE_message.read | 读取消息 |
| GET | /api/message/write | SCOPE_message.write | 写入消息 |
| GET | /api/token/info | 需认证 | Token 信息 |

## 启动方式

```bash
cd resource-server
mvn spring-boot:run
```

服务启动后访问: http://127.0.0.1:8081

## 源码调用链

### Token 验证流程

```
JwtAuthenticationFilter
    → JwtDecoder (验证 JWT 签名)
    → JwtAuthenticationProvider
    → JwtGrantedAuthoritiesConverter (提取权限)
    → SecurityContextHolder
    → AuthorizationFilter (权限校验)
```

### 关键类

- **JwtAuthenticationFilter**: 处理 Bearer Token
- **JwtDecoder**: 验证 JWT 签名和有效期
- **JwtGrantedAuthoritiesConverter**: 从 JWT 提取权限
- **OAuth2ResourceServerConfigurer**: 资源服务器配置

## 生产环境扩展点

1. **自定义 JwtAuthenticationConverter** - 提取自定义 Claims
2. **自定义 JwtDecoder** - 集成外部 JWT 验证服务
3. **方法级安全** - 使用 @PreAuthorize 注解
4. **自定义权限表达式** - 实现 PermissionEvaluator