# ELK 配置迁移规范

## 需求概述

将 ELK 配置文件从当前 `huochai-core/src/main/resources/elk` 目录迁移到 `/Users/huochai/elk` 目录使 ELK 服务可以被项目所有模块使用。日志统一维护在 `/Users/huochai/elk/logs` 目录下，新项目创建子目录存储日志，日志格式采用标准 log4j2 框架。

## 当前状态分析

### 现有文件结构
- `huochai-core/src/main/resources/elk/` - 当前 ELK 配置目录，包含：
  - docker-compose.yml - ELK 容器编排配置
  - filebeat.yml - 日志采集配置
  - logstash.conf - 日志处理管道配置
  - elasticsearch-template.json - ES 索引模板
  - kibana-dashboard.ndjson - Kibana 仪表盘配置
  - README.md - 说明文档

### 当前日志目录
项目中已有日志目录：`/Users/huochai/IdeaProjects/huochai/logs/`

## 目标架构

### 新目录结构
```
/Users/huochai/elk/                      # ELK 配置根目录
├── docker-compose.yml
├── elasticsearch/
│   └── elasticsearch-template.json
├── logstash/
│   └── logstash.conf
├── filebeat/
│   └── filebeat.yml
├── kibana/
│   └── kibana-dashboard.ndjson
├── logs/                                # 日志存储目录（由业务模块写入）
│   ├── auth-server/
│   │   └── auth-server.json
│   ├── client-app/
│   │   └── client-app.json
│   └── resource-server/
│       └── resource-server.json
└── README.md
```

### 日志格式
采用 log4j2 JSON 格式（log4j2 自带的 JSON 模板或自定义 JSON 布局），例如：
```json
{
  "timeMillis": 1234567890123,
  "thread": "main",
  "level": "INFO",
  "loggerName": "com.huochai.auth",
  "message": "User login successful",
  "endOfBatch": false,
  "loggerFqcn": "org.apache.logging.log4j",
  "stackTrace": "..."
}
```

## 文件操作

### 1. 创建目录结构
在 `/Users/huochai/elk/` 下创建子目录：
- elasticsearch/
- logstash/
- filebeat/
- kibana/
- logs/auth-server/
- logs/client-app/
- logs/resource-server/

### 2. 迁移配置文件
- 移动 `docker-compose.yml` 到 `/Users/huochai/elk/`
- 创建 `elasticsearch/` 子目录，迁移 elasticsearch-template.json
- 创建 `logstash/` 子目录，迁移 logstash.conf
- 创建 `filebeat/` 子目录，迁移 filebeat.yml
- 创建 `kibana/` 子目录，迁移 kibana-dashboard.ndjson

### 3. 修改 docker-compose.yml
更新 volume 挂载路径和日志目录挂载：
- `./elk/elasticsearch-template.json` → `./elasticsearch/elasticsearch-template.json`
- `./elk/logstash.conf` → `./logstash/logstash.conf`
- `./elk/filebeat.yml` → `./filebeat/filebeat.yml`
- `./logs:/var/log/huochai` 保持，指向新位置

### 4. 修改 filebeat.yml（重点更新）
- 配置日志目录：`/var/log/huochai/*/*.json`
- 使用 log4j2 JSON 格式解析
- 动态提取 service 字段用于索引分类
- 支持通配符匹配新模块

### 5. 修改 logstash.conf
- 动态索引命名：`huochai-%{service}-%{+YYYY.MM.dd}`
- 解析 log4j2 JSON 格式日志

### 6. 修改 elasticsearch-template.json
- 更新 index_patterns 支持多模块：`huochai-*-*`
- 保留通用字段映射

### 7. 删除旧文件
迁移完成后删除 `huochai-core/src/main/resources/elk/` 目录