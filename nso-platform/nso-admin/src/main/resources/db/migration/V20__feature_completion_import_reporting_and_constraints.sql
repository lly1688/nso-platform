-- Remaining acceptance items: import batches, supplier execution data and reporting indexes.

ALTER TABLE nso_purchase_task
    ADD COLUMN supplier_contact VARCHAR(128) NULL AFTER supplier_name,
    ADD COLUMN material_exception_code VARCHAR(64) NULL AFTER exception_summary;

ALTER TABLE nso_import_task
    ADD COLUMN batch_no VARCHAR(64) NULL AFTER import_type,
    ADD COLUMN result_summary JSON NULL AFTER error_file_id;

CREATE UNIQUE INDEX uk_import_batch ON nso_import_task (tenant_id, import_type, batch_no);
CREATE INDEX idx_delay_rework_dimensions ON nso_delay_rework (tenant_id, responsibility_stage, reason_type, recorded_at);
CREATE INDEX idx_sample_round_report ON nso_sample (tenant_id, project_id, round_no, status, plan_finish_date);

CREATE TABLE nso_import_batch_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    import_task_id BIGINT NOT NULL,
    batch_index INT NOT NULL,
    success_count INT NOT NULL DEFAULT 0,
    fail_count INT NOT NULL DEFAULT 0,
    result_file_id BIGINT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_import_batch_result (tenant_id, import_task_id, batch_index)
) COMMENT='大批量导入分批结果';
