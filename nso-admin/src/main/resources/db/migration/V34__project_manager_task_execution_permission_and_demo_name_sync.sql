-- Allow project managers to execute and report their own project-manager tasks.
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.permission_code IN (
    'task:execute', 'task:feedback', 'nso:task:execute', 'nso:task:feedback'
)
WHERE r.tenant_id = 1
  AND r.role_code = 'project_manager'
  AND r.status = 'ENABLED'
  AND r.deleted = 0
  AND m.status = 'ENABLED'
  AND m.deleted = 0;

-- Finish normalizing names for the tenant-1 demo accounts without changing
-- responsibility or audit records for other tenants or users.
UPDATE nso_project_member pm
JOIN sys_user u ON u.id = pm.user_id AND u.tenant_id = pm.tenant_id
SET pm.member_name = u.nickname
WHERE pm.tenant_id = 1
  AND pm.deleted = 0
  AND u.username IN (
      'demo-admin', 'demo-pm', 'demo-tech', 'demo-process', 'demo-purchase',
      'demo-production', 'demo-quality', 'demo-field', 'demo-executive'
  )
  AND NOT (pm.member_name <=> u.nickname);

UPDATE nso_task t
JOIN sys_user u ON u.id = t.assignee_id AND u.tenant_id = t.tenant_id
SET t.responsible_name = u.nickname
WHERE t.tenant_id = 1
  AND t.deleted = 0
  AND u.username IN (
      'demo-admin', 'demo-pm', 'demo-tech', 'demo-process', 'demo-purchase',
      'demo-production', 'demo-quality', 'demo-field', 'demo-executive'
  )
  AND NOT (t.responsible_name <=> u.nickname);
