package com.huochai.auth.domain.event;

import com.huochai.auth.domain.model.DeviceInfo;
import lombok.Getter;

/**
 * 用户登录成功事件
 *
 * @author huochai
 */
@Getter
public class UserLoginEvent extends DomainEvent {

    public static final String EVENT_TYPE = "user.login";

    /**
     * 用户ID
     */
    private final Long userId;

    /**
     * 用户名
     */
    private final String username;

    /**
     * 设备信息
     */
    private final DeviceInfo deviceInfo;

    /**
     * 登录时间
     */
    private final Long loginTime;

    public UserLoginEvent(Long userId, String username, DeviceInfo deviceInfo) {
        super(EVENT_TYPE);
        this.userId = userId;
        this.username = username;
        this.deviceInfo = deviceInfo;
        this.loginTime = System.currentTimeMillis();
    }
}