-- V1.0 full business schema extension.
-- Keep V1__mvp_core_schema.sql immutable; this migration adds formal V1 tables and common governance columns.

ALTER TABLE nso_customer
    ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1,
    ADD COLUMN version INT NOT NULL DEFAULT 0,
    ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0,
    ADD COLUMN create_by VARCHAR(64),
    ADD COLUMN update_by VARCHAR(64);

ALTER TABLE nso_project
    ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1,
    ADD COLUMN plan_start_date DATE,
    ADD COLUMN current_doc_version_id BIGINT,
    ADD COLUMN risk_score INT NOT NULL DEFAULT 0,
    ADD COLUMN version INT NOT NULL DEFAULT 0,
    ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0,
    ADD COLUMN create_by VARCHAR(64),
    ADD COLUMN update_by VARCHAR(64);

ALTER TABLE nso_document_version
    ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1,
    ADD COLUMN document_id BIGINT,
    ADD COLUMN object_code VARCHAR(128),
    ADD COLUMN publish_by VARCHAR(64),
    ADD COLUMN publish_time DATETIME,
    ADD COLUMN version INT NOT NULL DEFAULT 0,
    ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0;

ALTER TABLE nso_sample
    ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1,
    ADD COLUMN doc_version_id BIGINT,
    ADD COLUMN bom_version_id BIGINT,
    ADD COLUMN process_version_id BIGINT,
    ADD COLUMN version INT NOT NULL DEFAULT 0,
    ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0;

ALTER TABLE nso_change_order
    ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1,
    ADD COLUMN source_stage VARCHAR(64),
    ADD COLUMN approval_node VARCHAR(64),
    ADD COLUMN version INT NOT NULL DEFAULT 0,
    ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0;

ALTER TABLE nso_task
    ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1,
    ADD COLUMN assignee_id BIGINT,
    ADD COLUMN referenced_doc_version_id BIGINT,
    ADD COLUMN actual_start DATETIME,
    ADD COLUMN actual_finish DATETIME,
    ADD COLUMN version INT NOT NULL DEFAULT 0,
    ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0;

ALTER TABLE nso_risk
    ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1,
    ADD COLUMN rule_code VARCHAR(64),
    ADD COLUMN rule_version VARCHAR(32),
    ADD COLUMN owner_id BIGINT,
    ADD COLUMN version INT NOT NULL DEFAULT 0,
    ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0;

ALTER TABLE nso_message
    ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1,
    ADD COLUMN receiver_id BIGINT,
    ADD COLUMN channel VARCHAR(32) NOT NULL DEFAULT 'IN_APP',
    ADD COLUMN template_code VARCHAR(64),
    ADD COLUMN send_result VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN read_time DATETIME,
    ADD COLUMN version INT NOT NULL DEFAULT 0,
    ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS crm_contact (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    customer_id BIGINT NOT NULL,
    contact_name VARCHAR(64) NOT NULL,
    phone VARCHAR(32),
    email VARCHAR(128),
    position_name VARCHAR(64),
    preferred_channel VARCHAR(32),
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_crm_contact_customer (tenant_id, customer_id)
) COMMENT='客户联系人';

CREATE TABLE IF NOT EXISTS nso_project_member (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    project_id BIGINT NOT NULL,
    user_id BIGINT,
    member_name VARCHAR(64) NOT NULL,
    project_role VARCHAR(64) NOT NULL,
    dept_id BIGINT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_project_member_role (tenant_id, project_id, user_id, project_role),
    KEY idx_project_member_project (tenant_id, project_id)
) COMMENT='项目成员与项目角色';

CREATE TABLE IF NOT EXISTS nso_requirement_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    requirement_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    before_content VARCHAR(1000),
    after_content VARCHAR(1000),
    before_status VARCHAR(32),
    after_status VARCHAR(32),
    change_reason VARCHAR(500),
    operator_name VARCHAR(64),
    operated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_requirement_history (tenant_id, project_id, requirement_id)
) COMMENT='需求项变更历史';

CREATE TABLE IF NOT EXISTS nso_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    project_id BIGINT NOT NULL,
    document_code VARCHAR(64) NOT NULL,
    file_type VARCHAR(64) NOT NULL,
    object_code VARCHAR(128),
    title VARCHAR(255) NOT NULL,
    current_version_id BIGINT,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_document_code (tenant_id, document_code),
    KEY idx_document_project (tenant_id, project_id, file_type)
) COMMENT='技术文件主表';

CREATE TABLE IF NOT EXISTS nso_bom (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    project_id BIGINT NOT NULL,
    bom_no VARCHAR(64) NOT NULL,
    version_no VARCHAR(64) NOT NULL,
    bound_doc_version_id BIGINT,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    publish_time DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_bom_version (tenant_id, project_id, bom_no, version_no)
) COMMENT='BOM 版本';

CREATE TABLE IF NOT EXISTS nso_bom_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    bom_id BIGINT NOT NULL,
    material_code VARCHAR(64) NOT NULL,
    material_name VARCHAR(128) NOT NULL,
    specification VARCHAR(255),
    quantity DECIMAL(12,3) NOT NULL,
    unit VARCHAR(32) NOT NULL,
    source_type VARCHAR(32) NOT NULL DEFAULT 'PURCHASE',
    substitute_code VARCHAR(64),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    KEY idx_bom_item_bom (tenant_id, bom_id)
) COMMENT='BOM 明细';

CREATE TABLE IF NOT EXISTS nso_process_route (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    project_id BIGINT NOT NULL,
    route_no VARCHAR(64) NOT NULL,
    version_no VARCHAR(64) NOT NULL,
    bound_doc_version_id BIGINT,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_process_route_version (tenant_id, project_id, route_no, version_no)
) COMMENT='工艺路线版本';

CREATE TABLE IF NOT EXISTS nso_process_step (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    route_id BIGINT NOT NULL,
    step_no INT NOT NULL,
    step_name VARCHAR(128) NOT NULL,
    work_instruction VARCHAR(1000),
    equipment_name VARCHAR(128),
    standard_hours DECIMAL(10,2),
    outsource_flag TINYINT NOT NULL DEFAULT 0,
    inspection_point TINYINT NOT NULL DEFAULT 0,
    KEY idx_process_step_route (tenant_id, route_id, step_no)
) COMMENT='工艺工序';

CREATE TABLE IF NOT EXISTS nso_inspection_spec (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    project_id BIGINT NOT NULL,
    spec_no VARCHAR(64) NOT NULL,
    version_no VARCHAR(64) NOT NULL,
    bound_doc_version_id BIGINT,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_inspection_spec_version (tenant_id, project_id, spec_no, version_no)
) COMMENT='检验规范版本';

CREATE TABLE IF NOT EXISTS nso_inspection_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    spec_id BIGINT NOT NULL,
    item_name VARCHAR(128) NOT NULL,
    standard_value VARCHAR(255),
    sampling_rule VARCHAR(255),
    attachment_file_id BIGINT,
    KEY idx_inspection_item_spec (tenant_id, spec_id)
) COMMENT='检验项目';

CREATE TABLE IF NOT EXISTS nso_sample_check (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    sample_id BIGINT NOT NULL,
    check_item VARCHAR(128) NOT NULL,
    measured_value VARCHAR(255),
    result VARCHAR(32) NOT NULL,
    issue_summary VARCHAR(1000),
    corrective_action VARCHAR(1000),
    checker_name VARCHAR(64),
    checked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_sample_check_sample (tenant_id, sample_id)
) COMMENT='样品检验记录';

CREATE TABLE IF NOT EXISTS nso_sample_confirm (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    sample_id BIGINT NOT NULL,
    token VARCHAR(128) NOT NULL,
    confirmer VARCHAR(64),
    company_name VARCHAR(128),
    contact VARCHAR(128),
    conclusion VARCHAR(32),
    opinion VARCHAR(1000),
    ip_summary VARCHAR(128),
    device_summary VARCHAR(255),
    submitted_at DATETIME,
    expire_at DATETIME,
    used_flag TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_sample_confirm_token (tenant_id, token),
    KEY idx_sample_confirm_sample (tenant_id, sample_id)
) COMMENT='客户样品确认';

CREATE TABLE IF NOT EXISTS nso_change_approval (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    change_id BIGINT NOT NULL,
    node_code VARCHAR(64) NOT NULL,
    approver_id BIGINT,
    approver_name VARCHAR(64),
    decision VARCHAR(32) NOT NULL,
    opinion VARCHAR(1000),
    approved_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_change_approval_node (tenant_id, change_id, node_code, approver_id)
) COMMENT='变更审批记录';

CREATE TABLE IF NOT EXISTS nso_task_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    task_id BIGINT NOT NULL,
    before_status VARCHAR(32),
    after_status VARCHAR(32) NOT NULL,
    operation_type VARCHAR(64) NOT NULL,
    summary VARCHAR(1000),
    operator_name VARCHAR(64),
    operated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_task_log_task (tenant_id, task_id, operated_at)
) COMMENT='任务状态流水';

CREATE TABLE IF NOT EXISTS nso_risk_action (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    risk_id BIGINT NOT NULL,
    action_plan VARCHAR(1000) NOT NULL,
    responsible_name VARCHAR(64),
    plan_finish_time DATETIME,
    close_summary VARCHAR(1000),
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_risk_action_risk (tenant_id, risk_id)
) COMMENT='风险处置记录';

CREATE TABLE IF NOT EXISTS nso_delay_rework (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    project_id BIGINT NOT NULL,
    change_id BIGINT,
    task_id BIGINT,
    reason_type VARCHAR(64) NOT NULL,
    delay_days INT NOT NULL DEFAULT 0,
    rework_qty DECIMAL(12,3) NOT NULL DEFAULT 0,
    scrap_qty DECIMAL(12,3) NOT NULL DEFAULT 0,
    extra_cost DECIMAL(14,2) NOT NULL DEFAULT 0,
    responsibility_stage VARCHAR(64),
    recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_delay_rework_project (tenant_id, project_id)
) COMMENT='延期返工归因';

CREATE TABLE IF NOT EXISTS nso_message_receipt (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    message_id BIGINT NOT NULL,
    receiver_id BIGINT,
    channel VARCHAR(32) NOT NULL,
    send_status VARCHAR(32) NOT NULL,
    send_time DATETIME,
    read_time DATETIME,
    retry_count INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_message_receipt (tenant_id, message_id, receiver_id, channel)
) COMMENT='消息回执';

CREATE TABLE IF NOT EXISTS nso_rule_param (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    rule_code VARCHAR(64) NOT NULL,
    rule_name VARCHAR(128) NOT NULL,
    rule_version VARCHAR(32) NOT NULL,
    param_json JSON NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    published_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_rule_version (tenant_id, rule_code, rule_version)
) COMMENT='规则参数版本';

CREATE TABLE IF NOT EXISTS nso_import_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    import_type VARCHAR(64) NOT NULL,
    file_id BIGINT,
    mode VARCHAR(32) NOT NULL DEFAULT 'INSERT_ONLY',
    status VARCHAR(32) NOT NULL DEFAULT 'UPLOADED',
    success_count INT NOT NULL DEFAULT 0,
    fail_count INT NOT NULL DEFAULT 0,
    error_file_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_import_task_status (tenant_id, import_type, status)
) COMMENT='数据导入任务';

CREATE TABLE IF NOT EXISTS nso_integration_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    direction VARCHAR(16) NOT NULL,
    system_name VARCHAR(64) NOT NULL,
    business_type VARCHAR(64),
    business_id BIGINT,
    request_summary VARCHAR(1000),
    response_code VARCHAR(64),
    retry_count INT NOT NULL DEFAULT 0,
    final_status VARCHAR(32) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_integration_log_business (tenant_id, system_name, business_type, business_id)
) COMMENT='外部集成日志';

CREATE TABLE IF NOT EXISTS sys_login_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    username VARCHAR(64),
    client_type VARCHAR(32),
    ip_address VARCHAR(64),
    user_agent VARCHAR(255),
    result VARCHAR(32) NOT NULL,
    failure_reason VARCHAR(500),
    logged_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_login_log_time (tenant_id, logged_at)
) COMMENT='登录日志';

CREATE TABLE IF NOT EXISTS sys_job (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_name VARCHAR(128) NOT NULL,
    bean_name VARCHAR(128) NOT NULL,
    cron_expression VARCHAR(128) NOT NULL,
    concurrent_policy VARCHAR(32) NOT NULL DEFAULT 'FORBID',
    status VARCHAR(32) NOT NULL DEFAULT 'STOPPED',
    allow_manual TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_job_bean (bean_name)
) COMMENT='Quartz 任务配置';

CREATE TABLE IF NOT EXISTS sys_job_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_id BIGINT,
    job_name VARCHAR(128),
    result VARCHAR(32) NOT NULL,
    message VARCHAR(1000),
    started_at DATETIME,
    finished_at DATETIME,
    KEY idx_job_log_time (job_id, started_at)
) COMMENT='Quartz 执行日志';

CREATE TABLE IF NOT EXISTS gen_table (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    table_name VARCHAR(128) NOT NULL,
    table_comment VARCHAR(255),
    module_name VARCHAR(64),
    business_name VARCHAR(64),
    class_name VARCHAR(128),
    package_name VARCHAR(255),
    template_type VARCHAR(64) NOT NULL DEFAULT 'crud',
    status VARCHAR(32) NOT NULL DEFAULT 'IMPORTED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_gen_table_name (table_name)
) COMMENT='代码生成表配置';

CREATE TABLE IF NOT EXISTS gen_table_column (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    table_id BIGINT NOT NULL,
    column_name VARCHAR(128) NOT NULL,
    column_comment VARCHAR(255),
    column_type VARCHAR(64),
    java_type VARCHAR(64),
    java_field VARCHAR(128),
    required_flag TINYINT NOT NULL DEFAULT 0,
    list_flag TINYINT NOT NULL DEFAULT 1,
    query_flag TINYINT NOT NULL DEFAULT 0,
    sort_no INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_gen_column (table_id, column_name)
) COMMENT='代码生成字段配置';

INSERT INTO sys_job (job_name, bean_name, cron_expression, concurrent_policy, status, allow_manual)
VALUES
('项目风险扫描', 'projectRiskScanTask', '0 0/10 * * * ?', 'FORBID', 'RUNNING', 1),
('样品确认催办', 'sampleConfirmReminderTask', '0 0 9 * * ?', 'FORBID', 'RUNNING', 1),
('变更超时升级', 'changeTimeoutTask', '0 0/30 * * * ?', 'FORBID', 'RUNNING', 1),
('任务临期提醒', 'taskDeadlineWarningTask', '0 0 8 * * ?', 'FORBID', 'RUNNING', 1),
('消息重试', 'messageRetryTask', '0 0/5 * * * ?', 'FORBID', 'RUNNING', 1),
('报表汇总', 'reportSummaryTask', '0 10 1 * * ?', 'FORBID', 'RUNNING', 1)
ON DUPLICATE KEY UPDATE job_name = VALUES(job_name), cron_expression = VALUES(cron_expression), status = VALUES(status);
