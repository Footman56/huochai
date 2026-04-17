package com.huochai.domain.auth.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huochai.domain.auth.bean.OAuth2Client;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * OAuth2 客户端 Mapper
 *
 * @author huochai
 */
@Mapper
public interface OAuth2ClientMapper extends BaseMapper<OAuth2Client> {

    @Select("SELECT * FROM oauth2_registered_client WHERE client_id = #{clientId}")
    OAuth2Client selectByClientId(String clientId);

    @Select("SELECT COUNT(*) FROM oauth2_registered_client WHERE client_id = #{clientId}")
    int countByClientId(String clientId);
}