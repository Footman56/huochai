package com.huochai.auth.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huochai.auth.domain.model.AuthUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 *
 * @author huochai
 */
@Mapper
public interface AuthUserMapper extends BaseMapper<AuthUser> {

}