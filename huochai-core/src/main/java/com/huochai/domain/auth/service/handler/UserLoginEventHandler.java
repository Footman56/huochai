package com.huochai.domain.auth.service.handler;

import com.huochai.domain.auth.bean.event.UserLoginEvent;
import com.huochai.domain.auth.bean.DeviceInfo;
import com.huochai.domain.auth.bean.LoginLog;
import com.huochai.domain.auth.repository.LoginLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 用户登录事件处理器
 *
 * @author huochai
 */
@Slf4j
@Component
public class UserLoginEventHandler {

    @Autowired
    private LoginLogRepository loginLogRepository;

    /**
     * 处理用户登录事件 - 记录登录日志
     */
    @Async
    @EventListener
    public void handleUserLogin(UserLoginEvent event) {
        log.info("处理用户登录事件: userId={}, username={}, clientType={}", 
                event.getUserId(), event.getUsername(), 
                event.getDeviceInfo() != null ? event.getDeviceInfo().getClientType() : null);

        try {
            // 记录登录日志
            DeviceInfo deviceInfo = event.getDeviceInfo();
            if (deviceInfo == null) {
                deviceInfo = DeviceInfo.defaultDevice();
            }

            LoginLog loginLog = LoginLog.success(event.getUserId(), event.getUsername(), deviceInfo);
            loginLogRepository.save(loginLog);

            log.info("登录日志保存成功: userId={}, loginTime={}", event.getUserId(), loginLog.getLoginTime());

        } catch (Exception e) {
            log.error("保存登录日志失败: userId={}, error={}", event.getUserId(), e.getMessage(), e);
        }
    }
}