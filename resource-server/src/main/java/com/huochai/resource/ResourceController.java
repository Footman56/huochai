package com.huochai.resource;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ResourceController {

    /**
     * 公开接口 - 无需认证
     */
    @GetMapping("/public/hello")
    public Map<String, Object> publicHello() {
        return Map.of(
            "message", "Hello from public endpoint",
            "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 受保护接口 - 需要认证
     */
    @GetMapping("/protected/hello")
    public Map<String, Object> protectedHello(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
            "message", "Hello from protected endpoint",
            "subject", jwt.getSubject(),
            "claims", jwt.getClaims()
        );
    }

    /**
     * 用户接口 - 需要 USER 角色
     */
    @GetMapping("/user/profile")
    public Map<String, Object> userProfile(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
            "username", jwt.getSubject(),
            "email", jwt.getClaimAsString("email"),
            "roles", jwt.getClaimAsStringList("roles")
        );
    }

    /**
     * 读取消息接口 - 需要 message.read 权限
     */
    @GetMapping("/message/read")
    public Map<String, Object> readMessage(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
            "message", "This is a read message",
            "user", jwt.getSubject()
        );
    }

    /**
     * 写入消息接口 - 需要 message.write 权限
     */
    @GetMapping("/message/write")
    public Map<String, Object> writeMessage(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
            "message", "Message written successfully",
            "user", jwt.getSubject()
        );
    }

    /**
     * 带路径参数的受保护接口
     */
    @GetMapping("/protected/{id}")
    public Map<String, Object> getById(@PathVariable String id, @AuthenticationPrincipal Jwt jwt) {
        return Map.of(
            "id", id,
            "message", "Resource with ID: " + id,
            "requestedBy", jwt.getSubject()
        );
    }

    /**
     * 获取当前 Token 信息
     */
    @GetMapping("/token/info")
    public Map<String, Object> tokenInfo(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
            "issuer", jwt.getIssuer().toString(),
            "subject", jwt.getSubject(),
            "expiresAt", jwt.getExpiresAt(),
            "issuedAt", jwt.getIssuedAt(),
            "claims", jwt.getClaims()
        );
    }
}