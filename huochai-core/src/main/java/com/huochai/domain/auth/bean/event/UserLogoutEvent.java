package com.huochai.domain.auth.bean.event;

import lombok.Getter;

/**
 * 用户登出事件
 *
 * @author huochai
 */
@Getter
public class UserLogoutEvent extends DomainEvent {

    public static final String EVENT_TYPE = "user.logout";

    /**
     * 用户ID
     */
    private final Long userId;

    /**
     * 用户名
     */
    private final String username;

    /**
     * 客户端类型
     */
    private final String clientType;

    /**
     * 设备ID
     */
    private final String deviceId;

    public UserLogoutEvent(Long userId, String username, String clientType, String deviceId) {
        super(EVENT_TYPE);
        this.userId = userId;
        this.username = username;
        this.clientType = clientType;
        this.deviceId = deviceId;
    }
}