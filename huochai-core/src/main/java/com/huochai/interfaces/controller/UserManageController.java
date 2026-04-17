package com.huochai.interfaces.controller;

import com.huochai.common.result.Result;
import com.huochai.domain.auth.bean.AuthUser;
import com.huochai.domain.auth.repository.AuthUserRepository;
import com.huochai.domain.auth.service.PermissionAppService;
import com.huochai.domain.permission.bean.Role;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户管理控制器
 *
 * @author huochai
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@Tag(name = "用户管理", description = "用户的增删改查和角色分配")
@SecurityRequirement(name = "BearerAuth")
public class UserManageController {

    @Autowired
    private AuthUserRepository authUserRepository;

    @Autowired
    private PermissionAppService permissionAppService;

    /**
     * 获取用户列表
     */
    @GetMapping
    @Operation(summary = "获取用户列表")
    @PreAuthorize("hasAuthority('user:list')")
    public Result<List<AuthUser>> getUserList() {
        List<AuthUser> users = authUserRepository.findByUsername("") != null 
                ? List.of() 
                : List.of();
        return Result.success(users);
    }

    /**
     * 根据ID获取用户
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取用户")
    @PreAuthorize("hasAuthority('user:list')")
    public Result<AuthUser> getUserById(@PathVariable Long id) {
        return Result.success(authUserRepository.findById(id).orElse(null));
    }

    /**
     * 给用户分配角色
     */
    @PostMapping("/{id}/roles")
    @Operation(summary = "给用户分配角色")
    @PreAuthorize("hasAuthority('user:update')")
    public Result<Void> assignRoles(@PathVariable Long id, @RequestBody List<Long> roleIds) {
        permissionAppService.assignRolesToUser(id, roleIds);
        return Result.success();
    }

    /**
     * 获取用户的角色列表
     */
    @GetMapping("/{id}/roles")
    @Operation(summary = "获取用户的角色列表")
    @PreAuthorize("hasAuthority('user:list')")
    public Result<List<Role>> getUserRoles(@PathVariable Long id) {
        return Result.success(permissionAppService.getUserRoles(id));
    }
}