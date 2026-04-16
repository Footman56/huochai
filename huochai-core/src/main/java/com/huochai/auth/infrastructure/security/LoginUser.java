package com.huochai.auth.infrastructure.security;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 登录用户详情
 *
 * @author huochai
 */
@Data
public class LoginUser implements UserDetails {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 权限列表
     */
    private List<String> permissions;

    /**
     * 角色列表
     */
    private List<String> roles;

    /**
     * 客户端类型
     */
    private String clientType;

    /**
     * 设备ID
     */
    private String deviceId;

    /**
     * Token
     */
    private String token;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 将权限和角色转换为 GrantedAuthority
        return permissions.stream()
                .map(permission -> (GrantedAuthority) () -> permission)
                .toList();
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.status == null || this.status == 1;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.status == null || this.status == 1;
    }

    /**
     * 检查是否有某个权限
     */
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    /**
     * 检查是否有某个角色
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}