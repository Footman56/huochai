package com.huochai.auth.interfaces.controller;

import com.huochai.auth.application.dto.CaptchaResult;
import com.huochai.auth.application.service.AuthService;
import com.huochai.auth.application.service.CaptchaService;
import com.huochai.auth.interfaces.dto.*;
import com.huochai.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * @author huochai
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@Tag(name = "认证管理", description = "登录、登出、Token刷新等接口")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private CaptchaService captchaService;

    /**
     * 获取验证码
     */
    @GetMapping("/captcha")
    @Operation(summary = "获取验证码", description = "生成图形验证码")
    public Result<CaptchaResponse> getCaptcha() {
        CaptchaResult captchaResult = captchaService.generateCaptcha();
        CaptchaResponse response = new CaptchaResponse(captchaResult.getUuid(), captchaResult.getImage());
        return Result.success(response);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "通过用户名密码登录")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Result.success("登录成功", response);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "退出登录，将Token加入黑名单")
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authService.logout(token);
        }
        return Result.success("登出成功");
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新Token", description = "使用RefreshToken获取新的Token")
    public Result<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = authService.refreshToken(request);
        return Result.success("刷新成功", response);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/user-info")
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    public Result<UserInfoResponse> getUserInfo() {
        UserInfoResponse response = authService.getCurrentUserInfo();
        return Result.success(response);
    }
}