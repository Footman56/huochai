package com.huochai.domain.auth.service;

import com.huochai.domain.permission.bean.Permission;
import com.huochai.domain.permission.bean.Role;

import java.util.List;

/**
 * 权限应用服务接口
 *
 * @author huochai
 */
public interface PermissionAppService {

    // ========== 角色管理 ==========

    /**
     * 获取所有角色
     */
    List<Role> getAllRoles();

    /**
     * 根据ID获取角色
     */
    Role getRoleById(Long id);

    /**
     * 创建角色
     */
    Role createRole(Role role);

    /**
     * 更新角色
     */
    Role updateRole(Role role);

    /**
     * 删除角色
     */
    void deleteRole(Long id);

    /**
     * 给角色分配权限
     */
    void assignPermissionsToRole(Long roleId, List<Long> permissionIds);

    // ========== 权限管理 ==========

    /**
     * 获取所有权限
     */
    List<Permission> getAllPermissions();

    /**
     * 根据ID获取权限
     */
    Permission getPermissionById(Long id);

    /**
     * 创建权限
     */
    Permission createPermission(Permission permission);

    /**
     * 更新权限
     */
    Permission updatePermission(Permission permission);

    /**
     * 删除权限
     */
    void deletePermission(Long id);

    // ========== 用户角色管理 ==========

    /**
     * 给用户分配角色
     */
    void assignRolesToUser(Long userId, List<Long> roleIds);

    /**
     * 获取用户的角色列表
     */
    List<Role> getUserRoles(Long userId);
}