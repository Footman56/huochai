package com.huochai.auth.domain.event.handler;

import com.huochai.auth.domain.event.UserKickedOutEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 用户被踢出事件处理器
 *
 * @author huochai
 */
@Slf4j
@Component
public class UserKickedOutEventHandler {

    /**
     * 处理用户被踢出事件
     */
    @Async
    @EventListener
    public void handleUserKickedOut(UserKickedOutEvent event) {
        log.warn("用户被踢出: userId={}, clientType={}, deviceId={}, reason={}", 
                event.getUserId(), event.getClientType(), event.getDeviceId(), event.getReason());

        // TODO: 可以在这里实现通知逻辑
        // 例如：推送消息给被踢出的客户端
        // 例如：记录安全审计日志
    }
}