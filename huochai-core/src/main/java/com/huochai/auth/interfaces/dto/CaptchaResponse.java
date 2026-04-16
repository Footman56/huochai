package com.huochai.auth.interfaces.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证码响应 DTO
 *
 * @author huochai
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "验证码响应")
public class CaptchaResponse {

    @Schema(description = "验证码UUID")
    private String uuid;

    @Schema(description = "验证码图片（Base64）")
    private String image;
}