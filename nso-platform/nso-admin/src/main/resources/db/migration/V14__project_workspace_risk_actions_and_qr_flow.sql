ALTER TABLE nso_risk_action
    ADD COLUMN responsible_user_id BIGINT NULL AFTER action_plan,
    ADD COLUMN idempotency_key VARCHAR(64) NULL AFTER status,
    ADD COLUMN closed_by BIGINT NULL AFTER idempotency_key,
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at,
    ADD COLUMN closed_at DATETIME NULL AFTER updated_at,
    ADD COLUMN version INT NOT NULL DEFAULT 0 AFTER closed_at,
    ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 AFTER version,
    ADD UNIQUE KEY uk_risk_action_idempotency (tenant_id, risk_id, idempotency_key),
    ADD KEY idx_risk_action_owner (tenant_id, responsible_user_id, status);

ALTER TABLE nso_qrcode_binding
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at,
    ADD UNIQUE KEY uk_qrcode_business (tenant_id, business_type, business_id);

INSERT INTO sys_menu (menu_name, route_path, permission_code, component_name, visible, sort_no, status)
VALUES
    ('风险处置', NULL, 'risk:dispose', NULL, 0, 73, 'ENABLED'),
    ('技术包任务同步', NULL, 'document:sync', NULL, 0, 36, 'ENABLED')
ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name), route_path = VALUES(route_path), component_name = VALUES(component_name),
    visible = VALUES(visible), sort_no = VALUES(sort_no), status = 'ENABLED', deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.permission_code = 'risk:dispose'
WHERE r.tenant_id = 1
  AND r.role_code IN ('project_manager', 'technical', 'process', 'purchaser', 'production', 'quality')
  AND m.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.permission_code = 'document:sync'
WHERE r.tenant_id = 1
  AND r.role_code IN ('project_manager', 'technical')
  AND m.deleted = 0;
