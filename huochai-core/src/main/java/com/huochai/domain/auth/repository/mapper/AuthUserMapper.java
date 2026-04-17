package com.huochai.domain.auth.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huochai.domain.auth.bean.AuthUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper
 *
 * @author huochai
 */
@Mapper
public interface AuthUserMapper extends BaseMapper<AuthUser> {

}