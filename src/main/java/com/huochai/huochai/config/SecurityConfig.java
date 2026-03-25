package com.huochai.huochai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 *
 *@author peilizhi 
 *@date 2026/3/25 19:19
 **/
//@Configuration
//@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .requiresChannel()
                .requestMatchers(r -> r.getRequestURI().startsWith("/health"))
                .requiresInsecure()  // health 端点允许 HTTP
                .anyRequest()
                .requiresSecure()    // 其他所有请求强制 HTTPS
                .and()
                .authorizeRequests()
                .anyRequest().permitAll();

        return http.build();
    }
}
