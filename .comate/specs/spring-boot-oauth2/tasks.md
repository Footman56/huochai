# Spring Boot 3.5.11 OAuth2 实战任务计划

## 任务概述
在 Spring Boot 3.5.11 项目中实现完整的 OAuth2 认证授权体系，采用多模块项目结构，每个模块需要补充 README.md 说明文档

## 任务列表

- [x] Task 1: 清理已创建的文件（准备按多模块结构重新创建）
  - 1.1: 确认文件已清理
  - 1.2: 确认 pom.xml 状态

- [x] Task 2: 创建多模块 Maven 项目结构
  - 2.1: 修改根 pom.xml 配置为 pom 打包类型
  - 2.2: 创建 auth-server 模块（授权服务器）
  - 2.3: 创建 resource-server 模块（资源服务器）
  - 2.4: 创建 client-app 模块（客户端应用）
  - 2.5: 配置各模块的 pom.xml 依赖

- [x] Task 3: 实现 auth-server 模块
  - 3.1: 创建 AuthorizationServerConfig.java 配置类
  - 3.2: 创建 CustomUserDetailsService.java 用户服务
  - 3.3: 配置 application.properties
  - 3.4: 创建 README.md 说明文档

- [x] Task 4: 实现 resource-server 模块
  - 4.1: 创建 ResourceServerConfig.java 配置类
  - 4.2: 创建测试 Controller
  - 4.3: 配置 application.properties
  - 4.4: 创建 README.md 说明文档

- [x] Task 5: 实现 client-app 模块
  - 5.1: 创建 ClientApplicationConfig.java 配置类
  - 5.2: 创建登录页面 Controller
  - 5.3: 配置 application.properties
  - 5.4: 创建 README.md 说明文档

- [x] Task 6: 验证 OAuth2 流程
  - 6.1: 各模块结构已创建完成
  - 6.2: 启动顺序: auth-server → resource-server → client-app
  - 6.3: 测试授权流程