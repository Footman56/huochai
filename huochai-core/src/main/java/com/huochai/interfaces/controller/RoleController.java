package com.huochai.interfaces.controller;

import com.huochai.common.result.Result;
import com.huochai.domain.auth.service.PermissionAppService;
import com.huochai.domain.permission.bean.Role;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * 角色管理控制器
 *
 * @author huochai
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/roles")
@Tag(name = "角色管理", description = "角色的增删改查和权限分配")
@SecurityRequirement(name = "BearerAuth")
public class RoleController {

    @Autowired
    private PermissionAppService permissionAppService;

    /**
     * 获取所有角色
     */
    @GetMapping
    @Operation(summary = "获取所有角色")
    @PreAuthorize("hasAuthority('role:list')")
    public Result<List<Role>> getAllRoles() {
        return Result.success(permissionAppService.getAllRoles());
    }

    /**
     * 根据ID获取角色
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取角色")
    @PreAuthorize("hasAuthority('role:list')")
    public Result<Role> getRoleById(@PathVariable Long id) {
        return Result.success(permissionAppService.getRoleById(id));
    }

    /**
     * 创建角色
     */
    @PostMapping
    @Operation(summary = "创建角色")
    @PreAuthorize("hasAuthority('role:create')")
    public Result<Role> createRole(@RequestBody Role role) {
        return Result.success(permissionAppService.createRole(role));
    }

    /**
     * 更新角色
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新角色")
    @PreAuthorize("hasAuthority('role:update')")
    public Result<Role> updateRole(@PathVariable Long id, @RequestBody Role role) {
        role.setId(id);
        return Result.success(permissionAppService.updateRole(role));
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除角色")
    @PreAuthorize("hasAuthority('role:delete')")
    public Result<Void> deleteRole(@PathVariable Long id) {
        permissionAppService.deleteRole(id);
        return Result.success();
    }

    /**
     * 给角色分配权限
     */
    @PostMapping("/{id}/permissions")
    @Operation(summary = "给角色分配权限")
    @PreAuthorize("hasAuthority('role:update')")
    public Result<Void> assignPermissions(@PathVariable Long id, @RequestBody List<Long> permissionIds) {
        permissionAppService.assignPermissionsToRole(id, permissionIds);
        return Result.success();
    }
}