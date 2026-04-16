package com.huochai.permission.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huochai.permission.domain.model.RolePermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色权限 Mapper
 *
 * @author huochai
 */
@Mapper
public interface RolePermissionMapper extends BaseMapper<RolePermission> {

    /**
     * 根据角色ID查询权限ID列表
     */
    List<Long> selectPermissionIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据角色ID列表查询权限编码列表
     */
    List<String> selectPermissionCodesByRoleIds(@Param("roleIds") List<Long> roleIds);

    /**
     * 删除角色权限关联
     */
    int deleteByRoleId(@Param("roleId") Long roleId);
}