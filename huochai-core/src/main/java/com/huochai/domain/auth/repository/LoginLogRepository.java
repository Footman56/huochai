package com.huochai.domain.auth.repository;

import com.huochai.domain.auth.bean.LoginLog;

import java.util.List;

/**
 * 登录日志仓储接口
 *
 * @author huochai
 */
public interface LoginLogRepository {

    /**
     * 保存登录日志
     */
    LoginLog save(LoginLog loginLog);

    /**
     * 根据用户ID查询登录日志
     */
    List<LoginLog> findByUserId(Long userId);

    /**
     * 根据用户ID查询最近的登录日志
     */
    List<LoginLog> findRecentByUserId(Long userId, int limit);

    /**
     * 统计用户登录失败次数（指定时间范围内）
     */
    int countFailedLoginByUsername(String username, long startTime);
}