package com.huochai.interfaces.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huochai.domain.auth.bean.LoginLog;
import com.huochai.domain.auth.repository.mapper.LoginLogMapper;
import com.huochai.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 登录日志控制器
 *
 * @author huochai
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/login-logs")
@Tag(name = "登录日志", description = "登录日志查询")
@SecurityRequirement(name = "BearerAuth")
public class LoginLogController {

    @Autowired
    private LoginLogMapper loginLogMapper;

    /**
     * 获取登录日志列表
     */
    @GetMapping
    @Operation(summary = "获取登录日志列表")
    @PreAuthorize("hasAuthority('login-log:list')")
    public Result<List<LoginLog>> getLoginLogList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        
        Page<LoginLog> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(LoginLog::getLoginTime);
        
        Page<LoginLog> result = loginLogMapper.selectPage(pageParam, wrapper);
        return Result.success(result.getRecords());
    }

    /**
     * 根据用户ID获取登录日志
     */
    @GetMapping("/user/{userId}")
    @Operation(summary = "根据用户ID获取登录日志")
    @PreAuthorize("hasAuthority('login-log:list')")
    public Result<List<LoginLog>> getLoginLogsByUserId(@PathVariable Long userId) {
        LambdaQueryWrapper<LoginLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(LoginLog::getUserId, userId)
                .orderByDesc(LoginLog::getLoginTime)
                .last("LIMIT 50");
        
        return Result.success(loginLogMapper.selectList(wrapper));
    }
}