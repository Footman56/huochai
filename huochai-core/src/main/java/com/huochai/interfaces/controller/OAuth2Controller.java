package com.huochai.interfaces.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuth2 控制器
 * 
 * 提供：
 * 1. 授权登录页面
 * 2. 授权同意页面
 * 3. OIDC UserInfo 端点
 *
 * @author huochai
 */
@Controller
public class OAuth2Controller {

    /**
     * 授权登录页面
     */
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    /**
     * OIDC UserInfo 端点
     * 返回标准 OIDC 用户信息
     */
    @GetMapping("/userinfo")
    @ResponseBody
    public Map<String, Object> userInfo(@AuthenticationPrincipal OAuth2User user) {
        Map<String, Object> userInfo = new HashMap<>();
        
        if (user != null) {
            // 标准OIDC字段
            userInfo.put("sub", user.getAttribute("sub"));
            userInfo.put("name", user.getAttribute("name"));
            userInfo.put("preferred_username", user.getAttribute("preferred_username"));
            userInfo.put("email", user.getAttribute("email"));
            userInfo.put("email_verified", user.getAttribute("email_verified"));
            userInfo.put("phone_number", user.getAttribute("phone_number"));
            
            // 自定义扩展字段
            userInfo.put("roles", user.getAttribute("roles"));
            userInfo.put("permissions", user.getAttribute("permissions"));
        }
        
        return userInfo;
    }

    /**
     * 授权同意页面
     */
    @GetMapping("/oauth2/consent")
    public String consentPage() {
        return "consent";
    }
}