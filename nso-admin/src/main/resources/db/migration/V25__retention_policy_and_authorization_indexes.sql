-- Retention is controlled, auditable and never silently deletes business history.

CREATE TABLE nso_retention_policy (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    policy_code VARCHAR(64) NOT NULL,
    retention_days INT NOT NULL,
    archive_after_days INT NOT NULL,
    purge_requires_confirmation TINYINT NOT NULL DEFAULT 1,
    enabled TINYINT NOT NULL DEFAULT 1,
    updated_by BIGINT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_retention_policy (tenant_id, policy_code)
) COMMENT='数据保留与归档策略';

CREATE TABLE nso_retention_run (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    policy_code VARCHAR(64) NOT NULL,
    run_type VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    affected_count INT NOT NULL DEFAULT 0,
    confirmation_ref VARCHAR(128) NULL,
    operator_id BIGINT NULL,
    detail VARCHAR(1000) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_retention_run_policy (tenant_id, policy_code, created_at)
) COMMENT='归档与清理执行记录';

INSERT INTO nso_retention_policy (tenant_id, policy_code, retention_days, archive_after_days, purge_requires_confirmation, enabled)
VALUES
    (1, 'TEMPORARY_SECURITY', 30, 1, 0, 1),
    (1, 'IMPORT_EXPORT_TEMPORARY', 30, 7, 0, 1),
    (1, 'BUSINESS_HISTORY', 2555, 365, 1, 1),
    (1, 'AUTHORIZATION_AUDIT', 2555, 365, 1, 1)
ON DUPLICATE KEY UPDATE retention_days = VALUES(retention_days), archive_after_days = VALUES(archive_after_days), purge_requires_confirmation = VALUES(purge_requires_confirmation), enabled = VALUES(enabled);

CREATE INDEX idx_project_member_responsibility_reconciliation ON nso_project_responsibility_reconciliation (tenant_id, status, project_id);
CREATE INDEX idx_authorization_audit_created ON nso_authorization_audit (tenant_id, created_at);

INSERT INTO sys_job (job_name, bean_name, cron_expression, concurrent_policy, status, allow_manual)
VALUES ('临时数据保留清理', 'retentionCleanupTask', '0 20 2 * * ?', 'FORBID', 'RUNNING', 1)
ON DUPLICATE KEY UPDATE job_name = VALUES(job_name), cron_expression = VALUES(cron_expression), status = VALUES(status), allow_manual = VALUES(allow_manual);
