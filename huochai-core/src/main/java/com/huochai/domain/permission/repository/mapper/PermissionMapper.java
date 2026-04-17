package com.huochai.domain.permission.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huochai.domain.permission.bean.Permission;
import org.apache.ibatis.annotations.Mapper;

/**
 * 权限 Mapper
 *
 * @author huochai
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {

}