package com.huochai.domain.auth.bean.event;

import lombok.Getter;

/**
 * 用户被踢出事件
 *
 * @author huochai
 */
@Getter
public class UserKickedOutEvent extends DomainEvent {

    public static final String EVENT_TYPE = "user.kicked";

    /**
     * 用户ID
     */
    private final Long userId;

    /**
     * 客户端类型
     */
    private final String clientType;

    /**
     * 设备ID
     */
    private final String deviceId;

    /**
     * 踢出原因
     */
    private final String reason;

    public UserKickedOutEvent(Long userId, String clientType, String deviceId, String reason) {
        super(EVENT_TYPE);
        this.userId = userId;
        this.clientType = clientType;
        this.deviceId = deviceId;
        this.reason = reason;
    }
}