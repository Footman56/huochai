package com.huochai.domain.auth.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huochai.domain.auth.bean.AuthUser;
import com.huochai.domain.auth.repository.AuthUserRepository;
import com.huochai.domain.auth.repository.mapper.AuthUserMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户仓储实现
 *
 * @author huochai
 */
@Repository
public class AuthUserRepositoryImpl implements AuthUserRepository {

    @Autowired
    private AuthUserMapper authUserMapper;

    @Override
    public Optional<AuthUser> findById(Long id) {
        return Optional.ofNullable(authUserMapper.selectById(id));
    }

    @Override
    public Optional<AuthUser> findByUsername(String username) {
        LambdaQueryWrapper<AuthUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuthUser::getUsername, username);
        return Optional.ofNullable(authUserMapper.selectOne(wrapper));
    }

    @Override
    public Optional<AuthUser> findByPhone(String phone) {
        LambdaQueryWrapper<AuthUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuthUser::getPhone, phone);
        return Optional.ofNullable(authUserMapper.selectOne(wrapper));
    }

    @Override
    public Optional<AuthUser> findByEmail(String email) {
        LambdaQueryWrapper<AuthUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuthUser::getEmail, email);
        return Optional.ofNullable(authUserMapper.selectOne(wrapper));
    }

    @Override
    public AuthUser save(AuthUser user) {
        authUserMapper.insert(user);
        return user;
    }

    @Override
    public AuthUser update(AuthUser user) {
        authUserMapper.updateById(user);
        return user;
    }

    @Override
    public void deleteById(Long id) {
        authUserMapper.deleteById(id);
    }

    @Override
    public boolean existsByUsername(String username) {
        LambdaQueryWrapper<AuthUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AuthUser::getUsername, username);
        return authUserMapper.selectCount(wrapper) > 0;
    }
}