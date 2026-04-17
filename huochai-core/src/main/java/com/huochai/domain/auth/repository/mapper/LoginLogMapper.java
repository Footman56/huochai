package com.huochai.domain.auth.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huochai.domain.auth.bean.LoginLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录日志 Mapper
 *
 * @author huochai
 */
@Mapper
public interface LoginLogMapper extends BaseMapper<LoginLog> {

}