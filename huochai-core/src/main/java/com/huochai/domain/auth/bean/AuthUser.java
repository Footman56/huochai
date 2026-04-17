package com.huochai.domain.auth.bean;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.huochai.common.exception.AuthErrorCode;
import com.huochai.common.exception.AuthException;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户聚合根 - 认证上下文
 * 
 * DDD 设计说明：
 * 1. 聚合根：AuthUser 是认证上下文的聚合根
 * 2. 领域行为：封装业务逻辑，保证业务规则一致性
 * 3. 领域验证：在领域模型中进行业务规则验证
 *
 * @author huochai
 */
@Slf4j
@Data
@TableName("sys_auth_user")
public class AuthUser {

    /**
     * 用户ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码（BCrypt加密）
     */
    private String password;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 状态: 1正常 0禁用
     */
    private Integer status;

    /**
     * 删除标记: 0未删除 1已删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // ========== 领域行为 - 密码相关 ==========

    /**
     * 验证密码
     */
    public boolean verifyPassword(String rawPassword, org.springframework.security.crypto.password.PasswordEncoder encoder) {
        log.info("[领域行为] 验证密码: userId={}, username={}", this.id, this.username);
        
        if (rawPassword == null || rawPassword.isEmpty()) {
            log.warn("[领域行为] 密码验证失败: 密码为空, userId={}", this.id);
            return false;
        }
        
        boolean result = encoder.matches(rawPassword, this.password);
        log.info("[领域行为] 密码验证结果: userId={}, result={}", this.id, result);
        
        return result;
    }

    /**
     * 更新密码
     */
    public void updatePassword(String encodedPassword) {
        log.info("[领域行为] 更新密码: userId={}, username={}", this.id, this.username);
        
        if (encodedPassword == null || encodedPassword.isEmpty()) {
            throw new AuthException(AuthErrorCode.BAD_REQUEST, "密码不能为空");
        }
        
        this.password = encodedPassword;
        this.updatedAt = LocalDateTime.now();
        
        log.info("[领域行为] 密码更新成功: userId={}", this.id);
    }

    // ========== 领域行为 - 状态管理 ==========

    /**
     * 禁用账号
     */
    public void disable() {
        log.info("[领域行为] 禁用账号: userId={}, username={}, currentStatus={}", 
                this.id, this.username, this.status);
        
        if (this.status != null && this.status == 0) {
            log.debug("[领域行为] 账号已是禁用状态: userId={}", this.id);
            return;
        }
        
        this.status = 0;
        this.updatedAt = LocalDateTime.now();
        
        log.info("[领域行为] 账号禁用成功: userId={}", this.id);
    }

    /**
     * 启用账号
     */
    public void enable() {
        log.info("[领域行为] 启用账号: userId={}, username={}, currentStatus={}", 
                this.id, this.username, this.status);
        
        if (this.status != null && this.status == 1) {
            log.debug("[领域行为] 账号已是启用状态: userId={}", this.id);
            return;
        }
        
        this.status = 1;
        this.updatedAt = LocalDateTime.now();
        
        log.info("[领域行为] 账号启用成功: userId={}", this.id);
    }

    // ========== 领域行为 - 账号验证 ==========

    /**
     * 检查账号是否有效
     */
    public boolean isAccountValid() {
        boolean statusValid = this.status != null && this.status == 1;
        boolean notDeleted = this.deleted == null || this.deleted == 0;
        boolean valid = statusValid && notDeleted;
        
        log.debug("[领域验证] 账号有效性检查: userId={}, status={}, deleted={}, valid={}", 
                this.id, this.status, this.deleted, valid);
        
        return valid;
    }

    /**
     * 检查账号是否被禁用
     */
    public boolean isDisabled() {
        boolean disabled = this.status != null && this.status == 0;
        log.debug("[领域验证] 账号禁用检查: userId={}, disabled={}", this.id, disabled);
        return disabled;
    }

    /**
     * 检查账号是否被删除
     */
    public boolean isDeleted() {
        boolean deleted = this.deleted != null && this.deleted == 1;
        log.debug("[领域验证] 账号删除检查: userId={}, deleted={}", this.id, deleted);
        return deleted;
    }

    // ========== 领域行为 - 信息更新 ==========

    /**
     * 更新邮箱
     */
    public void updateEmail(String email) {
        log.info("[领域行为] 更新邮箱: userId={}, oldEmail={}, newEmail={}", 
                this.id, this.email, email);
        
        this.email = email;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新手机号
     */
    public void updatePhone(String phone) {
        log.info("[领域行为] 更新手机号: userId={}, oldPhone={}, newPhone={}", 
                this.id, this.phone, phone);
        
        this.phone = phone;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新头像
     */
    public void updateAvatar(String avatar) {
        log.info("[领域行为] 更新头像: userId={}", this.id);
        
        this.avatar = avatar;
        this.updatedAt = LocalDateTime.now();
    }

    // ========== 领域验证 - 业务规则 ==========

    /**
     * 验证账号可以登录
     * 如果验证失败抛出异常
     */
    public void validateCanLogin() {
        log.debug("[领域验证] 验证账号可登录: userId={}, username={}", this.id, this.username);
        
        // 状态分支：检查是否被禁用
        if (this.isDisabled()) {
            log.warn("[领域验证] 登录失败-账号被禁用: userId={}, username={}", this.id, this.username);
            throw new AuthException(AuthErrorCode.ACCOUNT_DISABLED, "账号已被禁用，请联系管理员");
        }
        
        // 状态分支：检查是否被删除
        if (this.isDeleted()) {
            log.warn("[领域验证] 登录失败-账号已删除: userId={}, username={}", this.id, this.username);
            throw new AuthException(AuthErrorCode.ACCOUNT_LOCKED, "账号不存在");
        }
        
        // 状态分支：检查状态是否正常
        if (this.status == null || this.status != 1) {
            log.warn("[领域验证] 登录失败-账号状态异常: userId={}, status={}", this.id, this.status);
            throw new AuthException(AuthErrorCode.ACCOUNT_LOCKED, "账号状态异常");
        }
        
        log.debug("[领域验证] 账号验证通过: userId={}", this.id);
    }

    /**
     * 验证密码正确性
     * 如果验证失败抛出异常
     */
    public void validatePassword(String rawPassword, 
                                  org.springframework.security.crypto.password.PasswordEncoder encoder) {
        log.debug("[领域验证] 验证密码: userId={}", this.id);
        
        if (!this.verifyPassword(rawPassword, encoder)) {
            log.warn("[领域验证] 密码验证失败: userId={}, username={}", this.id, this.username);
            throw new AuthException(AuthErrorCode.PASSWORD_ERROR, "用户名或密码错误");
        }
        
        log.debug("[领域验证] 密码验证通过: userId={}", this.id);
    }

    // ========== 静态工厂方法 ==========

    /**
     * 创建新用户
     */
    public static AuthUser create(String username, String encodedPassword, String email, String phone) {
        log.info("[领域行为] 创建新用户: username={}, email={}, phone={}", username, email, phone);
        
        AuthUser user = new AuthUser();
        user.setUsername(username);
        user.setPassword(encodedPassword);
        user.setEmail(email);
        user.setPhone(phone);
        user.setStatus(1);
        user.setDeleted(0);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        return user;
    }
}