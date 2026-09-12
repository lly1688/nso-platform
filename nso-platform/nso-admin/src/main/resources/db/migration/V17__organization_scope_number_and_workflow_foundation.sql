-- V1.0 acceptance completion: organization scope, business numbers and workflow history.
-- This migration is deliberately additive to the immutable V1-V16 chain.

ALTER TABLE sys_dept
    ADD COLUMN leader_user_id BIGINT NULL AFTER leader_name,
    ADD COLUMN ancestors VARCHAR(1000) NOT NULL DEFAULT '' AFTER parent_id;

ALTER TABLE sys_role
    ADD COLUMN data_scope VARCHAR(32) NOT NULL DEFAULT 'SELF' AFTER status;

UPDATE sys_role
SET data_scope = CASE role_code
    WHEN 'admin' THEN 'ALL'
    WHEN 'executive' THEN 'ALL'
    WHEN 'project_manager' THEN 'DEPT_AND_CHILD'
    ELSE 'SELF'
END;

CREATE TABLE nso_sensitive_field_policy (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    field_code VARCHAR(128) NOT NULL,
    read_mode VARCHAR(32) NOT NULL DEFAULT 'MASKED',
    write_allowed TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sensitive_field_policy (tenant_id, role_code, field_code)
) COMMENT='敏感字段读取与编辑策略';

INSERT INTO nso_sensitive_field_policy (tenant_id, role_code, field_code, read_mode, write_allowed)
VALUES
    (1, 'admin', 'CUSTOMER_CONTACT', 'FULL', 1),
    (1, 'project_manager', 'CUSTOMER_CONTACT', 'FULL', 1),
    (1, 'technical', 'CUSTOMER_CONTACT', 'MASKED', 0),
    (1, 'process', 'CUSTOMER_CONTACT', 'MASKED', 0),
    (1, 'purchaser', 'CUSTOMER_CONTACT', 'MASKED', 0),
    (1, 'production', 'CUSTOMER_CONTACT', 'MASKED', 0),
    (1, 'quality', 'CUSTOMER_CONTACT', 'MASKED', 0),
    (1, 'executive', 'CUSTOMER_CONTACT', 'MASKED', 0)
ON DUPLICATE KEY UPDATE read_mode = VALUES(read_mode), write_allowed = VALUES(write_allowed);

CREATE TABLE nso_business_sequence (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    business_type VARCHAR(64) NOT NULL,
    period_key VARCHAR(32) NOT NULL,
    next_value BIGINT NOT NULL DEFAULT 1,
    version INT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_business_sequence (tenant_id, business_type, period_key)
) COMMENT='业务编号流水号';

ALTER TABLE nso_project
    ADD COLUMN project_name VARCHAR(128) NULL AFTER product_name,
    ADD COLUMN archived_at DATETIME NULL AFTER sample_status,
    ADD COLUMN archived_by BIGINT NULL AFTER archived_at,
    ADD COLUMN archive_reason VARCHAR(1000) NULL AFTER archived_by,
    ADD COLUMN suspended_reason VARCHAR(1000) NULL AFTER archive_reason;

UPDATE nso_project SET project_name = product_name WHERE project_name IS NULL;

ALTER TABLE nso_change_order
    ADD COLUMN parent_change_id BIGINT NULL AFTER project_id,
    ADD COLUMN revoked_at DATETIME NULL AFTER approval_node,
    ADD COLUMN revoke_reason VARCHAR(1000) NULL AFTER revoked_at,
    ADD COLUMN verified_at DATETIME NULL AFTER revoke_reason,
    ADD COLUMN verified_by BIGINT NULL AFTER verified_at;

ALTER TABLE nso_sample
    ADD COLUMN parent_sample_id BIGINT NULL AFTER project_id,
    ADD COLUMN round_no INT NOT NULL DEFAULT 1 AFTER parent_sample_id,
    ADD COLUMN previous_issue_summary VARCHAR(1000) NULL AFTER round_no;

ALTER TABLE nso_process_step ADD COLUMN version INT NOT NULL DEFAULT 0, ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0;
ALTER TABLE nso_inspection_item ADD COLUMN version INT NOT NULL DEFAULT 0, ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0;
ALTER TABLE nso_sample_check ADD COLUMN version INT NOT NULL DEFAULT 0, ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0;
ALTER TABLE nso_sample_confirm ADD COLUMN version INT NOT NULL DEFAULT 0, ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0;
ALTER TABLE nso_change_impact ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0;

CREATE TABLE nso_project_status_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    before_status VARCHAR(64),
    after_status VARCHAR(64) NOT NULL,
    action_code VARCHAR(64) NOT NULL,
    reason VARCHAR(1000),
    operator_id BIGINT,
    occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_project_status_history (tenant_id, project_id, occurred_at)
) COMMENT='项目状态迁移历史';

CREATE TABLE nso_change_status_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    change_id BIGINT NOT NULL,
    before_status VARCHAR(64),
    after_status VARCHAR(64) NOT NULL,
    action_code VARCHAR(64) NOT NULL,
    reason VARCHAR(1000),
    operator_id BIGINT,
    occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_change_status_history (tenant_id, change_id, occurred_at)
) COMMENT='变更状态迁移历史';

CREATE INDEX idx_task_scope_assignee_status_finish ON nso_task (tenant_id, assignee_id, status, plan_finish);
CREATE INDEX idx_risk_scope_project_status_level ON nso_risk (tenant_id, project_id, status, level);
