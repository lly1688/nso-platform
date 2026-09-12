-- Supporting indexes for the unified server-side pagination queries.
-- This migration is additive; previously released migrations remain untouched.
CREATE INDEX idx_project_page_scope ON nso_project (tenant_id, deleted, id);
CREATE INDEX idx_customer_page_scope ON nso_customer (tenant_id, deleted, id);
CREATE INDEX idx_document_page_scope ON nso_document_version (tenant_id, deleted, project_id, id);
CREATE INDEX idx_sample_page_scope ON nso_sample (tenant_id, deleted, project_id, id);
CREATE INDEX idx_change_page_scope ON nso_change_order (tenant_id, deleted, project_id, id);
CREATE INDEX idx_task_page_scope ON nso_task (tenant_id, deleted, project_id, id);
CREATE INDEX idx_risk_page_scope ON nso_risk (tenant_id, deleted, project_id, id);
CREATE INDEX idx_delivery_page_scope ON nso_delivery_record (tenant_id, project_id, shipped_at, id);
CREATE INDEX idx_message_page_scope ON nso_message (tenant_id, deleted, id);
