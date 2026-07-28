ALTER TABLE nso_project_member
    ADD COLUMN department_name VARCHAR(64);

CREATE INDEX idx_project_requirement_project_status
    ON nso_project_requirement (project_id, confirm_status);
