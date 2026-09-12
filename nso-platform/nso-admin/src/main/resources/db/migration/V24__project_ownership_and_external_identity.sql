-- Canonical project responsibilities and external customer identities.

ALTER TABLE nso_project_member_responsibility
    ADD COLUMN owner_slot VARCHAR(160) NULL AFTER primary_flag;

CREATE UNIQUE INDEX uk_project_active_owner_slot ON nso_project_member_responsibility (tenant_id, owner_slot);

-- Legacy responsibilities become members first. Ownership is only inferred when safe.
UPDATE nso_project_member_responsibility SET responsibility_code = 'TECH_MEMBER'
WHERE responsibility_code = 'TECHNICAL';
UPDATE nso_project_member_responsibility SET responsibility_code = 'PROCESS_MEMBER'
WHERE responsibility_code = 'PROCESS';
UPDATE nso_project_member_responsibility SET responsibility_code = 'PURCHASE_MEMBER'
WHERE responsibility_code = 'PURCHASER';
UPDATE nso_project_member_responsibility SET responsibility_code = 'PRODUCTION_MEMBER'
WHERE responsibility_code = 'PRODUCTION';
UPDATE nso_project_member_responsibility SET responsibility_code = 'QUALITY_MEMBER'
WHERE responsibility_code = 'QUALITY';
UPDATE nso_project_member_responsibility pmr
JOIN (
    SELECT tenant_id, project_id, responsibility_code
    FROM nso_project_member_responsibility
    WHERE status = 'ACTIVE' AND deleted = 0
      AND responsibility_code IN ('TECH_MEMBER','PROCESS_MEMBER','PURCHASE_MEMBER','PRODUCTION_MEMBER','QUALITY_MEMBER')
    GROUP BY tenant_id, project_id, responsibility_code
    HAVING COUNT(*) = 1
) single_member ON single_member.tenant_id = pmr.tenant_id AND single_member.project_id = pmr.project_id
    AND single_member.responsibility_code = pmr.responsibility_code
SET pmr.responsibility_code = CASE pmr.responsibility_code
    WHEN 'TECH_MEMBER' THEN 'TECH_OWNER'
    WHEN 'PROCESS_MEMBER' THEN 'PROCESS_OWNER'
    WHEN 'PURCHASE_MEMBER' THEN 'PURCHASE_OWNER'
    WHEN 'PRODUCTION_MEMBER' THEN 'PRODUCTION_OWNER'
    WHEN 'QUALITY_MEMBER' THEN 'QUALITY_OWNER'
END,
pmr.owner_slot = CONCAT(pmr.project_id, ':', CASE pmr.responsibility_code
    WHEN 'TECH_MEMBER' THEN 'TECH_OWNER'
    WHEN 'PROCESS_MEMBER' THEN 'PROCESS_OWNER'
    WHEN 'PURCHASE_MEMBER' THEN 'PURCHASE_OWNER'
    WHEN 'PRODUCTION_MEMBER' THEN 'PRODUCTION_OWNER'
    WHEN 'QUALITY_MEMBER' THEN 'QUALITY_OWNER'
END)
WHERE pmr.status = 'ACTIVE' AND pmr.deleted = 0;

UPDATE nso_project_member_responsibility pmr
JOIN (
    SELECT tenant_id, project_id FROM nso_project_member_responsibility
    WHERE responsibility_code = 'PROJECT_MANAGER' AND status = 'ACTIVE' AND deleted = 0
    GROUP BY tenant_id, project_id HAVING COUNT(*) = 1
) single_manager ON single_manager.tenant_id = pmr.tenant_id AND single_manager.project_id = pmr.project_id
SET pmr.owner_slot = CONCAT(pmr.project_id, ':PROJECT_MANAGER')
WHERE pmr.responsibility_code = 'PROJECT_MANAGER' AND pmr.status = 'ACTIVE' AND pmr.deleted = 0;

CREATE TABLE nso_project_responsibility_reconciliation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    responsibility_code VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    detail VARCHAR(1000) NOT NULL,
    resolved_by BIGINT NULL,
    resolved_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_responsibility_reconciliation (tenant_id, project_id, responsibility_code, status)
) COMMENT='项目唯一负责人待补齐清单';

INSERT IGNORE INTO nso_project_responsibility_reconciliation (tenant_id, project_id, responsibility_code, detail)
SELECT pmr.tenant_id, pmr.project_id,
       REPLACE(pmr.responsibility_code, '_MEMBER', '_OWNER'),
       '存量项目存在多名专业成员，未自动推断唯一负责人'
FROM nso_project_member_responsibility pmr
WHERE pmr.status = 'ACTIVE' AND pmr.deleted = 0
  AND pmr.responsibility_code IN ('TECH_MEMBER','PROCESS_MEMBER','PURCHASE_MEMBER','PRODUCTION_MEMBER','QUALITY_MEMBER')
GROUP BY pmr.tenant_id, pmr.project_id, pmr.responsibility_code
HAVING COUNT(*) > 1;

CREATE TABLE nso_external_identity (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    customer_id BIGINT NULL,
    legacy_user_id BIGINT NULL,
    name VARCHAR(128) NOT NULL,
    mobile VARCHAR(64) NULL,
    email VARCHAR(128) NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_external_identity_legacy_user (tenant_id, legacy_user_id),
    KEY idx_external_identity_customer (tenant_id, customer_id, status)
) COMMENT='客户确认外部身份，不进入内部角色体系';

CREATE TABLE nso_external_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    identity_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    sample_id BIGINT NULL,
    token_hash VARCHAR(128) NOT NULL,
    expire_at DATETIME NOT NULL,
    max_uses INT NOT NULL DEFAULT 1,
    used_count INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at DATETIME NULL,
    UNIQUE KEY uk_external_token_hash (token_hash),
    KEY idx_external_token_scope (tenant_id, identity_id, project_id, status, expire_at)
) COMMENT='客户确认短期令牌，仅保存哈希值';

INSERT IGNORE INTO nso_external_identity (tenant_id, legacy_user_id, name, mobile, email, status)
SELECT tenant_id, id, COALESCE(NULLIF(nickname, ''), username), phone, email, 'ACTIVE'
FROM sys_user WHERE user_type = 'EXTERNAL';

UPDATE sys_user SET user_type = 'EXTERNAL_LEGACY', force_change_password = 1
WHERE user_type = 'EXTERNAL';
