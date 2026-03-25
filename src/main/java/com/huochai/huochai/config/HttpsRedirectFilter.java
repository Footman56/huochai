package com.huochai.huochai.config;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 *
 *@author peilizhi 
 *@date 2026/3/25 23:16
 **/

//@Component
public class HttpsRedirectFilter implements Filter {

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) servletRequest;
        HttpServletResponse res = (HttpServletResponse) servletResponse;

        // 检查是否是 HTTP 请求
        if (!req.isSecure()) {
            System.out.println("HttpsRedirectFilter doFilter");
            String redirectUrl = "https://" + req.getServerName() +
                    ":" + 8443 +
                    req.getRequestURI();

            if (req.getQueryString() != null) {
                redirectUrl += "?" + req.getQueryString();
            }

            res.sendRedirect(redirectUrl);
            return;
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }
}
