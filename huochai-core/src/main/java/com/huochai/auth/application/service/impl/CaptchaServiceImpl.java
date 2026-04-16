package com.huochai.auth.application.service.impl;

import cn.hutool.core.util.IdUtil;
import com.huochai.auth.application.dto.CaptchaResult;
import com.huochai.auth.application.service.CaptchaService;
import com.huochai.common.enums.AuthConstants;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 验证码服务实现
 *
 * @author huochai
 */
@Slf4j
@Service
public class CaptchaServiceImpl implements CaptchaService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public CaptchaResult generateCaptcha() {
        // 生成UUID
        String uuid = IdUtil.fastSimpleUUID();

        // 生成验证码（130x48，4位字符）
        SpecCaptcha captcha = new SpecCaptcha(130, 48, 4);
        captcha.setCharType(Captcha.TYPE_DEFAULT);

        // 获取验证码文本
        String code = captcha.text().toLowerCase();

        // 存储到Redis
        String key = AuthConstants.CAPTCHA_KEY_PREFIX + uuid;
        redisTemplate.opsForValue().set(key, code, AuthConstants.CAPTCHA_EXPIRE_SECONDS, TimeUnit.SECONDS);

        // 返回结果
        CaptchaResult result = new CaptchaResult();
        result.setUuid(uuid);
        result.setImage(captcha.toBase64());

        log.debug("生成验证码: uuid={}, code={}", uuid, code);

        return result;
    }

    @Override
    public boolean verifyCaptcha(String uuid, String code) {
        if (uuid == null || code == null) {
            return false;
        }

        String key = AuthConstants.CAPTCHA_KEY_PREFIX + uuid;
        Object storedCode = redisTemplate.opsForValue().get(key);

        if (storedCode == null) {
            return false;
        }

        // 验证后删除（一次性使用）
        redisTemplate.delete(key);

        boolean result = code.toLowerCase().equals(storedCode.toString().toLowerCase());
        log.debug("验证验证码: uuid={}, result={}", uuid, result);

        return result;
    }

    @Override
    public void deleteCaptcha(String uuid) {
        String key = AuthConstants.CAPTCHA_KEY_PREFIX + uuid;
        redisTemplate.delete(key);
    }
}