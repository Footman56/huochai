package com.huochai.auth.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求 DTO
 *
 * @author huochai
 */
@Data
@Schema(description = "登录请求")
public class LoginRequest {

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "用户名", example = "admin")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "admin123")
    private String password;

    @Schema(description = "客户端类型", example = "WEB")
    private String clientType = "WEB";

    @Schema(description = "设备ID", example = "device-001")
    private String deviceId;

    @Schema(description = "验证码UUID")
    private String captchaUuid;

    @Schema(description = "验证码")
    private String captchaCode;
}