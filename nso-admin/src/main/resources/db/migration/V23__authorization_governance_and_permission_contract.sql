-- Three-way system governance and canonical permission contract.

CREATE TABLE sys_role_dept (
    role_id BIGINT NOT NULL,
    dept_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, dept_id),
    KEY idx_sys_role_dept_tenant_dept (tenant_id, dept_id)
) COMMENT='CUSTOM_DEPT 数据范围授权部门';

CREATE TABLE nso_authorization_change_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    target_user_id BIGINT NOT NULL,
    requested_role_code VARCHAR(64) NOT NULL,
    request_type VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    reason VARCHAR(1000) NOT NULL,
    requested_by BIGINT NOT NULL,
    approved_by BIGINT NULL,
    approved_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_authorization_request_target (tenant_id, target_user_id, status),
    KEY idx_authorization_request_status (tenant_id, status, created_at)
) COMMENT='受保护系统角色授权申请';

INSERT INTO sys_role (tenant_id, role_code, role_name, status, data_scope, version, deleted)
VALUES
    (1, 'superadmin', '超级管理员', 'ENABLED', 'ALL', 0, 0),
    (1, 'system_admin', '系统管理员', 'ENABLED', 'ALL', 0, 0),
    (1, 'hr_admin', '人事管理员', 'ENABLED', 'ALL', 0, 0)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), status = 'ENABLED', data_scope = VALUES(data_scope), deleted = 0;

-- Preserve the existing bootstrap administrator by explicitly converting it into the first super administrator.
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT ur.user_id, super_role.id
FROM sys_user_role ur
JOIN sys_role admin_role ON admin_role.id = ur.role_id AND admin_role.tenant_id = 1 AND admin_role.role_code = 'admin'
JOIN sys_role super_role ON super_role.tenant_id = 1 AND super_role.role_code = 'superadmin' AND super_role.deleted = 0;

-- Canonical system permissions. Existing business codes are retained as compatibility
-- aliases, while canonical nso:* permissions are added below for every existing menu.
INSERT IGNORE INTO sys_menu (menu_name, route_path, permission_code, component_name, visible, sort_no, status)
VALUES
    ('账号查看', NULL, 'sys:user:read', NULL, 0, 901, 'ENABLED'),
    ('账号创建', NULL, 'sys:user:create', NULL, 0, 902, 'ENABLED'),
    ('账号编辑', NULL, 'sys:user:update', NULL, 0, 903, 'ENABLED'),
    ('账号状态与交接', NULL, 'sys:user:lifecycle', NULL, 0, 904, 'ENABLED'),
    ('密码重置', NULL, 'sys:user:reset-password', NULL, 0, 905, 'ENABLED'),
    ('部门查看', NULL, 'sys:dept:read', NULL, 0, 906, 'ENABLED'),
    ('部门维护', NULL, 'sys:dept:manage', NULL, 0, 907, 'ENABLED'),
    ('岗位维护', NULL, 'sys:post:manage', NULL, 0, 908, 'ENABLED'),
    ('角色查看', NULL, 'sys:role:read', NULL, 0, 909, 'ENABLED'),
    ('角色维护', NULL, 'sys:role:manage', NULL, 0, 910, 'ENABLED'),
    ('角色授权', NULL, 'sys:role:grant', NULL, 0, 911, 'ENABLED'),
    ('权限目录维护', NULL, 'sys:permission:manage', NULL, 0, 912, 'ENABLED'),
    ('授权审计查看', NULL, 'sys:authorization:audit', NULL, 0, 913, 'ENABLED');

-- Every existing business permission is exposed through the canonical nso:* namespace.
INSERT IGNORE INTO sys_menu (menu_name, route_path, permission_code, component_name, visible, sort_no, status)
SELECT CONCAT('兼容-', menu_name), NULL, CONCAT('nso:', permission_code), NULL, 0, sort_no + 1000, 'ENABLED'
FROM sys_menu
WHERE permission_code IS NOT NULL
  AND permission_code NOT LIKE 'sys:%' AND permission_code NOT LIKE 'nso:%' AND deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.tenant_id = 1 AND r.role_code IN ('admin', 'superadmin')
  AND m.permission_code LIKE 'sys:%' AND m.status = 'ENABLED' AND m.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.permission_code IN
    ('sys:user:read','sys:user:update','sys:user:lifecycle','sys:user:reset-password','sys:role:read','sys:role:manage','sys:role:grant','sys:permission:manage','sys:authorization:audit')
WHERE r.tenant_id = 1 AND r.role_code = 'system_admin' AND m.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.permission_code IN
    ('sys:user:read','sys:user:create','sys:user:update','sys:user:lifecycle','sys:dept:read','sys:dept:manage','sys:post:manage','sys:authorization:audit')
WHERE r.tenant_id = 1 AND r.role_code = 'hr_admin' AND m.deleted = 0;

-- Canonical business permissions mirror each legacy grant during the transition.
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT rm.role_id, canonical.id
FROM sys_role_menu rm
JOIN sys_menu legacy ON legacy.id = rm.menu_id
JOIN sys_menu canonical ON canonical.permission_code = CONCAT('nso:', legacy.permission_code)
WHERE legacy.permission_code NOT LIKE 'sys:%' AND legacy.permission_code NOT LIKE 'nso:%';
