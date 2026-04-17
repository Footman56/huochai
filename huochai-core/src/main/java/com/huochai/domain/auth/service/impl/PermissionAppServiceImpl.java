package com.huochai.domain.auth.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huochai.domain.auth.service.PermissionAppService;
import com.huochai.domain.permission.bean.Permission;
import com.huochai.domain.permission.bean.Role;
import com.huochai.domain.permission.bean.RolePermission;
import com.huochai.domain.permission.bean.UserRole;
import com.huochai.domain.permission.service.RbacDomainService;
import com.huochai.domain.permission.repository.mapper.PermissionMapper;
import com.huochai.domain.permission.repository.mapper.RoleMapper;
import com.huochai.domain.permission.repository.mapper.RolePermissionMapper;
import com.huochai.domain.permission.repository.mapper.UserRoleMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限应用服务实现
 *
 * @author huochai
 */
@Slf4j
@Service
public class PermissionAppServiceImpl implements PermissionAppService {

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private PermissionMapper permissionMapper;

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Autowired
    private RbacDomainService rbacDomainService;

    // ========== 角色管理 ==========

    @Override
    public List<Role> getAllRoles() {
        return roleMapper.selectList(new LambdaQueryWrapper<Role>().orderByAsc(Role::getSortOrder));
    }

    @Override
    public Role getRoleById(Long id) {
        return roleMapper.selectById(id);
    }

    @Override
    public Role createRole(Role role) {
        roleMapper.insert(role);
        return role;
    }

    @Override
    public Role updateRole(Role role) {
        roleMapper.updateById(role);
        return role;
    }

    @Override
    public void deleteRole(Long id) {
        roleMapper.deleteById(id);
        // 删除角色权限关联
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissionsToRole(Long roleId, List<Long> permissionIds) {
        // 删除旧关联
        rolePermissionMapper.delete(new LambdaQueryWrapper<RolePermission>().eq(RolePermission::getRoleId, roleId));

        // 创建新关联
        if (CollUtil.isNotEmpty(permissionIds)) {
            for (Long permissionId : permissionIds) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(roleId);
                rp.setPermissionId(permissionId);
                rolePermissionMapper.insert(rp);
            }
        }
        
        log.info("分配权限到角色: roleId={}, permissionIds={}", roleId, permissionIds);
    }

    // ========== 权限管理 ==========

    @Override
    public List<Permission> getAllPermissions() {
        return permissionMapper.selectList(new LambdaQueryWrapper<Permission>().orderByAsc(Permission::getSortOrder));
    }

    @Override
    public Permission getPermissionById(Long id) {
        return permissionMapper.selectById(id);
    }

    @Override
    public Permission createPermission(Permission permission) {
        permissionMapper.insert(permission);
        return permission;
    }

    @Override
    public Permission updatePermission(Permission permission) {
        permissionMapper.updateById(permission);
        return permission;
    }

    @Override
    public void deletePermission(Long id) {
        permissionMapper.deleteById(id);
    }

    // ========== 用户角色管理 ==========

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRolesToUser(Long userId, List<Long> roleIds) {
        // 删除旧关联
        userRoleMapper.delete(new LambdaQueryWrapper<UserRole>().eq(UserRole::getUserId, userId));

        // 创建新关联
        if (CollUtil.isNotEmpty(roleIds)) {
            for (Long roleId : roleIds) {
                UserRole ur = new UserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                userRoleMapper.insert(ur);
            }
        }

        // 清除权限缓存
        rbacDomainService.clearPermissionCache(userId);

        log.info("分配角色到用户: userId={}, roleIds={}", userId, roleIds);
    }

    @Override
    public List<Role> getUserRoles(Long userId) {
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        if (CollUtil.isEmpty(roleIds)) {
            return new ArrayList<>();
        }
        return roleMapper.selectBatchIds(roleIds);
    }
}