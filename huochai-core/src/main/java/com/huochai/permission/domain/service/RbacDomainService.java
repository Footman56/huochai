package com.huochai.permission.domain.service;

import java.util.List;

/**
 * RBAC 领域服务接口
 *
 * @author huochai
 */
public interface RbacDomainService {

    /**
     * 加载用户权限列表
     */
    List<String> loadUserPermissions(Long userId);

    /**
     * 检查用户是否有某个权限
     */
    boolean hasPermission(Long userId, String permissionCode);

    /**
     * 检查URL访问权限
     */
    boolean hasUrlPermission(Long userId, String url, String method);

    /**
     * 清除用户权限缓存
     */
    void clearPermissionCache(Long userId);

    /**
     * 获取用户角色编码列表
     */
    List<String> getUserRoleCodes(Long userId);
}