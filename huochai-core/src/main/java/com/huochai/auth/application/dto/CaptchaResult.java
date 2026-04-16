package com.huochai.auth.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 验证码结果
 *
 * @author huochai
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaResult {

    /**
     * 验证码唯一标识
     */
    private String uuid;

    /**
     * 验证码图片（Base64编码）
     */
    private String image;

    /**
     * 验证码值（仅用于测试环境）
     */
    private String code;
}