package com.huochai.auth.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huochai.auth.domain.model.LoginLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录日志 Mapper
 *
 * @author huochai
 */
@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {

}