# Mini Tomcat 实现任务清单

## 任务总览
实现一个精简版Tomcat服务器，采用设计模式思想，包结构清晰。

---

- [x] Task 1: 创建基础常量和工具类
  - 1.1: 创建 Constants.java 定义HTTP状态码、默认端口等常量
  - 1.2: 创建 HttpUtil.java 提供HTTP解析工具方法

- [x] Task 2: 创建异常类
  - 2.1: 创建 ServletException.java 定义Servlet相关异常

- [x] Task 3: 创建Servlet核心接口和抽象类
  - 3.1: 创建 Servlet.java 定义Servlet接口
  - 3.2: 创建 GenericServlet.java 抽象类实现Servlet
  - 3.3: 创建 HttpServlet.java 抽象类(模板方法模式)

- [x] Task 4: 创建配置相关类
  - 4.1: 创建 ServletConfig.java Servlet配置接口
  - 4.2: 创建 ServletContext.java Servlet上下文接口

- [x] Task 5: 创建Filter相关类
  - 5.1: 创建 Filter.java 定义Filter接口
  - 5.2: 创建 FilterChain.java Filter链(责任链模式)
  - 5.3: 创建 FilterConfig.java Filter配置

- [x] Task 6: 创建请求响应封装
  - 6.1: 创建 Request.java 封装HTTP请求
  - 6.2: 创建 Response.java 封装HTTP响应

- [x] Task 7: 创建映射器
  - 7.1: 创建 Mapping.java 映射数据类
  - 7.2: 创建 ServletMapper.java URL映射(策略模式)

- [x] Task 8: 创建容器组件
  - 8.1: 创建 Pipeline.java 管道(责任链模式)
  - 8.2: 创建 Wrapper.java Servlet包装器
  - 8.3: 创建 Context.java Web应用上下文(单例模式)

- [x] Task 9: 创建连接器
  - 9.1: 创建 Connector.java 监听端口、接受请求

- [x] Task 10: 创建服务器核心
  - 10.1: 创建 ServerListener.java 服务器事件监听(观察者模式)
  - 10.2:创建 TomcatServer.java 服务器入口(单例模式)
  - 10.3: 创建 ServerFactory.java 服务器工厂(工厂模式)

- [x] Task 11: 创建示例Servlet
  - 11.1: 创建 HelloServlet.java 示例
  - 11.2: 创建 EchoServlet.java 示例

- [x] Task 12: 创建使用示例
  - 12.1: 创建 TomcatDemo.java 演示启动和使用

- [x] Task 13: 生成设计模式解析文档
  - 13.1: 创建 design_patterns.md 解释各设计模式的应用
