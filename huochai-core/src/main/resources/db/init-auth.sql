-- =============================================
-- 完整登录体系数据库初始化脚本
-- 数据库: huochai_auth
-- =============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS huochai_auth DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE huochai_auth;

-- =============================================
-- 用户表 (认证上下文)
-- =============================================
DROP TABLE IF EXISTS sys_auth_user;
CREATE TABLE sys_auth_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    avatar VARCHAR(255) COMMENT '头像URL',
    status TINYINT DEFAULT 1 COMMENT '状态: 1正常 0禁用',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记: 0未删除 1已删除',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username),
    KEY idx_phone (phone),
    KEY idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- =============================================
-- 角色表
-- =============================================
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    role_name VARCHAR(50) NOT NULL COMMENT '角色名称',
    role_code VARCHAR(50) NOT NULL COMMENT '角色编码',
    description VARCHAR(255) COMMENT '角色描述',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态: 1正常 0禁用',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- =============================================
-- 权限表
-- =============================================
DROP TABLE IF EXISTS sys_permission;
CREATE TABLE sys_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '权限ID',
    permission_name VARCHAR(100) NOT NULL COMMENT '权限名称',
    permission_code VARCHAR(100) NOT NULL COMMENT '权限编码',
    resource_type VARCHAR(20) DEFAULT 'API' COMMENT '资源类型: MENU/BUTTON/API',
    resource_url VARCHAR(255) COMMENT '资源URL',
    http_method VARCHAR(10) COMMENT 'HTTP方法: GET/POST/PUT/DELETE',
    parent_id BIGINT DEFAULT 0 COMMENT '父级ID',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态: 1正常 0禁用',
    deleted TINYINT DEFAULT 0 COMMENT '删除标记',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_permission_code (permission_code),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- =============================================
-- 用户角色关联表
-- =============================================
DROP TABLE IF EXISTS sys_user_role;
CREATE TABLE sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_user_role (user_id, role_id),
    KEY idx_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- =============================================
-- 角色权限关联表
-- =============================================
DROP TABLE IF EXISTS sys_role_permission;
CREATE TABLE sys_role_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    permission_id BIGINT NOT NULL COMMENT '权限ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_role_permission (role_id, permission_id),
    KEY idx_permission_id (permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- =============================================
-- 登录日志表 (审计)
-- =============================================
DROP TABLE IF EXISTS sys_login_log;
CREATE TABLE sys_login_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    user_id BIGINT COMMENT '用户ID',
    username VARCHAR(50) COMMENT '用户名',
    client_type VARCHAR(20) COMMENT '客户端类型: WEB/APP/MINI_PROGRAM',
    device_id VARCHAR(100) COMMENT '设备ID',
    device_name VARCHAR(255) COMMENT '设备名称',
    os VARCHAR(100) COMMENT '操作系统',
    browser VARCHAR(100) COMMENT '浏览器',
    ip VARCHAR(50) COMMENT 'IP地址',
    location VARCHAR(255) COMMENT '登录地点',
    login_result TINYINT COMMENT '登录结果: 1成功 0失败',
    fail_reason VARCHAR(255) COMMENT '失败原因',
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
    KEY idx_user_time (user_id, login_time),
    KEY idx_login_time (login_time),
    KEY idx_ip (ip)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

-- =============================================
-- 操作日志表 (审计)
-- =============================================
DROP TABLE IF EXISTS sys_operation_log;
CREATE TABLE sys_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    trace_id VARCHAR(64) COMMENT '链路追踪ID',
    user_id BIGINT COMMENT '用户ID',
    username VARCHAR(50) COMMENT '用户名',
    operation_type VARCHAR(50) COMMENT '操作类型',
    operation_desc VARCHAR(255) COMMENT '操作描述',
    request_method VARCHAR(10) COMMENT '请求方法',
    request_url VARCHAR(500) COMMENT '请求URL',
    request_params TEXT COMMENT '请求参数',
    response_result TEXT COMMENT '响应结果',
    ip VARCHAR(50) COMMENT 'IP地址',
    status TINYINT COMMENT '状态: 1成功 0失败',
    error_msg TEXT COMMENT '错误信息',
    cost_time BIGINT COMMENT '耗时(毫秒)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_trace_id (trace_id),
    KEY idx_user_id (user_id),
    KEY idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- =============================================
-- 初始化数据
-- =============================================

-- 插入默认角色
INSERT INTO sys_role (role_name, role_code, description, sort_order) VALUES 
('超级管理员', 'ROLE_ADMIN', '系统超级管理员，拥有所有权限', 1),
('普通用户', 'ROLE_USER', '普通用户，拥有基本权限', 2);

-- 插入默认权限
INSERT INTO sys_permission (permission_name, permission_code, resource_type, resource_url, http_method, parent_id, sort_order) VALUES
-- 用户管理
('用户管理', 'user', 'MENU', '/system/user', NULL, 0, 1),
('用户列表', 'user:list', 'BUTTON', '/api/admin/users', 'GET', 1, 1),
('创建用户', 'user:create', 'BUTTON', '/api/admin/users', 'POST', 1, 2),
('更新用户', 'user:update', 'BUTTON', '/api/admin/users/*', 'PUT', 1, 3),
('删除用户', 'user:delete', 'BUTTON', '/api/admin/users/*', 'DELETE', 1, 4),

-- 角色管理
('角色管理', 'role', 'MENU', '/system/role', NULL, 0, 2),
('角色列表', 'role:list', 'BUTTON', '/api/admin/roles', 'GET', 6, 1),
('创建角色', 'role:create', 'BUTTON', '/api/admin/roles', 'POST', 6, 2),
('更新角色', 'role:update', 'BUTTON', '/api/admin/roles/*', 'PUT', 6, 3),
('删除角色', 'role:delete', 'BUTTON', '/api/admin/roles/*', 'DELETE', 6, 4),

-- 权限管理
('权限管理', 'permission', 'MENU', '/system/permission', NULL, 0, 3),
('权限列表', 'permission:list', 'BUTTON', '/api/admin/permissions', 'GET', 11, 1),

-- 登录日志
('登录日志', 'login-log', 'MENU', '/system/login-log', NULL, 0, 4),
('日志列表', 'login-log:list', 'BUTTON', '/api/admin/login-logs', 'GET', 15, 1);

-- 插入默认管理员 (密码: admin123, BCrypt加密)
-- BCrypt $2a$10$ 开头，使用 spring-security-crypto 生成
INSERT INTO sys_auth_user (username, password, email, phone, status) VALUES 
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt8P0Tq', 'admin@huochai.com', '13800138000', 1);

-- 分配管理员角色
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- 分配管理员权限（超级管理员拥有所有权限）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission;

-- 分配普通用户角色权限
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(2, 1), (2, 2),  -- 用户管理-查看
(2, 11), (2, 12); -- 权限管理-查看