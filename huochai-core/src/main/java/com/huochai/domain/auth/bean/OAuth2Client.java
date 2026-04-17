package com.huochai.domain.auth.bean;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * OAuth2 客户端领域模型
 * 
 * 遵循现有 DDD 架构风格
 *
 * @author huochai
 */
@Data
@TableName("oauth2_registered_client")
public class OAuth2Client {

    @TableId(type = IdType.INPUT)
    private String id;

    private String clientId;

    private LocalDateTime clientIdIssuedAt;

    private String clientSecret;

    private LocalDateTime clientSecretExpiresAt;

    private String clientName;

    private String clientAuthenticationMethods;

    private String authorizationGrantTypes;

    private String redirectUris;

    private String postLogoutRedirectUris;

    private String scopes;

    private String clientSettings;

    private String tokenSettings;
}