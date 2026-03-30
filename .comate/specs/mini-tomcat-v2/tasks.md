# Mini Tomcat V2 实现任务清单

## 任务总览
完善Tomcat，实现完整的Engine→Host→Context→Wrapper层次结构，采用模板方法、责任链模式，支持灵活配置和Spring Boot适配。

---

- [x] Task 1: 创建核心接口 (Container/Lifecycle/Pipeline/Valve)
  - 1.1: 创建 Lifecycle.java 生命周期接口(模板方法)
  - 1.2:创建 Container.java 容器接口
  - 1.3: 创建 Pipeline.java 管道接口
  - 1.4: 创建 Valve.java 阀门接口

- [x] Task 2: 创建容器基类
  - 2.1: 创建 ContainerBase.java 容器基类(模板方法模式)
  - 2.2: 创建 LifecycleBase.java 生命周期基础实现

- [x] Task 3: 创建Engine组件
  - 3.1: 创建 Engine.java 引擎接口
  - 3.2: 创建 StandardEngine.java 引擎实现

- [x] Task 4: 创建Host组件
  - 4.1: 创建 Host.java 虚拟主机接口
  - 4.2: 创建 StandardHost.java 虚拟主机实现

- [x] Task 5: 创建Context组件
  - 5.1: 重构 Context.java 标准上下文
  - 5.2: 创建 StandardContext.java 完整实现

- [x] Task 6: 创建Wrapper组件
  - 6.1: 重构 Wrapper.java 接口
  - 6.2: 创建 StandardWrapper.java 完整实现

- [x] Task 7: 完善Pipeline和Valve
  - 7.1: 创建 StandardPipeline.java 标准管道实现
  - 7.2: 创建 StandardValve.java 标准阀门实现

- [x] Task 8: 创建配置系统
  - 8.1: 创建 ServerConfig.java 服务器配置
  - 8.2: 创建 Tomcat.java 主启动类(Builder模式)
  - 8.3: 创建 TomcatBuilder.java 配置构建器

- [ ] Task 9: 创建类加载器
  - 9.1: 创建 WebAppClassLoader.java Web应用类加载器

- [ ] Task 10: 创建Mapper组件
  - 10.1: 创建 Mapper.java 映射器接口
  - 10.2: 创建 ContextMapper.java 上下文映射器
  - 10.3: 创建 HostMapper.java 主机映射器

- [ ] Task 11: 重构Connector
  - 11.1: 更新 Connector.java 支持容器层次

- [x] Task 12: Spring Boot适配
  - 12.1: 创建 SpringBootServletContainerInitializer.java
  - 12.2: 创建 SpringWebApplicationInitializer.java
  - 12.3: 创建 EmbeddedTomcat.java 内嵌Tomcat支持

- [x] Task 13: 创建示例
  - 13.1: 创建 SpringBootDemo.java Spring Boot启动示例

- [x] Task 14: 生成总结文档
