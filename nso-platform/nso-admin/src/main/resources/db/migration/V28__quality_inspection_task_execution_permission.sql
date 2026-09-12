-- Quality assignees execute only their own QUALITY and INSPECTION tasks;
-- TaskServiceImpl enforces the task assignee, global role, and project responsibility.
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.permission_code IN ('task:execute', 'nso:task:execute')
WHERE r.tenant_id = 1
  AND r.role_code = 'quality'
  AND r.deleted = 0
  AND m.status = 'ENABLED'
  AND m.deleted = 0;
