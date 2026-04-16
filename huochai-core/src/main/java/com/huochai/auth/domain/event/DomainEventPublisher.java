package com.huochai.auth.domain.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 领域事件发布器
 *
 * @author huochai
 */
@Slf4j
@Component
public class DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public DomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * 发布领域事件
     */
    public void publish(DomainEvent event) {
        log.info("发布领域事件: type={}, eventId={}", event.getEventType(), event.getEventId());
        applicationEventPublisher.publishEvent(event);
    }

    /**
     * 发布用户登录事件
     */
    public void publishUserLogin(Long userId, String username, com.huochai.auth.domain.model.DeviceInfo deviceInfo) {
        publish(new UserLoginEvent(userId, username, deviceInfo));
    }

    /**
     * 发布用户登出事件
     */
    public void publishUserLogout(Long userId, String username, String clientType, String deviceId) {
        publish(new UserLogoutEvent(userId, username, clientType, deviceId));
    }

    /**
     * 发布用户被踢出事件
     */
    public void publishUserKickedOut(Long userId, String clientType, String deviceId, String reason) {
        publish(new UserKickedOutEvent(userId, clientType, deviceId, reason));
    }

    /**
     * 发布Token刷新事件
     */
    public void publishTokenRefreshed(Long userId, String username, String newJti, String oldJti) {
        publish(new TokenRefreshedEvent(userId, username, newJti, oldJti));
    }
}