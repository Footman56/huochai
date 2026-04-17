package com.huochai.domain.auth.repository.impl;

import com.huochai.domain.auth.bean.OAuth2Client;
import com.huochai.domain.auth.repository.OAuth2ClientRepository;
import com.huochai.domain.auth.repository.mapper.OAuth2ClientMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * OAuth2 客户端仓储实现
 *
 * @author huochai
 */
@Repository
public class OAuth2ClientRepositoryImpl implements OAuth2ClientRepository {

    @Autowired
    private OAuth2ClientMapper oauth2ClientMapper;

    @Override
    public Optional<OAuth2Client> findByClientId(String clientId) {
        OAuth2Client client = oauth2ClientMapper.selectByClientId(clientId);
        return Optional.ofNullable(client);
    }

    @Override
    public Optional<OAuth2Client> findById(String id) {
        OAuth2Client client = oauth2ClientMapper.selectById(id);
        return Optional.ofNullable(client);
    }

    @Override
    public OAuth2Client save(OAuth2Client client) {
        oauth2ClientMapper.insert(client);
        return client;
    }

    @Override
    public boolean existsByClientId(String clientId) {
        return oauth2ClientMapper.countByClientId(clientId) > 0;
    }
}