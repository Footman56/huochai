# Mini Tomcat 实现规范

## 1. 需求概述

手写一个精简版Tomcat服务器，实现基本的Servlet容器功能，采用设计模式思想，代码结构清晰。

## 2. 功能需求

### 2.1 核心功能
- **HTTP服务器**: 监听端口，接受HTTP请求
- **请求解析**: 解析HTTP请求行、请求头、请求体
- **Servlet映射**: 根据URL路径映射到对应的Servlet处理
- **响应处理**: 生成HTTP响应，返回给客户端
- **生命周期管理**: Servlet的init、service、destroy生命周期

### 2.2 设计模式应用
- **单例模式**: Server、Context管理组件
- **工厂模式**: ServletFactory创建Servlet实例
- **策略模式**: 不同类型的请求处理器
- **观察者模式**: 容器事件监听机制
- **责任链模式**: Filter链处理
- **模板方法模式**: HttpServlet抽象doGet/doPost

## 3. 包结构设计

```
tomcat/
├── server/                    # 服务器核心
│   ├── TomcatServer.java      # 启动入口，单例模式
│   ├── ServerFactory.java     # 服务器工厂
│   └── ServerListener.java    # 观察者-服务器事件监听
├── connector/                 # 连接器
│   ├── Connector.java         # 监听端口，接受请求
│   ├── Request.java           # HTTP请求封装
│   └── Response.java          # HTTP响应封装
├── container/                 # 容器
│   ├── Context.java           # Web应用上下文，单例
│   ├── Wrapper.java           # Servlet包装器
│   └── Pipeline.java          # 责任链-管道
├── servlet/                   # Servlet相关
│   ├── Servlet.java           # Servlet接口
│   ├── GenericServlet.java    # 抽象基类
│   ├── HttpServlet.java       # HTTP Servlet抽象类(模板方法)
│   ├── ServletConfig.java     # Servlet配置
│   └── ServletContext.java    # Servlet上下文
├── filter/                    # 过滤器
│   ├── Filter.java            # Filter接口
│   ├── FilterChain.java       # Filter链
│   └── FilterConfig.java      # Filter配置
├── mapper/                    # 映射器
│   ├── ServletMapper.java     # URL到Servlet映射
│   └── MappingData.java       # 映射数据
├── util/                      # 工具类
│   ├── HttpUtil.java          # HTTP解析工具
│   └── Constants.java         # 常量定义
└── exception/                 # 异常
    └── ServletException.java  # Servlet异常
```

## 4. 详细设计

### 4.1 核心类说明

#### TomcatServer (单例模式)
- 启动/停止服务器
- 管理Context容器
- 负责整体生命周期

#### Connector
- 绑定端口
- 循环接受连接
- 委托给容器处理

#### Request/Response
- Request: 解析HTTP请求，封装method、uri、headers、parameters
- Response: 构建HTTP响应，封装status、headers、body

#### Context
- 管理Wrapper(Servlet)
- 管理FilterChain
- 提供ServletContext接口

#### Wrapper
- 包装单个Servlet
- 管理Servlet生命周期

#### Pipeline (责任链模式)
- 包含多个Valve
- 依次执行Valve处理请求

#### FilterChain (责任链模式)
- 包含多个Filter
- 依次执行Filter过滤请求

#### ServletMapper (策略模式)
- 根据URL查找对应的Wrapper
- 支持精确匹配和路径匹配

### 4.2 请求处理流程

```
Client -> Connector.accept()
         -> Request.parse()
         -> Context.getWrapper()
         -> FilterChain.doFilter()
         -> Pipeline.invoke()
         -> Wrapper.invoke()
         -> Servlet.service()
         -> Response.build()
         -> Connector.sendResponse()
```

### 4.3 生命周期

```
Server.start() -> Context.init()
                 -> Wrapper.loadServlet()
                 -> Servlet.init()
                 
Request arrives -> Servlet.service()
                 
Server.stop() -> Servlet.destroy()
                -> Context.destroy()
```

## 5. 注解设计

为便于理解，添加以下注解：
- `@Description`: 描述类/方法功能
- `@DesignPattern`: 标注使用的设计模式
- `@Lifecycle`: 标注生命周期方法
- `@Important`: 标注重要逻辑

## 6. 边界条件处理

- 404: 未找到匹配的Servlet
- 500: 服务器内部错误
- 400: 请求格式错误
- 不支持的HTTP方法返回405

## 7. 测试用例

提供示例Servlet:
- `HelloServlet`: 演示基本请求处理
- `EchoServlet`: 演示参数接收和响应

## 8. 预期输出

- 完整的tomcat包实现
- 包含详细注释和注解
- 使用说明文档
- 设计模式解析文档
