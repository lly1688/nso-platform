-- Unify the PC navigation around work, projects, actions, reports and system governance.
-- Authorization changes use a dedicated version so refresh tokens remain usable while
-- access tokens carrying an obsolete permission snapshot are rejected.

ALTER TABLE sys_user
    ADD COLUMN permission_version INT NOT NULL DEFAULT 0 AFTER auth_version;

UPDATE sys_menu
SET menu_name = CASE permission_code
        WHEN 'dashboard:view' THEN '我的工作台'
        WHEN 'project:view' THEN '项目中心'
        WHEN 'action:view' THEN '行动中心'
        WHEN 'report:view' THEN '统计分析'
        WHEN 'system:manage' THEN '系统管理'
        ELSE menu_name
    END,
    route_path = CASE permission_code
        WHEN 'dashboard:view' THEN '/dashboard'
        WHEN 'project:view' THEN '/projects'
        WHEN 'action:view' THEN '/actions'
        WHEN 'report:view' THEN '/reports'
        WHEN 'system:manage' THEN '/system'
        ELSE route_path
    END,
    component_name = CASE permission_code
        WHEN 'dashboard:view' THEN 'Dashboard'
        WHEN 'project:view' THEN 'Projects'
        WHEN 'action:view' THEN 'ActionCenter'
        WHEN 'report:view' THEN 'Reports'
        WHEN 'system:manage' THEN 'System'
        ELSE component_name
    END,
    visible = 1,
    sort_no = CASE permission_code
        WHEN 'dashboard:view' THEN 1
        WHEN 'project:view' THEN 2
        WHEN 'action:view' THEN 3
        WHEN 'report:view' THEN 4
        WHEN 'system:manage' THEN 5
        ELSE sort_no
    END,
    status = 'ENABLED',
    deleted = 0
WHERE permission_code IN ('dashboard:view', 'project:view', 'action:view', 'report:view', 'system:manage');

-- Legacy module routes stay addressable for deep-link compatibility but no longer
-- participate in the server-projected navigation.
UPDATE sys_menu
SET visible = 0
WHERE permission_code IN ('customer:view', 'document:view', 'sample:view', 'change:view', 'task:view');

-- Every internal business role gets the three operational entry points.
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.permission_code IN ('dashboard:view', 'project:view', 'action:view')
WHERE r.tenant_id = 1
  AND r.role_code IN ('admin', 'project_manager', 'field_user', 'technical', 'process',
                      'purchaser', 'production', 'quality', 'sales', 'executive')
  AND r.status = 'ENABLED' AND r.deleted = 0
  AND m.status = 'ENABLED' AND m.deleted = 0;

-- Management roles receive analysis; the administrator also receives system governance.
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.permission_code = 'report:view'
WHERE r.tenant_id = 1
  AND r.role_code IN ('admin', 'project_manager', 'executive')
  AND r.status = 'ENABLED' AND r.deleted = 0
  AND m.status = 'ENABLED' AND m.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.permission_code = 'system:manage'
WHERE r.tenant_id = 1
  AND r.role_code = 'admin'
  AND r.status = 'ENABLED' AND r.deleted = 0
  AND m.status = 'ENABLED' AND m.deleted = 0;

-- External customer confirmation identities stay outside the PC management navigation.
DELETE rm
FROM sys_role_menu rm
JOIN sys_role r ON r.id = rm.role_id
JOIN sys_menu m ON m.id = rm.menu_id
WHERE r.tenant_id = 1
  AND r.role_code = 'customer_confirm'
  AND m.permission_code IN ('dashboard:view', 'project:view', 'action:view', 'report:view', 'system:manage');

-- Backfill canonical aliases for permissions introduced after V23 (notably action:view),
-- then mirror every current legacy role grant to its canonical alias.
INSERT IGNORE INTO sys_menu (menu_name, route_path, permission_code, component_name, visible, sort_no, status)
SELECT CONCAT('兼容-', menu_name), NULL, CONCAT('nso:', permission_code), NULL, 0, sort_no + 1000, 'ENABLED'
FROM sys_menu
WHERE permission_code IS NOT NULL
  AND permission_code NOT LIKE 'sys:%'
  AND permission_code NOT LIKE 'nso:%'
  AND deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT rm.role_id, canonical.id
FROM sys_role_menu rm
JOIN sys_menu legacy ON legacy.id = rm.menu_id
JOIN sys_menu canonical ON canonical.permission_code = CONCAT('nso:', legacy.permission_code)
WHERE legacy.permission_code NOT LIKE 'sys:%'
  AND legacy.permission_code NOT LIKE 'nso:%'
  AND legacy.deleted = 0
  AND canonical.deleted = 0;
