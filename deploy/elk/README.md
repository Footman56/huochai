# ELK 部署文档

## 架构说明

```
SpringBoot 应用
    ↓ (写本地文件)
Logback (JSON日志)
    ↓
Filebeat (采集)
    ↓
Logstash (解析/清洗)
    ↓
Elasticsearch (存储)
    ↓
Kibana (查询分析)
```

## 快速部署

### 使用 Docker Compose

```bash
cd deploy/elk
docker-compose up -d
```

### 服务访问

| 服务 | 地址 | 说明 |
|------|------|------|
| Elasticsearch | http://localhost:9200 | 日志存储 |
| Kibana | http://localhost:5601 | 可视化界面 |
| Logstash | localhost:5044 | 日志接收端口 |

## 配置说明

### 1. Elasticsearch 配置

编辑 `elasticsearch/elasticsearch.yml`：

```yaml
cluster.name: huochai-logs
node.name: node-1
network.host: 0.0.0.0
http.port: 9200
discovery.type: single-node
xpack.security.enabled: false
```

### 2. Logstash 配置

编辑 `logstash/logstash.conf`：

- 配置输入源（Filebeat）
- 配置过滤器（解析日志）
- 配置输出（Elasticsearch）

### 3. Filebeat 配置

编辑 `filebeat/filebeat.yml`：

```yaml
filebeat.inputs:
  - type: log
    paths:
      - /var/log/huochai/*.json
    json.keys_under_root: true

output.logstash:
  hosts: ["logstash:5044"]
```

### 4. Kibana 配置

编辑 `kibana/kibana.yml`：

```yaml
server.host: "0.0.0.0"
elasticsearch.hosts: ["http://elasticsearch:9200"]
i18n.locale: "zh-CN"
```

## 索引模板

导入索引模板：

```bash
curl -X PUT "http://localhost:9200/_index_template/huochai-template" \n  -H 'Content-Type: application/json' \n  -d @elasticsearch/index-template.json
```

## Kibana 使用

### 1. 创建索引模式

1. 打开 Kibana → Stack Management → Index Patterns
2. 点击 "Create index pattern"
3. 输入索引模式：`huochai-*`
4. 选择时间字段：`@timestamp`

### 2. 日志查询

进入 Discover 页面，可按以下方式查询：

- 按 traceId 查询：`traceId: "abc123"`
- 按日志类型：`logType: "CONTROLLER"`
- 按错误：`level: "ERROR"`
- 慢查询：`duration > 1000`

### 3. 创建仪表盘

1. 创建可视化图表
2. 添加到仪表盘
3. 常用图表：
   - 请求量趋势
   - 错误率统计
   - 接口响应时间
   - 慢查询 Top 10

## 日志采集配置

### Spring Boot 应用配置

1. 添加依赖
2. 配置 Logback
3. 设置日志路径

```yaml
logging:
  file:
    path: /var/log/huochai
    name: application.json
```

### Filebeat 安装

#### Linux

```bash
# 下载
curl -L -O https://artifacts.elastic.co/downloads/beats/filebeat/filebeat-8.12.0-linux-x86_64.tar.gz

# 解压
tar xzvf filebeat-8.12.0-linux-x86_64.tar.gz

# 配置
cp filebeat.yml filebeat.yml.bak
# 编辑 filebeat.yml

# 启动
./filebeat -e -c filebeat.yml
```

#### Docker

```bash
docker run -d \n  --name=filebeat \n  --user=root \n  -v /var/log/huochai:/var/log/huochai:ro \n  -v ./filebeat/filebeat.yml:/usr/share/filebeat/filebeat.yml:ro \n  docker.elastic.co/beats/filebeat:8.12.0
```

## 性能调优

### Elasticsearch

```yaml
# 内存设置
ES_JAVA_OPTS: "-Xms4g -Xmx4g"

# 索引设置
index:
  refresh_interval: 5s
  number_of_shards: 3
  number_of_replicas: 1
```

### Logstash

```yaml
# JVM 设置
LS_JAVA_OPTS: "-Xms2g -Xmx2g"

# Pipeline 设置
pipeline.workers: 4
pipeline.batch.size: 125
pipeline.batch.delay: 50
```

### Filebeat

```yaml
# 批量发送
output.logstash.bulk_max_size: 2048

# 内存队列
queue.mem.events: 4096
queue.mem.flush.min_events: 512
```

## 监控告警

### 告警规则示例

1. **错误率告警**
   ```
   WHEN count() OF log
   WHERE level = "ERROR"
   GROUPED OVER top 5 'serviceName'
   FOR THE LAST 5 minutes
   ```

2. **慢查询告警**
   ```
   WHEN count() OF log
   WHERE logType = "MYSQL" AND duration > 3000
   ```

3. **接口超时告警**
   ```
   WHEN count() OF log
   WHERE logType = "CONTROLLER" AND duration > 5000
   ```

## 故障排查

### 1. Filebeat 无法连接 Logstash

```bash
# 检查 Logstash 端口
telnet logstash 5044

# 查看 Filebeat 日志
docker logs huochai-filebeat
```

### 2. Elasticsearch 内存不足

```bash
# 查看 ES 状态
curl http://localhost:9200/_cluster/health

# 查看节点状态
curl http://localhost:9200/_nodes/stats
```

### 3. Kibana 无法连接 ES

```bash
# 检查 ES 连接
curl http://localhost:9200

# 查看 Kibana 日志
docker logs huochai-kibana
```