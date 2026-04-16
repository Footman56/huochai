package com.huochai.permission.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huochai.permission.domain.model.Role;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色 Mapper
 *
 * @author huochai
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

}