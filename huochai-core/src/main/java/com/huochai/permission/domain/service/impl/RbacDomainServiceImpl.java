package com.huochai.permission.domain.service.impl;

import com.huochai.common.enums.AuthConstants;
import com.huochai.permission.domain.service.RbacDomainService;
import com.huochai.permission.infrastructure.mapper.RoleMapper;
import com.huochai.permission.infrastructure.mapper.RolePermissionMapper;
import com.huochai.permission.infrastructure.mapper.UserRoleMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * RBAC 领域服务实现
 *
 * @author huochai
 */
@Slf4j
@Service
public class RbacDomainServiceImpl implements RbacDomainService {

    @Autowired
    private UserRoleMapper userRoleMapper;

    @Autowired
    private RolePermissionMapper rolePermissionMapper;

    @Autowired
    private RoleMapper roleMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 权限缓存过期时间（秒）
     */
    private static final long PERMISSION_CACHE_EXPIRE = 3600;

    @Override
    public List<String> loadUserPermissions(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }

        // 尝试从缓存获取
        String cacheKey = AuthConstants.PERMISSIONS_KEY_PREFIX + userId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof List) {
            log.debug("从缓存加载权限: userId={}", userId);
            return (List<String>) cached;
        }

        // 从数据库加载
        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        if (CollUtil.isEmpty(roleIds)) {
            return new ArrayList<>();
        }

        List<String> permissions = rolePermissionMapper.selectPermissionCodesByRoleIds(roleIds);
        if (permissions == null) {
            permissions = new ArrayList<>();
        }

        // 存入缓存
        redisTemplate.opsForValue().set(cacheKey, permissions, PERMISSION_CACHE_EXPIRE, TimeUnit.SECONDS);

        log.debug("从数据库加载权限: userId={}, permissions={}", userId, permissions);
        return permissions;
    }

    @Override
    public boolean hasPermission(Long userId, String permissionCode) {
        if (userId == null || StrUtil.isBlank(permissionCode)) {
            return false;
        }

        List<String> permissions = loadUserPermissions(userId);
        return permissions.contains(permissionCode);
    }

    @Override
    public boolean hasUrlPermission(Long userId, String url, String method) {
        if (userId == null || StrUtil.isBlank(url)) {
            return false;
        }

        // TODO: 实现URL匹配逻辑（支持通配符）
        // 这里可以扩展为更复杂的URL权限匹配
        List<String> permissions = loadUserPermissions(userId);

        // 简单实现：检查是否有对应URL的权限
        return permissions.stream().anyMatch(perm -> {
            // 这里可以根据权限编码和URL进行匹配
            return perm.contains(url) || url.contains(perm);
        });
    }

    @Override
    public void clearPermissionCache(Long userId) {
        if (userId != null) {
            String cacheKey = AuthConstants.PERMISSIONS_KEY_PREFIX + userId;
            redisTemplate.delete(cacheKey);
            log.debug("清除权限缓存: userId={}", userId);
        }
    }

    @Override
    public List<String> getUserRoleCodes(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }

        List<Long> roleIds = userRoleMapper.selectRoleIdsByUserId(userId);
        if (CollUtil.isEmpty(roleIds)) {
            return new ArrayList<>();
        }

        // 查询角色编码
        List<String> roleCodes = new ArrayList<>();
        for (Long roleId : roleIds) {
            var role = roleMapper.selectById(roleId);
            if (role != null && role.isValid()) {
                roleCodes.add(role.getRoleCode());
            }
        }

        return roleCodes;
    }
}