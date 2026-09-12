-- Account recovery, support ticketing and contextual-help authorization.
-- This migration is additive; previously released migrations remain immutable.

CREATE TABLE nso_password_recovery_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    username VARCHAR(64) NOT NULL,
    contact_name VARCHAR(64) NOT NULL,
    contact_value VARCHAR(128) NOT NULL,
    requester_note VARCHAR(1000) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    active_key VARCHAR(160) NULL,
    handler_id BIGINT NULL,
    handling_note VARCHAR(1000) NULL,
    reviewed_at DATETIME NULL,
    reset_at DATETIME NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_password_recovery_active (active_key),
    KEY idx_password_recovery_queue (tenant_id, status, created_at),
    KEY idx_password_recovery_user (tenant_id, user_id, created_at)
) COMMENT='内部账号密码恢复申请';

CREATE TABLE nso_support_ticket (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    ticket_no VARCHAR(64) NOT NULL,
    requester_user_id BIGINT NULL,
    requester_username VARCHAR(64) NULL,
    contact_name VARCHAR(64) NOT NULL,
    contact_value VARCHAR(128) NULL,
    source VARCHAR(16) NOT NULL,
    category VARCHAR(32) NOT NULL,
    priority VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
    page_context VARCHAR(256) NULL,
    description VARCHAR(2000) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    handler_id BIGINT NULL,
    handling_note VARCHAR(2000) NULL,
    resolved_at DATETIME NULL,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_support_ticket_no (tenant_id, ticket_no),
    KEY idx_support_ticket_queue (tenant_id, status, priority, created_at),
    KEY idx_support_ticket_requester (tenant_id, requester_user_id, created_at)
) COMMENT='平台技术支持工单';

CREATE TABLE nso_support_ticket_attachment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    ticket_id BIGINT NOT NULL,
    file_object_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_support_ticket_attachment (tenant_id, ticket_id, file_object_id),
    KEY idx_support_attachment_ticket (tenant_id, ticket_id)
) COMMENT='技术支持工单图片附件';

INSERT IGNORE INTO sys_menu (menu_name, route_path, permission_code, component_name, visible, sort_no, status)
VALUES ('账户支持', '/system/account-support', 'sys:account-support:manage', 'SystemAccountSupport', 0, 920, 'ENABLED');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.permission_code = 'sys:account-support:manage'
WHERE r.tenant_id = 1
  AND r.role_code IN ('superadmin', 'system_admin', 'admin')
  AND r.deleted = 0
  AND m.deleted = 0;
