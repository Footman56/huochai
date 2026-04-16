package com.huochai.common.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huochai.common.log.OperationLogEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 操作日志 Mapper
 *
 * @author huochai
 */
@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLogEntity> {

}