package com.huochai.huochai.config;

import org.apache.catalina.connector.Connector;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;

/**
 *
 *@author peilizhi 
 *@date 2026/3/24 19:03
 **/
//@Configuration
public class DualProtocolConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> servletContainerCustomizer() {
        return factory -> {
            // 创建 HTTP 连接器
            Connector httpConnector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
            httpConnector.setScheme("http");
            httpConnector.setPort(8080);  // HTTP 端口
            httpConnector.setSecure(false);

            // 可选：设置 HTTP 自动跳转到 HTTPS
            // httpConnector.setRedirectPort(8443);

            factory.addAdditionalTomcatConnectors(httpConnector);
        };
    }
}
