package com.huochai.huochai.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;

/**
 *
 *@author peilizhi 
 *@date 2026/3/25 23:18
 **/
//@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<HttpsRedirectFilter> httpsRedirectFilter() {
        FilterRegistrationBean<HttpsRedirectFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new HttpsRedirectFilter());
        registration.addUrlPatterns("/*");
        registration.setOrder(1);
        return registration;
    }
}
