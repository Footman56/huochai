package com.huochai.auth.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷新Token请求 DTO
 *
 * @author huochai
 */
@Data
@Schema(description = "刷新Token请求")
public class RefreshTokenRequest {

    @NotBlank(message = "刷新Token不能为空")
    @Schema(description = "刷新Token")
    private String refreshToken;

    @Schema(description = "客户端类型", example = "WEB")
    private String clientType = "WEB";

    @Schema(description = "设备ID", example = "device-001")
    private String deviceId;
}