-- V12 has already been applied in existing development databases. Keep this
-- additive migration separate so historical migration checksums remain stable.
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.permission_code = 'change:close'
WHERE r.tenant_id = 1
  AND r.role_code = 'project_manager'
  AND m.deleted = 0;
