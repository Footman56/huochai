# OAuth2 功能集成任务计划

- [x] Task 1: 创建 OAuth2 数据库表
    - 1.1: 创建 oauth2_registered_client 表
    - 1.2: 创建 oauth2_authorization 表
    - 1.3: 创建 oauth2_authorization_consent 表
    - 1.4: 插入预置客户端数据

- [x] Task 2: 创建 OAuth2 客户端实体和仓储
    - 2.1: 创建 OAuth2Client 领域模型 (auth/domain/model)
    - 2.2: 创建 OAuth2ClientRepository 接口 (auth/domain/repository)
    - 2.3: 创建 OAuth2ClientMapper (auth/infrastructure/mapper)
    - 2.4: 创建 OAuth2ClientRepositoryImpl 实现 (auth/infrastructure/repository)

- [x] Task 3: 改造 OAuth2ServerConfig
    - 3.1: 配置基于数据库的 RegisteredClientRepository
    - 3.2: 配置 OAuth2AuthorizationService
    - 3.3: 配置 OAuth2AuthorizationConsentService
    - 3.4: 配置 JWKSource 签名密钥
    - 3.5: 配置 AuthorizationServerSettings
    - 3.6: 配置 OAuth2 Authorization Server SecurityFilterChain

- [x] Task 4: 创建 OAuth2 控制器
    - 4.1: 创建 OAuth2Controller 提供授权页面
    - 4.2: 实现 UserInfo 端点

- [x] Task 5: 改造 SecurityConfig
    - 5.1: 添加 OAuth2 SecurityFilterChain (Order=1)
    - 5.2: 配置 OAuth2 端点放行

- [x] Task 6: 添加配置项
    - 6.1: 添加 application.properties OAuth2 配置

- [x] Task 7: 更新 README 文档
    - 7.1: 添加 OAuth2 功能说明
    - 7.2: 添加授权流程图
    - 7.3: 添加 API 端点文档
    - 7.4: 添加配置和使用示例