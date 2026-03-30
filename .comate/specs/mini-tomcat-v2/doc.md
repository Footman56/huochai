# Mini Tomcat 完善规范 (V2)

## 1. 需求概述

完善Mini Tomcat，实现完整的容器层次结构，支持灵活配置，完美适配Spring Boot。

## 2. 架构设计

### 2.1 容器层次结构

采用Tomcat的标准容器层次：

```
Engine (引擎)
  └── Host (虚拟主机)
        └── Context (Web应用)
              └── Wrapper (Servlet包装)
```

### 2.2 设计模式

- **模板方法模式**: 定义容器处理骨架，子类实现具体逻辑
- **责任链模式**: Pipeline/Valve链式处理请求
- **组合模式**: 容器组件的树形结构管理
- **策略模式**: 不同容器的处理策略
- **观察者模式**: 生命周期事件监听

### 2.3 包结构

```
tomcat/
├── core/                      # 核心接口
│   ├── Container.java         # 容器接口
│   ├── Engine.java            # 引擎接口
│   ├── Host.java              # 虚拟主机接口
│   ├── Context.java           # Web应用接口
│   ├── Wrapper.java           # Servlet包装器接口
│   ├── Lifecycle.java         # 生命周期接口
│   ├── Pipeline.java          # 管道接口
│   ├── Valve.java             # 阀门接口
│   └── Mapper.java            # 映射器接口
├── engine/                    # 引擎实现
│   └── StandardEngine.java
├── host/                      # 虚拟主机实现
│   └── StandardHost.java
├── context/                   # Context实现
│   └── StandardContext.java
├── wrapper/                   # Wrapper实现
│   └── StandardWrapper.java
├── pipeline/                  # 管道实现
│   ├── StandardPipeline.java
│   └── StandardValve.java
├── config/                    # 配置
│   ├── ServerConfig.java      # 服务器配置
│   ├── EngineConfig.java      # 引擎配置
│   ├── HostConfig.java        # 主机配置
│   ├── ContextConfig.java     # 上下文配置
│   └── WrapperConfig.java     # Wrapper配置
├── loader/                    # 类加载器
│   └── WebAppClassLoader.java
├── spring/                    # Spring Boot适配
│   ├── SpringBootServletContainerInitializer.java
│   ├── SpringServletContainerInitializer.java
│   └── SpringWebApplicationInitializer.java
├── tomcat/                    # 启动入口
│   └── Tomcat.java            # 主启动类
└── demo/                      # 示例
```

## 3. 详细设计

### 3.1 生命周期接口 (模板方法模式)

```java
public interface Lifecycle {
    // 生命周期状态
    enum State { NEW, INITIALIZED, STARTED, STOPPED, FAILED }
    
    void init() throws LifecycleException;
    void start() throws LifecycleException;
    void stop() throws LifecycleException;
    void destroy() throws LifecycleException;
    
    void addLifecycleListener(LifecycleListener listener);
    State getState();
}
```

### 3.2 容器基类 (模板方法模式)

```java
public abstract class ContainerBase implements Container, Lifecycle {
    // 模板方法 - 定义处理流程
    public final void start() {
        init();
        startInternal();
        setState(State.STARTED);
    }
    
    protected abstract void startInternal();
    protected abstract void stopInternal();
}
```

### 3.3 责任链 - Pipeline

```
Request → Engine Pipeline → Host Pipeline → Context Pipeline → Wrapper Pipeline
             ↓                   ↓                 ↓                 ↓
         Valve1             Valve1            Valve1            Servlet
             ↓                   ↓                 ↓
         Valve2             Valve2            Valve2
             ↓                   ↓                 
         Basic              Basic              Basic
```

### 3.4 配置灵活性

支持多种配置方式：
1. 编程式配置 (Builder模式)
2. 配置文件 (server.properties)
3. 注解配置 (@TomcatConfiguration)

## 4. Spring Boot适配

### 4.1 ServletContainerInitializer

实现Spring Boot的自动配置发现机制：

```java
@HandlesTypes(SpringBootServletInitializer.class)
public class SpringBootServletContainerInitializer 
    implements ServletContainerInitializer {
    
    // 发现SpringBootServletInitializer实现类
    // 创建WebApplicationContext
    // 注册DispatcherServlet
}
```

### 4.2 内嵌Tomcat支持

```java
public class Tomcat {
    public static void main(String[] args) {
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8080);
        tomcat.addWebapp("/", "/app");
        tomcat.start();
    }
}
```

## 5. 关键接口

### 5.1 Container接口

```java
public interface Container extends Lifecycle {
    String getName();
    void setName(String name);
    Container getParent();
    void setParent(Container parent);
    Pipeline getPipeline();
    ClassLoader getClassLoader();
    void addChild(Container child);
    Container findChild(String name);
}
```

### 5.2 Mapper接口

```java
public interface Mapper {
    Mapping map(Request request);
    void addWrapper(String contextPath, String urlPattern, Wrapper wrapper);
}
```

## 6. 实现要点

### 6.1 Engine
- 管理多个Host
- 默认Host处理
- 日志记录

### 6.2 Host
- 域名管理
- 多个Context
- 别名支持

### 6.3 Context
- Web应用根路径
- Servlet/Filter管理
- 欢迎文件处理
- Session管理(简化)

### 6.4 Wrapper
- 单个Servlet管理
- 加载onStartup
- 初始化参数

## 7. 请求处理流程

```
Connector.accept()
  → Request/Response创建
  → Engine.invoke()
    → Engine Pipeline
    → Host.invoke()
      → Host Pipeline
      → Context.invoke()
        → Context Pipeline
        → Wrapper.invoke()
          → Servlet.service()
  → Response.build()
  → Connector.sendResponse()
```
