package com.huochai.permission.interfaces.controller;

import com.huochai.common.result.Result;
import com.huochai.permission.application.service.PermissionAppService;
import com.huochai.permission.domain.model.Permission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理控制器
 *
 * @author huochai
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/permissions")
@Tag(name = "权限管理", description = "权限的增删改查")
@SecurityRequirement(name = "BearerAuth")
public class PermissionController {

    @Autowired
    private PermissionAppService permissionAppService;

    /**
     * 获取所有权限
     */
    @GetMapping
    @Operation(summary = "获取所有权限")
    @PreAuthorize("hasAuthority('permission:list')")
    public Result<List<Permission>> getAllPermissions() {
        return Result.success(permissionAppService.getAllPermissions());
    }

    /**
     * 根据ID获取权限
     */
    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取权限")
    @PreAuthorize("hasAuthority('permission:list')")
    public Result<Permission> getPermissionById(@PathVariable Long id) {
        return Result.success(permissionAppService.getPermissionById(id));
    }

    /**
     * 创建权限
     */
    @PostMapping
    @Operation(summary = "创建权限")
    @PreAuthorize("hasAuthority('role:update')") // 需要角色管理权限
    public Result<Permission> createPermission(@RequestBody Permission permission) {
        return Result.success(permissionAppService.createPermission(permission));
    }

    /**
     * 更新权限
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新权限")
    @PreAuthorize("hasAuthority('role:update')")
    public Result<Permission> updatePermission(@PathVariable Long id, @RequestBody Permission permission) {
        permission.setId(id);
        return Result.success(permissionAppService.updatePermission(permission));
    }

    /**
     * 删除权限
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除权限")
    @PreAuthorize("hasAuthority('role:update')")
    public Result<Void> deletePermission(@PathVariable Long id) {
        permissionAppService.deletePermission(id);
        return Result.success();
    }
}