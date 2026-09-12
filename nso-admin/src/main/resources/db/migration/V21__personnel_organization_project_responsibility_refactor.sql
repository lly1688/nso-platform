-- Personnel, organization and project-responsibility refactor.
-- Keep all historical V1-V20 migrations immutable; this script is additive.

ALTER TABLE sys_user
    ADD COLUMN user_type VARCHAR(16) NOT NULL DEFAULT 'INTERNAL' AFTER status;

CREATE INDEX idx_sys_user_directory ON sys_user (tenant_id, user_type, status, dept_id);

CREATE TABLE nso_project_member_responsibility (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    project_member_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    responsibility_code VARCHAR(64) NOT NULL,
    primary_flag TINYINT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_member_responsibility (tenant_id, project_member_id, responsibility_code, deleted),
    KEY idx_project_responsibility_scope (tenant_id, project_id, user_id, responsibility_code, status)
) COMMENT='项目成员职责';

CREATE TABLE nso_project_manager_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    previous_manager_user_id BIGINT NULL,
    next_manager_user_id BIGINT NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    operator_id BIGINT NULL,
    transferred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_project_manager_history (tenant_id, project_id, transferred_at)
) COMMENT='项目经理交接历史';

INSERT INTO sys_role (tenant_id, role_code, role_name, status, data_scope, version, deleted)
VALUES (1, 'sales', '销售人员', 'ENABLED', 'SELF', 0, 0)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), status = 'ENABLED', data_scope = VALUES(data_scope), deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.permission_code IN
    ('dashboard:view','customer:view','customer:manage','project:view','project:create',
     'project:requirement:manage','customer:invite','sample:view','sample:proxy-confirm',
     'change:view','change:create','task:view','risk:view','message:view')
WHERE r.tenant_id = 1 AND r.role_code = 'sales' AND m.deleted = 0 AND m.status = 'ENABLED';

-- Customer confirmation accounts are externally managed identities.
UPDATE sys_user u
JOIN sys_user_role ur ON ur.user_id = u.id
JOIN sys_role r ON r.id = ur.role_id AND r.role_code = 'customer_confirm'
SET u.user_type = 'EXTERNAL'
WHERE u.tenant_id = 1;

-- Project member display data is derived from the account directory, never from handwritten inputs.
UPDATE nso_project_member pm
JOIN sys_user u ON u.id = pm.user_id AND u.tenant_id = pm.tenant_id
LEFT JOIN sys_dept d ON d.id = u.dept_id AND d.tenant_id = u.tenant_id AND d.deleted = 0
SET pm.member_name = COALESCE(NULLIF(u.nickname, ''), u.username),
    pm.dept_id = u.dept_id,
    pm.department_name = d.dept_name
WHERE pm.tenant_id = 1 AND pm.deleted = 0;

UPDATE nso_project_member
SET project_role = 'CUSTOMER_CONFIRM'
WHERE tenant_id = 1 AND project_role = 'CUSTOMER' AND deleted = 0;

-- Preserve history while preventing records that no longer map to a usable account from granting access.
UPDATE nso_project_member pm
LEFT JOIN sys_user u ON u.id = pm.user_id AND u.tenant_id = pm.tenant_id
SET pm.status = 'REMOVED'
WHERE pm.tenant_id = 1 AND pm.deleted = 0
  AND (u.id IS NULL OR u.status <> 'ENABLED');

-- Backfill the new multi-responsibility model from legacy project_role values.
INSERT IGNORE INTO nso_project_member_responsibility
    (tenant_id, project_member_id, project_id, user_id, responsibility_code, primary_flag, status, version, deleted)
SELECT pm.tenant_id, pm.id, pm.project_id, pm.user_id,
       pm.project_role, 1, 'ACTIVE', 0, 0
FROM nso_project_member pm
WHERE pm.tenant_id = 1 AND pm.deleted = 0 AND pm.status = 'ACTIVE'
  AND pm.user_id IS NOT NULL
  AND pm.project_role IN ('SALES','PROJECT_MANAGER','TECHNICAL','PROCESS','PURCHASER','PRODUCTION','QUALITY','FIELD_USER','CUSTOMER_CONFIRM');

-- A project duty cannot outlive the minimum matching system role.  Keep existing
-- roles and only supplement the smallest required role for valid active members.
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT DISTINCT pmr.user_id, r.id
FROM nso_project_member_responsibility pmr
JOIN sys_role r ON r.tenant_id = pmr.tenant_id AND r.deleted = 0 AND r.status = 'ENABLED'
    AND r.role_code = CASE pmr.responsibility_code
        WHEN 'SALES' THEN 'sales'
        WHEN 'PROJECT_MANAGER' THEN 'project_manager'
        WHEN 'TECHNICAL' THEN 'technical'
        WHEN 'PROCESS' THEN 'process'
        WHEN 'PURCHASER' THEN 'purchaser'
        WHEN 'PRODUCTION' THEN 'production'
        WHEN 'QUALITY' THEN 'quality'
        WHEN 'FIELD_USER' THEN 'field_user'
        WHEN 'CUSTOMER_CONFIRM' THEN 'customer_confirm'
    END
JOIN sys_user u ON u.id = pmr.user_id AND u.tenant_id = pmr.tenant_id AND u.status = 'ENABLED'
WHERE pmr.tenant_id = 1 AND pmr.deleted = 0 AND pmr.status = 'ACTIVE';

-- Every project owner is a project manager and receives the minimum matching system role.
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT p.owner_user_id, r.id
FROM nso_project p
JOIN sys_user u ON u.id = p.owner_user_id AND u.tenant_id = p.tenant_id AND u.status = 'ENABLED'
JOIN sys_role r ON r.tenant_id = p.tenant_id AND r.role_code = 'project_manager' AND r.deleted = 0 AND r.status = 'ENABLED'
WHERE p.tenant_id = 1 AND p.deleted = 0 AND p.owner_user_id IS NOT NULL;

INSERT INTO nso_project_member
    (tenant_id, project_id, user_id, member_name, project_role, dept_id, department_name, status, version, deleted)
SELECT p.tenant_id, p.id, u.id, COALESCE(NULLIF(u.nickname, ''), u.username), 'PROJECT_MANAGER',
       u.dept_id, d.dept_name, 'ACTIVE', 0, 0
FROM nso_project p
JOIN sys_user u ON u.id = p.owner_user_id AND u.tenant_id = p.tenant_id AND u.status = 'ENABLED'
LEFT JOIN sys_dept d ON d.id = u.dept_id AND d.tenant_id = u.tenant_id AND d.deleted = 0
WHERE p.tenant_id = 1 AND p.deleted = 0 AND p.owner_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM nso_project_member pm
      WHERE pm.tenant_id = p.tenant_id AND pm.project_id = p.id AND pm.user_id = p.owner_user_id AND pm.deleted = 0
  );

INSERT IGNORE INTO nso_project_member_responsibility
    (tenant_id, project_member_id, project_id, user_id, responsibility_code, primary_flag, status, version, deleted)
SELECT pm.tenant_id, pm.id, pm.project_id, pm.user_id, 'PROJECT_MANAGER', 1, 'ACTIVE', 0, 0
FROM nso_project_member pm
JOIN nso_project p ON p.id = pm.project_id AND p.tenant_id = pm.tenant_id
WHERE pm.tenant_id = 1 AND pm.deleted = 0 AND pm.status = 'ACTIVE'
  AND p.owner_user_id = pm.user_id;

UPDATE nso_project p
JOIN sys_user u ON u.id = p.owner_user_id AND u.tenant_id = p.tenant_id
SET p.owner_name = COALESCE(NULLIF(u.nickname, ''), u.username)
WHERE p.tenant_id = 1 AND p.deleted = 0;
