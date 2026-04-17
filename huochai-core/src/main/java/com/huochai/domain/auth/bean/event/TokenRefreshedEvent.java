package com.huochai.domain.auth.bean.event;

import lombok.Getter;

/**
 * Token 刷新事件
 *
 * @author huochai
 */
@Getter
public class TokenRefreshedEvent extends DomainEvent {

    public static final String EVENT_TYPE = "token.refreshed";

    /**
     * 用户ID
     */
    private final Long userId;

    /**
     * 用户名
     */
    private final String username;

    /**
     * 新Token JTI
     */
    private final String newJti;

    /**
     * 旧Token JTI
     */
    private final String oldJti;

    public TokenRefreshedEvent(Long userId, String username, String newJti, String oldJti) {
        super(EVENT_TYPE);
        this.userId = userId;
        this.username = username;
        this.newJti = newJti;
        this.oldJti = oldJti;
    }
}