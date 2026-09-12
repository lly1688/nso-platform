-- Account lifecycle, posts, department management history and handover.
-- V1-V21 are immutable. This migration is additive and preserves every existing identity.

ALTER TABLE sys_user
    ADD COLUMN employee_no VARCHAR(64) NULL AFTER username,
    ADD COLUMN force_change_password TINYINT NOT NULL DEFAULT 0 AFTER password_hash,
    ADD COLUMN status_reason VARCHAR(512) NULL AFTER status,
    ADD COLUMN status_effective_until DATETIME NULL AFTER status_reason;

CREATE UNIQUE INDEX uk_sys_user_tenant_employee_no ON sys_user (tenant_id, employee_no);
CREATE INDEX idx_sys_user_lifecycle ON sys_user (tenant_id, user_type, status, dept_id);

-- Existing enabled/disabled directory records become the explicit lifecycle vocabulary.
UPDATE sys_user SET status = 'ACTIVE' WHERE status = 'ENABLED';
UPDATE sys_user SET status = 'DISABLED' WHERE status IS NULL OR status = '';

CREATE TABLE sys_post (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    post_code VARCHAR(64) NOT NULL,
    post_name VARCHAR(128) NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_post_tenant_code (tenant_id, post_code),
    KEY idx_sys_post_tenant_status (tenant_id, status, deleted)
) COMMENT='组织岗位，不等同于系统角色';

CREATE TABLE sys_user_post (
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    primary_flag TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, post_id),
    KEY idx_sys_user_post_tenant_post (tenant_id, post_id)
) COMMENT='用户与岗位关系';

CREATE TABLE sys_dept_manager (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    dept_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    manager_type VARCHAR(16) NOT NULL,
    effective_from DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    effective_to DATETIME NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT NULL,
    reason VARCHAR(512) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_dept_manager_current (tenant_id, dept_id, status, manager_type),
    KEY idx_dept_manager_user (tenant_id, user_id, status)
) COMMENT='部门负责人及副负责人任职历史';

CREATE TABLE nso_user_handover (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    from_user_id BIGINT NOT NULL,
    to_user_id BIGINT NULL,
    handover_type VARCHAR(32) NOT NULL,
    scope_type VARCHAR(32) NOT NULL,
    scope_id BIGINT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    reason VARCHAR(1000) NOT NULL,
    requested_by BIGINT NULL,
    completed_by BIGINT NULL,
    completed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_handover_from_status (tenant_id, from_user_id, status),
    KEY idx_user_handover_to_status (tenant_id, to_user_id, status)
) COMMENT='调岗、停用与离职交接单';

CREATE TABLE nso_authorization_audit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    subject_type VARCHAR(32) NOT NULL,
    subject_id BIGINT NOT NULL,
    change_type VARCHAR(64) NOT NULL,
    before_json JSON NULL,
    after_json JSON NULL,
    reason VARCHAR(1000) NOT NULL,
    operator_id BIGINT NULL,
    trace_id VARCHAR(128) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_authorization_audit_subject (tenant_id, subject_type, subject_id, created_at)
) COMMENT='账号、组织、授权和职责变更审计';
