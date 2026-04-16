# ELK 多模块迁移任务

- [x] Task 1: 创建 /Users/huochai/elk 目录结构
    - 1.1: 创建 elk 根目录
    - 1.2: 创建 elasticsearch/ 子目录
    - 1.3: 创建 logstash/ 子目录
    - 1.4: 创建 filebeat/ 子目录
    - 1.5: 创建 kibana/ 子目录
    - 1.6: 创建 logs/auth-server/ 子目录
    - 1.7: 创建 logs/client-app/ 子目录
    - 1.8: 创建 logs/resource-server/ 子目录

- [x] Task 2: 迁移 docker-compose.yml
    - 2.1: 复制 docker-compose.yml 到 /Users/huochai/elk/
    - 2.2: 更新 volume 挂载路径
    - 2.3: 添加 logs 目录挂载配置

- [x] Task 3: 迁移和修改 elasticsearch-template.json
    - 3.1: 复制文件到 elasticsearch/ 子目录
    - 3.2: 修改 index_patterns 支持多模块 huochai-*-\*

- [x] Task 4: 迁移和修改 logstash.conf
    - 4.1: 复制文件到 logstash/ 子目录
    - 4.2: 修改索引命名为动态 huochai-%{service}-%{+YYYY.MM.dd}
    - 4.3: 添加 log4j2 JSON 格式解析

- [x] Task 5: 创建和配置 filebeat.yml
    - 5.1: 创建 filebeat.yml 在 filebeat/ 子目录
    - 5.2: 配置 /var/log/huochai/*/*.json 采集路径
    - 5.3: 添加 log4j2 JSON 格式解析配置
    - 5.4: 动态提取 service 字段

- [x] Task 6: 迁移 kibana-dashboard.ndjson
    - 6.1: 复制文件到 kibana/ 子目录

- [x] Task 7: 删除旧 ELK 配置目录
    - 7.1: 删除 huochai-core/src/main/resources/elk/ 目录