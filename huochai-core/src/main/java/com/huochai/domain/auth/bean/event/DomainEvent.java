package com.huochai.domain.auth.bean.event;

import java.time.LocalDateTime;

/**
 * 领域事件基类
 *
 * @author huochai
 */
public abstract class DomainEvent {

    /**
     * 事件ID
     */
    private final String eventId;

    /**
     * 事件发生时间
     */
    private final LocalDateTime occurredOn;

    /**
     * 事件类型
     */
    private final String eventType;

    protected DomainEvent(String eventType) {
        this.eventId = java.util.UUID.randomUUID().toString();
        this.occurredOn = LocalDateTime.now();
        this.eventType = eventType;
    }

    public String getEventId() {
        return eventId;
    }

    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    public String getEventType() {
        return eventType;
    }
}