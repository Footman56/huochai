# 日志模块与链路追踪增强

## 需求场景

### 1. 日志模块优化
**当前问题：**
- 操作日志直接通过 MyBatis 同步写入 MySQL，影响主流程性能
- 日志存储与业务代码耦合，没有解耦
- 不支持高并发写入场景
- 无法保证日志写入顺序
- 日志模块无法被其他项目复用

**解决方案：**
- **独立模块：** 创建 `huochai-log-starter` 独立模块，可作为 Spring Boot Starter 被其他项目引入
- **零侵入设计：** 引入依赖后自动生效，无需添加任何注解，对业务代码完全无侵入
- **标准 ELK 生产架构：** SpringBoot → Logback(JSON) → Filebeat → Logstash → Elasticsearch → Kibana
- **避免直接写 ES：** 应用只负责写本地文件，由 Filebeat 采集，防止 ES 压力过大
- **JSON 格式日志：** 使用 Logstash Logback Encoder 输出结构化 JSON 日志
- **全链路日志支持：** 自动拦截所有操作，包括 Controller、Service、Dubbo、MQ、MySQL、Redis、ES 等
- **统一字段格式：** 所有日志类型使用统一的公共字段，便于数据处理和统计
- **自动拦截机制：** 通过 AOP、MyBatis 插件、Redis 拦截器等自动记录，无需业务代码修改
- 支持日志批量写入，提高吞吐量
- **高并发保障：** 使用 Disruptor 无锁队列实现百万级 TPS
- **顺序保障：** 按_traceId分片，同一链路日志保证顺序写入
- **生产友好：** 日志文件包含 traceId、HTTP路径、请求参数等关键信息，便于排查问题

### 2. 链路追踪增强
**当前问题：**
- TraceId 仅支持 HTTP 请求传递
- 不支持 RPC 调用（Feign/Dubbo）
- 不支持消息队列调用（RabbitMQ/Kafka）

**解决方案：**
- 采用 Micrometer Tracing（Spring Boot 3.x 官方推荐）
- 支持 OpenTelemetry 协议
- 集成 Zipkin 作为追踪后端
- 支持 Feign、RabbitMQ 调用链路传递

---

## 技术方案

### 模块架构设计

**独立模块设计（Spring Boot Starter）：**

```
huochai-parent/
├── huochai-log-starter/                    # 独立日志模块（Spring Boot Starter）
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/huochai/log/
│       │   ├── autoconfigure/              # 自动配置
│       │   │   ├── LogAutoConfiguration.java
│       │   │   └── LogProperties.java
│       │   ├── annotation/                 # 注解定义
│       │   │   └── EnableLogCollector.java
│       │   ├── collector/                  # 日志收集器
│       │   │   ├── LogCollector.java
│       │   │   └── LogCollectorImpl.java
│       │   ├── disruptor/                  # 高性能队列
│       │   │   ├── LogDisruptorConfig.java
│       │   │   ├── LogEventFactory.java
│       │   │   ├── LogEventTranslator.java
│       │   │   └── LogEventHandler.java
│       │   ├── writer/                     # 日志写入器
│       │   │   └── JsonLogWriter.java      # JSON 格式写入本地文件
│       │   ├── model/                      # 数据模型
│       │   │   ├── LogEntry.java
│       │   │   └── LogDocument.java
│       │   └── trace/                      # 链路追踪
│       │       ├── TraceContextHolder.java
│       │       └── TraceFilter.java
│       └── resources/
│           ├── META-INF/spring/
│           │   └── org.springframework.boot.autoconfigure.AutoConfiguration.imports
│           └── logback-spring.xml          # Logback JSON 配置
│
├── deploy/                                 # 部署配置
│   └── elk/                                # ELK 配置
│       ├── filebeat/filebeat.yml
│       ├── logstash/logstash.conf
│       ├── elasticsearch/elasticsearch.yml
│       ├── kibana/kibana.yml
│       └── docker-compose.yml
│
├── huochai-core/                           # 主项目（使用日志模块）
│   ├── pom.xml                             # 引入 huochai-log-starter 依赖
│   └── src/main/java/
│       └── com/huochai/
│           ├── HuochaiCoreApplication.java # @EnableLogCollector
│           └── ...                         # 业务代码无需修改
```

**使用方式（零侵入）：**

```java
// 1. 在启动类添加注解开启日志收集
@EnableLogCollector
@SpringBootApplication
public class HuochaiCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(HuochaiCoreApplication.class, args);
    }
}

// 2. 业务代码无需任何修改，日志自动收集
@RestController
public class UserController {
    @OperationLog(module = "用户管理", desc = "创建用户")
    @PostMapping("/users")
    public Result<User> createUser(@RequestBody User user) {
        // 业务逻辑，日志自动收集
        return Result.success(user);
    }
}
```

**Maven 依赖引入：**

```xml
<!-- 其他项目只需引入此依赖即可使用 -->
<dependency>
    <groupId>com.huochai</groupId>
    <artifactId>huochai-log-starter</artifactId>
    <version>${project.version}</version>
</dependency>
```

### 日志模块架构

**标准 ELK 生产架构：**

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           ELK 日志架构（生产标准）                                │
├─────────────────────────────────────────────────────────────────────────────────┤
│                                                                                 │
│  ┌──────────────┐                                                               │
│  │ SpringBoot   │                                                               │
│  │   应用       │                                                               │
│  │              │                                                               │
│  │ @OperationLog│                                                               │
│  └──────┬───────┘                                                               │
│         │                                                                       │
│         ▼                                                                       │
│  ┌──────────────┐     ┌──────────────┐                                         │
│  │   Logback    │────▶│  本地日志文件 │                                         │
│  │  JSON格式    │     │ logs/json/   │                                         │
│  │              │     │ operation-*. │                                         │
│  └──────────────┘     │ log          │                                         │
│                       └──────┬───────┘                                         │
│                              │                                                  │
│                              ▼                                                  │
│                       ┌──────────────┐                                         │
│                       │   Filebeat   │                                         │
│                       │   日志采集   │                                         │
│                       │   (轻量级)   │                                         │
│                       └──────┬───────┘                                         │
│                              │                                                  │
│                              ▼                                                  │
│                       ┌──────────────┐                                         │
│                       │   Logstash   │                                         │
│                       │  解析/清洗   │                                         │
│                       │  过滤/转换   │                                         │
│                       └──────┬───────┘                                         │
│                              │                                                  │
│                              ▼                                                  │
│                       ┌──────────────┐                                         │
│                       │Elasticsearch │                                         │
│                       │    存储      │                                         │
│                       │  (分布式)    │                                         │
│                       └──────┬───────┘                                         │
│                              │                                                  │
│                              ▼                                                  │
│                       ┌──────────────┐                                         │
│                       │   Kibana     │                                         │
│                       │  查询分析    │                                         │
│                       │  可视化      │                                         │
│                       └──────────────┘                                         │
│                                                                                 │
└─────────────────────────────────────────────────────────────────────────────────┘
```

**架构优势（生产环境）：**

| 组件 | 职责 | 优势 |
|-----|------|------|
| SpringBoot | 只负责写本地文件 | 应用简单，无外部依赖压力 |
| Logback | JSON 格式输出 | 结构化日志，便于解析 |
| Filebeat | 轻量级采集 | 资源占用低，支持多文件 |
| Logstash | 解析/清洗/转换 | 强大的数据处理能力 |
| Elasticsearch | 存储/索引 | 分布式存储，高可用 |
| Kibana | 查询/可视化 | 强大的分析能力 |

**为什么不在应用中直接写 ES？**
```
❌ 直接写 ES 的问题：
   - ES 压力过大，影响集群稳定性
   - 应用与 ES 强耦合，ES 故障影响业务
   - 无法缓冲高峰流量，可能导致丢失日志
   - 缺乏数据清洗环节，ES 数据质量差

✅ 标准 ELK 架构优势：
   - 应用只写本地文件，性能极高
   - Filebeat 轻量采集，支持背压控制
   - Logstash 统一清洗，数据质量高
   - ES 独立扩展，不影响应用
```

**日志 JSON 格式输出：**
```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Logback JSON 日志输出                                  │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌──────────────┐                                                          │
│   │ 业务代码      │                                                          │
│   │ @OperationLog│                                                          │
│   └──────┬───────┘                                                          │
│          │                                                                  │
│          ▼                                                                  │
│   ┌──────────────────────────────────────────────────────────────┐         │
│   │                   Logback 配置                                │         │
│   │                                                              │         │
│   │   <appender name="JSON_FILE" class="...RollingFileAppender">│         │
│   │     <encoder class="net.logstash.logback.encoder.LogstashEncoder">      │
│   │       <customFields>{"app_name":"huochai-auth"}</customFields>          │
│   │       <includeMdcKeyName>traceId</includeMdcKeyName>                    │
│   │       <includeMdcKeyName>userId</includeMdcKeyName>                     │
│   │     </encoder>                                              │         │
│   │   </appender>                                               │         │
│   └──────────────────────────────────────────────────────────────┘         │
│          │                                                                  │
│          ▼                                                                  │
│   ┌──────────────┐     ┌──────────────┐                                    │
│   │ logs/json/   │     │ logs/json/   │                                    │
│   │ operation-   │     │ error-       │                                    │
│   │ 2024-01-15.  │     │ 2024-01-15.  │                                    │
│   │ log          │     │ log          │                                    │
│   └──────────────┘     └──────────────┘                                    │
│          │                                                                  │
│          ▼                                                                  │
│   ┌──────────────────────────────────────────────────────────────┐         │
│   │  JSON 日志格式示例：                                         │         │
│   │  {                                                          │         │
│   │    "@timestamp": "2024-01-15T14:30:25.123+08:00",          │         │
│   │    "app_name": "huochai-auth",                              │         │
│   │    "traceId": "a1b2c3d4e5f6g7h8",                          │         │
│   │    "userId": 1,                                             │         │
│   │    "username": "admin",                                     │         │
│   │    "level": "INFO",                                         │         │
│   │    "logger": "c.h.log.LogFileWriter",                      │         │
│   │    "message": "操作日志",                                   │         │
│   │    "module": "用户管理",                                    │         │
│   │    "operationType": "CREATE",                               │         │
│   │    "operationDesc": "创建用户",                             │         │
│   │    "requestMethod": "POST",                                 │         │
│   │    "requestUrl": "/api/admin/users",                       │         │
│   │    "requestParams": "{\"username\":\"test\"}",             │         │
│   │    "responseResult": "{\"code\":200}",                      │         │
│   │    "ip": "192.168.1.100",                                   │         │
│   │    "status": 1,                                             │         │
│   │    "costTime": 125                                          │         │
│   │  }                                                          │         │
│   └──────────────────────────────────────────────────────────────┘         │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**Filebeat 采集配置：**
```yaml
# filebeat.yml
filebeat.inputs:
- type: log
  enabled: true
  paths:
    - /var/log/huochai/json/*.log
  json.keys_under_root: true
  json.add_error_key: true
  fields:
    app: huochai-auth
    env: production

output.logstash:
  hosts: ["logstash:5044"]
  bulk_max_size: 2048
  
# 背压控制
queue.mem:
  events: 4096
  flush.min_events: 512
  flush.timeout: 1s
```

**关键设计：**

#### 1. 高并发写入保障
```
┌─────────────────────────────────────────────────────────────────────┐
│                      高并发日志写入架构                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   业务线程 ──┐                                                      │
│   业务线程 ──┼──▶ Disruptor RingBuffer ──▶ 消费者线程池            │
│   业务线程 ──┤      (无锁CAS操作)           (批量聚合)              │
│   业务线程 ──┘            │                     │                  │
│                           ▼                     ▼                  │
│                    ┌──────────────┐     ┌──────────────┐           │
│                    │  多生产者模式  │     │  批量发送    │           │
│                    │  单消费者模式  │     │  100条/批    │           │
│                    └──────────────┘     └──────────────┘           │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

- **Disruptor 无锁队列：** 使用 RingBuffer 实现百万级 TPS
- **多生产者单消费者模式：** 多线程写入，单线程聚合发送
- **批量聚合发送：** 每 100 条或每 100ms 聚合发送一次
- **背压控制：** 当队列达到 80% 容量时，触发限流

#### 2. 写入顺序保障
```
┌─────────────────────────────────────────────────────────────────────┐
│                      日志顺序保障策略                                 │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   日志事件 ──▶ 计算 shardKey = hash(traceId) % shardCount          │
│                                                                     │
│   ┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐           │
│   │ Shard 0 │   │ Shard 1 │   │ Shard 2 │   │ Shard N │           │
│   │ Queue-0 │   │ Queue-1 │   │ Queue-2 │   │ Queue-N │           │
│   └────┬────┘   └────┬────┘   └────┬────┘   └────┬────┘           │
│        │             │             │             │                 │
│        ▼             ▼             ▼             ▼                 │
│   Consumer-0    Consumer-1    Consumer-2    Consumer-N            │
│   (单线程)       (单线程)       (单线程)       (单线程)             │
│                                                                     │
│   保证：同一 traceId 的日志永远路由到同一队列，由同一消费者处理      │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

- **分片队列策略：** 按 traceId 哈希分片，保证同一链路日志进入同一队列
- **单消费者模式：** 每个分片队列只有 1 个消费者，保证顺序
- **Logstash 批处理：** 批量写入 ES，提高吞吐量

#### 3. 可靠性保障（标准 ELK 架构）
```
┌─────────────────────────────────────────────────────────────────────┐
│                      日志可靠性保障（ELK 标准）                        │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│   Level 1: Disruptor 本地缓存                                       │
│            └── 内存队列，高性能缓冲，百万级 TPS                       │
│                                                                     │
│   Level 2: 本地 JSON 日志文件                                       │
│            └── Logback 直接写入本地文件                              │
│            └── JSON 格式，包含完整字段                               │
│            └── 进程重启后数据仍在，生产问题排查首选                    │
│                                                                     │
│   Level 3: Filebeat 采集                                            │
│            └── 轻量级采集，资源占用低                                │
│            └── 支持背压控制，不会拖垮应用                             │
│            └── 断点续传，网络恢复后自动续传                           │
│                                                                     │
│   Level 4: Logstash 缓冲                                            │
│            └── 持久化队列（可配置）                                  │
│            └── 数据清洗、过滤、转换                                  │
│            └── 批量写入 ES，减少压力                                 │
│                                                                     │
│   Level 5: Elasticsearch 副本                                       │
│            └── 多副本存储，数据安全                                  │
│            └── 分布式存储，高可用                                    │
│            └── 支持 Kibana 高级查询                                  │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

#### 4. 多场景日志格式设计

**日志类型分类：**

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           多场景日志类型                                      │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐  │
│   │                        日志类型 (logType)                            │  │
│   ├─────────────────────────────────────────────────────────────────────┤  │
│   │                                                                     │  │
│   │   CONTROLLER  - HTTP 请求日志                                       │  │
│   │      └── 包含：请求路径、HTTP方法、请求参数、响应结果、耗时          │  │
│   │                                                                     │  │
│   │   SERVICE    - 业务逻辑日志                                         │  │
│   │      └── 包含：业务操作、处理结果、关键数据                          │  │
│   │                                                                     │  │
│   │   DUBBO      - Dubbo RPC 调用日志                                   │  │
│   │      └── 包含：服务名、方法名、参数、结果、远程地址                   │  │
│   │                                                                     │  │
│   │   MQ_CONSUMER - MQ 消息消费日志                                     │  │
│   │      └── 包含：队列名、消息ID、消息内容、消费结果                     │  │
│   │                                                                     │  │
│   │   MQ_PRODUCER - MQ 消息生产日志                                     │  │
│   │      └── 包含：队列名、消息ID、消息内容、发送结果                     │  │
│   │                                                                     │  │
│   │   SCHEDULED  - 定时任务日志                                         │  │
│   │      └── 包含：任务名、执行时间、执行结果、耗时                       │  │
│   │                                                                     │  │
│   │   MYSQL      - MySQL 数据库操作日志                                 │  │
│   │      └── 包含：SQL语句、参数、执行时间、影响行数、表名               │  │
│   │                                                                     │  │
│   │   REDIS      - Redis 缓存操作日志                                   │  │
│   │      └── 包含：命令、Key、Value、TTL、执行时间                       │  │
│   │                                                                     │  │
│   │   ES         - Elasticsearch 操作日志                               │  │
│   │      └── 包含：索引名、操作类型、文档ID、查询条件、执行时间          │  │
│   │                                                                     │  │
│   │   EXCEPTION  - 异常日志                                             │  │
│   │      └── 包含：异常类型、异常消息、完整堆栈                          │  │
│   │                                                                     │  │
│   │   BUSINESS   - 业务自定义日志                                       │  │
│   │      └── 包含：业务编码、业务数据、操作结果                          │  │
│   │                                                                     │  │
│   └─────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

**日志文件路径：**
```
logs/
├── json/                           # JSON 格式日志（供 Filebeat 采集）
│   ├── all-2024-01-15.log          # 所有日志（统一入口）
│   ├── controller-2024-01-15.log   # Controller 日志
│   ├── dubbo-2024-01-15.log        # Dubbo RPC 日志
│   ├── mq-2024-01-15.log           # MQ 消息日志
│   ├── mysql-2024-01-15.log        # MySQL 数据库日志
│   ├── redis-2024-01-15.log        # Redis 缓存日志
│   ├── es-2024-01-15.log           # Elasticsearch 日志
│   ├── scheduled-2024-01-15.log    # 定时任务日志
│   └── error-2024-01-15.log        # 错误日志
├── console/                        # 控制台格式日志（人工查看）
│   └── application.log
└── archive/                        # 归档日志
```

---

#### 5. 统一字段规范

**所有日志类型必须包含以下公共字段（统一规范）：**

```json
{
  // ========== 基础字段（所有日志类型必须包含）==========
  "@timestamp": "2024-01-15T14:30:25.123+08:00",    // ISO8601 时间戳
  "traceId": "a1b2c3d4e5f6g7h8",                    // 链路追踪ID
  "spanId": "span-001",                              // 当前 Span ID
  "parentSpanId": null,                              // 父 Span ID（可选）
  "logType": "CONTROLLER",                           // 日志类型（枚举值）
  "level": "INFO",                                   // 日志级别
  "logger": "c.h.auth.controller.UserController",   // 日志记录器类名
  "thread": "http-nio-8080-exec-1",                 // 线程名
  "message": "操作日志",                             // 日志消息
  
  // ========== 应用信息字段 ==========
  "app_name": "huochai-auth",                        // 应用名称
  "app_version": "1.0.0",                            // 应用版本
  "env": "production",                               // 环境
  "host": "192.168.1.100",                          // 主机IP
  "port": 8080,                                      // 端口
  
  // ========== 用户上下文字段 ==========
  "userId": 1,                                       // 用户ID（可选）
  "username": "admin",                               // 用户名（可选）
  "clientType": "WEB",                               // 客户端类型（可选）
  "deviceId": "device-123",                          // 设备ID（可选）
  "ip": "192.168.1.100",                            // 客户端IP（可选）
  
  // ========== 执行结果字段 ==========
  "status": 1,                                       // 状态：1成功/0失败
  "costTime": 125,                                   // 耗时（毫秒）
  "success": true,                                   // 是否成功（布尔值）
  "errorMsg": null,                                  // 错误信息（失败时）
  
  // ========== 场景扩展字段 ==========
  // 不同日志类型在此处扩展特有字段，如：http、mysql、redis、es 等
}
```

**统一字段说明表：**

| 字段名 | 类型 | 必填 | 说明 |
|-------|------|-----|------|
| @timestamp | string | 是 | ISO8601 格式时间戳 |
| traceId | string | 是 | 链路追踪ID，贯穿整个调用链 |
| spanId | string | 是 | 当前 Span ID |
| parentSpanId | string | 否 | 父 Span ID |
| logType | string | 是 | 日志类型枚举值 |
| level | string | 是 | 日志级别：INFO/WARN/ERROR/DEBUG |
| logger | string | 是 | 记录日志的类名 |
| thread | string | 是 | 线程名 |
| message | string | 是 | 日志消息 |
| app_name | string | 是 | 应用名称 |
| app_version | string | 否 | 应用版本 |
| env | string | 是 | 环境：dev/test/staging/production |
| host | string | 是 | 主机IP |
| port | int | 是 | 应用端口 |
| userId | long | 否 | 操作用户ID |
| username | string | 否 | 操作用户名 |
| clientType | string | 否 | 客户端类型：WEB/APP/API |
| deviceId | string | 否 | 设备标识 |
| ip | string | 否 | 客户端IP |
| status | int | 是 | 状态：1成功/0失败 |
| costTime | long | 是 | 执行耗时（毫秒） |
| success | boolean | 是 | 是否成功 |
| errorMsg | string | 否 | 错误信息 |

**场景扩展字段命名规范：**

| 日志类型 | 扩展字段前缀 | 示例字段 |
|---------|-------------|---------|
| CONTROLLER | http. | http.method, http.url, http.requestBody |
| DUBBO | dubbo. | dubbo.serviceName, dubbo.methodName |
| MQ_CONSUMER / MQ_PRODUCER | mq. | mq.queue, mq.messageId, mq.payload |
| MYSQL | mysql. | mysql.sql, mysql.parameters, mysql.affectedRows |
| REDIS | redis. | redis.command, redis.key, redis.ttl |
| ES | es. | es.index, es.operation, es.query |
| SCHEDULED | scheduled. | scheduled.taskName, scheduled.cronExpression |
| EXCEPTION | exception. | exception.type, exception.message, exception.stackTrace |
| BUSINESS | business. | business.code, business.module, business.action |

---

#### 6. 各场景日志格式详解

##### 6.1 Controller 日志格式

```json
{
  // ========== 公共字段 ==========
  "@timestamp": "2024-01-15T14:30:25.123+08:00",
  "traceId": "a1b2c3d4e5f6g7h8",
  "spanId": "span-001",
  "logType": "CONTROLLER",
  "level": "INFO",
  "app_name": "huochai-auth",
  "app_version": "1.0.0",
  "env": "production",
  "host": "192.168.1.100",
  "port": 8080,
  "logger": "c.h.auth.controller.UserController",
  "thread": "http-nio-8080-exec-1",
  "message": "HTTP请求处理",
  
  // ========== 用户上下文 ==========
  "userId": 1,
  "username": "admin",
  "clientType": "WEB",
  "deviceId": "device-123",
  "ip": "192.168.1.100",
  
  // ========== 执行结果 ==========
  "status": 1,
  "success": true,
  "costTime": 125,
  "errorMsg": null,
  
  // ========== Controller 扩展字段 ==========
  "http": {
    "method": "POST",
    "url": "/api/admin/users",
    "path": "/api/admin/users",
    "queryString": null,
    "headers": {
      "Content-Type": "application/json",
      "User-Agent": "Mozilla/5.0..."
    },
    "requestBody": "{"username":"test","email":"test@example.com"}",
    "responseStatus": 200,
    "responseBody": "{"code":200,"data":{"id":1}}"
  }
}
```

##### 6.2 Dubbo RPC 日志格式

```json
{
  // ========== 公共字段 ==========
  "@timestamp": "2024-01-15T14:30:25.123+08:00",
  "traceId": "a1b2c3d4e5f6g7h8",
  "spanId": "span-002",
  "parentSpanId": "span-001",
  "logType": "DUBBO",
  "level": "INFO",
  "app_name": "huochai-auth",
  "app_version": "1.0.0",
  "env": "production",
  "host": "192.168.1.100",
  "port": 8080,
  "logger": "c.h.auth.service.UserServiceImpl",
  "thread": "dubbo-protocol-handler-1",
  "message": "Dubbo RPC调用",
  
  // ========== 执行结果 ==========
  "status": 1,
  "success": true,
  "costTime": 15,
  "errorMsg": null,
  
  // ========== Dubbo 扩展字段 ==========
  "dubbo": {
    "direction": "CONSUMER",
    "serviceName": "userService",
    "group": "huochai-auth",
    "version": "1.0.0",
    "methodName": "getUserById",
    "parameterTypes": ["java.lang.Long"],
    "arguments": [1],
    "result": "{"id":1,"username":"admin"}",
    "remoteAddress": "192.168.1.101:20880",
    "localAddress": "192.168.1.100:54321",
    "costTime": 15,
    "success": true
  },
  
  "status": 1
}
```

##### 5.3 MQ 消息消费日志格式

```json
{
  "@timestamp": "2024-01-15T14:30:25.123+08:00",
  "traceId": "a1b2c3d4e5f6g7h8",
  "spanId": "span-003",
  "logType": "MQ_CONSUMER",
  "level": "INFO",
  "app_name": "huochai-auth",
  "logger": "c.h.auth.mq.UserMessageListener",
  
  "mq": {
    "type": "RABBITMQ",
    "exchange": "user.exchange",
    "queue": "user.queue",
    "routingKey": "user.created",
    "messageId": "msg-uuid-12345",
    "consumerTag": "consumer-1",
    "payload": "{\"userId\":1,\"action\":\"created\"}",
    "payloadType": "JSON",
    "retryCount": 0,
    "costTime": 50,
    "success": true,
    "ack": true
  },
  
  "status": 1
}
```

##### 5.4 MQ 消息生产日志格式

```json
{
  "@timestamp": "2024-01-15T14:30:25.123+08:00",
  "traceId": "a1b2c3d4e5f6g7h8",
  "spanId": "span-004",
  "logType": "MQ_PRODUCER",
  "level": "INFO",
  "app_name": "huochai-auth",
  "logger": "c.h.auth.service.UserService",
  
  "mq": {
    "type": "RABBITMQ",
    "exchange": "user.exchange",
    "routingKey": "user.created",
    "messageId": "msg-uuid-12345",
    "payload": "{\"userId\":1,\"action\":\"created\"}",
    "payloadType": "JSON",
    "confirm": true,
    "success": true
  },
  
  "status": 1
}
```

##### 5.5 定时任务日志格式

```json
{
  "@timestamp": "2024-01-15T14:30:25.123+08:00",
  "traceId": "a1b2c3d4e5f6g7h8",
  "logType": "SCHEDULED",
  "level": "INFO",
  "app_name": "huochai-auth",
  "logger": "c.h.auth.task.TokenCleanupTask",
  
  "scheduled": {
    "taskName": "tokenCleanupTask",
    "taskGroup": "auth",
    "cronExpression": "0 0 2 * * ?",
    "triggerType": "CRON",
    "startTime": "2024-01-15T02:00:00",
    "endTime": "2024-01-15T02:00:05",
    "costTime": 5000,
    "status": "SUCCESS",
    "processedCount": 100,
    "errorMessage": null
  },
  
  "status": 1
}
```

##### 5.6 MySQL 数据库操作日志格式

```json
{
  "@timestamp": "2024-01-15T14:30:25.123+08:00",
  "traceId": "a1b2c3d4e5f6g7h8",
  "spanId": "span-006",
  "logType": "MYSQL",
  "level": "DEBUG",
  "app_name": "huochai-auth",
  "logger": "c.h.auth.mapper.UserMapper",
  
  "mysql": {
    "dataSource": "master",
    "database": "huochai_auth",
    "table": "sys_user",
    "operation": "SELECT",
    "sql": "SELECT id, username, password, status FROM sys_user WHERE username = ?",
    "sqlHash": "a1b2c3d4",
    "parameters": ["admin"],
    "parameterTypes": ["java.lang.String"],
    "affectedRows": 1,
    "resultSetSize": 1,
    "costTime": 5,
    "connectionId": "conn-123",
    "transactionId": null,
    "success": true
  },
  
  "slowQuery": false,
  "status": 1
}
```

**MySQL 慢查询日志（超过阈值）：**
```json
{
  "@timestamp": "2024-01-15T14:30:25.123+08:00",
  "traceId": "a1b2c3d4e5f6g7h8",
  "logType": "MYSQL",
  "level": "WARN",
  
  "mysql": {
    "sql": "SELECT * FROM sys_operation_log WHERE created_at > ? ORDER BY id DESC LIMIT 1000",
    "costTime": 1500,
    "slowQuery": true,
    "slowThreshold": 1000
  },
  
  "status": 1
}
```

##### 5.7 Redis 缓存操作日志格式

```json
{
  "@timestamp": "2024-01-15T14:30:25.123+08:00",
  "traceId": "a1b2c3d4e5f6g7h8",
  "spanId": "span-007",
  "logType": "REDIS",
  "level": "DEBUG",
  "app_name": "huochai-auth",
  "logger": "c.h.auth.cache.TokenCacheService",
  
  "redis": {
    "command": "SET",
    "key": "auth:token:user:1:WEB",
    "keyPattern": "auth:token:user:*:*",
    "value": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "valueSize": 256,
    "ttl": 7200,
    "ttlUnit": "SECONDS",
    "nx": false,
    "xx": false,
    "costTime": 2,
    "node": "redis-master:6379",
    "dbIndex": 0,
    "success": true
  },
  
  "cacheHit": null,
  "status": 1
}
```

**Redis 缓存命中/未命中日志：**
```json
{
  "@timestamp": "2024-01-15T14:30:25.123+08:00",
  "traceId": "a1b2c3d4e5f6g7h8",
  "logType": "REDIS",
  "level": "DEBUG",
  
  "redis": {
    "command": "GET",
    "key": "auth:permissions:1",
    "costTime": 1,
    "success": true
  },
  
  "cacheHit": true,
  "status": 1
}
```

**Redis 常用命令日志字段：**

| 命令 | 特殊字段 | 说明 |
|-----|---------|------|
| GET/SET | value, ttl, valueSize | 字符串操作 |
| HGET/HSET | field, value | Hash 操作 |
| LPUSH/RPUSH | listSize | List 操作 |
| SADD/SREM | setSize | Set 操作 |
| ZADD/ZREM | score, setSize | ZSet 操作 |
| DEL | deletedCount | 删除操作 |
| EXPIRE/TTL | ttl | 过期时间 |
| KEYS/SCAN | pattern, count | 模糊查询 |
| MULTI/EXEC | commands | 事务操作 |

##### 5.8 Elasticsearch 操作日志格式

```json
{
  "@timestamp": "2024-01-15T14:30:25.123+08:00",
  "traceId": "a1b2c3d4e5f6g7h8",
  "spanId": "span-008",
  "logType": "ES",
  "level": "DEBUG",
  "app_name": "huochai-auth",
  "logger": "c.h.auth.search.UserSearchService",
  
  "elasticsearch": {
    "operation": "SEARCH",
    "index": "operation-log-2024-01",
    "indices": ["operation-log-2024-01"],
    "type": "_doc",
    "documentId": null,
    "routing": null,
    
    "query": {
      "bool": {
        "must": [
          {"term": {"userId": 1}},
          {"range": {"@timestamp": {"gte": "2024-01-01"}}}
        ]
      }
    },
    "querySize": 20,
    "from": 0,
    "size": 20,
    "sort": ["@timestamp:desc"],
    
    "hits": 100,
    "took": 15,
    "timedOut": false,
    "shards": {
      "total": 5,
      "successful": 5,
      "failed": 0
    },
    
    "costTime": 20,
    "success": true
  },
  
  "status": 1
}
```

**ES 索引操作日志：**
```json
{
  "@timestamp": "2024-01-15T14:30:25.123+08:00",
  "traceId": "a1b2c3d4e5f6g7h8",
  "logType": "ES",
  "level": "DEBUG",
  
  "elasticsearch": {
    "operation": "INDEX",
    "index": "operation-log-2024-01",
    "documentId": "doc-uuid-123",
    "source": "{"traceId":"a1b2c3d4e5f6g7h8","userId":1}",
    "version": 1,
    "result": "CREATED",
    "costTime": 5,
    "success": true
  },
  
  "status": 1
}
```

**ES 批量操作日志：**
```json
{
  "@timestamp": "2024-01-15T14:30:25.123+08:00",
  "traceId": "a1b2c3d4e5f6g7h8",
  "logType": "ES",
  "level": "DEBUG",
  
  "elasticsearch": {
    "operation": "BULK",
    "index": "operation-log-2024-01",
    "batchSize": 100,
    "successCount": 100,
    "failedCount": 0,
    "costTime": 50,
    "success": true
  },
  
  "status": 1
}
```

**ES 操作类型说明：**

| 操作类型 | 说明 | 关键字段 |
|---------|------|---------|
| SEARCH | 查询 | query, hits, took |
| INDEX | 索引文档 | documentId, source, version |
| UPDATE | 更新文档 | documentId, script |
| DELETE | 删除文档 | documentId |
| BULK | 批量操作 | batchSize, successCount |
| MGET | 批量获取 | ids, docs |
| SCROLL | 滚动查询 | scrollId, scroll |
| BULK | 批量操作 | batchSize |

##### 5.9 异常日志格式

```json
{
  "@timestamp": "2024-01-15T14:30:25.123+08:00",
  "traceId": "a1b2c3d4e5f6g7h8",
  "spanId": "span-005",
  "logType": "EXCEPTION",
  "level": "ERROR",
  "app_name": "huochai-auth",
  "logger": "c.h.auth.service.UserService",
  
  "exception": {
    "type": "BusinessException",
    "className": "com.huochai.common.exception.BusinessException",
    "message": "用户名已存在",
    "errorCode": "USER_001",
    "stackTrace": "com.huochai.common.exception.BusinessException: 用户名已存在\n\tat com.huochai.auth.service.UserService.createUser...",
    "cause": null
  },
  
  "context": {
    "userId": 1,
    "username": "admin",
    "operation": "创建用户"
  },
  
  "status": 0
}
```

##### 5.7 业务自定义日志格式

```json
{
  "@timestamp": "2024-01-15T14:30:25.123+08:00",
  "traceId": "a1b2c3d4e5f6g7h8",
  "logType": "BUSINESS",
  "level": "INFO",
  "app_name": "huochai-auth",
  "logger": "c.h.auth.service.AuthService",
  
  "business": {
    "code": "LOGIN_SUCCESS",
    "module": "认证模块",
    "action": "用户登录",
    "data": {
      "loginType": "PASSWORD",
      "clientType": "WEB",
      "deviceId": "device-123"
    },
    "result": "SUCCESS"
  },
  
  "userId": 1,
  "username": "admin",
  "status": 1
}
```

---

#### 7. 日志记录方式（零侵入自动拦截）

**设计原则：引入依赖后自动生效，业务代码无需任何修改。**

##### 7.1 自动拦截机制

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           自动拦截机制（零侵入）                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   ┌──────────────────────────────────────────────────────────────────┐     │
│   │                      业务代码（无需修改）                          │     │
│   │                                                                  │     │
│   │   @RestController          @Service          @Mapper             │     │
│   │   public class UserController {              UserMapper {       │     │
│   │       // 无需添加任何注解        // 正常写业务逻辑    // 正常写SQL     │     │
│   │   }                          }                }                  │     │
│   └──────────────────────────────────────────────────────────────────┘     │
│                                    │                                        │
│                                    ▼                                        │
│   ┌──────────────────────────────────────────────────────────────────┐     │
│   │                      自动拦截层（透明）                            │     │
│   ├──────────────────────────────────────────────────────────────────┤     │
│   │                                                                  │     │
│   │   ControllerInterceptor    ServiceInterceptor    DubboFilter    │     │
│   │        (AOP)                   (AOP)             (SPI扩展点)     │     │
│   │                                                                  │     │
│   │   MyBatisInterceptor       RedisInterceptor     ESInterceptor  │     │
│   │      (插件)                (Template包装)       (Client包装)    │     │
│   │                                                                  │     │
│   │   MqConsumerInterceptor    ScheduledInterceptor               │     │
│   │      (容器工厂)               (AOP)                           │     │
│   └──────────────────────────────────────────────────────────────────┘     │
│                                    │                                        │
│                                    ▼                                        │
│   ┌──────────────────────────────────────────────────────────────────┐     │
│   │                      日志收集（统一入口）                          │     │
│   │                                                                  │     │
│   │   Disruptor 队列 → JSON 日志文件 → Filebeat → Logstash → ES     │     │
│   └──────────────────────────────────────────────────────────────────┘     │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

##### 7.2 Controller 日志（自动拦截）

```java
// 业务代码：无需添加任何注解，日志自动记录
@RestController
public class UserController {

    @PostMapping("/users")
    public Result<User> createUser(@RequestBody User user) {
        return Result.success(userService.createUser(user));
        // 日志框架自动记录：
        // - HTTP 方法、URL、请求参数、响应结果
        // - 执行时间、用户信息、客户端IP
    }
}
```

**拦截实现：** 通过 Spring AOP 自动拦截所有 `@RequestMapping` 方法

##### 7.3 Service 日志（自动拦截）

```java
// 业务代码：正常写 Service，日志自动记录
@Service
public class UserServiceImpl implements UserService {

    public LoginResult login(LoginRequest request) {
        // 业务逻辑
        // 日志框架自动记录方法入参、出参、执行时间
        return result;
    }
}
```

**拦截实现：** 通过 Spring AOP 自动拦截所有 `@Service` 类的公共方法

##### 7.4 Dubbo RPC 日志（自动拦截）

```java
// 业务代码：正常写 Dubbo 服务，日志自动记录
@DubboService
public class UserServiceImpl implements UserService {

    @Override
    public User getUserById(Long id) {
        // 日志框架自动记录：
        // - 服务名、方法名、参数、结果
        // - 远程地址、调用方向（消费端/提供端）
        return user;
    }
}
```

**拦截实现：** 通过 Dubbo Filter 扩展点自动拦截

##### 7.5 MySQL 数据库日志（自动拦截）

```java
// 业务代码：正常写 MyBatis Mapper，日志自动记录
@Mapper
public interface UserMapper {

    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    User findByUsername(String username);
    // 日志框架自动记录：
    // - SQL 语句、参数、执行时间
    // - 影响行数、慢查询标记
}
```

**拦截实现：** 通过 MyBatis Interceptor 插件自动拦截

##### 7.6 Redis 缓存日志（自动拦截）

```java
// 业务代码：正常使用 RedisTemplate，日志自动记录
@Service
public class TokenService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    public void cacheToken(String key, String token) {
        redisTemplate.opsForValue().set(key, token, 2, TimeUnit.HOURS);
        // 日志框架自动记录：
        // - Redis 命令、Key、TTL、执行时间
        // - 缓存命中/未命中
    }
}
```

**拦截实现：** 通过 RedisTemplate 包装器自动拦截

##### 7.7 Elasticsearch 日志（自动拦截）

```java
// 业务代码：正常使用 ES Client，日志自动记录
@Service
public class LogSearchService {

    @Autowired
    private ElasticsearchTemplate esTemplate;

    public void saveLog(LogEntry entry) {
        esTemplate.save(entry);
        // 日志框架自动记录：
        // - ES 操作类型、索引名、文档ID
        // - 查询条件、执行时间
    }
}
```

**拦截实现：** 通过 Elasticsearch Client 包装器自动拦截

##### 7.8 MQ 消息日志（自动拦截）

```java
// 业务代码：正常使用 MQ，日志自动记录

// 消费者
@RabbitListener(queues = "user.queue")
public void handleUserMessage(UserMessage message) {
    // 业务处理
    // 日志框架自动记录：
    // - 队列名、消息ID、消息内容
    // - 消费结果、重试次数
}

// 生产者
@Autowired
private RabbitTemplate rabbitTemplate;

public void sendMessage(UserMessage message) {
    rabbitTemplate.convertAndSend("user.exchange", "user.created", message);
    // 日志框架自动记录：
    // - 交换机、路由Key、消息ID
    // - 发送结果、确认状态
}
```

**拦截实现：** 
- 消费者：通过 `RabbitListenerContainerFactory` 包装
- 生产者：通过 `RabbitTemplate` 回调拦截

##### 7.9 定时任务日志（自动拦截）

```java
// 业务代码：正常写定时任务，日志自动记录
@Component
public class TokenCleanupTask {

    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredTokens() {
        // 业务逻辑
        // 日志框架自动记录：
        // - 任务名、执行时间、执行结果
        // - 处理数量、异常信息
    }
}
```

**拦截实现：** 通过 Spring AOP 自动拦截所有 `@Scheduled` 方法

##### 7.10 自定义业务日志（可选）

```java
// 如需记录特殊业务日志，可使用工具类
@Service
public class OrderService {

    public void createOrder(Order order) {
        // 正常业务逻辑...

        // 可选：记录业务自定义日志
        LogHelper.business()
            .code("ORDER_CREATED")
            .module("订单模块")
            .action("创建订单")
            .data("orderId", order.getId())
            .data("amount", order.getAmount())
            .log();
    }
}
```

---

#### 8. 日志字段说明（解决生产问题）

| 字段 | 说明 | 用途 |
|-----|------|------|
| traceId | 链路追踪ID | 全链路日志关联，排查分布式问题 |
| spanId | 当前 Span ID | 定位具体调用层级 |
| parentSpanId | 父 Span ID | 追溯调用链 |
| logType | 日志类型 | 区分不同场景（CONTROLLER/DUBBO/MQ等） |
| level | 日志级别 | INFO/WARN/ERROR |
| userId/username | 操作用户 | 安全审计，问题定位 |
| costTime | 耗时 | 性能问题排查 |
| status | 状态 | 1成功/0失败 |
| ip | 客户端IP | 安全审计 |
| exception | 异常信息 | 异常问题定位 |

### 链路追踪架构

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  HTTP 请求   │────▶│  TraceFilter │────▶│  Micrometer  │
└──────────────┘     └──────────────┘     └──────────────┘
                           │                     │
                           ▼                     ▼
                    ┌──────────────┐     ┌──────────────┐
                    │  Feign 调用  │     │  Zipkin      │
                    │  自动传播    │     │  后端存储    │
                    └──────────────┘     └──────────────┘
                           │
                           ▼
                    ┌──────────────┐
                    │  RabbitMQ    │
                    │  Trace传播   │
                    └──────────────┘
```

**关键设计：**
1. 使用 Micrometer Tracing + Brave（Zipkin适配）
2. HTTP：通过 TraceFilter 自动创建/传播 TraceId
3. Feign：自动注入 TraceHeader
4. RabbitMQ：通过 MessageProperties 传递 TraceContext
5. 支持 W3C TraceContext 标准

---

## 涉及文件

### 新增模块：huochai-log-starter

| 文件路径 | 说明 |
|---------|------|
| `huochai-log-starter/pom.xml` | 模块 Maven 配置 |
| `huochai-log-starter/src/main/java/com/huochai/log/autoconfigure/LogAutoConfiguration.java` | 自动配置类 |
| `huochai-log-starter/src/main/java/com/huochai/log/autoconfigure/LogProperties.java` | 配置属性类 |
| `huochai-log-starter/src/main/java/com/huochai/log/enums/LogType.java` | 日志类型枚举 |
| `huochai-log-starter/src/main/java/com/huochai/log/collector/LogCollector.java` | 日志收集接口 |
| `huochai-log-starter/src/main/java/com/huochai/log/collector/LogCollectorImpl.java` | 日志收集实现 |
| `huochai-log-starter/src/main/java/com/huochai/log/interceptor/ControllerInterceptor.java` | Controller 日志拦截器（AOP） |
| `huochai-log-starter/src/main/java/com/huochai/log/interceptor/ServiceInterceptor.java` | Service 日志拦截器（AOP） |
| `huochai-log-starter/src/main/java/com/huochai/log/interceptor/DubboLogFilter.java` | Dubbo 日志过滤器（SPI） |
| `huochai-log-starter/src/main/java/com/huochai/log/interceptor/MySqlLogInterceptor.java` | MySQL 日志拦截器（MyBatis插件） |
| `huochai-log-starter/src/main/java/com/huochai/log/interceptor/RedisLogInterceptor.java` | Redis 日志拦截器（Template包装） |
| `huochai-log-starter/src/main/java/com/huochai/log/interceptor/ElasticsearchLogInterceptor.java` | ES 日志拦截器（Client包装） |
| `huochai-log-starter/src/main/java/com/huochai/log/interceptor/MqConsumerInterceptor.java` | MQ 消费日志拦截器（容器工厂） |
| `huochai-log-starter/src/main/java/com/huochai/log/interceptor/MqProducerInterceptor.java` | MQ 生产日志拦截器（Template回调） |
| `huochai-log-starter/src/main/java/com/huochai/log/interceptor/ScheduledInterceptor.java` | 定时任务日志拦截器（AOP） |
| `huochai-log-starter/src/main/java/com/huochai/log/helper/LogHelper.java` | 日志工具类（可选使用） |
| `huochai-log-starter/src/main/java/com/huochai/log/helper/LogContext.java` | 日志上下文 |
| `huochai-log-starter/src/main/java/com/huochai/log/helper/BusinessLogBuilder.java` | 业务日志构建器 |
| `huochai-log-starter/src/main/java/com/huochai/log/helper/ExceptionLogBuilder.java` | 异常日志构建器 |
| `huochai-log-starter/src/main/java/com/huochai/log/disruptor/LogDisruptorConfig.java` | Disruptor 配置 |
| `huochai-log-starter/src/main/java/com/huochai/log/disruptor/LogEventFactory.java` | 事件工厂 |
| `huochai-log-starter/src/main/java/com/huochai/log/disruptor/LogEventTranslator.java` | 事件翻译器 |
| `huochai-log-starter/src/main/java/com/huochai/log/disruptor/LogEventHandler.java` | 事件处理器 |
| `huochai-log-starter/src/main/java/com/huochai/log/disruptor/LogEventData.java` | 事件数据 |
| `huochai-log-starter/src/main/java/com/huochai/log/writer/JsonLogWriter.java` | JSON 日志写入器 |
| `huochai-log-starter/src/main/java/com/huochai/log/model/LogEntry.java` | 日志实体（基类，统一字段） |
| `huochai-log-starter/src/main/java/com/huochai/log/model/ControllerLogEntry.java` | Controller 日志实体 |
| `huochai-log-starter/src/main/java/com/huochai/log/model/DubboLogEntry.java` | Dubbo 日志实体 |
| `huochai-log-starter/src/main/java/com/huochai/log/model/MqLogEntry.java` | MQ 日志实体 |
| `huochai-log-starter/src/main/java/com/huochai/log/model/ScheduledLogEntry.java` | 定时任务日志实体 |
| `huochai-log-starter/src/main/java/com/huochai/log/model/MySqlLogEntry.java` | MySQL 日志实体 |
| `huochai-log-starter/src/main/java/com/huochai/log/model/RedisLogEntry.java` | Redis 日志实体 |
| `huochai-log-starter/src/main/java/com/huochai/log/model/ElasticsearchLogEntry.java` | ES 日志实体 |
| `huochai-log-starter/src/main/java/com/huochai/log/model/ExceptionLogEntry.java` | 异常日志实体 |
| `huochai-log-starter/src/main/java/com/huochai/log/model/BusinessLogEntry.java` | 业务日志实体 |
| `huochai-log-starter/src/main/java/com/huochai/log/trace/TraceContextHolder.java` | Trace 上下文 |
| `huochai-log-starter/src/main/java/com/huochai/log/trace/TraceFilter.java` | Trace 过滤器 |
| `huochai-log-starter/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | 自动配置注册 |
| `huochai-log-starter/src/main/resources/logback-spring.xml` | Logback 配置（JSON 输出） |
| `huochai-log-starter/README.md` | 模块说明文档 |

### 修改文件（huochai-core）

| 文件路径 | 说明 |
|---------|------|
| `huochai-core/pom.xml` | 添加 huochai-log-starter 依赖（引入即生效，无需其他修改） |
| `huochai-core/src/main/resources/application.properties` | 添加日志配置项（可选） |
| `huochai-core/README.md` | 更新日志模块文档 |

## 实现细节

### 1. huochai-log-starter pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.huochai</groupId>
        <artifactId>huochai-parent</artifactId>
        <version>0.0.1-SNAPSHOT</version>
    </parent>
    
    <artifactId>huochai-log-starter</artifactId>
    <packaging>jar</packaging>
    <name>huochai-log-starter</name>
    <description>高性能日志收集模块 - 标准 ELK 架构，支持 JSON 日志输出、链路追踪</description>
    
    <dependencies>
        <!-- Spring Boot Starter -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- Disruptor 高性能队列 -->
        <dependency>
            <groupId>com.lmax</groupId>
            <artifactId>disruptor</artifactId>
            <version>3.4.4</version>
        </dependency>
        
        <!-- Micrometer Tracing（链路追踪） -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-tracing-bridge-brave</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- Hutool 工具库 -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>5.8.26</version>
        </dependency>
        
        <!-- Logstash Logback Encoder（JSON 日志输出） -->
        <dependency>
            <groupId>net.logstash.logback</groupId>
            <artifactId>logstash-logback-encoder</artifactId>
            <version>7.4</version>
        </dependency>
        
        <!-- Configuration Processor -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

### 2. 核心注解定义

```java
/**
 * 开启日志收集功能
 * 在启动类上添加此注解即可启用日志收集模块
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(LogAutoConfiguration.class)
@Documented
public @interface EnableLogCollector {
}
```

### 3. 自动配置类

```java
/**
 * 日志模块自动配置
 * 条件装配，按需加载各组件
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(LogProperties.class)
@ConditionalOnProperty(prefix = "huochai.log", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LogCollector logCollector(Disruptor<LogEventData> disruptor, LogEventTranslator translator) {
        log.info("初始化日志收集器");
        return new LogCollectorImpl(disruptor, translator);
    }

    @Bean
    @ConditionalOnProperty(prefix = "huochai.log.disruptor", name = "enabled", havingValue = "true", matchIfMissing = true)
    public Disruptor<LogEventData> logDisruptor(LogProperties properties, LogEventHandler handler) {
        LogProperties.DisruptorConfig config = properties.getDisruptor();
        
        Disruptor<LogEventData> disruptor = new Disruptor<>(
            new LogEventFactory(),
            config.getBufferSize(),
            DaemonThreadFactory.INSTANCE,
            ProducerType.MULTI,
            new BlockingWaitStrategy()
        );
        
        disruptor.handleEventsWith(handler);
        disruptor.start();
        
        log.info("Disruptor 日志队列初始化完成, bufferSize={}", config.getBufferSize());
        return disruptor;
    }

    @Bean
    @ConditionalOnClass(WebMvcConfigurer.class)
    public FilterRegistrationBean<TraceFilter> traceFilterRegistration(TraceFilter traceFilter) {
        FilterRegistrationBean<TraceFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(traceFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        registration.setName("traceFilter");
        return registration;
    }

    @Bean
    @ConditionalOnClass(RabbitTemplate.class)
    @ConditionalOnProperty(prefix = "huochai.log.rabbitmq", name = "enabled", havingValue = "true", matchIfMissing = true)
    public LogMessageProducer logMessageProducer(RabbitTemplate rabbitTemplate, LogProperties properties) {
        return new LogMessageProducer(rabbitTemplate, properties);
    }
}
```

### 4. 配置属性类

```java
/**
 * 日志模块配置属性
 */
@Data
@ConfigurationProperties(prefix = "huochai.log")
public class LogProperties {

    /**
     * 是否启用日志收集
     */
    private boolean enabled = true;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * Disruptor 配置
     */
    private DisruptorConfig disruptor = new DisruptorConfig();

    /**
     * 文件配置
     */
    private FileConfig file = new FileConfig();

    /**
     * RabbitMQ 配置
     */
    private RabbitmqConfig rabbitmq = new RabbitmqConfig();

    /**
     * ES 配置
     */
    private EsConfig elasticsearch = new EsConfig();

    @Data
    public static class DisruptorConfig {
        private boolean enabled = true;
        private int bufferSize = 65536;
        private int batchSize = 100;
        private long batchTimeoutMs = 100;
    }

    @Data
    public static class FileConfig {
        private boolean enabled = true;
        private String dir = "logs";
        private int maxSizeMb = 100;
        private int maxHistoryDays = 30;
        private boolean accessLogEnabled = true;
        private boolean errorLogEnabled = true;
    }

    @Data
    public static class RabbitmqConfig {
        private boolean enabled = true;
        private String exchange = "log.exchange";
        private int shardCount = 8;
    }

    @Data
    public static class EsConfig {
        private boolean enabled = true;
        private String indexPrefix = "operation-log";
    }
}
```
@Slf4j
@Configuration
public class LogDisruptorConfig {

    /**
     * RingBuffer 大小，必须是 2 的幂次方
     */
    private static final int BUFFER_SIZE = 1024 * 64;  // 65536

    /**
     * 批量发送阈值
     */
    private static final int BATCH_SIZE = 100;

    /**
     * 批量发送最大等待时间（毫秒）
     */
    private static final long BATCH_TIMEOUT_MS = 100;

    @Bean(destroyMethod = "shutdown")
    public Disruptor<LogEventData> logDisruptor(
            LogEventFactory factory,
            LogEventHandler handler) {
        
        // 创建 Disruptor
        Disruptor<LogEventData> disruptor = new Disruptor<>(
            factory,
            BUFFER_SIZE,
            DaemonThreadFactory.INSTANCE,
            ProducerType.MULTI,           // 多生产者模式
            new BlockingWaitStrategy()    // 阻塞等待策略，平衡性能和CPU
        );

        // 设置单消费者处理（保证顺序）
        disruptor.handleEventsWith(handler);

        // 启动 Disruptor
        disruptor.start();

        log.info("Disruptor 日志队列初始化完成, bufferSize={}", BUFFER_SIZE);
        return disruptor;
    }
}

/**
 * 日志事件处理器 - 批量聚合发送 + 本地文件双写
 */
@Slf4j
@Component
public class LogEventHandler implements EventHandler<LogEventData> {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private LocalLogFallback localFallback;
    
    @Autowired
    private LogFileWriter logFileWriter;

    /**
     * 批量缓冲区
     */
    private final List<LogEventData> batch = new ArrayList<>(BATCH_SIZE);

    /**
     * 上次发送时间
     */
    private long lastSendTime = System.currentTimeMillis();

    @Override
    public void onEvent(LogEventData event, long sequence, boolean endOfBatch) {
        try {
            // 1. 首先写入本地文件（同步，确保不丢失）
            logFileWriter.writeOperationLog(event);
            
            // 2. 添加到批量缓冲区（异步发送到 MQ）
            batch.add(event.copy());

            // 判断是否需要发送：达到批量大小 或 超时
            boolean shouldSend = batch.size() >= BATCH_SIZE 
                    || (System.currentTimeMillis() - lastSendTime) >= BATCH_TIMEOUT_MS;

            if (shouldSend) {
                sendBatch();
            }
        } catch (Exception e) {
            log.error("处理日志事件失败", e);
            // 降级到本地存储
            localFallback.save(event);
        }
    }

    /**
     * 批量发送到 RabbitMQ
     */
    private void sendBatch() {
        if (batch.isEmpty()) {
            return;
        }

        try {
            // 按分片分组发送
            Map<Integer, List<LogEventData>> shardGroups = batch.stream()
                    .collect(Collectors.groupingBy(this::calculateShard));

            for (Map.Entry<Integer, List<LogEventData>> entry : shardGroups.entrySet()) {
                String routingKey = "log.operation.shard." + entry.getKey();
                String json = JSONUtil.toJsonStr(entry.getValue());
                rabbitTemplate.convertAndSend("log.exchange", routingKey, json);
            }

            log.debug("批量发送日志成功, count={}", batch.size());
        } catch (Exception e) {
            log.error("发送日志到 RabbitMQ 失败", e);
            // 降级：所有消息写入本地
            batch.forEach(localFallback::save);
        } finally {
            batch.clear();
            lastSendTime = System.currentTimeMillis();
        }
    }

    /**
     * 计算分片：根据 traceId 哈希
     */
    private int calculateShard(LogEventData event) {
        if (StrUtil.isBlank(event.getTraceId())) {
            return 0;
        }
        return Math.abs(event.getTraceId().hashCode()) % 8;  // 8个分片
    }
}

/**
 * 日志事件数据
 */
@Data
public class LogEventData {
    private String traceId;
    private String spanId;
    private Long userId;
    private String username;
    private String module;
    private String operationType;
    private String operationDesc;
    private String requestMethod;
    private String requestUrl;
    private String requestParams;
    private String responseResult;
    private String ip;
    private Integer status;
    private String errorMsg;
    private Long costTime;
    private LocalDateTime createdAt;

    /**
     * 复制数据（因为 Disruptor 复用对象）
     */
    public LogEventData copy() {
        LogEventData copy = new LogEventData();
        BeanUtil.copyProperties(this, copy);
        return copy;
    }
}
```

### 3. Logback JSON 日志配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration scan="true" scanPeriod="30 seconds">
    
    <!-- 定义变量 -->
    <property name="APP_NAME" value="huochai-auth"/>
    <property name="LOG_PATH" value="logs"/>
    
    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{traceId}] [%thread] %-5level %logger{36} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- JSON 格式日志 - 供 Filebeat 采集 -->
    <appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/json/operation.log</file>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <!-- 自定义字段 -->
            <customFields>{"app_name":"${APP_NAME}","log_type":"operation"}</customFields>
            
            <!-- MDC 字段 -->
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>spanId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
            <includeMdcKeyName>username</includeMdcKeyName>
            <includeMdcKeyName>clientType</includeMdcKeyName>
            <includeMdcKeyName>deviceId</includeMdcKeyName>
            
            <!-- 字段名称映射 -->
            <fieldNames>
                <timestamp>@timestamp</timestamp>
                <level>level</level>
                <logger>logger</logger>
                <thread>thread</thread>
                <message>message</message>
                <stackTrace>stack_trace</stackTrace>
            </fieldNames>
            
            <!-- 异常堆栈处理 -->
            <throwableConverter class="net.logstash.logback.stacktrace.ShortenedThrowableConverter">
                <maxDepthPerThrowable>30</maxDepthPerThrowable>
                <maxLength>2048</maxLength>
                <shortenedClassNameLength>20</shortenedClassNameLength>
                <rootCauseFirst>true</rootCauseFirst>
            </throwableConverter>
        </encoder>
        
        <!-- 滚动策略 -->
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/json/operation-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>50GB</totalSizeCap>
        </rollingPolicy>
    </appender>

    <!-- JSON 错误日志 -->
    <appender name="JSON_ERROR_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/json/error.log</file>
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>ERROR</level>
        </filter>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"app_name":"${APP_NAME}","log_type":"error"}</customFields>
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/json/error-%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>

    <!-- 操作日志专用 Logger -->
    <logger name="OPERATION_LOG" level="INFO" additivity="false">
        <appender-ref ref="JSON_FILE"/>
        <appender-ref ref="JSON_ERROR_FILE"/>
    </logger>

    <!-- 根日志配置 -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="JSON_FILE"/>
    </root>

</configuration>
```

    /**
     * RabbitMQ 发送确认回调
     */
    @Bean
    public RabbitTemplate.ConfirmCallback logConfirmCallback() {
        return (correlationData, ack, cause) -> {
            if (!ack) {
                log.error("日志消息发送失败: {}", cause);
                // 可以在这里实现重试逻辑
            }
        };
    }
}
```

### 4. Trace 核心实现

```java
/**
 * Trace 上下文工具类
 * 用于在非 Spring 管理的代码中获取 TraceId
 */
@Slf4j
public class TraceContextHolder {
    
    private static final ThreadLocal<String> TRACE_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> SPAN_ID = new ThreadLocal<>();
    
    public static void setTraceId(String traceId) {
        TRACE_ID.set(traceId);
        MDC.put("traceId", traceId);
    }
    
    public static String getTraceId() {
        String traceId = TRACE_ID.get();
        if (StrUtil.isBlank(traceId)) {
            traceId = MDC.get("traceId");
        }
        return traceId;
    }
    
    public static void clear() {
        TRACE_ID.remove();
        SPAN_ID.remove();
        MDC.clear();
    }
}
```

### 5. Logback JSON 配置（logback-spring.xml）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration scan="true" scanPeriod="30 seconds">
    
    <!-- 定义变量 -->
    <property name="APP_NAME" value="${APP_NAME:-huochai-app}"/>
    <property name="LOG_PATH" value="${LOG_PATH:-logs}"/>
    
    <!-- JSON 格式日志输出（供 Filebeat 采集） -->
    <appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/json/operation.log</file>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"app_name":"${APP_NAME}","log_type":"operation"}</customFields>
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>spanId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
            <includeMdcKeyName>username</includeMdcKeyName>
            <includeMdcKeyName>clientType</includeMdcKeyName>
            <fieldNames>
                <timestamp>@timestamp</timestamp>
                <level>level</level>
                <logger>logger</logger>
                <thread>thread</thread>
                <message>message</message>
                <stackTrace>stack_trace</stackTrace>
            </fieldNames>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/json/operation-%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>7</maxHistory>
        </rollingPolicy>
    </appender>

    <!-- 错误日志 JSON 输出 -->
    <appender name="ERROR_JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/json/error.log</file>
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>ERROR</level>
        </filter>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"app_name":"${APP_NAME}","log_type":"error"}</customFields>
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
            <fieldNames>
                <timestamp>@timestamp</timestamp>
                <level>level</level>
                <message>message</message>
                <stackTrace>stack_trace</stackTrace>
            </fieldNames>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/json/error-%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
    </appender>

    <!-- 控制台输出（开发环境） -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{traceId}] [%thread] %-5level %logger{36} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- 操作日志 Logger -->
    <logger name="OPERATION_LOG" level="INFO" additivity="false">
        <appender-ref ref="JSON_FILE"/>
        <appender-ref ref="ERROR_JSON_FILE"/>
    </logger>

    <!-- 根日志配置 -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="JSON_FILE"/>
        <appender-ref ref="ERROR_JSON_FILE"/>
    </root>

</configuration>
```

### 6. 本地日志文件写入器（JSON 格式）

```java
/**
 * 本地日志文件写入器
 * 实现日志实时双写到本地文件，确保生产问题可追溯
 */
@Slf4j
@Component
public class LogFileWriter {

    private static final String LOG_DIR = "logs";
    private static final String OPERATION_LOG_FILE = "operation-trace.log";
    private static final String ERROR_LOG_FILE = "operation-trace-error.log";
    private static final String ACCESS_LOG_FILE = "operation-trace-access.log";
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Value("${log.file.enabled:true}")
    private boolean fileLogEnabled;

    @Value("${log.file.max-size-mb:100}")
    private int maxSizeMb;

    @Value("${log.file.max-history-days:30}")
    private int maxHistoryDays;

    /**
     * 写入操作日志（主入口）
     */
    public void writeOperationLog(LogEventData event) {
        if (!fileLogEnabled) {
            return;
        }

        try {
            String logLine = formatOperationLog(event);
            
            // 写入主日志文件
            writeToFile(getLogFile(OPERATION_LOG_FILE), logLine);
            
            // 如果是错误日志，额外写入错误日志文件
            if (event.getStatus() != null && event.getStatus() == 0) {
                String errorLine = formatErrorLog(event);
                writeToFile(getLogFile(ERROR_LOG_FILE), errorLine);
            }
            
        } catch (Exception e) {
            log.error("写入操作日志文件失败", e);
        }
    }

    /**
     * 写入访问日志（请求入口）
     */
    public void writeAccessLog(LogEventData event) {
        if (!fileLogEnabled) {
            return;
        }

        try {
            String logLine = formatAccessLog(event);
            writeToFile(getLogFile(ACCESS_LOG_FILE), logLine);
        } catch (Exception e) {
            log.error("写入访问日志文件失败", e);
        }
    }

    /**
     * 格式化操作日志（生产友好格式）
     */
    private String formatOperationLog(LogEventData event) {
        StringBuilder sb = new StringBuilder();
        
        // 时间戳 + traceId + 线程 + 日志级别
        sb.append(LocalDateTime.now().format(TIME_FORMATTER));
        sb.append(" [").append(event.getTraceId()).append("] ");
        sb.append("[").append(Thread.currentThread().getName()).append("] ");
        sb.append(event.getStatus() == 1 ? "INFO " : "ERROR");
        sb.append(" c.h.c.l.LogFileWriter - \n");
        
        // 详细信息（便于排查问题）
        sb.append("  | TRACE_ID: ").append(event.getTraceId()).append("\n");
        sb.append("  | USER: ").append(event.getUsername())
          .append("(userId=").append(event.getUserId()).append(")\n");
        sb.append("  | REQUEST: ").append(event.getRequestMethod())
          .append(" ").append(event.getRequestUrl()).append("\n");
        sb.append("  | PARAMS: ").append(truncate(event.getRequestParams(), 500)).append("\n");
        
        if (event.getStatus() == 1) {
            sb.append("  | RESULT: SUCCESS\n");
            sb.append("  | RESPONSE: ").append(truncate(event.getResponseResult(), 500)).append("\n");
        } else {
            sb.append("  | RESULT: FAILED\n");
            sb.append("  | ERROR: ").append(event.getErrorMsg()).append("\n");
        }
        
        sb.append("  | COST: ").append(event.getCostTime()).append("ms\n");
        sb.append("  | IP: ").append(event.getIp()).append("\n");
        sb.append("  | MODULE: ").append(event.getModule()).append("\n");
        sb.append("  | OPERATION: ").append(event.getOperationDesc()).append("\n");
        
        return sb.toString();
    }

    /**
     * 格式化错误日志（更详细的错误信息）
     */
    private String formatErrorLog(LogEventData event) {
        StringBuilder sb = new StringBuilder();
        sb.append(LocalDateTime.now().format(TIME_FORMATTER));
        sb.append(" [").append(event.getTraceId()).append("] ");
        sb.append("ERROR c.h.c.l.LogFileWriter - \n");
        sb.append("  | TRACE_ID: ").append(event.getTraceId()).append("\n");
        sb.append("  | USER: ").append(event.getUsername()).append("\n");
        sb.append("  | REQUEST: ").append(event.getRequestMethod())
          .append(" ").append(event.getRequestUrl()).append("\n");
        sb.append("  | PARAMS: ").append(event.getRequestParams()).append("\n");
        sb.append("  | ERROR_MSG: ").append(event.getErrorMsg()).append("\n");
        sb.append("  | STACK_TRACE: \n").append(getStackTrace()).append("\n");
        
        return sb.toString();
    }

    /**
     * 格式化访问日志（请求入口记录）
     */
    private String formatAccessLog(LogEventData event) {
        return String.format("%s [%s] ACCESS %s %s %s %sms %s\n",
            LocalDateTime.now().format(TIME_FORMATTER),
            event.getTraceId(),
            event.getRequestMethod(),
            event.getRequestUrl(),
            event.getStatus() == 1 ? "200" : "500",
            event.getCostTime(),
            event.getIp()
        );
    }

    /**
     * 写入文件（自动滚动）
     */
    private synchronized void writeToFile(Path file, String content) throws IOException {
        // 检查文件大小，超过限制则滚动
        if (Files.exists(file) && Files.size(file) > maxSizeMb * 1024 * 1024) {
            rollLogFile(file);
        }
        
        Files.write(file, content.getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * 获取日志文件路径（按日期分目录）
     */
    private Path getLogFile(String fileName) {
        String dateDir = LocalDateTime.now().format(DATE_FORMATTER);
        Path dir = Paths.get(LOG_DIR, dateDir);
        
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                log.error("创建日志目录失败: {}", dir, e);
            }
        }
        
        return dir.resolve(fileName);
    }

    /**
     * 日志文件滚动
     */
    private void rollLogFile(Path file) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
        Path rolledFile = Paths.get(file.toString() + "." + timestamp);
        Files.move(file, rolledFile);
        
        // 清理过期日志
        cleanOldLogs();
    }

    /**
     * 清理过期日志文件
     */
    private void cleanOldLogs() {
        try {
            Path logDir = Paths.get(LOG_DIR);
            if (!Files.exists(logDir)) {
                return;
            }
            
            LocalDate cutoffDate = LocalDate.now().minusDays(maxHistoryDays);
            
            Files.list(logDir)
                .filter(Files::isDirectory)
                .filter(dir -> {
                    try {
                        LocalDate dirDate = LocalDate.parse(dir.getFileName().toString(), DATE_FORMATTER);
                        return dirDate.isBefore(cutoffDate);
                    } catch (Exception e) {
                        return false;
                    }
                })
                .forEach(dir -> {
                    try {
                        Files.walk(dir)
                            .sorted(Comparator.reverseOrder())
                            .forEach(path -> {
                                try {
                                    Files.delete(path);
                                } catch (IOException e) {
                                    log.warn("删除过期日志失败: {}", path);
                                }
                            });
                    } catch (IOException e) {
                        log.warn("清理过期日志目录失败: {}", dir);
                    }
                });
                
        } catch (Exception e) {
            log.error("清理过期日志失败", e);
        }
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int maxLength) {
        if (str == null) {
            return "";
        }
        return str.length() > maxLength ? str.substring(0, maxLength) + "..." : str;
    }

    /**
     * 获取当前线程堆栈（用于错误日志）
     */
    private String getStackTrace() {
        StringBuilder sb = new StringBuilder();
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 3; i < Math.min(stackTrace.length, 15); i++) {
            sb.append("    at ").append(stackTrace[i].toString()).append("\n");
        }
        return sb.toString();
    }
}
```

### 6. 本地降级存储

```java
/**
 * 本地日志降级存储
 * 当 RabbitMQ 不可用时，日志写入本地文件
 */
@Slf4j
@Component
public class LocalLogFallback {

    private static final String FALLBACK_DIR = "logs/fallback";
    private static final String FALLBACK_FILE = "operation-log-fallback.json";
    
    @Autowired
    private RabbitTemplate rabbitTemplate;

    private final AtomicLong fallbackCount = new AtomicLong(0);

    /**
     * 保存日志到本地文件
     */
    public void save(LogEventData eventData) {
        try {
            Path dir = Paths.get(FALLBACK_DIR);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            
            Path file = dir.resolve(FALLBACK_FILE);
            String json = JSONUtil.toJsonStr(eventData) + "\n";
            Files.write(file, json.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            
            long count = fallbackCount.incrementAndGet();
            log.warn("日志降级到本地存储, 累计降级数量: {}", count);
            
        } catch (Exception e) {
            log.error("本地降级存储失败", e);
        }
    }

    /**
     * 定时重试发送降级日志
     * 每分钟执行一次
     */
    @Scheduled(fixedRate = 60000)
    public void retryFallbackLogs() {
        Path file = Paths.get(FALLBACK_DIR, FALLBACK_FILE);
        if (!Files.exists(file)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(file);
            int successCount = 0;
            
            for (String line : lines) {
                try {
                    LogEventData eventData = JSONUtil.toBean(line, LogEventData.class);
                    String routingKey = "log.operation.shard." + 
                            Math.abs(eventData.getTraceId().hashCode()) % 8;
                    rabbitTemplate.convertAndSend("log.exchange", routingKey, line);
                    successCount++;
                } catch (Exception e) {
                    log.error("重试发送日志失败: {}", line, e);
                }
            }

            // 发送成功后清空文件
            if (successCount == lines.size()) {
                Files.delete(file);
                log.info("重试发送降级日志成功, count={}", successCount);
                fallbackCount.set(0);
            }
            
        } catch (Exception e) {
            log.error("重试降级日志失败", e);
        }
    }
}
```

### 7. application.properties 配置

```properties
# ========== 日志收集配置 ==========
# 是否启用日志收集
huochai.log.enabled=true
# 应用名称（用于日志标识）
huochai.log.app-name=huochai-auth

# ========== Disruptor 高性能队列配置 ==========
huochai.log.disruptor.buffer-size=65536
huochai.log.disruptor.batch-size=100
huochai.log.disruptor.batch-timeout-ms=100

# ========== 本地日志文件配置 ==========
# JSON 日志文件（供 Filebeat 采集）
huochai.log.file.json.enabled=true
huochai.log.file.json.dir=logs/json
huochai.log.file.json.max-size-mb=100
huochai.log.file.json.max-history-days=30

# 控制台格式日志文件（人工查看）
huochai.log.file.console.enabled=true
huochai.log.file.console.dir=logs/console

# ========== 链路追踪配置 ==========
# Zipkin 配置
management.tracing.enabled=true
management.tracing.sampling.probability=1.0
management.zipkin.tracing.endpoint=http://localhost:9411/api/v2/spans
```

### 8. Logback 配置（JSON 输出）

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration scan="true" scanPeriod="30 seconds">
    
    <!-- 定义变量 -->
    <property name="APP_NAME" value="${huochai.log.app-name:-huochai-app}"/>
    <property name="LOG_PATH" value="${huochai.log.file.json.dir:-logs/json}"/>
    
    <!-- JSON 格式日志输出 - 供 Filebeat 采集 -->
    <appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/operation.log</file>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"app_name":"${APP_NAME}","log_type":"operation"}</customFields>
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>spanId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
            <includeMdcKeyName>username</includeMdcKeyName>
            <includeMdcKeyName>clientType</includeMdcKeyName>
            <includeMdcKeyName>deviceId</includeMdcKeyName>
            <fieldNames>
                <timestamp>@timestamp</timestamp>
                <level>level</level>
                <logger>logger</logger>
                <thread>thread</thread>
                <message>message</message>
                <stackTrace>stack_trace</stackTrace>
            </fieldNames>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/operation.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>50GB</totalSizeCap>
        </rollingPolicy>
    </appender>

    <!-- 错误日志单独输出 -->
    <appender name="ERROR_JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>${LOG_PATH}/error.log</file>
        <filter class="ch.qos.logback.classic.filter.ThresholdFilter">
            <level>ERROR</level>
        </filter>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"app_name":"${APP_NAME}","log_type":"error"}</customFields>
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>userId</includeMdcKeyName>
        </encoder>
        <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
            <fileNamePattern>${LOG_PATH}/error.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>90</maxHistory>
        </rollingPolicy>
    </appender>

    <!-- 控制台输出 -->
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{traceId}] [%thread] %-5level %logger{36} - %msg%n</pattern>
            <charset>UTF-8</charset>
        </encoder>
    </appender>

    <!-- 操作日志 Logger -->
    <logger name="OPERATION_LOG" level="INFO" additivity="false">
        <appender-ref ref="JSON_FILE"/>
        <appender-ref ref="ERROR_JSON_FILE"/>
    </logger>

    <!-- 根日志配置 -->
    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
    </root>

</configuration>
```

### 9. Filebeat 配置

```yaml
# filebeat.yml
filebeat.inputs:
- type: log
  enabled: true
  paths:
    - /var/log/huochai/json/*.log
  # JSON 解析
  json.keys_under_root: true
  json.add_error_key: true
  json.message_key: message
  # 多行处理
  multiline.pattern: '^\{'
  multiline.negate: true
  multiline.match: after
  # 添加字段
  fields:
    app: huochai-auth
    env: production
  fields_under_root: true

# 输出到 Logstash
output.logstash:
  hosts: ["logstash:5044"]
  bulk_max_size: 2048
  loadbalance: true
  
# 性能优化
queue.mem:
  events: 4096
  flush.min_events: 512
  flush.timeout: 1s

# 监控
monitoring.enabled: true
monitoring.cluster_uuid: "huochai-cluster"

# 日志
logging.level: info
logging.to_files: true
logging.files:
  path: /var/log/filebeat
  name: filebeat
  keepfiles: 7
```

### 10. Logstash 配置

```ruby
# logstash.conf
input {
  beats {
    port => 5044
  }
}

filter {
  # 解析时间戳
  date {
    match => ["@timestamp", "ISO8601"]
    target => "@timestamp"
  }
  
  # 解析 traceId
  if [traceId] {
    mutate {
      add_field => { "trace_id" => "%{traceId}" }
    }
  }
  
  # 解析用户信息
  if [userId] {
    mutate {
      convert => { "userId" => "integer" }
    }
  }
  
  # 解析请求信息
  if [requestUrl] {
    grok {
      match => { "requestUrl" => "/%{PATH:path}" }
    }
  }
  
  # 解析 IP
  if [ip] {
    geoip {
      source => "ip"
      target => "geoip"
    }
  }
  
  # 移除不需要的字段
  mutate {
    remove_field => ["host", "agent", "ecs", "input", "log"]
  }
}

output {
  elasticsearch {
    hosts => ["elasticsearch:9200"]
    index => "huochai-operation-log-%{+YYYY.MM.dd}"
    # 使用 ILM 索引生命周期管理
    ilm_enabled => true
    ilm_rollover_alias => "huochai-operation-log"
    ilm_policy => "huochai-log-policy"
  }
  
  # 错误日志单独索引
  if [log_type] == "error" {
    elasticsearch {
      hosts => ["elasticsearch:9200"]
      index => "huochai-error-log-%{+YYYY.MM.dd}"
    }
  }
}
```

---

## README 文档更新

在项目 README.md 中需要添加以下内容：

```markdown
## 日志模块

### 架构说明

本项目采用标准 ELK 日志架构：
- **SpringBoot → Logback(JSON) → Filebeat → Logstash → Elasticsearch → Kibana**
- **应用只写本地文件：** 避免直接写 ES 导致 ES 压力过大
- **Disruptor 无锁队列：** 百万级 TPS 高并发写入
- **JSON 格式日志：** 结构化输出，便于 Filebeat 采集

### 日志文件

| 文件 | 说明 | 路径 |
|-----|------|------|
| JSON日志 | 供 Filebeat 采集 | logs/json/operation-*.log |
| 错误日志 | 失败操作详情 | logs/json/error-*.log |
| 控制台日志 | 人工查看 | logs/console/*.log |

### 日志格式

JSON 格式日志，便于 Filebeat 采集和 Logstash 解析：

\`\`\`json
{
  "@timestamp": "2024-01-15T14:30:25.123+08:00",
  "traceId": "a1b2c3d4e5f6g7h8",
  "userId": 1,
  "username": "admin",
  "module": "用户管理",
  "operationType": "CREATE",
  "operationDesc": "创建用户",
  "requestMethod": "POST",
  "requestUrl": "/api/admin/users",
  "requestParams": "{"username":"test"}",
  "ip": "192.168.1.100",
  "status": 1,
  "costTime": 125
}
\`\`\`
  | PARAMS: {"username":"test"}     # 请求参数
  | RESULT: SUCCESS                 # 执行结果
  | COST: 125ms                     # 耗时
  | IP: 192.168.1.100              # 客户端IP
  | MODULE: 用户管理                # 操作模块
  | OPERATION: 创建用户             # 操作描述
\`\`\`

### 链路追踪

支持以下场景的 TraceId 自动传播：
- HTTP 请求：自动创建/传播 TraceId
- Feign 调用：自动注入 Trace Header
- RabbitMQ 消息：自动传播 Trace 信息
- Zipkin 集成：完整调用链路可视化

### 配置项

| 配置项 | 默认值 | 说明 |
|-------|--------|------|
| log.file.enabled | true | 本地日志文件开关 |
| log.file.max-size-mb | 100 | 单文件最大大小 |
| log.file.max-history-days | 30 | 日志保留天数 |
| management.zipkin.tracing.endpoint | http://localhost:9411 | Zipkin 地址 |
```

---

## 边界条件与异常处理

### 1. 高并发场景处理

| 场景 | 处理策略 |
|-----|---------|
| Disruptor 队列满（>80%） | 触发背压控制，记录 WARN 日志 |
| Disruptor 队列满（100%） | 直接阻塞等待，确保不丢失 |
| Logback 写入失败 | 记录到备用文件，告警通知 |
| Filebeat 采集延迟 | 本地文件堆积，后续自动续传 |

### 2. Filebeat/Logstash 故障
- **Filebeat 停止：** 日志继续写入本地文件，Filebeat 恢复后自动续传
- **Logstash 故障：** Filebeat 缓冲，或配置多个 Logstash 实例
- **ES 故障：** Logstash 持久化队列，ES 恢复后自动写入

### 3. TraceContext 为空
- 自动生成新的 TraceId（UUID格式）
- 确保链路不中断
- 日志记录警告信息

### 4. 本地磁盘满
- Logback 自动触发删除旧日志
- 发送告警通知
- 配置磁盘使用率监控

---

## 数据流

### 日志流程（标准 ELK 架构）
```
1. Controller 方法执行
2. AOP 切面捕获操作
3. 发布到 Disruptor RingBuffer（无锁CAS操作）
4. Disruptor 消费者写入本地 JSON 日志文件
5. Filebeat 采集日志文件
6. 发送到 Logstash 进行解析/清洗
7. Logstash 批量写入 Elasticsearch
8. Kibana 可视化查询
```

### Trace 流程
```
1. HTTP 请求进入
2. TraceFilter 创建/提取 TraceContext
3. 设置到 MDC 和 TraceContextHolder
4. 业务代码执行
5. Feign 调用自动传播 Trace（拦截器注入 Header）
6. 响应返回，发送 Span 到 Zipkin
7. Zipkin UI 查看完整链路
```

---

## 预期结果

1. **日志模块**
   - 支持百万级 TPS 高并发写入
   - **标准 ELK 架构：** 应用只写本地文件，Filebeat 采集，避免 ES 压力过大
   - **JSON 格式日志：** 结构化输出，便于解析和查询
   - **本地文件保障：** 日志持久化到本地，确保数据不丢失
   - 支持 Kibana 实时查询和分析
   - 主流程零阻塞，完全异步解耦

2. **本地日志文件**
   - `logs/json/operation-*.log`：JSON 格式操作日志，供 Filebeat 采集
   - `logs/json/error-*.log`：错误日志，便于快速定位问题
   - 按日期自动滚动，自动清理过期日志
   - 单文件超限时自动滚动

3. **链路追踪**
   - HTTP 请求自动生成 TraceId
   - Feign 调用自动传播 Trace
   - Zipkin 展示完整调用链路
   - 支持 Dubbo RPC 调用（可选扩展）

4. **文档更新**
   - README.md 包含日志模块完整说明
   - 配置项说明
   - 日志格式示例
   - 问题排查指南

5. **模块复用**
   - 其他项目只需引入 `huochai-log-starter` 依赖
   - 添加 `@EnableLogCollector` 注解即可开启功能
   - 对业务代码零侵入

---

## huochai-log-starter 模块说明文档

```markdown
# huochai-log-starter

高性能日志收集模块，支持 ES 存储、本地文件双写、链路追踪。

## 功能特性

- **高并发：** Disruptor 无锁队列，百万级 TPS
- **双写保障：** 本地文件 + Elasticsearch 同时写入
- **顺序保障：** 按 traceId 分片，同一链路日志顺序写入
- **链路追踪：** 支持 HTTP/Feign/RabbitMQ 调用链路传递
- **零侵入：** 注解开启，对业务代码无侵入
- **易扩展：** 可被其他项目直接复用

## 快速开始

### 1. 添加依赖

\`\`\`xml
<dependency>
    <groupId>com.huochai</groupId>
    <artifactId>huochai-log-starter</artifactId>
    <version>${project.version}</version>
</dependency>
\`\`\`

### 2. 开启日志收集

\`\`\`java
@EnableLogCollector
@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
\`\`\`

### 3. 使用操作日志注解

\`\`\`java
@RestController
public class UserController {
    
    @OperationLog(module = "用户管理", desc = "创建用户")
    @PostMapping("/users")
    public Result<User> createUser(@RequestBody User user) {
        return Result.success(user);
    }
}
\`\`\`

## 配置项

| 配置项 | 默认值 | 说明 |
|-------|--------|------|
| `huochai.log.enabled` | true | 是否启用日志收集 |
| `huochai.log.file.enabled` | true | 是否写入本地文件 |
| `huochai.log.file.dir` | logs | 日志文件目录 |
| `huochai.log.file.max-size-mb` | 100 | 单文件最大大小(MB) |
| `huochai.log.file.max-history-days` | 30 | 日志保留天数 |
| `huochai.log.es.enabled` | true | 是否写入 ES |
| `huochai.log.es.index` | operation-log | ES 索引名 |
| `huochai.log.mq.enabled` | true | 是否启用 MQ |
| `huochai.log.disruptor.buffer-size` | 65536 | Disruptor 缓冲区大小 |
| `huochai.log.disruptor.batch-size` | 100 | 批量发送大小 |
| `huochai.trace.enabled` | true | 是否启用链路追踪 |

## 日志格式

### 本地日志文件

\`\`\`
logs/
├── 2024-01-15/
│   ├── operation-trace.log       # 操作日志
│   ├── operation-trace-error.log # 错误日志
│   └── operation-trace-access.log # 访问日志
└── fallback/                      # 降级日志
\`\`\`

### 日志内容格式

\`\`\`
2024-01-15 14:30:25.123 [traceId] [thread] INFO  c.h.l.w.LogFileWriter - 
  | TRACE_ID: a1b2c3d4e5f6g7h8
  | USER: admin(userId=1)
  | REQUEST: POST /api/admin/users
  | PARAMS: {"username":"test"}
  | RESULT: SUCCESS
  | COST: 125ms
  | IP: 192.168.1.100
\`\`\`

## Elasticsearch 索引

### 索引结构

\`\`\`json
{
  "mappings": {
    "properties": {
      "traceId": { "type": "keyword" },
      "spanId": { "type": "keyword" },
      "userId": { "type": "long" },
      "username": { "type": "keyword" },
      "module": { "type": "keyword" },
      "operationType": { "type": "keyword" },
      "operationDesc": { "type": "text" },
      "requestMethod": { "type": "keyword" },
      "requestUrl": { "type": "keyword" },
      "requestParams": { "type": "text" },
      "responseResult": { "type": "text" },
      "ip": { "type": "ip" },
      "status": { "type": "integer" },
      "errorMsg": { "type": "text" },
      "costTime": { "type": "long" },
      "createdAt": { "type": "date" }
    }
  }
}
\`\`\`

### Kibana 查询示例

\`\`\`
# 按 traceId 查询
traceId: "a1b2c3d4e5f6g7h8"

# 按用户查询
userId: 1

# 按时间范围查询
@timestamp >= "2024-01-15" AND @timestamp < "2024-01-16"

# 查询错误日志
status: 0

# 按模块查询
module: "用户管理"
\`\`\`

## 架构设计

### 数据流（标准 ELK 架构）

\`\`\`
SpringBoot -> Logback(JSON) -> 本地文件 -> Filebeat -> Logstash -> ES -> Kibana
\`\`\`

**为什么不直接写 ES？**
- 应用只负责写本地文件，性能极高
- Filebeat 轻量采集，支持背压控制
- Logstash 统一清洗，数据质量高
- ES 独立扩展，不影响应用

## 扩展点

### 自定义日志处理器

\`\`\`java
@Component
public class CustomLogHandler implements LogHandler {
    
    @Override
    public void handle(LogEntry entry) {
        // 自定义处理逻辑
    }
}
\`\`\`

## 常见问题

### Q: Filebeat/Logstash 故障怎么办？
A: 日志会保留在本地文件中，Filebeat/Logstash 恢复后自动续传，不会丢失数据。

### Q: 如何查看完整的调用链路？
A: 使用 traceId 在 Kibana 或 Zipkin 中查询。

### Q: 日志写入会影响主流程性能吗？
A: 不会。Disruptor 无锁队列 + 本地文件写入，主流程零阻塞。

### Q: 如何调整日志级别？
A: 在 application.properties 中配置：
\`\`\`properties
logging.level.com.huochai.log=DEBUG
\`\`\`

## 版本历史

| 版本 | 日期 | 说明 |
|-----|------|------|
| 1.0.0 | 2024-01-15 | 初始版本，支持基础日志收集 |
| 1.1.0 | 2024-01-20 | 添加 ES 双写支持 |
| 1.2.0 | 2024-01-25 | 添加链路追踪支持 |
```
