# Huochai Log Starter

高性能日志追踪模块，支持 ELK 架构，零侵入自动拦截多种场景日志。

## 特性

- **零侵入设计**：无需注解，自动拦截日志
- **ELK 架构**：SpringBoot → Logback(JSON) → Filebeat → Logstash → ES → Kibana
- **高性能**：基于 Disruptor 实现百万级 TPS 日志处理
- **多场景支持**：Controller、Service、Dubbo、MQ、MySQL、Redis、ES、定时任务
- **统一字段格式**：所有日志类型字段统一，便于数据处理
- **链路追踪**：自动生成 TraceId，支持分布式追踪

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.huochai</groupId>
    <artifactId>huochai-log-starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

### 2. 配置参数

```yaml
huochai:
  log:
    enabled: true
    service-name: ${spring.application.name}
    
    # Controller 日志配置
    controller:
      enabled: true
      log-request-body: true
      log-response-body: true
      exclude-paths:
        - /actuator
        - /swagger
      sensitive-fields:
        - password
        - token
        - secret
    
    # Service 日志配置
    service:
      enabled: true
      log-params: true
      log-result: true
    
    # 数据库日志配置
    database:
      mysql-enabled: true
      redis-enabled: true
      es-enabled: true
      slow-query-threshold: 1000
    
    # MQ 日志配置
    mq:
      consumer-enabled: true
      producer-enabled: true
    
    # Dubbo 日志配置
    dubbo:
      enabled: true
    
    # 链路追踪配置
    trace:
      enabled: true
      zipkin-endpoint: http://localhost:9411/api/v2/spans
      sample-rate: 1.0
    
    # Disruptor 配置
    disruptor:
      ring-buffer-size: 16384
      consumer-threads: 1
```

### 3. Logback 配置

在 `logback-spring.xml` 中添加 JSON 日志 Appender：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- JSON 日志输出 (供 Filebeat 采集) -->
    <appender name="JSON_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/var/log/huochai/application.json</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>/var/log/huochai/application.%d{yyyy-MM-dd}.json</fileNamePattern>
            <maxHistory>7</maxHistory>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>spanId</includeMdcKeyName>
            <customFields>{"app":"huochai"}</customFields>
        </encoder>
    </appender>

    <!-- huochai-json-log Logger -->
    <logger name="huochai-json-log" level="INFO" additivity="false">
        <appender-ref ref="JSON_FILE"/>
    </logger>

    <root level="INFO">
        <appender-ref ref="JSON_FILE"/>
    </root>
</configuration>
```

## 日志类型

| 类型 | 枚举值 | 说明 |
|------|--------|------|
| Controller | CONTROLLER | HTTP 请求日志 |
| Service | SERVICE | 服务层方法日志 |
| Dubbo | DUBBO | RPC 调用日志 |
| MQ消费者 | MQ_CONSUMER | 消息消费日志 |
| MQ生产者 | MQ_PRODUCER | 消息发送日志 |
| MySQL | MYSQL | 数据库操作日志 |
| Redis | REDIS | 缓存操作日志 |
| ES | ES | 搜索引擎日志 |
| 定时任务 | SCHEDULED | 定时任务日志 |
| 异常 | EXCEPTION | 异常日志 |
| 业务 | BUSINESS | 业务日志 |

## 统一字段规范

所有日志类型必须包含以下公共字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| timestamp | ISO8601 | 日志时间戳 |
| traceId | string | 链路追踪ID |
| spanId | string | Span ID |
| parentSpanId | string | 父 Span ID |
| logType | string | 日志类型 |
| level | string | 日志级别 |
| serviceName | string | 服务名称 |
| host | string | 服务实例IP |
| className | string | 类名 |
| methodName | string | 方法名 |
| message | string | 日志消息 |
| duration | long | 执行耗时(ms) |
| status | string | 结果状态 |

## 使用 LogHelper 记录业务日志

```java
import com.huochai.log.context.LogHelper;

// 记录业务日志
LogHelper.business("用户登录成功");

// 带额外信息
LogHelper.business("订单创建成功", Map.of("orderId", "12345"));

// 记录警告
LogHelper.warn("库存不足");

// 记录异常
try {
    // ...
} catch (Exception e) {
    LogHelper.error("处理失败", e);
}

// 记录操作日志
LogHelper.operation("删除用户", "userId=123", "SUCCESS");
```

## ELK 部署

详见 [deploy/elk/README.md](../../deploy/elk/README.md)

## 架构图

```
┌─────────────────┐
│  Spring Boot    │
│   Application   │
└────────┬────────┘
         │ 写入
         ▼
┌─────────────────┐
│    Logback      │
│  (JSON格式)      │
└────────┬────────┘
         │ 文件
         ▼
┌─────────────────┐
│    Filebeat     │
│   (日志采集)      │
└────────┬────────┘
         │ 发送
         ▼
┌─────────────────┐
│    Logstash     │
│   (解析清洗)      │
└────────┬────────┘
         │ 存储
         ▼
┌─────────────────┐
│  Elasticsearch  │
│    (索引存储)     │
└────────┬────────┘
         │ 查询
         ▼
┌─────────────────┐
│     Kibana      │
│   (可视化分析)    │
└─────────────────┘
```

## 性能优化

- **Disruptor 队列**：LMAX 架构，单线程处理 600万+ TPS
- **异步写入**：日志写入不影响业务性能
- **RingBuffer**：无锁设计，避免 GC 压力
- **批量处理**：支持批量发送到 Elasticsearch