访问地址
后端 Swagger: https://localhost:8443/swagger-ui/index.html
前端管理: http://localhost:3000
Kibana 日志: http://localhost:5601
Elasticsearch: http://localhost:9200
默认账号
用户名: admin
密码: admin123



# 1. 初始化数据库
mysql -u root -p < src/main/resources/db/init-auth.sql

# 2. 启动 ELK
cd src/main/resources/elk && docker-compose up -d

# 3. 启动后端
cd huochai-core && mvn spring-boot:run

# 4. 启动前端
cd huochai-admin-ui && npm install && npm run dev