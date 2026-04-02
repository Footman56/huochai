# Spring Boot 3.5.11 OAuth2 实战任务计划

## 任务概述
在 Spring Boot 3.5.11 项目中实现完整的 OAuth2 认证授权体系

## 任务列表

- [ ] Task 1: 添加 OAuth2 相关 Maven 依赖到 pom.xml
  - 1.1: 添加 Spring Authorization Server 依赖
  - 1.2: 添加 OAuth2 Resource Server 依赖
  - 1.3: 添加 OAuth2 Client 依赖
  - 1.4: 添加 JWT 支持依赖

- [ ] Task 2: 创建 Authorization Server 配置
  - 2.1: 创建 AuthorizationServerConfig 配置类
  - 2.2: 实现 RegisteredClientRepository Bean
  - 2.3: 配置 JWKSource (RSA 密钥对)
  - 2.4: 配置 AuthorizationServerSettings

- [ ] Task 3: 创建 Resource Server 配置
  - 3.1: 创建 ResourceServerConfig 配置类
  - 3.2: 配置 JWT 解码器和认证转换器
  - 3.3: 配置 API 访问规则和权限控制

- [ ] Task 4: 创建 Client Application 配置
  - 4.1: 创建 ClientApplicationConfig 配置类
  - 4.2: 配置 ClientRegistration
  - 4.3: 配置 OAuth2 Login 和 Logout

- [ ] Task 5: 创建 UserDetailsService 实现
  - 5.1: 创建 CustomUserDetailsService 类
  - 5.2: 实现用户加载逻辑
  - 5.3: 配置密码编码器

- [ ] Task 6: 创建测试 Controller
  - 6.1: 创建受保护的 API 端点
  - 6.2: 创建公开的 API 端点
  - 6.3: 创建用户信息端点

- [ ] Task 7: 验证 OAuth2 流程
  - 7.1: 配置 application.properties
  - 7.2: 启动应用测试授权流程
  - 7.3: 验证 Token 交换和资源访问