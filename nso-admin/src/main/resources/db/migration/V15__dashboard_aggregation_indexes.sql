CREATE INDEX idx_project_dashboard_state
    ON nso_project (tenant_id, deleted, status, target_date);

CREATE INDEX idx_task_dashboard_state
    ON nso_task (tenant_id, deleted, project_id, status, plan_finish);

CREATE INDEX idx_task_dashboard_completed
    ON nso_task (tenant_id, deleted, project_id, actual_finish);

CREATE INDEX idx_change_dashboard_period
    ON nso_change_order (tenant_id, deleted, project_id, created_at, status);

CREATE INDEX idx_sample_dashboard_state
    ON nso_sample (tenant_id, deleted, project_id, status);

CREATE INDEX idx_document_dashboard_state
    ON nso_document_version (tenant_id, deleted, project_id, status);

CREATE INDEX idx_risk_dashboard_state
    ON nso_risk (tenant_id, deleted, project_id, status, calculated_at);

CREATE INDEX idx_delay_rework_dashboard_period
    ON nso_delay_rework (tenant_id, project_id, recorded_at);
