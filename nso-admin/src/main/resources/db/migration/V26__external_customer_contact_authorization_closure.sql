-- External customer closure: contacts, project access, hashed confirmation tokens and owner-slot repair.
-- V1-V25 are immutable.  This migration preserves legacy accounts, confirmation links and audit evidence.

ALTER TABLE nso_external_identity
    ADD COLUMN contact_id BIGINT NULL AFTER customer_id;

ALTER TABLE nso_external_token
    ADD COLUMN legacy_confirmation_id BIGINT NULL AFTER sample_id,
    ADD COLUMN revocation_reason VARCHAR(512) NULL AFTER revoked_at;

CREATE UNIQUE INDEX uk_external_identity_contact ON nso_external_identity (tenant_id, contact_id);
CREATE UNIQUE INDEX uk_external_token_legacy_confirmation ON nso_external_token (tenant_id, legacy_confirmation_id);

CREATE TABLE nso_external_project_access (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    identity_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    valid_until DATETIME NULL,
    granted_by BIGINT NULL,
    granted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_by BIGINT NULL,
    revoked_at DATETIME NULL,
    revoke_reason VARCHAR(512) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_external_project_access (tenant_id, identity_id, project_id),
    KEY idx_external_project_access_scope (tenant_id, project_id, status, valid_until)
) COMMENT='客户外部身份的项目授权，不属于内部项目成员';

CREATE TABLE nso_external_identity_reconciliation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_id BIGINT NOT NULL,
    project_id BIGINT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    detail VARCHAR(1000) NOT NULL,
    resolved_by BIGINT NULL,
    resolved_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_external_identity_reconciliation (tenant_id, source_type, source_id, status)
) COMMENT='历史外部身份或确认链接待补齐联系人清单';

-- Reuse the customer contact table.  Existing customer master data becomes an idempotent initial contact.
INSERT INTO crm_contact (tenant_id, customer_id, contact_name, phone, email, position_name, preferred_channel, status, deleted)
SELECT c.tenant_id, c.id, c.contact_name, c.phone, NULL, '客户确认联系人', 'LINK', 'ENABLED', 0
FROM nso_customer c
WHERE c.deleted = 0 AND c.contact_name IS NOT NULL AND TRIM(c.contact_name) <> ''
  AND NOT EXISTS (
      SELECT 1 FROM crm_contact cc
      WHERE cc.tenant_id = c.tenant_id AND cc.customer_id = c.id AND cc.deleted = 0
        AND cc.contact_name = c.contact_name
  );

-- Attach legacy external identities to the project customer only when the relationship is unambiguous.
UPDATE nso_external_identity ei
JOIN (
    SELECT pm.tenant_id, pm.user_id, MIN(p.customer_id) AS customer_id
    FROM nso_project_member pm
    JOIN nso_project p ON p.id = pm.project_id AND p.tenant_id = pm.tenant_id AND p.deleted = 0
    JOIN sys_user u ON u.id = pm.user_id AND u.tenant_id = pm.tenant_id
    WHERE pm.deleted = 0 AND u.user_type = 'EXTERNAL_LEGACY' AND p.customer_id IS NOT NULL
    GROUP BY pm.tenant_id, pm.user_id
    HAVING COUNT(DISTINCT p.customer_id) = 1
) inferred ON inferred.tenant_id = ei.tenant_id AND inferred.user_id = ei.legacy_user_id
LEFT JOIN (
    SELECT tenant_id, customer_id, MIN(id) AS contact_id
    FROM crm_contact
    WHERE deleted = 0 AND status = 'ENABLED'
    GROUP BY tenant_id, customer_id
    HAVING COUNT(*) = 1
) contact_choice ON contact_choice.tenant_id = inferred.tenant_id AND contact_choice.customer_id = inferred.customer_id
SET ei.customer_id = COALESCE(ei.customer_id, inferred.customer_id),
    ei.contact_id = COALESCE(ei.contact_id, contact_choice.contact_id)
WHERE ei.customer_id IS NULL OR ei.contact_id IS NULL;

-- Create external identities for customer contacts that already own a live legacy confirmation link.
INSERT IGNORE INTO nso_external_identity (tenant_id, customer_id, contact_id, legacy_user_id, name, mobile, email, status)
SELECT DISTINCT p.tenant_id, p.customer_id, cc.id, NULL, cc.contact_name, cc.phone, cc.email, 'ACTIVE'
FROM nso_sample_confirm sc
JOIN nso_sample s ON s.id = sc.sample_id AND s.tenant_id = sc.tenant_id
JOIN nso_project p ON p.id = s.project_id AND p.tenant_id = s.tenant_id AND p.deleted = 0
JOIN (
    SELECT tenant_id, customer_id, MIN(id) AS contact_id
    FROM crm_contact
    WHERE deleted = 0 AND status = 'ENABLED'
    GROUP BY tenant_id, customer_id
    HAVING COUNT(*) = 1
) contact_choice ON contact_choice.tenant_id = p.tenant_id AND contact_choice.customer_id = p.customer_id
JOIN crm_contact cc ON cc.id = contact_choice.contact_id AND cc.tenant_id = contact_choice.tenant_id
WHERE sc.token IS NOT NULL AND sc.token <> '';

-- Every active legacy link becomes a hashed external token when a customer contact can be identified.
INSERT IGNORE INTO nso_external_project_access (tenant_id, identity_id, project_id, status, granted_at)
SELECT DISTINCT ei.tenant_id, ei.id, p.id, 'ACTIVE', NOW()
FROM nso_external_identity ei
JOIN nso_sample_confirm sc ON sc.tenant_id = ei.tenant_id
JOIN nso_sample s ON s.id = sc.sample_id AND s.tenant_id = sc.tenant_id
JOIN nso_project p ON p.id = s.project_id AND p.tenant_id = s.tenant_id
WHERE ei.status = 'ACTIVE' AND ei.customer_id = p.customer_id;

-- Preserve the former customer member's project relationship as external access before removing it from the internal team.
INSERT IGNORE INTO nso_external_project_access (tenant_id, identity_id, project_id, status, granted_at)
SELECT ei.tenant_id, ei.id, pm.project_id, 'ACTIVE', NOW()
FROM nso_external_identity ei
JOIN nso_project_member pm ON pm.tenant_id = ei.tenant_id AND pm.user_id = ei.legacy_user_id
JOIN nso_project p ON p.id = pm.project_id AND p.tenant_id = pm.tenant_id AND p.deleted = 0
WHERE ei.status = 'ACTIVE' AND ei.contact_id IS NOT NULL AND pm.deleted = 0
  AND pm.status = 'ACTIVE' AND p.customer_id = ei.customer_id;

INSERT IGNORE INTO nso_external_token
    (tenant_id, identity_id, project_id, sample_id, legacy_confirmation_id, token_hash, expire_at, max_uses, used_count, status, created_at)
SELECT sc.tenant_id, ei.id, p.id, s.id, sc.id, SHA2(sc.token, 256), sc.expire_at,
       GREATEST(1, COALESCE(sc.max_use_count, 1)), COALESCE(sc.used_count, 0),
       CASE WHEN sc.expire_at <= NOW() THEN 'EXPIRED'
            WHEN COALESCE(sc.used_count, 0) >= GREATEST(1, COALESCE(sc.max_use_count, 1)) OR COALESCE(sc.used_flag, 0) = 1 THEN 'USED'
            ELSE 'ACTIVE' END,
       NOW()
FROM nso_sample_confirm sc
JOIN nso_sample s ON s.id = sc.sample_id AND s.tenant_id = sc.tenant_id
JOIN nso_project p ON p.id = s.project_id AND p.tenant_id = s.tenant_id
JOIN (
    SELECT tenant_id, customer_id, MIN(id) AS identity_id
    FROM nso_external_identity
    WHERE status = 'ACTIVE' AND contact_id IS NOT NULL
    GROUP BY tenant_id, customer_id
    HAVING COUNT(*) = 1
) identity_choice ON identity_choice.tenant_id = p.tenant_id AND identity_choice.customer_id = p.customer_id
JOIN nso_external_identity ei ON ei.id = identity_choice.identity_id AND ei.tenant_id = identity_choice.tenant_id
JOIN nso_external_project_access epa ON epa.tenant_id = ei.tenant_id AND epa.identity_id = ei.id AND epa.project_id = p.id AND epa.status = 'ACTIVE'
WHERE sc.token IS NOT NULL AND sc.token <> '';

INSERT IGNORE INTO nso_external_identity_reconciliation (tenant_id, source_type, source_id, project_id, detail)
SELECT sc.tenant_id, 'LEGACY_CONFIRMATION', sc.id, s.project_id, '历史确认链接未能唯一关联到启用的客户联系人，请在 PC 客户授权页补齐后重新发起链接'
FROM nso_sample_confirm sc
JOIN nso_sample s ON s.id = sc.sample_id AND s.tenant_id = sc.tenant_id
LEFT JOIN nso_external_token et ON et.tenant_id = sc.tenant_id AND et.legacy_confirmation_id = sc.id
WHERE et.id IS NULL AND sc.token IS NOT NULL AND sc.token <> '';

-- The former demo customer remains historical evidence only; it no longer grants membership or login access.
UPDATE nso_project_member_responsibility pmr
JOIN nso_project_member pm ON pm.id = pmr.project_member_id AND pm.tenant_id = pmr.tenant_id
JOIN sys_user u ON u.id = pm.user_id AND u.tenant_id = pm.tenant_id
SET pmr.status = 'REMOVED', pmr.primary_flag = 0, pmr.owner_slot = NULL
WHERE u.user_type = 'EXTERNAL_LEGACY' AND pmr.status = 'ACTIVE' AND pmr.deleted = 0;

UPDATE nso_project_member pm
JOIN sys_user u ON u.id = pm.user_id AND u.tenant_id = pm.tenant_id
SET pm.status = 'REMOVED'
WHERE u.user_type = 'EXTERNAL_LEGACY' AND pm.status = 'ACTIVE' AND pm.deleted = 0;

UPDATE sys_user
SET status = 'DISABLED', dept_id = NULL, status_reason = '已迁移为客户外部身份，仅保留历史关联'
WHERE user_type = 'EXTERNAL_LEGACY' AND status = 'ACTIVE';

-- V24 wrote owner_slot with the old value after responsibility_code changed.  Rebuild slots only where the owner is unique.
UPDATE nso_project_member_responsibility
SET owner_slot = NULL
WHERE deleted = 0 AND status = 'ACTIVE'
  AND responsibility_code IN ('PROJECT_MANAGER', 'TECH_OWNER', 'PROCESS_OWNER', 'PURCHASE_OWNER', 'PRODUCTION_OWNER', 'QUALITY_OWNER');

UPDATE nso_project_member_responsibility pmr
JOIN (
    SELECT tenant_id, project_id, responsibility_code
    FROM nso_project_member_responsibility
    WHERE deleted = 0 AND status = 'ACTIVE'
      AND responsibility_code IN ('PROJECT_MANAGER', 'TECH_OWNER', 'PROCESS_OWNER', 'PURCHASE_OWNER', 'PRODUCTION_OWNER', 'QUALITY_OWNER')
    GROUP BY tenant_id, project_id, responsibility_code
    HAVING COUNT(*) = 1
) unique_owner ON unique_owner.tenant_id = pmr.tenant_id
    AND unique_owner.project_id = pmr.project_id
    AND unique_owner.responsibility_code = pmr.responsibility_code
SET pmr.owner_slot = CONCAT(pmr.project_id, ':', pmr.responsibility_code)
WHERE pmr.deleted = 0 AND pmr.status = 'ACTIVE';

INSERT IGNORE INTO nso_project_responsibility_reconciliation (tenant_id, project_id, responsibility_code, detail)
SELECT tenant_id, project_id, responsibility_code, '项目存在多名同类负责人，未自动指定唯一 Owner，请在 PC 项目页补齐'
FROM nso_project_member_responsibility
WHERE deleted = 0 AND status = 'ACTIVE'
  AND responsibility_code IN ('PROJECT_MANAGER', 'TECH_OWNER', 'PROCESS_OWNER', 'PURCHASE_OWNER', 'PRODUCTION_OWNER', 'QUALITY_OWNER')
GROUP BY tenant_id, project_id, responsibility_code
HAVING COUNT(*) > 1;
