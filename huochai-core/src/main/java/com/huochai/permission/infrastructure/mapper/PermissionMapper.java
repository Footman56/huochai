package com.huochai.permission.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huochai.permission.domain.model.Permission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 权限 Mapper
 *
 * @author huochai
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

}