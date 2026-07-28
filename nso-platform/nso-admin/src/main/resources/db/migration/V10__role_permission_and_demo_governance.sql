-- Role, permission, session-version and responsibility-separation governance.
-- Earlier migrations are immutable.

ALTER TABLE sys_user
    ADD COLUMN auth_version INT NOT NULL DEFAULT 0;

ALTER TABLE nso_project
    ADD COLUMN owner_user_id BIGINT NULL;

ALTER TABLE nso_document_version
    ADD COLUMN uploaded_by BIGINT NULL,
    ADD COLUMN published_by BIGINT NULL;

ALTER TABLE nso_sample
    ADD COLUMN created_by BIGINT NULL,
    ADD COLUMN quality_confirmed_by BIGINT NULL,
    ADD COLUMN proxy_confirmed_by BIGINT NULL,
    ADD COLUMN proxy_confirmation TINYINT NOT NULL DEFAULT 0;

ALTER TABLE nso_change_order
    ADD COLUMN applicant_user_id BIGINT NULL;

CREATE TABLE nso_special_release (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    sample_id BIGINT NULL,
    applicant_user_id BIGINT NOT NULL,
    approver_user_id BIGINT NULL,
    release_scope VARCHAR(500) NOT NULL,
    valid_until DATETIME NOT NULL,
    risk_statement VARCHAR(1000) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    approved_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_special_release_project (tenant_id, project_id, status),
    KEY idx_special_release_sample (tenant_id, sample_id, status)
) COMMENT='样品与投产特殊放行记录';

INSERT INTO sys_role (tenant_id, role_code, role_name, status, version, deleted)
VALUES
    (1, 'admin', '系统管理员', 'ENABLED', 0, 0),
    (1, 'project_manager', '销售/项目经理', 'ENABLED', 0, 0),
    (1, 'technical', '技术/设计人员', 'ENABLED', 0, 0),
    (1, 'process', '工艺人员', 'ENABLED', 0, 0),
    (1, 'purchaser', '采购人员', 'ENABLED', 0, 0),
    (1, 'production', '计划/生产人员', 'ENABLED', 0, 0),
    (1, 'quality', '质量人员', 'ENABLED', 0, 0),
    (1, 'customer_confirm', '客户确认人', 'ENABLED', 0, 0),
    (1, 'executive', '管理层', 'ENABLED', 0, 0)
ON DUPLICATE KEY UPDATE
    role_name = VALUES(role_name), status = 'ENABLED', deleted = 0;

INSERT INTO sys_menu (menu_name, route_path, permission_code, component_name, visible, sort_no, status)
VALUES
    ('客户管理', '/projects', 'customer:view', 'Projects', 1, 11, 'ENABLED'),
    ('客户维护', NULL, 'customer:manage', NULL, 0, 12, 'ENABLED'),
    ('项目新建', NULL, 'project:create', NULL, 0, 21, 'ENABLED'),
    ('项目维护', NULL, 'project:manage', NULL, 0, 22, 'ENABLED'),
    ('项目成员维护', NULL, 'project:member:manage', NULL, 0, 23, 'ENABLED'),
    ('客户邀请', NULL, 'customer:invite', NULL, 0, 24, 'ENABLED'),
    ('需求维护', NULL, 'project:requirement:manage', NULL, 0, 25, 'ENABLED'),
    ('技术资料上传', NULL, 'document:upload', NULL, 0, 31, 'ENABLED'),
    ('技术版本发布', NULL, 'document:publish', NULL, 0, 32, 'ENABLED'),
    ('BOM维护', NULL, 'bom:manage', NULL, 0, 33, 'ENABLED'),
    ('工艺维护', NULL, 'process:manage', NULL, 0, 34, 'ENABLED'),
    ('检验规范维护', NULL, 'inspection:manage', NULL, 0, 35, 'ENABLED'),
    ('样品创建', NULL, 'sample:create', NULL, 0, 41, 'ENABLED'),
    ('样品提交客户确认', NULL, 'sample:submit', NULL, 0, 42, 'ENABLED'),
    ('样品质量检验', NULL, 'sample:inspect', NULL, 0, 43, 'ENABLED'),
    ('销售代录客户结论', NULL, 'sample:proxy-confirm', NULL, 0, 44, 'ENABLED'),
    ('特殊放行申请', NULL, 'sample:release:apply', NULL, 0, 45, 'ENABLED'),
    ('特殊放行批准', NULL, 'sample:release:approve', NULL, 0, 46, 'ENABLED'),
    ('变更发起', NULL, 'change:create', NULL, 0, 51, 'ENABLED'),
    ('变更影响分析', NULL, 'change:analyze', NULL, 0, 52, 'ENABLED'),
    ('变更审批', NULL, 'change:approve', NULL, 0, 53, 'ENABLED'),
    ('变更执行反馈', NULL, 'change:feedback', NULL, 0, 54, 'ENABLED'),
    ('变更关闭', NULL, 'change:close', NULL, 0, 55, 'ENABLED'),
    ('任务排程', NULL, 'task:plan', NULL, 0, 61, 'ENABLED'),
    ('任务执行', NULL, 'task:execute', NULL, 0, 62, 'ENABLED'),
    ('任务反馈', NULL, 'task:feedback', NULL, 0, 63, 'ENABLED'),
    ('风险查看', NULL, 'risk:view', NULL, 0, 71, 'ENABLED'),
    ('消息查看', NULL, 'message:view', NULL, 0, 72, 'ENABLED'),
    ('客户确认门户', NULL, 'customer:portal', NULL, 0, 81, 'ENABLED')
ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name), route_path = VALUES(route_path), component_name = VALUES(component_name),
    visible = VALUES(visible), sort_no = VALUES(sort_no), status = 'ENABLED', deleted = 0;

-- The administrator keeps every enabled permission.  Other rows describe the
-- standard least-privilege catalog and can later be adjusted through role management.
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_code = 'admin' AND r.tenant_id = 1 AND m.status = 'ENABLED' AND m.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.permission_code IN
('dashboard:view','customer:view','customer:manage','project:view','project:create','project:manage','project:member:manage','customer:invite','project:requirement:manage','document:view','sample:view','sample:create','sample:submit','sample:proxy-confirm','change:view','change:create','change:approve','change:feedback','task:view','task:plan','risk:view','report:view','message:view')
WHERE r.role_code = 'project_manager' AND r.tenant_id = 1 AND m.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.permission_code IN
('dashboard:view','project:view','document:view','document:upload','document:publish','bom:manage','process:manage','inspection:manage','sample:view','change:view','change:create','change:analyze','change:approve','task:view','risk:view','message:view')
WHERE r.role_code = 'technical' AND r.tenant_id = 1 AND m.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.permission_code IN
('dashboard:view','project:view','document:view','bom:manage','process:manage','inspection:manage','sample:view','change:view','change:analyze','change:feedback','task:view','message:view')
WHERE r.role_code = 'process' AND r.tenant_id = 1 AND m.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.permission_code IN
('dashboard:view','project:view','document:view','sample:view','change:view','change:feedback','task:view','task:execute','task:feedback','risk:view','message:view')
WHERE r.role_code = 'purchaser' AND r.tenant_id = 1 AND m.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.permission_code IN
('dashboard:view','project:view','document:view','sample:view','sample:release:apply','change:view','change:approve','change:feedback','task:view','task:plan','task:execute','task:feedback','risk:view','message:view')
WHERE r.role_code = 'production' AND r.tenant_id = 1 AND m.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.permission_code IN
('dashboard:view','project:view','document:view','sample:view','sample:inspect','sample:release:approve','change:view','change:approve','change:feedback','task:view','task:feedback','risk:view','message:view')
WHERE r.role_code = 'quality' AND r.tenant_id = 1 AND m.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.permission_code IN ('customer:portal')
WHERE r.role_code = 'customer_confirm' AND r.tenant_id = 1 AND m.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.permission_code IN ('dashboard:view','project:view','risk:view','report:view')
WHERE r.role_code = 'executive' AND r.tenant_id = 1 AND m.deleted = 0;
