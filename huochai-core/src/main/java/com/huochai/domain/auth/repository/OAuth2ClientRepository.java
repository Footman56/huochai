package com.huochai.domain.auth.repository;

import com.huochai.domain.auth.bean.OAuth2Client;

import java.util.Optional;

/**
 * OAuth2 客户端仓储接口
 *
 * @author huochai
 */
public interface OAuth2ClientRepository {

    /**
     * 根据客户端ID查询
     */
    Optional<OAuth2Client> findByClientId(String clientId);

    /**
     * 根据ID查询
     */
    Optional<OAuth2Client> findById(String id);

    /**
     * 保存客户端
     */
    OAuth2Client save(OAuth2Client client);

    /**
     * 检查客户端ID是否存在
     */
    boolean existsByClientId(String clientId);
}