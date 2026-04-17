package com.huochai.domain.auth.service;

import com.huochai.domain.auth.bean.CaptchaResult;

/**
 * 验证码服务接口
 *
 * @author huochai
 */
public interface CaptchaService {

    /**
     * 生成验证码
     */
    CaptchaResult generateCaptcha();

    /**
     * 验证验证码
     */
    boolean verifyCaptcha(String uuid, String code);

    /**
     * 删除验证码
     */
    void deleteCaptcha(String uuid);
}