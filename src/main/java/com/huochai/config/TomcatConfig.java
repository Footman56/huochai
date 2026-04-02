package com.huochai.config;

import org.apache.catalina.connector.Connector;
import org.apache.catalina.valves.RemoteIpValve;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;

/**
 *
 *@author peilizhi 
 *@date 2026/3/25 20:23
 **/
//@Configuration
public class TomcatConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> servletContainerCustomizer() {
        return factory -> {
            // 创建 HTTP 连接器
            Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
            connector.setScheme("http");
            connector.setPort(8080);
            connector.setSecure(false);
            connector.setRedirectPort(8443);

            // 添加阀门实现自动重定向
            RemoteIpValve remoteIpValve = new RemoteIpValve();
            remoteIpValve.setProtocolHeader("X-Forwarded-Proto");
            factory.addEngineValves(remoteIpValve);

            factory.addAdditionalTomcatConnectors(connector);

        };
    }
}
