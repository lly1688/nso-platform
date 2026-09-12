-- Versioned rule execution, risk details, notification recipients and escalation evidence.

ALTER TABLE nso_risk
    ADD COLUMN risk_no VARCHAR(64) NULL AFTER project_id,
    ADD COLUMN batch_key VARCHAR(64) NULL AFTER rule_version,
    ADD COLUMN input_snapshot JSON NULL AFTER batch_key,
    ADD COLUMN override_reason VARCHAR(1000) NULL AFTER input_snapshot,
    ADD COLUMN override_level VARCHAR(32) NULL AFTER override_reason,
    ADD COLUMN override_expires_at DATETIME NULL AFTER override_level,
    ADD COLUMN resolved_at DATETIME NULL AFTER override_expires_at;

CREATE UNIQUE INDEX uk_risk_rule_batch ON nso_risk (tenant_id, project_id, rule_code, batch_key, deleted);
CREATE INDEX idx_risk_no ON nso_risk (tenant_id, risk_no);

CREATE TABLE nso_risk_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    risk_id BIGINT NOT NULL,
    factor_code VARCHAR(64) NOT NULL,
    factor_name VARCHAR(128) NOT NULL,
    raw_value VARCHAR(500),
    score_delta INT NOT NULL,
    score_cap INT NULL,
    matched_flag TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_risk_detail_risk (tenant_id, risk_id)
) COMMENT='风险因子计算明细';

CREATE TABLE nso_rule_execution (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    rule_code VARCHAR(64) NOT NULL,
    rule_version VARCHAR(32) NOT NULL,
    business_type VARCHAR(64) NOT NULL,
    business_id BIGINT NOT NULL,
    batch_key VARCHAR(64) NOT NULL,
    input_snapshot JSON,
    decision_summary VARCHAR(2000),
    status VARCHAR(32) NOT NULL,
    failure_reason VARCHAR(1000),
    retry_count INT NOT NULL DEFAULT 0,
    duration_ms BIGINT NULL,
    trace_id VARCHAR(128) NULL,
    executed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_rule_execution (tenant_id, rule_code, business_type, business_id, batch_key),
    KEY idx_rule_execution_retry (tenant_id, status, retry_count, executed_at)
) COMMENT='规则执行生命周期记录';

CREATE TABLE nso_message_recipient_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    recipient_role VARCHAR(64) NULL,
    recipient_project_role VARCHAR(64) NULL,
    channel VARCHAR(32) NOT NULL DEFAULT 'IN_APP',
    escalation_minutes INT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    UNIQUE KEY uk_message_recipient_rule (tenant_id, event_type, recipient_role, recipient_project_role, channel)
) COMMENT='消息事件接收人矩阵';

CREATE TABLE nso_message_escalation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    project_id BIGINT NULL,
    escalation_level INT NOT NULL DEFAULT 1,
    target_user_id BIGINT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    due_at DATETIME NOT NULL,
    executed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_message_escalation_due (tenant_id, status, due_at)
) COMMENT='消息逐级升级记录';

INSERT INTO nso_message_recipient_rule (tenant_id, event_type, recipient_project_role, channel, escalation_minutes)
VALUES
    (1, 'DOCUMENT_PUBLISHED', 'PROJECT_MANAGER', 'IN_APP', NULL),
    (1, 'DOCUMENT_PUBLISHED', 'PURCHASE', 'IN_APP', NULL),
    (1, 'DOCUMENT_PUBLISHED', 'PRODUCTION', 'IN_APP', NULL),
    (1, 'DOCUMENT_PUBLISHED', 'QUALITY', 'IN_APP', NULL),
    (1, 'CHANGE_APPROVED', 'PROJECT_MANAGER', 'IN_APP', 120),
    (1, 'SAMPLE_CONFIRM_DUE', 'PROJECT_MANAGER', 'IN_APP', 1440),
    (1, 'RISK_SERIOUS', 'PROJECT_MANAGER', 'IN_APP', 120),
    (1, 'TASK_OVERDUE', 'PROJECT_MANAGER', 'IN_APP', 120)
ON DUPLICATE KEY UPDATE escalation_minutes = VALUES(escalation_minutes), enabled = 1;
