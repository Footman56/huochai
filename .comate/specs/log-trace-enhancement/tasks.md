# 日志模块与链路追踪增强任务计划

- [x] Task 1: 创建 huochai-log-starter 模块基础结构
    - 1.1: 创建模块目录结构和 pom.xml 配置
    - 1.2: 添加核心依赖（Disruptor、Logstash Logback Encoder、Micrometer Tracing）
    - 1.3: 创建自动配置类 LogAutoConfiguration
    - 1.4: 创建配置属性类 LogProperties
    - 1.5: 配置 Spring Boot AutoConfiguration 注册

- [x] Task 2: 实现统一日志模型和枚举
    - 2.1: 创建日志类型枚举 LogType（CONTROLLER/DUBBO/MQ/MYSQL/REDIS/ES 等）
    - 2.2: 创建基础日志实体 LogEntry（统一公共字段）
    - 2.3: 创建各场景日志实体（ControllerLogEntry、DubboLogEntry、MqLogEntry 等）
    - 2.4: 创建 MySQL 日志实体 MySqlLogEntry
    - 2.5: 创建 Redis 日志实体 RedisLogEntry
    - 2.6: 创建 ES 日志实体 ElasticsearchLogEntry

- [x] Task 3: 实现 Disruptor 高性能日志队列
    - 3.1: 创建 LogEventFactory 事件工厂
    - 3.2: 创建 LogEventData 事件数据
    - 3.3: 创建 LogEventTranslator 事件翻译器
    - 3.4: 创建 LogEventHandler 事件处理器
    - 3.5: 创建 LogDisruptorConfig 配置类

- [x] Task 4: 实现日志收集器和写入器
    - 4.1: 创建 LogCollector 接口
    - 4.2: 创建 LogCollectorImpl 实现
    - 4.3: 创建 JsonLogWriter（JSON 格式写入本地文件）
    - 4.4: 实现 Logback JSON 输出配置

- [x] Task 5: 实现自动拦截器（Controller/Service）
    - 5.1: 创建 ControllerInterceptor（AOP 自动拦截所有 Controller）
    - 5.2: 创建 ServiceInterceptor（AOP 自动拦截所有 Service）

- [x] Task 6: 实现数据库操作日志拦截器
    - 6.1: 创建 MySqlLogInterceptor（MyBatis 插件）
    - 6.2: 创建 RedisLogInterceptor（RedisTemplate 包装器）
    - 6.3: 创建 ElasticsearchLogInterceptor（ES Client 包装器）

- [x] Task 7: 实现 RPC 和 MQ 日志拦截器
    - 7.1: 创建 DubboLogFilter（Dubbo SPI 扩展点）
    - 7.2: 创建 MqConsumerInterceptor（RabbitMQ 容器工厂）
    - 7.3: 创建 MqProducerInterceptor（RabbitTemplate 回调）
    - 7.4: 创建 ScheduledInterceptor（定时任务拦截）

- [x] Task 8: 实现链路追踪模块
    - 8.1: 创建 TraceContextHolder（Trace 上下文工具类）
    - 8.2: 创建 TraceFilter（HTTP 请求 Trace 过滤器）
    - 8.3: 集成 Micrometer Tracing + Zipkin
    - 8.4: 实现 Trace 自动注入到 MDC

- [x] Task 9: 实现日志工具类（可选使用）
    - 9.1: 创建 LogHelper（日志记录工具类）
    - 9.2: 创建 LogContext（日志上下文）
    - 9.3: 创建 BusinessLogBuilder（业务日志构建器）
    - 9.4: 创建 ExceptionLogBuilder（异常日志构建器）

- [x] Task 10: 创建 ELK 部署配置
    - 10.1: 创建 Filebeat 配置文件（filebeat.yml）
    - 10.2: 创建 Logstash 配置文件（logstash.conf）
    - 10.3: 创建 Elasticsearch 配置和索引模板
    - 10.4: 创建 Kibana 配置文件
    - 10.5: 创建 Docker Compose 编排文件

- [x] Task 11: 集成到 huochai-core 项目
    - 11.1: 添加 huochai-log-starter 依赖到 huochai-core
    - 11.2: 删除 huochai-core 中的旧日志模块
    - 11.3: 配置 application.properties 日志参数
    - 11.4: 验证自动拦截功能正常工作

- [x] Task 12: 编写模块文档
    - 12.1: 编写 huochai-log-starter/README.md 模块说明文档
    - 12.2: 编写 deploy/elk/README.md ELK 部署文档
    - 12.3: 更新项目主 README.md 日志模块说明