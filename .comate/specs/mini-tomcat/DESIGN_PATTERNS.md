# Mini Tomcat 设计模式解析

## 概述

Mini Tomcat 是一个精简版的Servlet容器，实现了基本的HTTP服务器功能。在设计过程中，我们应用了多种经典的设计模式，以确保代码结构清晰、易于扩展和维护。

## 设计模式应用详解

### 1. 单例模式 (Singleton Pattern)

**应用位置**: `TomcatServer.java`

**描述**: 确保类只有一个实例，并提供一个全局访问点。

```java
public class TomcatServer {
    private static TomcatServer instance;
    
    private TomcatServer() {
    }
    
    public static synchronized TomcatServer getInstance() {
        if (instance == null) {
            instance = new TomcatServer();
        }
        return instance;
    }
}
```

**作用**:
- 整个应用只有一个TomcatServer实例
- 避免重复创建服务器造成的资源浪费
- 统一管理服务器生命周期

---

### 2. 工厂模式 (Factory Pattern)

**应用位置**: `ServerFactory.java`

**描述**: 封装对象的创建过程，客户端无需关心具体创建逻辑。

```java
public class ServerFactory {
    public static TomcatServer createServer(int port) {
        TomcatServer server = TomcatServer.getInstance(port);
        server.setServerName(Constants.SERVER_NAME);
        return server;
    }
    
    public static TomcatServer createAndStart(int port) throws Exception {
        TomcatServer server = createServer(port);
        server.start();
        return server;
    }
}
```

**作用**:
- 简化服务器创建过程
- 方便添加默认配置
- 提供多种创建方式

---

### 3. 模板方法模式 (Template Method Pattern)

**应用位置**: `HttpServlet.java`

**描述**: 定义算法骨架，将某些步骤延迟到子类中实现。

```java
public abstract class HttpServlet extends GenericServlet {
    @Override
    public void service(ServletRequest request, HttpServletResponse response) {
        // 根据HTTP方法分发
        String method = request.getMethod();
        if ("GET".equals(method)) {
            doGet(request, response);
        } else if ("POST".equals(method)) {
            doPost(request, response);
        }
    }
    
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        // 默认实现
    }
    
    protected void doPost(HttpServletRequest request, HttpServletResponse response) {
        // 默认实现
    }
}
```

**作用**:
- `service()` 方法作为模板，定义处理流程
- 子类只需重写 `doGet()`、`doPost()` 等方法
- 避免重复的请求分发逻辑

---

### 4. 责任链模式 (Chain of Responsibility Pattern)

**应用位置**: `Pipeline.java`, `FilterChain.java`

**描述**: 将请求沿着处理者链传递，直到某个处理者处理它。

```
Request → Filter1 → Filter2 → ... → Servlet
        ↓           ↓                  ↓
      handle()   handle()           service()
```

```java
// FilterChain实现
public class FilterChainImpl implements FilterChain {
    private List<Filter> filters = new ArrayList<>();
    
    @Override
    public void doFilter(HttpServletRequest request, HttpServletResponse response) {
        if (position < filters.size()) {
            Filter filter = filters.get(position++);
            filter.doFilter(request, response, this);  // 传递给下一个
        } else if (targetServlet != null) {
            targetServlet.service(request, response);  // 最后调用Servlet
        }
    }
}
```

**作用**:
- **Pipeline**: 管理Valve链，处理请求的不同阶段
- **FilterChain**: 管理Filter链，实现请求/响应的预处理和后处理
- 优点：解耦请求发送者和处理者；可以动态添加/删除处理者

---

### 5. 策略模式 (Strategy Pattern)

**应用位置**: `ServletMapper.java`

**描述**: 定义一系列算法，将每个算法封装起来，使它们可以互相替换。

```java
public class ServletMapper {
    // 根据不同策略查找
    public Mapping getMapping(String uri) {
        // 1. 精确匹配
        Mapping mapping = exactMappings.get(uri);
        if (mapping != null) return mapping;
        
        // 2. 路径匹配 /api/*
        for (Mapping pathMapping : pathMappings) {
            if (uri.startsWith(prefix)) return pathMapping;
        }
        
        // 3. 扩展名匹配 *.do
        mapping = extensionMappings.get(extension);
        if (mapping != null) return mapping;
        
        // 4. 默认Servlet
        return defaultMapping;
    }
}
```

**作用**:
- 支持多种URL匹配策略
- 可以在运行时添加新的匹配策略
- 符合开闭原则

---

### 6. 观察者模式 (Observer Pattern)

**应用位置**: `ServerListener.java`

**描述**: 当对象状态改变时，通知所有依赖它的对象。

```java
public interface ServerListener {
    void beforeStart(TomcatServer server);
    void afterStart(TomcatServer server);
    void beforeStop(TomcatServer server);
    void afterStop(TomcatServer server);
}

// 服务器通知监听器
public class TomcatServer {
    private ServerListener.Manager listenerManager = new ServerListener.Manager();
    
    public void start() {
        listenerManager.notifyBeforeStart(this);
        // 启动逻辑...
        listenerManager.notifyAfterStart(this);
    }
}
```

**作用**:
- 监听服务器生命周期事件
- 方便添加日志、性能监控等功能
- 解耦事件源和事件处理

---

### 7. 装饰器模式 (Decorator Pattern)

**应用位置**: `Wrapper.java` (WrapperImpl)

**描述**: 动态地给对象添加额外职责，比继承更灵活。

```java
class WrapperImpl implements Wrapper {
    private Servlet servlet;
    
    // 在Servlet基础上添加生命周期管理
    public void loadServlet() throws Exception {
        if (servlet == null) {
            servlet = (Servlet) Class.forName(servletClass).newInstance();
            servlet.init(servletConfig);
        }
    }
    
    @Override
    public void invoke(Request request, Response response) {
        // 添加额外处理后调用Servlet
        // ...
        servlet.service(request, response);
    }
}
```

**作用**:
- 为Servlet添加生命周期管理
- 无需继承即可增强功能

---

### 8. 外观模式 (Facade Pattern)

**应用位置**: `Connector.java`, `Context.java`

**描述**: 为复杂的子系统提供统一的接口。

```java
// Connector - 为服务器隐藏请求处理的复杂性
public class Connector {
    public void handleRequest(Socket socket) {
        // 创建请求/响应
        // 解析HTTP请求
        // 获取Servlet
        // 调用Filter链
        // 处理异常
        // 发送响应
        // 关闭连接
    }
}
```

**作用**:
- 简化客户端调用
- 隐藏内部复杂性

---

### 9. 封装模式 (Encapsulation)

**应用位置**: `Request.java`, `Response.java`

**描述**: 将原始HTTP数据封装为对象。

```java
// 将原始字节流封装为Request对象
public class Request implements HttpServletRequest {
    private InputStream inputStream;
    private String method;
    private String requestURI;
    private Map<String, String> headers;
    
    public void parse() {
        // 解析HTTP请求行、头、体
        // 封装为对象属性
    }
}
```

**作用**:
- 提供面向对象的API
- 隐藏解析细节

---

## 包结构总结

```
tomcat/
├── connector/          # 连接器 - 门面模式
│   ├── Connector.java  # 门面，简化请求处理
│   ├── Request.java   # 封装HTTP请求
│   └── Response.java  # 封装HTTP响应
├── container/         # 容器 - 装饰器模式
│   ├── Context.java   # Web应用上下文
│   ├── Pipeline.java  # 责任链-管道
│   └── Wrapper.java  # 装饰器-Servlet包装
├── filter/            # 过滤器 - 责任链模式
│   ├── Filter.java
│   ├── FilterChain.java
│   └── FilterConfig.java
├── mapper/            # 映射器 - 策略模式
│   ├── Mapping.java
│   └── ServletMapper.java
├── servlet/           # Servlet核心
│   ├── Servlet.java
│   ├── GenericServlet.java  # 模板方法
│   └── HttpServlet.java    # 模板方法
├── server/            # 服务器 - 单例/工厂/观察者
│   ├── TomcatServer.java   # 单例
│   ├── ServerFactory.java  # 工厂
│   └── ServerListener.java # 观察者
├── util/              # 工具类
│   ├── Constants.java
│   └── HttpUtil.java
└── exception/         # 异常
    └── ServletException.java
```

## 总结

通过以上设计模式的应用，Mini Tomcat 实现了：

1. **低耦合**: 各组件之间通过接口通信
2. **高内聚**: 相关功能集中在对应包中
3. **易扩展**: 通过策略、责任链等模式方便扩展
4. **易维护**: 代码结构清晰，职责明确
5. **可测试**: 依赖注入和接口设计便于单元测试

这些设计模式是Java Web开发中非常经典的实践，理解它们有助于更好地理解Servlet容器的工作原理。
