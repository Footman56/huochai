# ELK 日志系统使用指南

## 一、系统架构

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Spring    │────▶│  Filebeat   │────▶│  Logstash   │────▶│Elasticsearch│
│  Boot App   │     │  (采集)      │     │  (处理)      │     │  (存储)      │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
                                                                  │
                                                                  ▼
                                                           ┌─────────────┐
                                                           │   Kibana    │
                                                           │  (可视化)    │
                                                           └─────────────┘
```

## 二、快速启动

### 1. 启动 ELK 栈

```bash
cd src/main/resources/elk
docker-compose up -d
```

### 2. 验证服务状态

```bash
# 检查 Elasticsearch
curl http://localhost:9200/_cluster/health

# 检查 Kibana
curl http://localhost:5601/api/status
```

### 3. 访问 Kibana

浏览器打开: http://localhost:5601

## 三、配置说明

### 3.1 Filebeat 配置 (filebeat.yml)

- 监控日志目录: `./logs/huochai-auth.log`
- 输出到 Logstash: `localhost:5044`

### 3.2 Logstash 配置 (logstash.conf)

- 输入: Beats 协议 5044 端口
- 处理: JSON解析、TraceId提取、标签添加
- 输出: Elasticsearch

### 3.3 Elasticsearch 索引模板

- 索引模式: `huochai-auth-YYYY.MM.dd`
- 字段映射: traceId(keyword)、userId(keyword)、timestamp(date)

## 四、Kibana 使用

### 4.1 创建索引模式

1. 进入 Kibana -> Stack Management -> Index Patterns
2. 创建索引模式: `huochai-auth-*`
3. 选择时间字段: `@timestamp`

### 4.2 导入仪表板

1. 进入 Kibana -> Stack Management -> Saved Objects
2. 导入 `kibana-dashboard.ndjson` 文件

### 4.3 常用查询

```kibana
# 按TraceId查询
traceId: "abc-123-def"

# 按用户查询
username: "admin"

# 按日志级别
level: "ERROR"

# 按时间范围
@timestamp >= "2026-04-16" AND @timestamp < "2026-04-17"

# 登录相关
log_message: "登录"
```

## 五、告警配置

### 5.1 创建告警规则

1. 进入 Kibana -> Alerts & Insights -> Rules
2. 创建规则类型: "Elasticsearch query"
3. 配置查询条件和阈值

### 5.2 告警示例

- 登录失败超过10次/5分钟
- 错误日志超过5条/1分钟
- 系统异常关键词监控

## 六、性能调优

### 6.1 Logstash 性能

```ruby
pipeline {
  workers => 4        # CPU核心数
  batch_size => 125   # 批处理大小
  batch_delay => 50   # 批处理延迟
}
```

### 6.2 Elasticsearch 性能

- 分片数: 根据数据量调整 (建议每个分片 < 50GB)
- 副本数: 生产环境建议 1-2 个
- 刷新间隔: 默认 1s，可调整为 5s

### 6.3 索引生命周期

建议配置 ILM (Index Lifecycle Management):
- 热阶段: 7天
- 温阶段: 30天
- 冷阶段: 90天
- 删除: 180天

## 七、日志查询示例

### 7.1 查询登录链路

```kibana
traceId: "your-trace-id"
```

### 7.2 查询用户操作

```kibana
username: "admin" AND @timestamp >= now-1h
```

### 7.3 查询异常

```kibana
level: "ERROR" OR level: "WARN"
```

### 7.4 查询领域事件

```kibana
tags: "domain_action" OR tags: "domain_validation"
```

## 八、故障排查

### 8.1 Filebeat 无法连接 Logstash

```bash
# 检查 Logstash 端口
telnet localhost 5044

# 查看 Filebeat 日志
docker logs huochai-filebeat
```

### 8.2 Logstash 无法连接 Elasticsearch

```bash
# 检查 Elasticsearch 状态
curl http://localhost:9200/_cluster/health

# 查看 Logstash 日志
docker logs huochai-logstash
```

### 8.3 Kibana 无法显示数据

1. 检查索引模式是否正确
2. 检查时间范围选择
3. 检查字段映射