package com.huochai.auth.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huochai.auth.domain.model.LoginLog;
import com.huochai.auth.domain.repository.LoginLogRepository;
import com.huochai.auth.infrastructure.mapper.LoginLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 登录日志仓储实现
 *
 * @author huochai
 */
@Slf4j
@Repository
public class LoginLogRepositoryImpl implements LoginLogRepository {

    @Autowired
    private LoginLogMapper loginLogMapper;

    @Override
    public LoginLog save(LoginLog loginLog) {
        loginLogMapper.insert(loginLog);
        return loginLog;
    }

    @Override
    public List<LoginLog> findByUserId(Long userId) {
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LoginLog::getUserId, userId)
                .orderByDesc(LoginLog::getLoginTime);
        return loginLogMapper.selectList(wrapper);
    }

    @Override
    public List<LoginLog> findRecentByUserId(Long userId, int limit) {
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LoginLog::getUserId, userId)
                .orderByDesc(LoginLog::getLoginTime)
                .last("LIMIT " + limit);
        return loginLogMapper.selectList(wrapper);
    }

    @Override
    public int countFailedLoginByUsername(String username, long startTime) {
        LocalDateTime startDateTime = LocalDateTime.ofEpochSecond(startTime / 1000, 0, null);
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LoginLog::getUsername, username)
                .eq(LoginLog::getLoginResult, 0)
                .ge(LoginLog::getLoginTime, startDateTime);
        return Math.toIntExact(loginLogMapper.selectCount(wrapper));
    }
}