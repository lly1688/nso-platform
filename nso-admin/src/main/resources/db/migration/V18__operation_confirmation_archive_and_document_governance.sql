-- High-risk operation confirmation, archive lifecycle and controlled-file governance.

CREATE TABLE nso_operation_confirmation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    operation_code VARCHAR(64) NOT NULL,
    business_type VARCHAR(64) NOT NULL,
    business_id BIGINT NOT NULL,
    requester_id BIGINT,
    reason VARCHAR(1000) NOT NULL,
    challenge_id BIGINT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    expires_at DATETIME NOT NULL,
    confirmed_at DATETIME NULL,
    consumed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_operation_confirmation_lookup (tenant_id, operation_code, business_type, business_id, status, expires_at)
) COMMENT='高风险操作二次确认';

CREATE TABLE nso_verification_challenge (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    receiver_id BIGINT NULL,
    channel VARCHAR(32) NOT NULL,
    purpose VARCHAR(64) NOT NULL,
    code_hash VARCHAR(128) NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    expires_at DATETIME NOT NULL,
    verified_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_verification_challenge (tenant_id, receiver_id, purpose, status, expires_at)
) COMMENT='一次性验证码挑战';

CREATE TABLE nso_project_archive (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    project_no VARCHAR(64) NOT NULL,
    customer_name VARCHAR(128),
    archived_by BIGINT,
    archive_reason VARCHAR(1000) NOT NULL,
    archived_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    restored_by BIGINT NULL,
    restored_at DATETIME NULL,
    restore_reason VARCHAR(1000) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ARCHIVED',
    UNIQUE KEY uk_project_archive (tenant_id, project_id),
    KEY idx_project_archive_search (tenant_id, project_no, customer_name, archived_at)
) COMMENT='项目归档与恢复记录';

CREATE TABLE nso_document_compare (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    before_version_id BIGINT NOT NULL,
    after_version_id BIGINT NOT NULL,
    comparison_file_id BIGINT NULL,
    summary VARCHAR(2000),
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_document_compare (tenant_id, before_version_id, after_version_id)
) COMMENT='技术版本对比记录';

CREATE TABLE nso_file_cleanup_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    file_object_id BIGINT NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    last_error VARCHAR(1000),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_file_cleanup (tenant_id, file_object_id, status)
) COMMENT='孤立文件清理队列';

ALTER TABLE nso_export_task
    ADD COLUMN confirmation_id BIGINT NULL AFTER user_id;

ALTER TABLE sys_dict_data
    ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    ADD COLUMN is_system TINYINT NOT NULL DEFAULT 0,
    ADD COLUMN create_by VARCHAR(64) NULL,
    ADD COLUMN disabled_at DATETIME NULL;
