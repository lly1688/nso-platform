-- V1.0 生产治理扩展。V1-V6 均有意保持不可变。

CREATE TABLE IF NOT EXISTS nso_tenant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_code VARCHAR(64) NOT NULL,
    tenant_name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_nso_tenant_code (tenant_code)
) COMMENT='企业租户';

INSERT INTO nso_tenant (id, tenant_code, tenant_name, status)
VALUES (1, 'DEFAULT', '默认企业', 'ENABLED')
ON DUPLICATE KEY UPDATE tenant_name = VALUES(tenant_name), status = VALUES(status);

ALTER TABLE sys_user
    ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1,
    ADD COLUMN dept_id BIGINT NULL;

ALTER TABLE nso_file_object
    ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1,
    ADD COLUMN created_by BIGINT NULL;

ALTER TABLE nso_timeline_event
    ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1,
    ADD COLUMN business_type VARCHAR(64) NULL,
    ADD COLUMN business_id BIGINT NULL,
    ADD COLUMN trace_id VARCHAR(64) NULL;

ALTER TABLE nso_audit_log
    ADD COLUMN user_id BIGINT NULL,
    ADD COLUMN request_id VARCHAR(96) NULL;

ALTER TABLE nso_change_impact
    ADD COLUMN object_id BIGINT NULL,
    ADD COLUMN object_version VARCHAR(64) NULL;

CREATE INDEX idx_sys_user_tenant_status ON sys_user (tenant_id, status);
CREATE INDEX idx_file_object_tenant ON nso_file_object (tenant_id, id);
CREATE INDEX idx_timeline_tenant_project ON nso_timeline_event (tenant_id, project_id, occurred_at);
CREATE INDEX idx_change_impact_object ON nso_change_impact (tenant_id, change_id, object_type, object_id);

CREATE TABLE IF NOT EXISTS nso_idempotency_key (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL DEFAULT 0,
    request_id VARCHAR(96) NOT NULL,
    request_method VARCHAR(16) NOT NULL,
    request_path VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PROCESSING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME NULL,
    UNIQUE KEY uk_idempotency_request (tenant_id, user_id, request_id, request_method, request_path)
) COMMENT='关键写操作幂等键';

CREATE TABLE IF NOT EXISTS nso_business_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    project_id BIGINT NULL,
    business_type VARCHAR(64) NOT NULL,
    business_id BIGINT NULL,
    action_code VARCHAR(64) NOT NULL,
    before_summary VARCHAR(1000) NULL,
    after_summary VARCHAR(1000) NULL,
    operator_id BIGINT NULL,
    operator_name VARCHAR(64) NULL,
    trace_id VARCHAR(64) NULL,
    occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_business_event_project (tenant_id, project_id, occurred_at),
    KEY idx_business_event_object (tenant_id, business_type, business_id, occurred_at)
) COMMENT='不可变业务事件与状态历史';

CREATE TABLE IF NOT EXISTS nso_attachment_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    project_id BIGINT NULL,
    business_type VARCHAR(64) NOT NULL,
    business_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    evidence_type VARCHAR(64) NOT NULL DEFAULT 'ATTACHMENT',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_attachment_relation (tenant_id, business_type, business_id, file_id, evidence_type)
) COMMENT='业务附件与确认凭证关联';
