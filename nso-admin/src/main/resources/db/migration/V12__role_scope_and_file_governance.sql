-- Additive V1.0 role and file-governance hardening. Existing administrator
-- customizations are intentionally preserved.

ALTER TABLE nso_file_object
    ADD COLUMN project_id BIGINT NULL AFTER tenant_id,
    ADD KEY idx_file_project (tenant_id, project_id);

ALTER TABLE nso_document_version
    ADD COLUMN file_object_id BIGINT NULL AFTER document_id,
    ADD KEY idx_document_version_file (tenant_id, file_object_id);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.permission_code IN
    ('project:view', 'task:view', 'task:execute', 'task:feedback')
WHERE r.tenant_id = 1 AND r.role_code = 'field_user' AND m.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.permission_code = 'change:create'
WHERE r.tenant_id = 1
  AND r.role_code IN ('process', 'purchaser', 'production')
  AND m.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.permission_code = 'change:close'
WHERE r.tenant_id = 1 AND r.role_code = 'project_manager' AND m.deleted = 0;
