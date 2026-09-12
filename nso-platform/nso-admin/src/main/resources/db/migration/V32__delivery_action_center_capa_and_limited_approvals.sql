-- Delivery-loop capabilities: rebuildable action projection, CAPA, and limited approval templates.
-- Existing business tables remain the source of truth; this migration is additive.

CREATE TABLE nso_action_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_id BIGINT NOT NULL,
    action_code VARCHAR(64) NOT NULL,
    project_id BIGINT NULL,
    assignee_user_id BIGINT NULL,
    assignee_name VARCHAR(64) NULL,
    title VARCHAR(255) NOT NULL,
    summary VARCHAR(1000) NULL,
    priority VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
    sla_due_at DATETIME NULL,
    route VARCHAR(500) NULL,
    source_status VARCHAR(32) NULL,
    action_status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    source_version INT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    closed_at DATETIME NULL,
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_action_projection (tenant_id, source_type, source_id, action_code),
    KEY idx_action_assignee_state_due (tenant_id, assignee_user_id, action_status, sla_due_at),
    KEY idx_action_project_state (tenant_id, project_id, action_status)
) COMMENT='可重建的统一行动项投影';

CREATE TABLE nso_exception_case (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    case_no VARCHAR(64) NOT NULL,
    project_id BIGINT NOT NULL,
    task_id BIGINT NULL,
    risk_id BIGINT NULL,
    exception_type VARCHAR(64) NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    reporter_name VARCHAR(64) NULL,
    owner_user_id BIGINT NOT NULL,
    owner_name VARCHAR(64) NULL,
    due_at DATETIME NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    containment_plan VARCHAR(1000) NULL,
    root_cause VARCHAR(1000) NULL,
    corrective_plan VARCHAR(1000) NULL,
    verification_summary VARCHAR(1000) NULL,
    close_conclusion VARCHAR(1000) NULL,
    idempotency_key VARCHAR(64) NULL,
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    escalated_at DATETIME NULL,
    closed_at DATETIME NULL,
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_exception_case_no (tenant_id, case_no),
    UNIQUE KEY uk_exception_case_idempotency (tenant_id, idempotency_key),
    KEY idx_exception_project_state_due (tenant_id, project_id, status, due_at),
    KEY idx_exception_owner_state_due (tenant_id, owner_user_id, status, due_at)
) COMMENT='异常 CAPA 闭环案例';

CREATE TABLE nso_exception_evidence (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    exception_case_id BIGINT NOT NULL,
    evidence_type VARCHAR(32) NOT NULL DEFAULT 'REFERENCE',
    evidence_ref VARCHAR(500) NOT NULL,
    summary VARCHAR(1000) NULL,
    submitted_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_exception_evidence_case (tenant_id, exception_case_id, created_at)
) COMMENT='异常 CAPA 证据索引';

CREATE TABLE nso_capa_transition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    exception_case_id BIGINT NOT NULL,
    from_status VARCHAR(32) NULL,
    to_status VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(64) NOT NULL,
    comment VARCHAR(1000) NULL,
    operator_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_capa_transition_idempotency (tenant_id, exception_case_id, idempotency_key),
    KEY idx_capa_transition_case (tenant_id, exception_case_id, created_at)
) COMMENT='异常 CAPA 状态迁移与幂等记录';

ALTER TABLE nso_task
    ADD COLUMN capa_case_id BIGINT NULL AFTER project_id,
    ADD COLUMN capa_idempotency_key VARCHAR(64) NULL AFTER capa_case_id;

CREATE INDEX idx_task_capa_case ON nso_task (tenant_id, capa_case_id, status);
CREATE UNIQUE INDEX uk_task_capa_idempotency ON nso_task (tenant_id, capa_case_id, capa_idempotency_key);

CREATE TABLE nso_approval_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    template_code VARCHAR(64) NOT NULL,
    business_type VARCHAR(32) NOT NULL,
    template_version INT NOT NULL,
    template_name VARCHAR(128) NOT NULL,
    approval_mode VARCHAR(16) NOT NULL DEFAULT 'SERIAL',
    sla_minutes INT NOT NULL DEFAULT 1440,
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_approval_template_version (tenant_id, template_code, template_version),
    KEY idx_approval_template_business (tenant_id, business_type, status)
) COMMENT='预定义业务审批模板版本';

CREATE TABLE nso_approval_template_node (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    node_order INT NOT NULL,
    node_code VARCHAR(64) NOT NULL,
    node_name VARCHAR(128) NOT NULL,
    responsibility_code VARCHAR(64) NOT NULL,
    sla_minutes INT NOT NULL DEFAULT 1440,
    escalation_role VARCHAR(64) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_approval_template_node (tenant_id, template_id, node_code),
    KEY idx_approval_template_node_order (tenant_id, template_id, node_order)
) COMMENT='预定义业务审批模板节点';

CREATE TABLE nso_approval_instance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    business_type VARCHAR(32) NOT NULL,
    business_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    template_id BIGINT NULL,
    template_code VARCHAR(64) NOT NULL,
    template_version INT NOT NULL,
    approval_mode VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    current_node_order INT NULL,
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at DATETIME NULL,
    escalated_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_approval_business_instance (tenant_id, business_type, business_id),
    KEY idx_approval_instance_project_state (tenant_id, project_id, status)
) COMMENT='冻结后的有限审批实例';

CREATE TABLE nso_approval_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    instance_id BIGINT NOT NULL,
    template_node_id BIGINT NULL,
    node_order INT NOT NULL,
    node_code VARCHAR(64) NOT NULL,
    node_name VARCHAR(128) NOT NULL,
    responsibility_code VARCHAR(64) NOT NULL,
    assignee_user_id BIGINT NULL,
    assignee_name VARCHAR(64) NULL,
    decision VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    opinion VARCHAR(1000) NULL,
    decided_by BIGINT NULL,
    decided_by_name VARCHAR(64) NULL,
    decided_at DATETIME NULL,
    due_at DATETIME NULL,
    escalated_at DATETIME NULL,
    decision_idempotency_key VARCHAR(64) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_approval_instance_node (tenant_id, instance_id, node_code),
    KEY idx_approval_task_assignee_due (tenant_id, assignee_user_id, decision, due_at),
    KEY idx_approval_task_instance_state (tenant_id, instance_id, decision)
) COMMENT='审批实例待办节点';

INSERT INTO nso_approval_template (tenant_id, template_code, business_type, template_version, template_name, approval_mode, sla_minutes, status)
VALUES
    (1, 'CHANGE_STANDARD', 'CHANGE', 1, '变更标准审批', 'SERIAL', 1440, 'PUBLISHED'),
    (1, 'SPECIAL_RELEASE_STANDARD', 'SPECIAL_RELEASE', 1, '特殊放行审批', 'SERIAL', 480, 'PUBLISHED'),
    (1, 'EXCEPTION_CLOSE_STANDARD', 'EXCEPTION', 1, '异常关闭审批', 'SERIAL', 1440, 'PUBLISHED')
ON DUPLICATE KEY UPDATE
    template_name = VALUES(template_name), approval_mode = VALUES(approval_mode), sla_minutes = VALUES(sla_minutes),
    status = VALUES(status), deleted = 0;

INSERT INTO nso_approval_template_node (tenant_id, template_id, node_order, node_code, node_name, responsibility_code, sla_minutes, escalation_role)
SELECT tenant_id, id, 10, 'PROJECT_MANAGER', '项目经理审批', 'PROJECT_MANAGER', 720, 'PROJECT_MANAGER'
FROM nso_approval_template
WHERE tenant_id = 1 AND template_code = 'CHANGE_STANDARD' AND template_version = 1
ON DUPLICATE KEY UPDATE node_name = VALUES(node_name), responsibility_code = VALUES(responsibility_code), sla_minutes = VALUES(sla_minutes), escalation_role = VALUES(escalation_role), deleted = 0;

INSERT INTO nso_approval_template_node (tenant_id, template_id, node_order, node_code, node_name, responsibility_code, sla_minutes, escalation_role)
SELECT tenant_id, id, 20, 'TECHNICAL', '技术审批', 'TECHNICAL', 720, 'PROJECT_MANAGER'
FROM nso_approval_template
WHERE tenant_id = 1 AND template_code = 'CHANGE_STANDARD' AND template_version = 1
ON DUPLICATE KEY UPDATE node_name = VALUES(node_name), responsibility_code = VALUES(responsibility_code), sla_minutes = VALUES(sla_minutes), escalation_role = VALUES(escalation_role), deleted = 0;

INSERT INTO nso_approval_template_node (tenant_id, template_id, node_order, node_code, node_name, responsibility_code, sla_minutes, escalation_role)
SELECT tenant_id, id, 30, 'PRODUCTION', '生产审批', 'PRODUCTION', 720, 'PROJECT_MANAGER'
FROM nso_approval_template
WHERE tenant_id = 1 AND template_code = 'CHANGE_STANDARD' AND template_version = 1
ON DUPLICATE KEY UPDATE node_name = VALUES(node_name), responsibility_code = VALUES(responsibility_code), sla_minutes = VALUES(sla_minutes), escalation_role = VALUES(escalation_role), deleted = 0;

INSERT INTO nso_approval_template_node (tenant_id, template_id, node_order, node_code, node_name, responsibility_code, sla_minutes, escalation_role)
SELECT tenant_id, id, 40, 'QUALITY', '质量审批', 'QUALITY', 720, 'PROJECT_MANAGER'
FROM nso_approval_template
WHERE tenant_id = 1 AND template_code = 'CHANGE_STANDARD' AND template_version = 1
ON DUPLICATE KEY UPDATE node_name = VALUES(node_name), responsibility_code = VALUES(responsibility_code), sla_minutes = VALUES(sla_minutes), escalation_role = VALUES(escalation_role), deleted = 0;

INSERT INTO nso_approval_template_node (tenant_id, template_id, node_order, node_code, node_name, responsibility_code, sla_minutes, escalation_role)
SELECT tenant_id, id, 10, 'QUALITY', '质量放行审批', 'QUALITY', 480, 'PROJECT_MANAGER'
FROM nso_approval_template
WHERE tenant_id = 1 AND template_code = 'SPECIAL_RELEASE_STANDARD' AND template_version = 1
ON DUPLICATE KEY UPDATE node_name = VALUES(node_name), responsibility_code = VALUES(responsibility_code), sla_minutes = VALUES(sla_minutes), escalation_role = VALUES(escalation_role), deleted = 0;

INSERT INTO nso_approval_template_node (tenant_id, template_id, node_order, node_code, node_name, responsibility_code, sla_minutes, escalation_role)
SELECT tenant_id, id, 10, 'PROJECT_MANAGER', '项目经理关闭确认', 'PROJECT_MANAGER', 720, 'PROJECT_MANAGER'
FROM nso_approval_template
WHERE tenant_id = 1 AND template_code = 'EXCEPTION_CLOSE_STANDARD' AND template_version = 1
ON DUPLICATE KEY UPDATE node_name = VALUES(node_name), responsibility_code = VALUES(responsibility_code), sla_minutes = VALUES(sla_minutes), escalation_role = VALUES(escalation_role), deleted = 0;

INSERT INTO nso_approval_template_node (tenant_id, template_id, node_order, node_code, node_name, responsibility_code, sla_minutes, escalation_role)
SELECT tenant_id, id, 20, 'QUALITY', '质量关闭确认', 'QUALITY', 720, 'PROJECT_MANAGER'
FROM nso_approval_template
WHERE tenant_id = 1 AND template_code = 'EXCEPTION_CLOSE_STANDARD' AND template_version = 1
ON DUPLICATE KEY UPDATE node_name = VALUES(node_name), responsibility_code = VALUES(responsibility_code), sla_minutes = VALUES(sla_minutes), escalation_role = VALUES(escalation_role), deleted = 0;

INSERT INTO sys_menu (menu_name, route_path, permission_code, component_name, visible, sort_no, status)
VALUES
    ('我的行动', '/actions', 'action:view', 'ActionCenter', 1, 16, 'ENABLED'),
    ('行动项管理', NULL, 'action:manage', NULL, 0, 17, 'ENABLED'),
    ('异常闭环查看', NULL, 'exception:view', NULL, 0, 74, 'ENABLED'),
    ('异常闭环处置', NULL, 'exception:manage', NULL, 0, 75, 'ENABLED'),
    ('审批待办查看', NULL, 'approval:view', NULL, 0, 56, 'ENABLED'),
    ('审批待办处理', NULL, 'approval:decide', NULL, 0, 57, 'ENABLED'),
    ('审批模板管理', NULL, 'approval:template:manage', NULL, 0, 58, 'ENABLED')
ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name), route_path = VALUES(route_path), component_name = VALUES(component_name),
    visible = VALUES(visible), sort_no = VALUES(sort_no), status = 'ENABLED', deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.permission_code IN ('action:view', 'exception:view', 'approval:view')
WHERE r.tenant_id = 1
  AND r.role_code IN ('admin', 'executive', 'project_manager', 'technical', 'process', 'purchaser', 'production', 'quality')
  AND r.deleted = 0
  AND m.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.permission_code IN ('action:manage', 'exception:manage', 'approval:decide')
WHERE r.tenant_id = 1
  AND r.role_code IN ('admin', 'project_manager', 'technical', 'process', 'purchaser', 'production', 'quality')
  AND r.deleted = 0
  AND m.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.permission_code = 'approval:template:manage'
WHERE r.tenant_id = 1
  AND r.role_code = 'admin'
  AND r.deleted = 0
  AND m.deleted = 0;

INSERT INTO sys_job (job_name, bean_name, cron_expression, concurrent_policy, status, allow_manual)
VALUES ('行动项与 CAPA SLA 升级', 'actionEscalationTask', '0 0/10 * * * ?', 'FORBID', 'RUNNING', 1)
ON DUPLICATE KEY UPDATE job_name = VALUES(job_name), cron_expression = VALUES(cron_expression),
    concurrent_policy = VALUES(concurrent_policy), status = VALUES(status), allow_manual = VALUES(allow_manual);
