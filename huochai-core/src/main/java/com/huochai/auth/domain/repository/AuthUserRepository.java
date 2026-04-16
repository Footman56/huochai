package com.huochai.auth.domain.repository;

import com.huochai.auth.domain.model.AuthUser;

import java.util.Optional;

/**
 * 用户仓储接口
 *
 * @author huochai
 */
public interface AuthUserRepository {

    /**
     * 根据ID查询用户
     */
    Optional<AuthUser> findById(Long id);

    /**
     * 根据用户名查询用户
     */
    Optional<AuthUser> findByUsername(String username);

    /**
     * 根据手机号查询用户
     */
    Optional<AuthUser> findByPhone(String phone);

    /**
     * 根据邮箱查询用户
     */
    Optional<AuthUser> findByEmail(String email);

    /**
     * 保存用户
     */
    AuthUser save(AuthUser user);

    /**
     * 更新用户
     */
    AuthUser update(AuthUser user);

    /**
     * 删除用户
     */
    void deleteById(Long id);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);
}