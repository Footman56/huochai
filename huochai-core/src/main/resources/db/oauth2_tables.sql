-- OAuth2 授权服务器数据表
-- 参考 Spring Authorization Server 官方文档

-- =============================================
-- 1. OAuth2 注册客户端表
-- =============================================
CREATE TABLE IF NOT EXISTS oauth2_registered_client (
    id VARCHAR(100) PRIMARY KEY COMMENT '客户端ID',
    client_id VARCHAR(100) NOT NULL UNIQUE COMMENT '客户端标识',
    client_id_issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '客户端ID签发时间',
    client_secret VARCHAR(200) COMMENT '客户端密钥',
    client_secret_expires_at TIMESTAMP COMMENT '客户端密钥过期时间',
    client_name VARCHAR(200) COMMENT '客户端名称',
    client_authentication_methods VARCHAR(500) COMMENT '客户端认证方法',
    authorization_grant_types VARCHAR(500) COMMENT '授权类型',
    redirect_uris VARCHAR(1000) COMMENT '回调地址',
    post_logout_redirect_uris VARCHAR(1000) COMMENT '登出后回调地址',
    scopes VARCHAR(500) COMMENT '授权范围',
    client_settings VARCHAR(2000) COMMENT '客户端设置',
    token_settings VARCHAR(2000) COMMENT 'Token设置'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuth2注册客户端表';

-- =============================================
-- 2. OAuth2 授权记录表
-- =============================================
CREATE TABLE IF NOT EXISTS oauth2_authorization (
    id VARCHAR(100) PRIMARY KEY COMMENT '授权ID',
    registered_client_id VARCHAR(100) NOT NULL COMMENT '注册客户端ID',
    principal_name VARCHAR(200) NOT NULL COMMENT '用户名',
    authorization_grant_type VARCHAR(100) COMMENT '授权类型',
    authorized_scopes VARCHAR(1000) COMMENT '授权范围',
    attributes TEXT COMMENT '属性',
    state VARCHAR(500) COMMENT '状态',
    
    -- 授权码相关
    authorization_code_value TEXT COMMENT '授权码值',
    authorization_code_issued_at TIMESTAMP COMMENT '授权码签发时间',
    authorization_code_expires_at TIMESTAMP COMMENT '授权码过期时间',
    authorization_code_metadata VARCHAR(2000) COMMENT '授权码元数据',
    
    -- Access Token 相关
    access_token_value TEXT COMMENT 'Access Token值',
    access_token_issued_at TIMESTAMP COMMENT 'Access Token签发时间',
    access_token_expires_at TIMESTAMP COMMENT 'Access Token过期时间',
    access_token_metadata VARCHAR(2000) COMMENT 'Access Token元数据',
    access_token_type VARCHAR(100) COMMENT 'Token类型',
    access_token_scopes VARCHAR(500) COMMENT 'Token范围',
    
    -- OIDC ID Token 相关
    oidc_id_token_value TEXT COMMENT 'ID Token值',
    oidc_id_token_issued_at TIMESTAMP COMMENT 'ID Token签发时间',
    oidc_id_token_expires_at TIMESTAMP COMMENT 'ID Token过期时间',
    oidc_id_token_metadata VARCHAR(2000) COMMENT 'ID Token元数据',
    
    -- Refresh Token 相关
    refresh_token_value TEXT COMMENT 'Refresh Token值',
    refresh_token_issued_at TIMESTAMP COMMENT 'Refresh Token签发时间',
    refresh_token_expires_at TIMESTAMP COMMENT 'Refresh Token过期时间',
    refresh_token_metadata VARCHAR(2000) COMMENT 'Refresh Token元数据',
    
    -- Device Code 相关
    user_code_value TEXT COMMENT '用户码值',
    user_code_issued_at TIMESTAMP COMMENT '用户码签发时间',
    user_code_expires_at TIMESTAMP COMMENT '用户码过期时间',
    user_code_metadata VARCHAR(2000) COMMENT '用户码元数据',
    device_code_value TEXT COMMENT '设备码值',
    device_code_issued_at TIMESTAMP COMMENT '设备码签发时间',
    device_code_expires_at TIMESTAMP COMMENT '设备码过期时间',
    device_code_metadata VARCHAR(2000) COMMENT '设备码元数据',
    
    INDEX idx_client_id (registered_client_id),
    INDEX idx_principal_name (principal_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuth2授权记录表';

-- =============================================
-- 3. OAuth2 授权同意表
-- =============================================
CREATE TABLE IF NOT EXISTS oauth2_authorization_consent (
    registered_client_id VARCHAR(100) NOT NULL COMMENT '注册客户端ID',
    principal_name VARCHAR(200) NOT NULL COMMENT '用户名',
    authorities VARCHAR(1000) COMMENT '授权权限',
    PRIMARY KEY (registered_client_id, principal_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuth2授权同意表';

-- =============================================
-- 4. 预置客户端数据
-- =============================================

-- Web 客户端
-- client_secret: secret (BCrypt加密)
INSERT INTO oauth2_registered_client (
    id, client_id, client_secret, client_name, 
    client_authentication_methods, authorization_grant_types,
    redirect_uris, scopes, client_settings, token_settings
) VALUES (
    'web-app-001',
    'web-app',
    '{bcrypt}$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW',
    'Web应用客户端',
    'client_secret_basic',
    'authorization_code,refresh_token,client_credentials',
    'https://localhost:8443/login/oauth2/code/web-app,https://localhost:8443/callback',
    'openid,profile,email,read,write',
    '{"settings.client.require-proof-key":false,"settings.client.require-authorization-consent":true}',
    '{"settings.token.reuse-refresh-tokens":true,"settings.token.id-token-signature-algorithm":["RS256"],"settings.token.access-token-time-to-live":["PT2H"],"settings.token.access-token-format":{"type":"self-contained"},"settings.token.refresh-token-time-to-live":["PT168H"]}'
);

-- 移动端客户端
INSERT INTO oauth2_registered_client (
    id, client_id, client_secret, client_name, 
    client_authentication_methods, authorization_grant_types,
    redirect_uris, scopes, client_settings, token_settings
) VALUES (
    'mobile-app-001',
    'mobile-app',
    '{bcrypt}$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW',
    '移动应用客户端',
    'client_secret_basic',
    'authorization_code,refresh_token',
    'myapp://callback',
    'openid,profile,read,write',
    '{"settings.client.require-proof-key":false,"settings.client.require-authorization-consent":false}',
    '{"settings.token.reuse-refresh-tokens":true,"settings.token.id-token-signature-algorithm":["RS256"],"settings.token.access-token-time-to-live":["PT2H"],"settings.token.access-token-format":{"type":"self-contained"},"settings.token.refresh-token-time-to-live":["PT720H"]}'
);

-- 服务间调用客户端
INSERT INTO oauth2_registered_client (
    id, client_id, client_secret, client_name, 
    client_authentication_methods, authorization_grant_types,
    redirect_uris, scopes, client_settings, token_settings
) VALUES (
    'service-client-001',
    'service-client',
    '{bcrypt}$2a$10$GRLdNijSQMUvl/au9ofL.eDwmoohzzS7.rmNSJZ.0FxO/BTk76klW',
    '服务调用客户端',
    'client_secret_basic',
    'client_credentials',
    '',
    'read,write',
    '{"settings.client.require-proof-key":false,"settings.client.require-authorization-consent":false}',
    '{"settings.token.reuse-refresh-tokens":true,"settings.token.id-token-signature-algorithm":["RS256"],"settings.token.access-token-time-to-live":["PT1H"],"settings.token.access-token-format":{"type":"self-contained"}}'
);