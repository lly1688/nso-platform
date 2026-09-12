-- 智慧非标订单打样与变更协同平台汇总初始化脚本（V1-V27）
-- 来源：nso-admin/src/main/resources/db/migration
-- 仅用于全新的 MySQL 8 数据库。
-- 请在同一 MySQL 会话中执行完整文件；V11/V27 使用会话变量和演示修复数据。
-- 本文件不在 Flyway 迁移目录中，不得与 V1-V27 迁移脚本同时执行。
-- 未包含 CREATE DATABASE 或 USE 语句；执行前请选择目标数据库。

-- ===== 开始 V1__mvp_core_schema.sql =====
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    nickname VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT='系统用户';

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(64) NOT NULL UNIQUE,
    role_name VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) COMMENT='系统角色';

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id)
) COMMENT='用户角色关系';

CREATE TABLE IF NOT EXISTS sys_dict_data (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    dict_type VARCHAR(64) NOT NULL,
    dict_code VARCHAR(64) NOT NULL,
    dict_label VARCHAR(128) NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_dict_type_code (dict_type, dict_code)
) COMMENT='业务字典';

CREATE TABLE IF NOT EXISTS nso_customer (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    industry VARCHAR(64),
    contact_name VARCHAR(64),
    phone VARCHAR(32),
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='客户档案';

CREATE TABLE IF NOT EXISTS nso_project (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_no VARCHAR(64) NOT NULL UNIQUE,
    customer_id BIGINT,
    customer_name VARCHAR(128) NOT NULL,
    product_name VARCHAR(128) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    target_date DATE,
    owner_name VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    stage VARCHAR(64) NOT NULL,
    priority VARCHAR(32) NOT NULL DEFAULT 'MEDIUM',
    risk_level VARCHAR(32) NOT NULL DEFAULT 'LOW',
    sample_status VARCHAR(32) NOT NULL DEFAULT 'NONE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_project_customer (customer_id),
    KEY idx_project_status (status),
    KEY idx_project_risk (risk_level)
) COMMENT='非标订单项目';

CREATE TABLE IF NOT EXISTS nso_project_requirement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    category VARCHAR(64) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    confirm_status VARCHAR(32) NOT NULL DEFAULT 'UNCONFIRMED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_requirement_project (project_id)
) COMMENT='项目需求项';

CREATE TABLE IF NOT EXISTS nso_document_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(64) NOT NULL,
    version_no VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    effective_date DATE,
    change_summary VARCHAR(1000),
    current_version TINYINT NOT NULL DEFAULT 0,
    sha256 VARCHAR(128),
    storage_path VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_doc_project_type_version (project_id, file_type, version_no),
    KEY idx_doc_current (project_id, file_type, current_version)
) COMMENT='技术文件版本';

CREATE TABLE IF NOT EXISTS nso_sample (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    sample_no VARCHAR(64) NOT NULL UNIQUE,
    purpose VARCHAR(500) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    plan_finish_date DATE,
    referenced_version VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    confirm_conclusion VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    responsible_name VARCHAR(64),
    issue_summary VARCHAR(1000),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_sample_project (project_id),
    KEY idx_sample_status (status)
) COMMENT='样品单';

CREATE TABLE IF NOT EXISTS nso_change_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    change_no VARCHAR(64) NOT NULL UNIQUE,
    change_type VARCHAR(64) NOT NULL,
    urgency VARCHAR(32) NOT NULL DEFAULT 'NORMAL',
    before_content TEXT,
    after_content TEXT,
    reason VARCHAR(1000),
    status VARCHAR(32) NOT NULL,
    delay_days INT NOT NULL DEFAULT 0,
    rework_qty INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_change_project (project_id),
    KEY idx_change_status (status)
) COMMENT='变更单';

CREATE TABLE IF NOT EXISTS nso_change_impact (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    change_id BIGINT NOT NULL,
    object_type VARCHAR(64) NOT NULL,
    object_name VARCHAR(255) NOT NULL,
    department_name VARCHAR(64),
    suggested_action VARCHAR(1000),
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_FEEDBACK',
    feedback_result VARCHAR(1000),
    responsible_name VARCHAR(64),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_impact_change (change_id),
    KEY idx_impact_status (status)
) COMMENT='变更影响项';

CREATE TABLE IF NOT EXISTS nso_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    task_no VARCHAR(64) NOT NULL UNIQUE,
    task_type VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    referenced_version VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    responsible_name VARCHAR(64),
    plan_start DATE,
    plan_finish DATE,
    block_reason VARCHAR(1000),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_task_project (project_id),
    KEY idx_task_status (status)
) COMMENT='协同任务';

CREATE TABLE IF NOT EXISTS nso_risk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    level VARCHAR(32) NOT NULL,
    score INT NOT NULL,
    reasons JSON,
    suggestion VARCHAR(1000),
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    calculated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_risk_project (project_id),
    KEY idx_risk_level (level)
) COMMENT='交期风险';

CREATE TABLE IF NOT EXISTS nso_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'UNREAD',
    business_type VARCHAR(64),
    business_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_message_status (status),
    KEY idx_message_business (business_type, business_id)
) COMMENT='消息中心';

CREATE TABLE IF NOT EXISTS nso_timeline_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    project_id BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary VARCHAR(1000),
    operator_name VARCHAR(64),
    occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_timeline_project (project_id, occurred_at)
) COMMENT='项目时间轴';

CREATE TABLE IF NOT EXISTS nso_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_name VARCHAR(64),
    client_type VARCHAR(32),
    module_name VARCHAR(64),
    operation_type VARCHAR(64),
    business_type VARCHAR(64),
    business_id BIGINT,
    before_summary TEXT,
    after_summary TEXT,
    trace_id VARCHAR(64),
    result VARCHAR(32) NOT NULL,
    failure_reason VARCHAR(1000),
    operated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_audit_business (business_type, business_id),
    KEY idx_audit_operated_at (operated_at)
) COMMENT='审计日志';

CREATE TABLE IF NOT EXISTS nso_file_object (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(128),
    file_size BIGINT NOT NULL,
    sha256 VARCHAR(128) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_file_sha256 (sha256)
) COMMENT='文件对象';

INSERT INTO sys_user (id, username, password_hash, nickname, status)
VALUES (1, 'admin', '{noop}admin123', '演示管理员', 'ENABLED')
ON DUPLICATE KEY UPDATE nickname = VALUES(nickname);

INSERT INTO sys_role (id, role_code, role_name)
VALUES (1, 'admin', '系统管理员'), (2, 'project_manager', '项目经理'), (3, 'field_user', '现场人员')
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name);

INSERT INTO sys_user_role (user_id, role_id)
VALUES (1, 1), (1, 2)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);

INSERT INTO sys_dict_data (dict_type, dict_code, dict_label, sort_no)
VALUES
('project_status', 'DRAFT', '订单草稿', 1),
('project_status', 'TECH_PUBLISHED', '技术包已发布', 2),
('sample_status', 'WAIT_CUSTOMER_CONFIRM', '待客户确认', 1),
('sample_status', 'CONFIRMED', '已确认', 2),
('change_status', 'WAIT_IMPACT', '待影响分析', 1),
('change_status', 'EXECUTING', '执行中', 2),
('risk_level', 'LOW', '低风险', 1),
('risk_level', 'MEDIUM', '中风险', 2),
('risk_level', 'HIGH', '高风险', 3),
('risk_level', 'SERIOUS', '严重风险', 4)
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label), sort_no = VALUES(sort_no);

-- ===== 结束 V1__mvp_core_schema.sql =====

-- ===== 开始 V2__v1_full_schema.sql =====
-- V1.0 完整业务模式扩展。
-- 保持 V1__mvp_core_schema.sql 不可变；此迁移新增正式 V1 表与通用治理列。

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

-- ===== 结束 V2__v1_full_schema.sql =====

-- ===== 开始 V3__section6_feature_completion.sql =====
-- 第 6 节 V1 闭环功能补全。
-- V1/V2 保持不可变；此迁移新增缺失的工作流、治理与协同表。

ALTER TABLE nso_project_requirement
    ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1,
    ADD COLUMN last_reason VARCHAR(500),
    ADD COLUMN update_by VARCHAR(64),
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    ADD COLUMN version INT NOT NULL DEFAULT 0,
    ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0;

ALTER TABLE nso_change_impact
    ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1,
    ADD COLUMN feedback_plan VARCHAR(1000),
    ADD COLUMN delay_days INT NOT NULL DEFAULT 0,
    ADD COLUMN rework_qty DECIMAL(12,3) NOT NULL DEFAULT 0,
    ADD COLUMN extra_cost DECIMAL(14,2) NOT NULL DEFAULT 0,
    ADD COLUMN verified_flag TINYINT NOT NULL DEFAULT 0,
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE nso_audit_log
    ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1,
    ADD COLUMN request_uri VARCHAR(255),
    ADD COLUMN request_method VARCHAR(16),
    ADD COLUMN query_summary VARCHAR(1000),
    ADD COLUMN download_count INT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS nso_workbench_focus (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    user_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    focus_type VARCHAR(32) NOT NULL DEFAULT 'WATCH',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_workbench_focus (tenant_id, user_id, project_id, focus_type),
    KEY idx_workbench_focus_user (tenant_id, user_id)
) COMMENT='个人工作台关注项目';

CREATE TABLE IF NOT EXISTS nso_saved_view (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    user_id BIGINT NOT NULL DEFAULT 1,
    view_name VARCHAR(128) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    filter_json JSON NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_saved_view_name (tenant_id, user_id, target_type, view_name)
) COMMENT='个人组合筛选视图';

CREATE TABLE IF NOT EXISTS nso_qrcode_binding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    business_type VARCHAR(64) NOT NULL,
    business_id BIGINT NOT NULL,
    qr_code VARCHAR(128) NOT NULL,
    target_url VARCHAR(500) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_qrcode_code (tenant_id, qr_code),
    KEY idx_qrcode_business (tenant_id, business_type, business_id)
) COMMENT='二维码业务绑定';

CREATE TABLE IF NOT EXISTS nso_download_watermark_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    file_id BIGINT,
    document_version_id BIGINT,
    project_no VARCHAR(64),
    version_no VARCHAR(64),
    downloader_name VARCHAR(64),
    watermark_text VARCHAR(500),
    downloaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_watermark_doc (tenant_id, document_version_id)
) COMMENT='受控文件下载水印记录';

CREATE TABLE IF NOT EXISTS nso_purchase_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    project_id BIGINT NOT NULL,
    task_id BIGINT,
    material_code VARCHAR(64),
    material_name VARCHAR(128),
    supplier_name VARCHAR(128),
    plan_arrival_date DATE,
    actual_arrival_date DATE,
    exception_summary VARCHAR(1000),
    status VARCHAR(32) NOT NULL DEFAULT 'TODO',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_purchase_project (tenant_id, project_id, status)
) COMMENT='采购协同任务';

CREATE TABLE IF NOT EXISTS nso_production_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    project_id BIGINT NOT NULL,
    task_id BIGINT,
    part_name VARCHAR(128),
    process_step VARCHAR(128),
    team_name VARCHAR(128),
    actual_start DATETIME,
    actual_finish DATETIME,
    version_check_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    status VARCHAR(32) NOT NULL DEFAULT 'TODO',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_production_project (tenant_id, project_id, status)
) COMMENT='生产协同任务';

CREATE TABLE IF NOT EXISTS nso_execution_exception (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    project_id BIGINT NOT NULL,
    task_id BIGINT,
    exception_type VARCHAR(64) NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    image_file_id BIGINT,
    reporter_name VARCHAR(64),
    generated_risk_id BIGINT,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    reported_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_execution_exception_project (tenant_id, project_id, status)
) COMMENT='现场异常上报';

CREATE TABLE IF NOT EXISTS nso_delivery_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    project_id BIGINT NOT NULL,
    quantity INT NOT NULL DEFAULT 0,
    logistics_no VARCHAR(128),
    receiver VARCHAR(128),
    customer_feedback VARCHAR(1000),
    status VARCHAR(32) NOT NULL DEFAULT 'SHIPPED',
    shipped_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    signed_at DATETIME,
    KEY idx_delivery_project (tenant_id, project_id, shipped_at)
) COMMENT='交付记录';

CREATE TABLE IF NOT EXISTS nso_export_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    user_id BIGINT NOT NULL DEFAULT 1,
    export_type VARCHAR(64) NOT NULL,
    query_json JSON,
    field_json JSON,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    file_id BIGINT,
    file_name VARCHAR(255),
    download_count INT NOT NULL DEFAULT 0,
    expire_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at DATETIME,
    KEY idx_export_status (tenant_id, user_id, export_type, status)
) COMMENT='异步导出任务';

CREATE TABLE IF NOT EXISTS nso_import_task_detail (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    import_task_id BIGINT NOT NULL,
    row_no INT NOT NULL,
    business_key VARCHAR(255),
    row_json JSON,
    status VARCHAR(32) NOT NULL,
    error_message VARCHAR(1000),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_import_detail_task (tenant_id, import_task_id, status)
) COMMENT='导入明细与错误清单';

CREATE TABLE IF NOT EXISTS nso_rule_param_approval (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    rule_param_id BIGINT NOT NULL,
    node_code VARCHAR(64) NOT NULL,
    approver_name VARCHAR(64),
    decision VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    opinion VARCHAR(1000),
    decided_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_rule_approval (tenant_id, rule_param_id, decision)
) COMMENT='高风险规则参数审批';

CREATE TABLE IF NOT EXISTS nso_message_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    template_code VARCHAR(64) NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    channel VARCHAR(32) NOT NULL,
    title_template VARCHAR(255) NOT NULL,
    content_template VARCHAR(1000) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_message_template (tenant_id, template_code, channel)
) COMMENT='消息模板配置';

CREATE TABLE IF NOT EXISTS nso_notification_outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    message_id BIGINT,
    receiver_id BIGINT,
    channel VARCHAR(32) NOT NULL,
    payload_json JSON,
    send_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME,
    sent_at DATETIME,
    fail_reason VARCHAR(1000),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_outbox_retry (tenant_id, send_status, next_retry_at)
) COMMENT='通知发送适配器出站箱';

INSERT INTO sys_dict_data (dict_type, dict_code, dict_label, sort_no)
VALUES
('file_type', 'DRAWING', '图纸', 1),
('file_type', 'MODEL_3D', '3D模型', 2),
('file_type', 'BOM', 'BOM', 3),
('file_type', 'PROCESS', '工艺卡', 4),
('file_type', 'INSPECTION', '检验规范', 5),
('requirement_status', 'UNCONFIRMED', '未确认', 1),
('requirement_status', 'INTERNAL_CONFIRMED', '内部确认', 2),
('requirement_status', 'CUSTOMER_CONFIRMED', '客户确认', 3),
('requirement_status', 'DISPUTED', '存在异议', 4),
('exception_type', 'MATERIAL_SHORTAGE', '缺料', 1),
('exception_type', 'EQUIPMENT', '设备异常', 2),
('exception_type', 'QUALITY', '质量异常', 3),
('exception_type', 'TECHNICAL', '技术异常', 4),
('delivery_status', 'SHIPPED', '已出货', 1),
('delivery_status', 'SIGNED', '已签收', 2)
ON DUPLICATE KEY UPDATE dict_label = VALUES(dict_label), sort_no = VALUES(sort_no);

INSERT INTO nso_message_template (template_code, event_type, channel, title_template, content_template)
VALUES
('SAMPLE_CONFIRM_DUE', 'SAMPLE_CONFIRM', 'IN_APP', '样品确认催办', '${projectNo} 样品即将超期，请跟进客户确认'),
('CHANGE_APPROVED', 'CHANGE_APPROVED', 'IN_APP', '变更已批准', '${projectNo} ${changeNo} 已批准，请处理影响项'),
('TASK_DUE', 'TASK_DUE', 'IN_APP', '任务临期提醒', '${taskNo} 即将到期，请及时反馈'),
('EXPORT_DONE', 'EXPORT_DONE', 'IN_APP', '导出完成', '${fileName} 已生成')
ON DUPLICATE KEY UPDATE title_template = VALUES(title_template), content_template = VALUES(content_template), status = 'ENABLED';

INSERT INTO sys_job (job_name, bean_name, cron_expression, concurrent_policy, status, allow_manual)
VALUES
('导出任务清理', 'exportExpireCleanTask', '0 30 1 * * ?', 'FORBID', 'RUNNING', 1),
('导入结果清理', 'importResultCleanTask', '0 40 1 * * ?', 'FORBID', 'RUNNING', 1)
ON DUPLICATE KEY UPDATE job_name = VALUES(job_name), cron_expression = VALUES(cron_expression), status = VALUES(status);

-- ===== 结束 V3__section6_feature_completion.sql =====

-- ===== 开始 V4__secure_system_identity.sql =====
-- 原型模式后的身份加固。既有 V1/V2/V3 迁移保持不可变。
ALTER TABLE sys_user
    ADD COLUMN wechat_open_id VARCHAR(128),
    ADD COLUMN version INT NOT NULL DEFAULT 0,
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

CREATE UNIQUE INDEX uk_sys_user_wechat_open_id ON sys_user (wechat_open_id);

-- ===== 结束 V4__secure_system_identity.sql =====

-- ===== 开始 V5__project_member_department_and_constraints.sql =====
ALTER TABLE nso_project_member
    ADD COLUMN department_name VARCHAR(64);

CREATE INDEX idx_project_requirement_project_status
    ON nso_project_requirement (project_id, confirm_status);

-- ===== 结束 V5__project_member_department_and_constraints.sql =====

-- ===== 开始 V6__sample_confirmation_usage_control.sql =====
ALTER TABLE nso_sample_confirm
    ADD COLUMN max_use_count INT NOT NULL DEFAULT 1,
    ADD COLUMN used_count INT NOT NULL DEFAULT 0;

CREATE INDEX idx_sample_confirm_expire ON nso_sample_confirm (expire_at, used_flag);

-- ===== 结束 V6__sample_confirmation_usage_control.sql =====

-- ===== 开始 V7__tenant_governance_and_business_events.sql =====
-- V1.0 生产治理扩展。V1-V6 均有意保持不可变。

CREATE TABLE IF NOT EXISTS nso_tenant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_code VARCHAR(64) NOT NULL,
    tenant_name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_nso_tenant_code (tenant_code)
) COMMENT='企业租户';

INSERT INTO nso_tenant (id, tenant_code, tenant_name, status)
VALUES (1, 'DEFAULT', '默认企业', 'ENABLED')
ON DUPLICATE KEY UPDATE tenant_name = VALUES(tenant_name), status = VALUES(status);

ALTER TABLE sys_user
    ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1,
    ADD COLUMN dept_id BIGINT NULL;

ALTER TABLE nso_file_object
    ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1,
    ADD COLUMN created_by BIGINT NULL;

ALTER TABLE nso_timeline_event
    ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1,
    ADD COLUMN business_type VARCHAR(64) NULL,
    ADD COLUMN business_id BIGINT NULL,
    ADD COLUMN trace_id VARCHAR(64) NULL;

ALTER TABLE nso_audit_log
    ADD COLUMN user_id BIGINT NULL,
    ADD COLUMN request_id VARCHAR(96) NULL;

ALTER TABLE nso_change_impact
    ADD COLUMN object_id BIGINT NULL,
    ADD COLUMN object_version VARCHAR(64) NULL;

CREATE INDEX idx_sys_user_tenant_status ON sys_user (tenant_id, status);
CREATE INDEX idx_file_object_tenant ON nso_file_object (tenant_id, id);
CREATE INDEX idx_timeline_tenant_project ON nso_timeline_event (tenant_id, project_id, occurred_at);
CREATE INDEX idx_change_impact_object ON nso_change_impact (tenant_id, change_id, object_type, object_id);

CREATE TABLE IF NOT EXISTS nso_idempotency_key (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL DEFAULT 0,
    request_id VARCHAR(96) NOT NULL,
    request_method VARCHAR(16) NOT NULL,
    request_path VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PROCESSING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME NULL,
    UNIQUE KEY uk_idempotency_request (tenant_id, user_id, request_id, request_method, request_path)
) COMMENT='关键写操作幂等键';

CREATE TABLE IF NOT EXISTS nso_business_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    project_id BIGINT NULL,
    business_type VARCHAR(64) NOT NULL,
    business_id BIGINT NULL,
    action_code VARCHAR(64) NOT NULL,
    before_summary VARCHAR(1000) NULL,
    after_summary VARCHAR(1000) NULL,
    operator_id BIGINT NULL,
    operator_name VARCHAR(64) NULL,
    trace_id VARCHAR(64) NULL,
    occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_business_event_project (tenant_id, project_id, occurred_at),
    KEY idx_business_event_object (tenant_id, business_type, business_id, occurred_at)
) COMMENT='不可变业务事件与状态历史';

CREATE TABLE IF NOT EXISTS nso_attachment_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    project_id BIGINT NULL,
    business_type VARCHAR(64) NOT NULL,
    business_id BIGINT NOT NULL,
    file_id BIGINT NOT NULL,
    evidence_type VARCHAR(64) NOT NULL DEFAULT 'ATTACHMENT',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_attachment_relation (tenant_id, business_type, business_id, file_id, evidence_type)
) COMMENT='业务附件与确认凭证关联';

-- ===== 结束 V7__tenant_governance_and_business_events.sql =====

-- ===== 开始 V8__change_impact_optimistic_lock.sql =====
-- V1.0 后续补充：直接影响反馈是一项可独立编辑的业务动作。
-- 保留 V1-V7 校验和，并为既有影响记录赋予确定性的初始版本。
ALTER TABLE nso_change_impact
    ADD COLUMN version INT NOT NULL DEFAULT 0;

-- ===== 结束 V8__change_impact_optimistic_lock.sql =====

-- ===== 开始 V9__system_administration_completion.sql =====
-- V1.0 系统管理功能补全。早期迁移保持不可变。

ALTER TABLE sys_role
    ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1,
    ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    ADD COLUMN version INT NOT NULL DEFAULT 0,
    ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0;

CREATE TABLE IF NOT EXISTS sys_dept (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    parent_id BIGINT NULL,
    dept_name VARCHAR(64) NOT NULL,
    leader_name VARCHAR(64) NULL,
    phone VARCHAR(32) NULL,
    sort_no INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_dept_name (tenant_id, dept_name, deleted),
    KEY idx_sys_dept_parent (tenant_id, parent_id, status)
) COMMENT='组织部门';

CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parent_id BIGINT NULL,
    menu_name VARCHAR(64) NOT NULL,
    route_path VARCHAR(128) NULL,
    permission_code VARCHAR(128) NULL,
    component_name VARCHAR(128) NULL,
    visible TINYINT NOT NULL DEFAULT 1,
    sort_no INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ENABLED',
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_menu_permission (permission_code, deleted)
) COMMENT='系统菜单与功能权限';

CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, menu_id)
) COMMENT='角色菜单权限关系';

INSERT INTO sys_dept (tenant_id, dept_name, leader_name, sort_no, status)
VALUES (1, '默认部门', '系统管理员', 1, 'ENABLED')
ON DUPLICATE KEY UPDATE leader_name = VALUES(leader_name), status = VALUES(status);

INSERT INTO sys_menu (menu_name, route_path, permission_code, component_name, sort_no, status)
VALUES
('工作台', '/dashboard', 'dashboard:view', 'Dashboard', 1, 'ENABLED'),
('客户项目', '/projects', 'project:view', 'Projects', 2, 'ENABLED'),
('技术文件', '/documents', 'document:view', 'Documents', 3, 'ENABLED'),
('样品管理', '/samples', 'sample:view', 'Samples', 4, 'ENABLED'),
('变更中心', '/changes', 'change:view', 'Changes', 5, 'ENABLED'),
('任务风险', '/tasks', 'task:view', 'Tasks', 6, 'ENABLED'),
('统计分析', '/reports', 'report:view', 'Reports', 7, 'ENABLED'),
('系统管理', '/system', 'system:manage', 'System', 8, 'ENABLED')
ON DUPLICATE KEY UPDATE route_path = VALUES(route_path), status = VALUES(status), sort_no = VALUES(sort_no);

-- ===== 结束 V9__system_administration_completion.sql =====

-- ===== 开始 V10__role_permission_and_demo_governance.sql =====
-- 角色、权限、会话版本与职责分离治理。
-- 早期迁移保持不可变。

ALTER TABLE sys_user
    ADD COLUMN auth_version INT NOT NULL DEFAULT 0;

ALTER TABLE nso_project
    ADD COLUMN owner_user_id BIGINT NULL;

ALTER TABLE nso_document_version
    ADD COLUMN uploaded_by BIGINT NULL,
    ADD COLUMN published_by BIGINT NULL;

ALTER TABLE nso_sample
    ADD COLUMN created_by BIGINT NULL,
    ADD COLUMN quality_confirmed_by BIGINT NULL,
    ADD COLUMN proxy_confirmed_by BIGINT NULL,
    ADD COLUMN proxy_confirmation TINYINT NOT NULL DEFAULT 0;

ALTER TABLE nso_change_order
    ADD COLUMN applicant_user_id BIGINT NULL;

CREATE TABLE nso_special_release (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    sample_id BIGINT NULL,
    applicant_user_id BIGINT NOT NULL,
    approver_user_id BIGINT NULL,
    release_scope VARCHAR(500) NOT NULL,
    valid_until DATETIME NOT NULL,
    risk_statement VARCHAR(1000) NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    approved_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_special_release_project (tenant_id, project_id, status),
    KEY idx_special_release_sample (tenant_id, sample_id, status)
) COMMENT='样品与投产特殊放行记录';

INSERT INTO sys_role (tenant_id, role_code, role_name, status, version, deleted)
VALUES
    (1, 'admin', '系统管理员', 'ENABLED', 0, 0),
    (1, 'project_manager', '销售/项目经理', 'ENABLED', 0, 0),
    (1, 'technical', '技术/设计人员', 'ENABLED', 0, 0),
    (1, 'process', '工艺人员', 'ENABLED', 0, 0),
    (1, 'purchaser', '采购人员', 'ENABLED', 0, 0),
    (1, 'production', '计划/生产人员', 'ENABLED', 0, 0),
    (1, 'quality', '质量人员', 'ENABLED', 0, 0),
    (1, 'customer_confirm', '客户确认人', 'ENABLED', 0, 0),
    (1, 'executive', '管理层', 'ENABLED', 0, 0)
ON DUPLICATE KEY UPDATE
    role_name = VALUES(role_name), status = 'ENABLED', deleted = 0;

INSERT INTO sys_menu (menu_name, route_path, permission_code, component_name, visible, sort_no, status)
VALUES
    ('客户管理', '/projects', 'customer:view', 'Projects', 1, 11, 'ENABLED'),
    ('客户维护', NULL, 'customer:manage', NULL, 0, 12, 'ENABLED'),
    ('项目新建', NULL, 'project:create', NULL, 0, 21, 'ENABLED'),
    ('项目维护', NULL, 'project:manage', NULL, 0, 22, 'ENABLED'),
    ('项目成员维护', NULL, 'project:member:manage', NULL, 0, 23, 'ENABLED'),
    ('客户邀请', NULL, 'customer:invite', NULL, 0, 24, 'ENABLED'),
    ('需求维护', NULL, 'project:requirement:manage', NULL, 0, 25, 'ENABLED'),
    ('技术资料上传', NULL, 'document:upload', NULL, 0, 31, 'ENABLED'),
    ('技术版本发布', NULL, 'document:publish', NULL, 0, 32, 'ENABLED'),
    ('BOM维护', NULL, 'bom:manage', NULL, 0, 33, 'ENABLED'),
    ('工艺维护', NULL, 'process:manage', NULL, 0, 34, 'ENABLED'),
    ('检验规范维护', NULL, 'inspection:manage', NULL, 0, 35, 'ENABLED'),
    ('样品创建', NULL, 'sample:create', NULL, 0, 41, 'ENABLED'),
    ('样品提交客户确认', NULL, 'sample:submit', NULL, 0, 42, 'ENABLED'),
    ('样品质量检验', NULL, 'sample:inspect', NULL, 0, 43, 'ENABLED'),
    ('销售代录客户结论', NULL, 'sample:proxy-confirm', NULL, 0, 44, 'ENABLED'),
    ('特殊放行申请', NULL, 'sample:release:apply', NULL, 0, 45, 'ENABLED'),
    ('特殊放行批准', NULL, 'sample:release:approve', NULL, 0, 46, 'ENABLED'),
    ('变更发起', NULL, 'change:create', NULL, 0, 51, 'ENABLED'),
    ('变更影响分析', NULL, 'change:analyze', NULL, 0, 52, 'ENABLED'),
    ('变更审批', NULL, 'change:approve', NULL, 0, 53, 'ENABLED'),
    ('变更执行反馈', NULL, 'change:feedback', NULL, 0, 54, 'ENABLED'),
    ('变更关闭', NULL, 'change:close', NULL, 0, 55, 'ENABLED'),
    ('任务排程', NULL, 'task:plan', NULL, 0, 61, 'ENABLED'),
    ('任务执行', NULL, 'task:execute', NULL, 0, 62, 'ENABLED'),
    ('任务反馈', NULL, 'task:feedback', NULL, 0, 63, 'ENABLED'),
    ('风险查看', NULL, 'risk:view', NULL, 0, 71, 'ENABLED'),
    ('消息查看', NULL, 'message:view', NULL, 0, 72, 'ENABLED'),
    ('客户确认门户', NULL, 'customer:portal', NULL, 0, 81, 'ENABLED')
ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name), route_path = VALUES(route_path), component_name = VALUES(component_name),
    visible = VALUES(visible), sort_no = VALUES(sort_no), status = 'ENABLED', deleted = 0;

-- 管理员保留所有已启用权限。其他记录描述标准的最小权限目录，
-- 后续可通过角色管理调整。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.role_code = 'admin' AND r.tenant_id = 1 AND m.status = 'ENABLED' AND m.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.permission_code IN
('dashboard:view','customer:view','customer:manage','project:view','project:create','project:manage','project:member:manage','customer:invite','project:requirement:manage','document:view','sample:view','sample:create','sample:submit','sample:proxy-confirm','change:view','change:create','change:approve','change:feedback','task:view','task:plan','risk:view','report:view','message:view')
WHERE r.role_code = 'project_manager' AND r.tenant_id = 1 AND m.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.permission_code IN
('dashboard:view','project:view','document:view','document:upload','document:publish','bom:manage','process:manage','inspection:manage','sample:view','change:view','change:create','change:analyze','change:approve','task:view','risk:view','message:view')
WHERE r.role_code = 'technical' AND r.tenant_id = 1 AND m.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.permission_code IN
('dashboard:view','project:view','document:view','bom:manage','process:manage','inspection:manage','sample:view','change:view','change:analyze','change:feedback','task:view','message:view')
WHERE r.role_code = 'process' AND r.tenant_id = 1 AND m.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.permission_code IN
('dashboard:view','project:view','document:view','sample:view','change:view','change:feedback','task:view','task:execute','task:feedback','risk:view','message:view')
WHERE r.role_code = 'purchaser' AND r.tenant_id = 1 AND m.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.permission_code IN
('dashboard:view','project:view','document:view','sample:view','sample:release:apply','change:view','change:approve','change:feedback','task:view','task:plan','task:execute','task:feedback','risk:view','message:view')
WHERE r.role_code = 'production' AND r.tenant_id = 1 AND m.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.permission_code IN
('dashboard:view','project:view','document:view','sample:view','sample:inspect','sample:release:approve','change:view','change:approve','change:feedback','task:view','task:feedback','risk:view','message:view')
WHERE r.role_code = 'quality' AND r.tenant_id = 1 AND m.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.permission_code IN ('customer:portal')
WHERE r.role_code = 'customer_confirm' AND r.tenant_id = 1 AND m.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.permission_code IN ('dashboard:view','project:view','risk:view','report:view')
WHERE r.role_code = 'executive' AND r.tenant_id = 1 AND m.deleted = 0;

-- ===== 结束 V10__role_permission_and_demo_governance.sql =====

-- ===== 开始 V11__demo_business_data.sql =====
-- NSO 工作台开发演示数据。
-- 所有记录均使用 DEMO 前缀，以便识别和安全重复执行。

INSERT INTO nso_customer (tenant_id, customer_code, name, industry, contact_name, phone, status, create_by, update_by)
VALUES
    (1, 'CUST-DEMO-001', '华东智能装备有限公司', '智能制造', '王工', '13800001001', 'ENABLED', 'SYSTEM', 'SYSTEM'),
    (1, 'CUST-DEMO-002', '新能源精密科技有限公司', '新能源', '李经理', '13800001002', 'ENABLED', 'SYSTEM', 'SYSTEM'),
    (1, 'CUST-DEMO-003', '安康医疗器械有限公司', '医疗器械', '周经理', '13800001003', 'ENABLED', 'SYSTEM', 'SYSTEM')
ON DUPLICATE KEY UPDATE
    name = VALUES(name), industry = VALUES(industry), contact_name = VALUES(contact_name),
    phone = VALUES(phone), status = 'ENABLED', update_by = VALUES(update_by);

INSERT INTO nso_project (
    tenant_id, project_no, customer_id, customer_name, product_name, quantity,
    plan_start_date, target_date, owner_user_id, owner_name, status, stage,
    priority, risk_level, risk_score, sample_status, create_by, update_by
)
SELECT 1, 'NSO-DEMO-202607-001', c.id, c.name, '精密自动化装配线夹具', 12,
       CURDATE() - INTERVAL 18 DAY, CURDATE() + INTERVAL 12 DAY, 3, '陈晓明', 'TECH_PUBLISHED', 'SAMPLE',
       'HIGH', 'HIGH', 72, 'WAIT_CUSTOMER_CONFIRM', 'SYSTEM', 'SYSTEM'
FROM nso_customer c WHERE c.customer_code = 'CUST-DEMO-001'
ON DUPLICATE KEY UPDATE
    customer_id = VALUES(customer_id), customer_name = VALUES(customer_name), product_name = VALUES(product_name),
    quantity = VALUES(quantity), plan_start_date = VALUES(plan_start_date), target_date = VALUES(target_date),
    owner_user_id = VALUES(owner_user_id), owner_name = VALUES(owner_name), status = VALUES(status), stage = VALUES(stage),
    priority = VALUES(priority), risk_level = VALUES(risk_level), risk_score = VALUES(risk_score),
    sample_status = VALUES(sample_status), deleted = 0, update_by = VALUES(update_by);

INSERT INTO nso_project (
    tenant_id, project_no, customer_id, customer_name, product_name, quantity,
    plan_start_date, target_date, owner_user_id, owner_name, status, stage,
    priority, risk_level, risk_score, sample_status, create_by, update_by
)
SELECT 1, 'NSO-DEMO-202607-002', c.id, c.name, '新能源汽车电池托盘检具', 6,
       CURDATE() - INTERVAL 10 DAY, CURDATE() + INTERVAL 7 DAY, 3, '陈晓明', 'TECH_PUBLISHED', 'TECHNICAL',
       'MEDIUM', 'MEDIUM', 38, 'DRAFT', 'SYSTEM', 'SYSTEM'
FROM nso_customer c WHERE c.customer_code = 'CUST-DEMO-002'
ON DUPLICATE KEY UPDATE
    customer_id = VALUES(customer_id), customer_name = VALUES(customer_name), product_name = VALUES(product_name),
    quantity = VALUES(quantity), plan_start_date = VALUES(plan_start_date), target_date = VALUES(target_date),
    owner_user_id = VALUES(owner_user_id), owner_name = VALUES(owner_name), status = VALUES(status), stage = VALUES(stage),
    priority = VALUES(priority), risk_level = VALUES(risk_level), risk_score = VALUES(risk_score),
    sample_status = VALUES(sample_status), deleted = 0, update_by = VALUES(update_by);

INSERT INTO nso_project (
    tenant_id, project_no, customer_id, customer_name, product_name, quantity,
    plan_start_date, target_date, owner_user_id, owner_name, status, stage,
    priority, risk_level, risk_score, sample_status, create_by, update_by
)
SELECT 1, 'NSO-DEMO-202607-003', c.id, c.name, '医用监护仪外壳工装', 20,
       CURDATE() - INTERVAL 26 DAY, CURDATE() - INTERVAL 2 DAY, 3, '陈晓明', 'TECH_PUBLISHED', 'EXECUTION',
       'URGENT', 'SERIOUS', 90, 'CONFIRMED', 'SYSTEM', 'SYSTEM'
FROM nso_customer c WHERE c.customer_code = 'CUST-DEMO-003'
ON DUPLICATE KEY UPDATE
    customer_id = VALUES(customer_id), customer_name = VALUES(customer_name), product_name = VALUES(product_name),
    quantity = VALUES(quantity), plan_start_date = VALUES(plan_start_date), target_date = VALUES(target_date),
    owner_user_id = VALUES(owner_user_id), owner_name = VALUES(owner_name), status = VALUES(status), stage = VALUES(stage),
    priority = VALUES(priority), risk_level = VALUES(risk_level), risk_score = VALUES(risk_score),
    sample_status = VALUES(sample_status), deleted = 0, update_by = VALUES(update_by);

INSERT INTO nso_project (
    tenant_id, project_no, customer_id, customer_name, product_name, quantity,
    plan_start_date, target_date, owner_user_id, owner_name, status, stage,
    priority, risk_level, risk_score, sample_status, create_by, update_by
)
SELECT 1, 'NSO-DEMO-202607-004', c.id, c.name, '智能仓储升降机构', 4,
       CURDATE() - INTERVAL 3 DAY, CURDATE() + INTERVAL 28 DAY, 3, '陈晓明', 'REVIEWING', 'REQUIREMENT',
       'MEDIUM', 'LOW', 10, 'NONE', 'SYSTEM', 'SYSTEM'
FROM nso_customer c WHERE c.customer_code = 'CUST-DEMO-001'
ON DUPLICATE KEY UPDATE
    customer_id = VALUES(customer_id), customer_name = VALUES(customer_name), product_name = VALUES(product_name),
    quantity = VALUES(quantity), plan_start_date = VALUES(plan_start_date), target_date = VALUES(target_date),
    owner_user_id = VALUES(owner_user_id), owner_name = VALUES(owner_name), status = VALUES(status), stage = VALUES(stage),
    priority = VALUES(priority), risk_level = VALUES(risk_level), risk_score = VALUES(risk_score),
    sample_status = VALUES(sample_status), deleted = 0, update_by = VALUES(update_by);

SET @demo_project_1 = (SELECT id FROM nso_project WHERE project_no = 'NSO-DEMO-202607-001');
SET @demo_project_2 = (SELECT id FROM nso_project WHERE project_no = 'NSO-DEMO-202607-002');
SET @demo_project_3 = (SELECT id FROM nso_project WHERE project_no = 'NSO-DEMO-202607-003');
SET @demo_project_4 = (SELECT id FROM nso_project WHERE project_no = 'NSO-DEMO-202607-004');

INSERT INTO nso_project_requirement (tenant_id, project_id, category, content, confirm_status, last_reason)
SELECT 1, @demo_project_1, 'DIMENSION', '夹具关键定位尺寸满足客户二维图纸 V1.2 要求', 'CUSTOMER_CONFIRMED', '客户邮件已确认'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM nso_project_requirement
    WHERE tenant_id = 1 AND project_id = @demo_project_1 AND category = 'DIMENSION'
);

INSERT INTO nso_project_requirement (tenant_id, project_id, category, content, confirm_status, last_reason)
SELECT 1, @demo_project_2, 'QUALITY', '检具测量重复性 R&R 小于 10%', 'INTERNAL_CONFIRMED', '质量部已评审'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM nso_project_requirement
    WHERE tenant_id = 1 AND project_id = @demo_project_2 AND category = 'QUALITY'
);

INSERT INTO nso_project_requirement (tenant_id, project_id, category, content, confirm_status, last_reason)
SELECT 1, @demo_project_4, 'FUNCTION', '升降机构额定载荷 600kg，预留安全系数 1.5', 'UNCONFIRMED', '等待客户冻结需求'
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM nso_project_requirement
    WHERE tenant_id = 1 AND project_id = @demo_project_4 AND category = 'FUNCTION'
);

INSERT INTO nso_project_member (tenant_id, project_id, user_id, member_name, project_role, department_name, status)
SELECT 1, p.id, u.id, u.nickname, 'PROJECT_MANAGER', '项目管理部', 'ACTIVE'
FROM nso_project p JOIN sys_user u ON u.username = 'demo-pm'
WHERE p.project_no LIKE 'NSO-DEMO-%'
ON DUPLICATE KEY UPDATE member_name = VALUES(member_name), department_name = VALUES(department_name), status = 'ACTIVE', deleted = 0;

INSERT INTO nso_project_member (tenant_id, project_id, user_id, member_name, project_role, department_name, status)
SELECT 1, p.id, u.id, u.nickname, 'TECHNICAL', '技术部', 'ACTIVE'
FROM nso_project p JOIN sys_user u ON u.username = 'demo-tech'
WHERE p.project_no IN ('NSO-DEMO-202607-001', 'NSO-DEMO-202607-002', 'NSO-DEMO-202607-003')
ON DUPLICATE KEY UPDATE member_name = VALUES(member_name), department_name = VALUES(department_name), status = 'ACTIVE', deleted = 0;

INSERT INTO nso_document (tenant_id, project_id, document_code, file_type, object_code, title, status)
SELECT 1, @demo_project_1, 'DOC-DEMO-001', 'DRAWING', 'ASSY-FIX-001', '精密自动化装配线夹具总装图', 'PUBLISHED'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM nso_document WHERE tenant_id = 1 AND document_code = 'DOC-DEMO-001');

INSERT INTO nso_document (tenant_id, project_id, document_code, file_type, object_code, title, status)
SELECT 1, @demo_project_2, 'DOC-DEMO-002', 'DRAWING', 'BAT-TRAY-002', '电池托盘检具设计图', 'PUBLISHED'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM nso_document WHERE tenant_id = 1 AND document_code = 'DOC-DEMO-002');

SET @demo_doc_1 = (SELECT id FROM nso_document WHERE document_code = 'DOC-DEMO-001');
SET @demo_doc_2 = (SELECT id FROM nso_document WHERE document_code = 'DOC-DEMO-002');

INSERT INTO nso_document_version (
    tenant_id, document_id, project_id, file_name, file_type, version_no, status,
    effective_date, change_summary, current_version, object_code, publish_by, publish_time, uploaded_by, published_by
)
SELECT 1, @demo_doc_1, @demo_project_1, '精密自动化装配线夹具总装图_V1.2.pdf', 'DRAWING', 'V1.2', 'PUBLISHED',
       CURDATE() - INTERVAL 4 DAY, '优化定位销结构并补充装配公差', 1, 'ASSY-FIX-001', '演示技术设计', NOW() - INTERVAL 4 DAY, 4, 4
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM nso_document_version WHERE project_id = @demo_project_1 AND file_type = 'DRAWING' AND version_no = 'V1.2');

INSERT INTO nso_document_version (
    tenant_id, document_id, project_id, file_name, file_type, version_no, status,
    effective_date, change_summary, current_version, object_code, publish_by, publish_time, uploaded_by, published_by
)
SELECT 1, @demo_doc_2, @demo_project_2, '电池托盘检具设计图_V1.0.pdf', 'DRAWING', 'V1.0', 'PUBLISHED',
       CURDATE() - INTERVAL 2 DAY, '首版发布，待工艺会签', 1, 'BAT-TRAY-002', '演示技术设计', NOW() - INTERVAL 2 DAY, 4, 4
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM nso_document_version WHERE project_id = @demo_project_2 AND file_type = 'DRAWING' AND version_no = 'V1.0');

SET @demo_doc_version_1 = (SELECT id FROM nso_document_version WHERE project_id = @demo_project_1 AND file_type = 'DRAWING' AND version_no = 'V1.2');
SET @demo_doc_version_2 = (SELECT id FROM nso_document_version WHERE project_id = @demo_project_2 AND file_type = 'DRAWING' AND version_no = 'V1.0');

UPDATE nso_document SET current_version_id = @demo_doc_version_1, status = 'PUBLISHED' WHERE id = @demo_doc_1;
UPDATE nso_document SET current_version_id = @demo_doc_version_2, status = 'PUBLISHED' WHERE id = @demo_doc_2;
UPDATE nso_project SET current_doc_version_id = @demo_doc_version_1 WHERE id = @demo_project_1;
UPDATE nso_project SET current_doc_version_id = @demo_doc_version_2 WHERE id = @demo_project_2;

INSERT INTO nso_sample (
    tenant_id, project_id, sample_no, purpose, quantity, plan_finish_date, referenced_version,
    doc_version_id, status, confirm_conclusion, responsible_name, issue_summary, created_by
)
SELECT 1, @demo_project_1, 'SMP-DEMO-001', '验证夹具定位重复精度和装配节拍', 2, CURDATE() - INTERVAL 1 DAY, 'V1.2',
       @demo_doc_version_1, 'WAIT_CUSTOMER_CONFIRM', 'PENDING', '张工', '客户现场确认窗口尚未排定', 3
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM nso_sample WHERE sample_no = 'SMP-DEMO-001');

INSERT INTO nso_sample (
    tenant_id, project_id, sample_no, purpose, quantity, plan_finish_date, referenced_version,
    status, confirm_conclusion, responsible_name, created_by, quality_confirmed_by
)
SELECT 1, @demo_project_3, 'SMP-DEMO-002', '确认医用监护仪外壳表面与装配尺寸', 3, CURDATE() - INTERVAL 8 DAY, 'V1.0',
       'CONFIRMED', 'APPROVED', '刘工', 3, 8
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM nso_sample WHERE sample_no = 'SMP-DEMO-002');

INSERT INTO nso_sample (
    tenant_id, project_id, sample_no, purpose, quantity, plan_finish_date, referenced_version,
    doc_version_id, status, confirm_conclusion, responsible_name, issue_summary, created_by
)
SELECT 1, @demo_project_2, 'SMP-DEMO-003', '验证检具测量基准与重复性', 1, CURDATE() + INTERVAL 1 DAY, 'V1.0',
       @demo_doc_version_2, 'WAIT_CUSTOMER_CONFIRM', 'PENDING', '赵工', '等待客户到厂验收', 3
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM nso_sample WHERE sample_no = 'SMP-DEMO-003');

SET @demo_sample_1 = (SELECT id FROM nso_sample WHERE sample_no = 'SMP-DEMO-001');
SET @demo_sample_2 = (SELECT id FROM nso_sample WHERE sample_no = 'SMP-DEMO-002');
SET @demo_sample_3 = (SELECT id FROM nso_sample WHERE sample_no = 'SMP-DEMO-003');

INSERT INTO nso_sample_check (tenant_id, sample_id, check_item, measured_value, result, issue_summary, corrective_action, checker_name, checked_at)
SELECT 1, @demo_sample_1, '定位孔同轴度', '0.03mm', 'PASS', NULL, NULL, '演示质量人员', NOW() - INTERVAL 2 DAY
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM nso_sample_check WHERE tenant_id = 1 AND sample_id = @demo_sample_1 AND check_item = '定位孔同轴度');

INSERT INTO nso_sample_check (tenant_id, sample_id, check_item, measured_value, result, issue_summary, corrective_action, checker_name, checked_at)
SELECT 1, @demo_sample_2, '外壳表面粗糙度', 'Ra 0.8', 'PASS', NULL, NULL, '演示质量人员', NOW() - INTERVAL 9 DAY
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM nso_sample_check WHERE tenant_id = 1 AND sample_id = @demo_sample_2 AND check_item = '外壳表面粗糙度');

INSERT INTO nso_change_order (
    tenant_id, project_id, applicant_user_id, change_no, change_type, urgency, before_content,
    after_content, reason, status, source_stage, approval_node, delay_days, rework_qty
)
SELECT 1, @demo_project_1, 3, 'CHG-DEMO-001', 'DESIGN', 'URGENT', '定位销直径 10mm',
       '定位销直径调整为 12mm，并同步更新限位块', '客户现场装配间隙偏大', 'EXECUTING', 'SAMPLE', 'IMPLEMENTATION', 2, 2
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM nso_change_order WHERE change_no = 'CHG-DEMO-001');

INSERT INTO nso_change_order (
    tenant_id, project_id, applicant_user_id, change_no, change_type, urgency, before_content,
    after_content, reason, status, source_stage, approval_node, delay_days, rework_qty
)
SELECT 1, @demo_project_3, 3, 'CHG-DEMO-002', 'PROCESS', 'URGENT', '采用常规喷涂工艺',
       '改为医疗级粉末喷涂并增加烘烤工序', '客户新增耐腐蚀性要求', 'WAIT_IMPACT', 'EXECUTION', 'IMPACT_ANALYSIS', 3, 5
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM nso_change_order WHERE change_no = 'CHG-DEMO-002');

INSERT INTO nso_change_order (
    tenant_id, project_id, applicant_user_id, change_no, change_type, urgency, before_content,
    after_content, reason, status, source_stage, approval_node, delay_days, rework_qty
)
SELECT 1, @demo_project_2, 3, 'CHG-DEMO-003', 'DOCUMENT', 'NORMAL', '检具标识采用中文标签',
       '检具标识改为中英文双语标签', '出口客户合规要求', 'CLOSED', 'TECHNICAL', 'CLOSED', 0, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM nso_change_order WHERE change_no = 'CHG-DEMO-003');

SET @demo_change_1 = (SELECT id FROM nso_change_order WHERE change_no = 'CHG-DEMO-001');
SET @demo_change_2 = (SELECT id FROM nso_change_order WHERE change_no = 'CHG-DEMO-002');

INSERT INTO nso_change_impact (
    tenant_id, change_id, object_type, object_name, department_name, suggested_action,
    status, feedback_result, responsible_name, feedback_plan, delay_days, rework_qty, extra_cost,
    verified_flag, object_id, object_version
)
SELECT 1, @demo_change_1, 'DRAWING', '精密自动化装配线夹具总装图', '技术部', '更新二维图纸 V1.3 并重新发布',
       'FEEDBACK_DONE', '图纸已完成修订，等待工艺确认', '演示技术设计', '今日完成图纸发布并通知生产', 1, 0, 0, 1, @demo_doc_version_1, 'V1.2'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM nso_change_impact WHERE tenant_id = 1 AND change_id = @demo_change_1 AND object_type = 'DRAWING');

INSERT INTO nso_change_impact (
    tenant_id, change_id, object_type, object_name, department_name, suggested_action,
    status, responsible_name, feedback_plan, delay_days, rework_qty, extra_cost, verified_flag
)
SELECT 1, @demo_change_2, 'PROCESS_ROUTE', '医用监护仪外壳喷涂工艺', '工艺部', '评估新增烘烤工序的设备产能与交期影响',
       'PENDING_FEEDBACK', '演示工艺人员', '待工艺与生产联合评审', 3, 5, 12800.00, 0
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM nso_change_impact WHERE tenant_id = 1 AND change_id = @demo_change_2 AND object_type = 'PROCESS_ROUTE');

INSERT INTO nso_task (
    tenant_id, project_id, task_no, task_type, title, referenced_version, referenced_doc_version_id,
    status, assignee_id, responsible_name, plan_start, plan_finish, block_reason
)
SELECT 1, @demo_project_1, 'TSK-DEMO-001', 'PURCHASE', '采购 12mm 高精度定位销', 'V1.2', @demo_doc_version_1,
       'TODO', 6, '演示采购人员', CURDATE() - INTERVAL 5 DAY, CURDATE() - INTERVAL 2 DAY, '供应商交期确认中'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM nso_task WHERE task_no = 'TSK-DEMO-001');

INSERT INTO nso_task (
    tenant_id, project_id, task_no, task_type, title, referenced_version,
    status, assignee_id, responsible_name, plan_start, plan_finish, block_reason
)
SELECT 1, @demo_project_3, 'TSK-DEMO-002', 'PRODUCTION', '医用监护仪外壳工装试制', 'V1.0',
       'BLOCKED', 7, '演示计划生产', CURDATE() - INTERVAL 6 DAY, CURDATE() - INTERVAL 1 DAY, '喷涂工艺变更待评审'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM nso_task WHERE task_no = 'TSK-DEMO-002');

INSERT INTO nso_task (
    tenant_id, project_id, task_no, task_type, title, referenced_version, referenced_doc_version_id,
    status, assignee_id, responsible_name, plan_start, plan_finish
)
SELECT 1, @demo_project_2, 'TSK-DEMO-003', 'TECHNICAL', '确认检具测量基准方案', 'V1.0', @demo_doc_version_2,
       'TODO', 4, '演示技术设计', CURDATE() - INTERVAL 1 DAY, CURDATE() + INTERVAL 2 DAY
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM nso_task WHERE task_no = 'TSK-DEMO-003');

INSERT INTO nso_task (
    tenant_id, project_id, task_no, task_type, title, referenced_version,
    status, assignee_id, responsible_name, plan_start, plan_finish, actual_start, actual_finish
)
SELECT 1, @demo_project_3, 'TSK-DEMO-004', 'INSPECTION', '样品尺寸与外观全检', 'V1.0',
       'DONE', 8, '演示质量人员', CURDATE() - INTERVAL 12 DAY, CURDATE() - INTERVAL 9 DAY,
       NOW() - INTERVAL 12 DAY, NOW() - INTERVAL 9 DAY
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM nso_task WHERE task_no = 'TSK-DEMO-004');

SET @demo_task_1 = (SELECT id FROM nso_task WHERE task_no = 'TSK-DEMO-001');
SET @demo_task_2 = (SELECT id FROM nso_task WHERE task_no = 'TSK-DEMO-002');
SET @demo_task_3 = (SELECT id FROM nso_task WHERE task_no = 'TSK-DEMO-003');

INSERT INTO nso_task_log (tenant_id, task_id, before_status, after_status, operation_type, summary, operator_name)
SELECT 1, @demo_task_1, 'TODO', 'TODO', 'DEADLINE_WARNING', '采购定位销交期已超期，请尽快处理', 'SYSTEM'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM nso_task_log WHERE tenant_id = 1 AND task_id = @demo_task_1 AND operation_type = 'DEADLINE_WARNING');

INSERT INTO nso_task_log (tenant_id, task_id, before_status, after_status, operation_type, summary, operator_name)
SELECT 1, @demo_task_2, 'TODO', 'BLOCKED', 'TASK_BLOCKED', '因喷涂工艺变更待评审，任务已阻塞', '演示计划生产'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM nso_task_log WHERE tenant_id = 1 AND task_id = @demo_task_2 AND operation_type = 'TASK_BLOCKED');

INSERT INTO nso_risk (tenant_id, project_id, level, score, reasons, suggestion, status, rule_code, rule_version, owner_id, calculated_at)
SELECT 1, @demo_project_1, 'HIGH', 72,
       JSON_ARRAY('客户确认样品已临期', '关键采购任务超期'),
       '安排客户确认时间，并督促供应商提供准确到料承诺。', 'OPEN', 'DELIVERY_RISK_V1', 'V1', 3, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM nso_risk WHERE tenant_id = 1 AND project_id = @demo_project_1 AND rule_code = 'DELIVERY_RISK_V1');

INSERT INTO nso_risk (tenant_id, project_id, level, score, reasons, suggestion, status, rule_code, rule_version, owner_id, calculated_at)
SELECT 1, @demo_project_3, 'SERIOUS', 90,
       JSON_ARRAY('项目交期已超期', '生产任务阻塞', '工艺变更尚未完成影响评估'),
       '立即召开项目风险处置会，明确工艺变更、生产排程和客户沟通的责任人。', 'OPEN', 'DELIVERY_RISK_V1', 'V1', 3, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM nso_risk WHERE tenant_id = 1 AND project_id = @demo_project_3 AND rule_code = 'DELIVERY_RISK_V1');

INSERT INTO nso_risk (tenant_id, project_id, level, score, reasons, suggestion, status, rule_code, rule_version, owner_id, calculated_at)
SELECT 1, @demo_project_2, 'MEDIUM', 38,
       JSON_ARRAY('样品确认窗口临近', '技术确认任务待完成'),
       '在客户到厂验收前完成测量基准的内部评审。', 'OPEN', 'DELIVERY_RISK_V1', 'V1', 3, NOW()
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM nso_risk WHERE tenant_id = 1 AND project_id = @demo_project_2 AND rule_code = 'DELIVERY_RISK_V1');

INSERT INTO nso_message (tenant_id, receiver_id, title, content, type, status, business_type, business_id, channel, template_code, send_result)
SELECT 1, 1, '样品确认已临期', 'SMP-DEMO-001 已超过计划完成日期，请及时跟进客户确认。', 'SAMPLE_CONFIRM', 'UNREAD', 'SAMPLE', @demo_sample_1, 'IN_APP', 'SAMPLE_CONFIRM_DUE', 'SENT'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM nso_message WHERE tenant_id = 1 AND receiver_id = 1 AND type = 'SAMPLE_CONFIRM' AND business_id = @demo_sample_1);

INSERT INTO nso_message (tenant_id, receiver_id, title, content, type, status, business_type, business_id, channel, template_code, send_result)
SELECT 1, 1, '高风险项目需要处置', 'NSO-DEMO-202607-003 存在交期超期和生产阻塞风险，请立即处理。', 'RISK_ALERT', 'UNREAD', 'RISK', @demo_project_3, 'IN_APP', NULL, 'SENT'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM nso_message WHERE tenant_id = 1 AND receiver_id = 1 AND type = 'RISK_ALERT' AND business_id = @demo_project_3);

INSERT INTO nso_message (tenant_id, receiver_id, title, content, type, status, business_type, business_id, channel, template_code, send_result)
SELECT 1, 1, '变更影响分析待反馈', 'CHG-DEMO-002 的喷涂工艺影响分析待工艺部反馈。', 'CHANGE_PENDING', 'UNREAD', 'CHANGE', @demo_change_2, 'IN_APP', NULL, 'SENT'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM nso_message WHERE tenant_id = 1 AND receiver_id = 1 AND type = 'CHANGE_PENDING' AND business_id = @demo_change_2);

INSERT INTO nso_timeline_event (tenant_id, project_id, event_type, title, summary, operator_name, occurred_at, business_type, business_id, trace_id)
SELECT 1, @demo_project_1, 'SAMPLE_SUBMITTED_CONFIRM', '样品已提交客户确认', 'SMP-DEMO-001 等待客户确认', '陈晓明', NOW() - INTERVAL 2 DAY, 'SAMPLE', @demo_sample_1, 'DEMO-001'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM nso_timeline_event WHERE tenant_id = 1 AND project_id = @demo_project_1 AND event_type = 'SAMPLE_SUBMITTED_CONFIRM');

INSERT INTO nso_timeline_event (tenant_id, project_id, event_type, title, summary, operator_name, occurred_at, business_type, business_id, trace_id)
SELECT 1, @demo_project_1, 'CHANGE_APPROVED', '紧急设计变更进入执行', 'CHG-DEMO-001 已完成审批，正在执行图纸修订', '陈晓明', NOW() - INTERVAL 1 DAY, 'CHANGE', @demo_change_1, 'DEMO-002'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM nso_timeline_event WHERE tenant_id = 1 AND project_id = @demo_project_1 AND event_type = 'CHANGE_APPROVED');

INSERT INTO nso_timeline_event (tenant_id, project_id, event_type, title, summary, operator_name, occurred_at, business_type, business_id, trace_id)
SELECT 1, @demo_project_3, 'TASK_BLOCKED', '生产任务已阻塞', '喷涂工艺变更未完成影响评估，试制任务暂停', '演示计划生产', NOW() - INTERVAL 1 DAY, 'TASK', @demo_task_2, 'DEMO-003'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM nso_timeline_event WHERE tenant_id = 1 AND project_id = @demo_project_3 AND event_type = 'TASK_BLOCKED');

-- ===== 结束 V11__demo_business_data.sql =====

-- ===== 开始 V12__role_scope_and_file_governance.sql =====
-- 增量式 V1.0 角色与文件治理加固。既有管理员
-- 自定义配置会被有意保留。

ALTER TABLE nso_file_object
    ADD COLUMN project_id BIGINT NULL AFTER tenant_id,
    ADD KEY idx_file_project (tenant_id, project_id);

ALTER TABLE nso_document_version
    ADD COLUMN file_object_id BIGINT NULL AFTER document_id,
    ADD KEY idx_document_version_file (tenant_id, file_object_id);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.permission_code IN
    ('project:view', 'task:view', 'task:execute', 'task:feedback')
WHERE r.tenant_id = 1 AND r.role_code = 'field_user' AND m.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.permission_code = 'change:create'
WHERE r.tenant_id = 1
  AND r.role_code IN ('process', 'purchaser', 'production')
  AND m.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.permission_code = 'change:close'
WHERE r.tenant_id = 1 AND r.role_code = 'project_manager' AND m.deleted = 0;

-- ===== 结束 V12__role_scope_and_file_governance.sql =====

-- ===== 开始 V13__project_manager_change_close_permission.sql =====
-- V12 已在现有开发数据库中应用。保持此
-- 增量迁移独立，以确保历史迁移校验和稳定。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.permission_code = 'change:close'
WHERE r.tenant_id = 1
  AND r.role_code = 'project_manager'
  AND m.deleted = 0;

-- ===== 结束 V13__project_manager_change_close_permission.sql =====

-- ===== 开始 V14__project_workspace_risk_actions_and_qr_flow.sql =====
ALTER TABLE nso_risk_action
    ADD COLUMN responsible_user_id BIGINT NULL AFTER action_plan,
    ADD COLUMN idempotency_key VARCHAR(64) NULL AFTER status,
    ADD COLUMN closed_by BIGINT NULL AFTER idempotency_key,
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at,
    ADD COLUMN closed_at DATETIME NULL AFTER updated_at,
    ADD COLUMN version INT NOT NULL DEFAULT 0 AFTER closed_at,
    ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 AFTER version,
    ADD UNIQUE KEY uk_risk_action_idempotency (tenant_id, risk_id, idempotency_key),
    ADD KEY idx_risk_action_owner (tenant_id, responsible_user_id, status);

ALTER TABLE nso_qrcode_binding
    ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at,
    ADD UNIQUE KEY uk_qrcode_business (tenant_id, business_type, business_id);

INSERT INTO sys_menu (menu_name, route_path, permission_code, component_name, visible, sort_no, status)
VALUES
    ('风险处置', NULL, 'risk:dispose', NULL, 0, 73, 'ENABLED'),
    ('技术包任务同步', NULL, 'document:sync', NULL, 0, 36, 'ENABLED')
ON DUPLICATE KEY UPDATE
    menu_name = VALUES(menu_name), route_path = VALUES(route_path), component_name = VALUES(component_name),
    visible = VALUES(visible), sort_no = VALUES(sort_no), status = 'ENABLED', deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.permission_code = 'risk:dispose'
WHERE r.tenant_id = 1
  AND r.role_code IN ('project_manager', 'technical', 'process', 'purchaser', 'production', 'quality')
  AND m.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.permission_code = 'document:sync'
WHERE r.tenant_id = 1
  AND r.role_code IN ('project_manager', 'technical')
  AND m.deleted = 0;

-- ===== 结束 V14__project_workspace_risk_actions_and_qr_flow.sql =====

-- ===== 开始 V15__dashboard_aggregation_indexes.sql =====
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

-- ===== 结束 V15__dashboard_aggregation_indexes.sql =====

-- ===== 开始 V16__user_profile_and_demo_departments.sql =====
ALTER TABLE sys_user
    ADD COLUMN phone VARCHAR(32) NULL AFTER nickname,
    ADD COLUMN email VARCHAR(128) NULL AFTER phone,
    ADD COLUMN gender VARCHAR(16) NULL AFTER email,
    ADD COLUMN avatar_file_id BIGINT NULL AFTER gender;

CREATE INDEX idx_sys_user_avatar ON sys_user (tenant_id, avatar_file_id);

INSERT INTO sys_dept (tenant_id, dept_name, leader_name, sort_no, status)
VALUES
    (1, '平台运营部', '演示系统管理员', 10, 'ENABLED'),
    (1, '项目管理部', '演示销售项目经理', 20, 'ENABLED'),
    (1, '技术设计部', '演示技术设计', 30, 'ENABLED'),
    (1, '工艺工程部', '演示工艺人员', 40, 'ENABLED'),
    (1, '采购供应部', '演示采购人员', 50, 'ENABLED'),
    (1, '生产计划部', '演示计划生产', 60, 'ENABLED'),
    (1, '质量管理部', '演示质量人员', 70, 'ENABLED'),
    (1, '客户协同组', '演示客户确认人', 80, 'ENABLED'),
    (1, '经营管理部', '演示管理层', 90, 'ENABLED')
ON DUPLICATE KEY UPDATE leader_name = VALUES(leader_name), status = 'ENABLED', deleted = 0;

-- ===== 结束 V16__user_profile_and_demo_departments.sql =====

-- ===== 开始 V17__organization_scope_number_and_workflow_foundation.sql =====
-- V1.0 验收补全：组织范围、业务编号与工作流历史。
-- 此迁移有意作为不可变 V1-V16 链上的增量补充。

ALTER TABLE sys_dept
    ADD COLUMN leader_user_id BIGINT NULL AFTER leader_name,
    ADD COLUMN ancestors VARCHAR(1000) NOT NULL DEFAULT '' AFTER parent_id;

ALTER TABLE sys_role
    ADD COLUMN data_scope VARCHAR(32) NOT NULL DEFAULT 'SELF' AFTER status;

UPDATE sys_role
SET data_scope = CASE role_code
    WHEN 'admin' THEN 'ALL'
    WHEN 'executive' THEN 'ALL'
    WHEN 'project_manager' THEN 'DEPT_AND_CHILD'
    ELSE 'SELF'
END;

CREATE TABLE nso_sensitive_field_policy (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    field_code VARCHAR(128) NOT NULL,
    read_mode VARCHAR(32) NOT NULL DEFAULT 'MASKED',
    write_allowed TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sensitive_field_policy (tenant_id, role_code, field_code)
) COMMENT='敏感字段读取与编辑策略';

INSERT INTO nso_sensitive_field_policy (tenant_id, role_code, field_code, read_mode, write_allowed)
VALUES
    (1, 'admin', 'CUSTOMER_CONTACT', 'FULL', 1),
    (1, 'project_manager', 'CUSTOMER_CONTACT', 'FULL', 1),
    (1, 'technical', 'CUSTOMER_CONTACT', 'MASKED', 0),
    (1, 'process', 'CUSTOMER_CONTACT', 'MASKED', 0),
    (1, 'purchaser', 'CUSTOMER_CONTACT', 'MASKED', 0),
    (1, 'production', 'CUSTOMER_CONTACT', 'MASKED', 0),
    (1, 'quality', 'CUSTOMER_CONTACT', 'MASKED', 0),
    (1, 'executive', 'CUSTOMER_CONTACT', 'MASKED', 0)
ON DUPLICATE KEY UPDATE read_mode = VALUES(read_mode), write_allowed = VALUES(write_allowed);

CREATE TABLE nso_business_sequence (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    business_type VARCHAR(64) NOT NULL,
    period_key VARCHAR(32) NOT NULL,
    next_value BIGINT NOT NULL DEFAULT 1,
    version INT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_business_sequence (tenant_id, business_type, period_key)
) COMMENT='业务编号流水号';

ALTER TABLE nso_project
    ADD COLUMN project_name VARCHAR(128) NULL AFTER product_name,
    ADD COLUMN archived_at DATETIME NULL AFTER sample_status,
    ADD COLUMN archived_by BIGINT NULL AFTER archived_at,
    ADD COLUMN archive_reason VARCHAR(1000) NULL AFTER archived_by,
    ADD COLUMN suspended_reason VARCHAR(1000) NULL AFTER archive_reason;

UPDATE nso_project SET project_name = product_name WHERE project_name IS NULL;

ALTER TABLE nso_change_order
    ADD COLUMN parent_change_id BIGINT NULL AFTER project_id,
    ADD COLUMN revoked_at DATETIME NULL AFTER approval_node,
    ADD COLUMN revoke_reason VARCHAR(1000) NULL AFTER revoked_at,
    ADD COLUMN verified_at DATETIME NULL AFTER revoke_reason,
    ADD COLUMN verified_by BIGINT NULL AFTER verified_at;

ALTER TABLE nso_sample
    ADD COLUMN parent_sample_id BIGINT NULL AFTER project_id,
    ADD COLUMN round_no INT NOT NULL DEFAULT 1 AFTER parent_sample_id,
    ADD COLUMN previous_issue_summary VARCHAR(1000) NULL AFTER round_no;

ALTER TABLE nso_process_step ADD COLUMN version INT NOT NULL DEFAULT 0, ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0;
ALTER TABLE nso_inspection_item ADD COLUMN version INT NOT NULL DEFAULT 0, ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0;
ALTER TABLE nso_sample_check ADD COLUMN version INT NOT NULL DEFAULT 0, ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0;
ALTER TABLE nso_sample_confirm ADD COLUMN version INT NOT NULL DEFAULT 0, ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0;
ALTER TABLE nso_change_impact ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0;

CREATE TABLE nso_project_status_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    before_status VARCHAR(64),
    after_status VARCHAR(64) NOT NULL,
    action_code VARCHAR(64) NOT NULL,
    reason VARCHAR(1000),
    operator_id BIGINT,
    occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_project_status_history (tenant_id, project_id, occurred_at)
) COMMENT='项目状态迁移历史';

CREATE TABLE nso_change_status_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    change_id BIGINT NOT NULL,
    before_status VARCHAR(64),
    after_status VARCHAR(64) NOT NULL,
    action_code VARCHAR(64) NOT NULL,
    reason VARCHAR(1000),
    operator_id BIGINT,
    occurred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_change_status_history (tenant_id, change_id, occurred_at)
) COMMENT='变更状态迁移历史';

CREATE INDEX idx_task_scope_assignee_status_finish ON nso_task (tenant_id, assignee_id, status, plan_finish);
CREATE INDEX idx_risk_scope_project_status_level ON nso_risk (tenant_id, project_id, status, level);

-- ===== 结束 V17__organization_scope_number_and_workflow_foundation.sql =====

-- ===== 开始 V18__operation_confirmation_archive_and_document_governance.sql =====
-- 高风险操作确认、归档生命周期与受控文件治理。

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

-- ===== 结束 V18__operation_confirmation_archive_and_document_governance.sql =====

-- ===== 开始 V19__rule_execution_risk_details_and_notification_routing.sql =====
-- 版本化规则执行、风险明细、通知收件人与升级证据。

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

-- ===== 结束 V19__rule_execution_risk_details_and_notification_routing.sql =====

-- ===== 开始 V20__feature_completion_import_reporting_and_constraints.sql =====
-- 剩余验收项：导入批次、供应商执行数据与报表索引。

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

-- ===== 结束 V20__feature_completion_import_reporting_and_constraints.sql =====

-- ===== 开始 V21__personnel_organization_project_responsibility_refactor.sql =====
-- 人员、组织与项目职责重构。
-- 保持所有历史 V1-V20 迁移不可变；此脚本为增量补充。

ALTER TABLE sys_user
    ADD COLUMN user_type VARCHAR(16) NOT NULL DEFAULT 'INTERNAL' AFTER status;

CREATE INDEX idx_sys_user_directory ON sys_user (tenant_id, user_type, status, dept_id);

CREATE TABLE nso_project_member_responsibility (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    project_member_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    responsibility_code VARCHAR(64) NOT NULL,
    primary_flag TINYINT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    version INT NOT NULL DEFAULT 0,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_project_member_responsibility (tenant_id, project_member_id, responsibility_code, deleted),
    KEY idx_project_responsibility_scope (tenant_id, project_id, user_id, responsibility_code, status)
) COMMENT='项目成员职责';

CREATE TABLE nso_project_manager_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    project_id BIGINT NOT NULL,
    previous_manager_user_id BIGINT NULL,
    next_manager_user_id BIGINT NOT NULL,
    reason VARCHAR(1000) NOT NULL,
    operator_id BIGINT NULL,
    transferred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_project_manager_history (tenant_id, project_id, transferred_at)
) COMMENT='项目经理交接历史';

INSERT INTO sys_role (tenant_id, role_code, role_name, status, data_scope, version, deleted)
VALUES (1, 'sales', '销售人员', 'ENABLED', 'SELF', 0, 0)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), status = 'ENABLED', data_scope = VALUES(data_scope), deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.permission_code IN
    ('dashboard:view','customer:view','customer:manage','project:view','project:create',
     'project:requirement:manage','customer:invite','sample:view','sample:proxy-confirm',
     'change:view','change:create','task:view','risk:view','message:view')
WHERE r.tenant_id = 1 AND r.role_code = 'sales' AND m.deleted = 0 AND m.status = 'ENABLED';

-- 客户确认账户是外部管理的身份。
UPDATE sys_user u
JOIN sys_user_role ur ON ur.user_id = u.id
JOIN sys_role r ON r.id = ur.role_id AND r.role_code = 'customer_confirm'
SET u.user_type = 'EXTERNAL'
WHERE u.tenant_id = 1;

-- 项目成员展示数据来自账户目录，绝不取自手工填写的输入。
UPDATE nso_project_member pm
JOIN sys_user u ON u.id = pm.user_id AND u.tenant_id = pm.tenant_id
LEFT JOIN sys_dept d ON d.id = u.dept_id AND d.tenant_id = u.tenant_id AND d.deleted = 0
SET pm.member_name = COALESCE(NULLIF(u.nickname, ''), u.username),
    pm.dept_id = u.dept_id,
    pm.department_name = d.dept_name
WHERE pm.tenant_id = 1 AND pm.deleted = 0;

UPDATE nso_project_member
SET project_role = 'CUSTOMER_CONFIRM'
WHERE tenant_id = 1 AND project_role = 'CUSTOMER' AND deleted = 0;

-- 在防止无法映射到可用账户的记录授予访问权限的同时保留历史。
UPDATE nso_project_member pm
LEFT JOIN sys_user u ON u.id = pm.user_id AND u.tenant_id = pm.tenant_id
SET pm.status = 'REMOVED'
WHERE pm.tenant_id = 1 AND pm.deleted = 0
  AND (u.id IS NULL OR u.status <> 'ENABLED');

-- 从旧版 project_role 值回填新的多职责模型。
INSERT IGNORE INTO nso_project_member_responsibility
    (tenant_id, project_member_id, project_id, user_id, responsibility_code, primary_flag, status, version, deleted)
SELECT pm.tenant_id, pm.id, pm.project_id, pm.user_id,
       pm.project_role, 1, 'ACTIVE', 0, 0
FROM nso_project_member pm
WHERE pm.tenant_id = 1 AND pm.deleted = 0 AND pm.status = 'ACTIVE'
  AND pm.user_id IS NOT NULL
  AND pm.project_role IN ('SALES','PROJECT_MANAGER','TECHNICAL','PROCESS','PURCHASER','PRODUCTION','QUALITY','FIELD_USER','CUSTOMER_CONFIRM');

-- 项目职责不得超出匹配系统角色的最低要求。保留既有
-- 角色，并仅为有效活跃成员补充最小必需角色。
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT DISTINCT pmr.user_id, r.id
FROM nso_project_member_responsibility pmr
JOIN sys_role r ON r.tenant_id = pmr.tenant_id AND r.deleted = 0 AND r.status = 'ENABLED'
    AND r.role_code = CASE pmr.responsibility_code
        WHEN 'SALES' THEN 'sales'
        WHEN 'PROJECT_MANAGER' THEN 'project_manager'
        WHEN 'TECHNICAL' THEN 'technical'
        WHEN 'PROCESS' THEN 'process'
        WHEN 'PURCHASER' THEN 'purchaser'
        WHEN 'PRODUCTION' THEN 'production'
        WHEN 'QUALITY' THEN 'quality'
        WHEN 'FIELD_USER' THEN 'field_user'
        WHEN 'CUSTOMER_CONFIRM' THEN 'customer_confirm'
    END
JOIN sys_user u ON u.id = pmr.user_id AND u.tenant_id = pmr.tenant_id AND u.status = 'ENABLED'
WHERE pmr.tenant_id = 1 AND pmr.deleted = 0 AND pmr.status = 'ACTIVE';

-- 每位项目负责人都是项目经理，并获得匹配系统角色的最低权限。
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT p.owner_user_id, r.id
FROM nso_project p
JOIN sys_user u ON u.id = p.owner_user_id AND u.tenant_id = p.tenant_id AND u.status = 'ENABLED'
JOIN sys_role r ON r.tenant_id = p.tenant_id AND r.role_code = 'project_manager' AND r.deleted = 0 AND r.status = 'ENABLED'
WHERE p.tenant_id = 1 AND p.deleted = 0 AND p.owner_user_id IS NOT NULL;

INSERT INTO nso_project_member
    (tenant_id, project_id, user_id, member_name, project_role, dept_id, department_name, status, version, deleted)
SELECT p.tenant_id, p.id, u.id, COALESCE(NULLIF(u.nickname, ''), u.username), 'PROJECT_MANAGER',
       u.dept_id, d.dept_name, 'ACTIVE', 0, 0
FROM nso_project p
JOIN sys_user u ON u.id = p.owner_user_id AND u.tenant_id = p.tenant_id AND u.status = 'ENABLED'
LEFT JOIN sys_dept d ON d.id = u.dept_id AND d.tenant_id = u.tenant_id AND d.deleted = 0
WHERE p.tenant_id = 1 AND p.deleted = 0 AND p.owner_user_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM nso_project_member pm
      WHERE pm.tenant_id = p.tenant_id AND pm.project_id = p.id AND pm.user_id = p.owner_user_id AND pm.deleted = 0
  );

INSERT IGNORE INTO nso_project_member_responsibility
    (tenant_id, project_member_id, project_id, user_id, responsibility_code, primary_flag, status, version, deleted)
SELECT pm.tenant_id, pm.id, pm.project_id, pm.user_id, 'PROJECT_MANAGER', 1, 'ACTIVE', 0, 0
FROM nso_project_member pm
JOIN nso_project p ON p.id = pm.project_id AND p.tenant_id = pm.tenant_id
WHERE pm.tenant_id = 1 AND pm.deleted = 0 AND pm.status = 'ACTIVE'
  AND p.owner_user_id = pm.user_id;

UPDATE nso_project p
JOIN sys_user u ON u.id = p.owner_user_id AND u.tenant_id = p.tenant_id
SET p.owner_name = COALESCE(NULLIF(u.nickname, ''), u.username)
WHERE p.tenant_id = 1 AND p.deleted = 0;

-- ===== 结束 V21__personnel_organization_project_responsibility_refactor.sql =====

-- ===== 开始 V22__account_lifecycle_posts_and_handover.sql =====
-- 账户生命周期、岗位、部门管理历史与交接。
-- V1-V21 保持不可变。此迁移为增量补充，并保留每个既有身份。

ALTER TABLE sys_user
    ADD COLUMN employee_no VARCHAR(64) NULL AFTER username,
    ADD COLUMN force_change_password TINYINT NOT NULL DEFAULT 0 AFTER password_hash,
    ADD COLUMN status_reason VARCHAR(512) NULL AFTER status,
    ADD COLUMN status_effective_until DATETIME NULL AFTER status_reason;

CREATE UNIQUE INDEX uk_sys_user_tenant_employee_no ON sys_user (tenant_id, employee_no);
CREATE INDEX idx_sys_user_lifecycle ON sys_user (tenant_id, user_type, status, dept_id);

-- 既有已启用/已禁用的目录记录成为明确的生命周期词汇。
UPDATE sys_user SET status = 'ACTIVE' WHERE status = 'ENABLED';
UPDATE sys_user SET status = 'DISABLED' WHERE status IS NULL OR status = '';

CREATE TABLE sys_post (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    post_code VARCHAR(64) NOT NULL,
    post_name VARCHAR(128) NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'ENABLED',
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_sys_post_tenant_code (tenant_id, post_code),
    KEY idx_sys_post_tenant_status (tenant_id, status, deleted)
) COMMENT='组织岗位，不等同于系统角色';

CREATE TABLE sys_user_post (
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    primary_flag TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, post_id),
    KEY idx_sys_user_post_tenant_post (tenant_id, post_id)
) COMMENT='用户与岗位关系';

CREATE TABLE sys_dept_manager (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    dept_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    manager_type VARCHAR(16) NOT NULL,
    effective_from DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    effective_to DATETIME NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_by BIGINT NULL,
    reason VARCHAR(512) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_dept_manager_current (tenant_id, dept_id, status, manager_type),
    KEY idx_dept_manager_user (tenant_id, user_id, status)
) COMMENT='部门负责人及副负责人任职历史';

CREATE TABLE nso_user_handover (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    from_user_id BIGINT NOT NULL,
    to_user_id BIGINT NULL,
    handover_type VARCHAR(32) NOT NULL,
    scope_type VARCHAR(32) NOT NULL,
    scope_id BIGINT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    reason VARCHAR(1000) NOT NULL,
    requested_by BIGINT NULL,
    completed_by BIGINT NULL,
    completed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_user_handover_from_status (tenant_id, from_user_id, status),
    KEY idx_user_handover_to_status (tenant_id, to_user_id, status)
) COMMENT='调岗、停用与离职交接单';

CREATE TABLE nso_authorization_audit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    subject_type VARCHAR(32) NOT NULL,
    subject_id BIGINT NOT NULL,
    change_type VARCHAR(64) NOT NULL,
    before_json JSON NULL,
    after_json JSON NULL,
    reason VARCHAR(1000) NOT NULL,
    operator_id BIGINT NULL,
    trace_id VARCHAR(128) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_authorization_audit_subject (tenant_id, subject_type, subject_id, created_at)
) COMMENT='账号、组织、授权和职责变更审计';

-- ===== 结束 V22__account_lifecycle_posts_and_handover.sql =====

-- ===== 开始 V23__authorization_governance_and_permission_contract.sql =====
-- 三方系统治理与规范权限契约。

CREATE TABLE sys_role_dept (
    role_id BIGINT NOT NULL,
    dept_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    PRIMARY KEY (role_id, dept_id),
    KEY idx_sys_role_dept_tenant_dept (tenant_id, dept_id)
) COMMENT='CUSTOM_DEPT 数据范围授权部门';

CREATE TABLE nso_authorization_change_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    target_user_id BIGINT NOT NULL,
    requested_role_code VARCHAR(64) NOT NULL,
    request_type VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    reason VARCHAR(1000) NOT NULL,
    requested_by BIGINT NOT NULL,
    approved_by BIGINT NULL,
    approved_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_authorization_request_target (tenant_id, target_user_id, status),
    KEY idx_authorization_request_status (tenant_id, status, created_at)
) COMMENT='受保护系统角色授权申请';

INSERT INTO sys_role (tenant_id, role_code, role_name, status, data_scope, version, deleted)
VALUES
    (1, 'superadmin', '超级管理员', 'ENABLED', 'ALL', 0, 0),
    (1, 'system_admin', '系统管理员', 'ENABLED', 'ALL', 0, 0),
    (1, 'hr_admin', '人事管理员', 'ENABLED', 'ALL', 0, 0)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), status = 'ENABLED', data_scope = VALUES(data_scope), deleted = 0;

-- 通过显式转换将既有引导管理员保留为首个超级管理员。
INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT ur.user_id, super_role.id
FROM sys_user_role ur
JOIN sys_role admin_role ON admin_role.id = ur.role_id AND admin_role.tenant_id = 1 AND admin_role.role_code = 'admin'
JOIN sys_role super_role ON super_role.tenant_id = 1 AND super_role.role_code = 'superadmin' AND super_role.deleted = 0;

-- 规范系统权限。既有业务编码作为兼容
-- 别名保留，同时为每个既有菜单新增下方的规范 nso:* 权限。
INSERT IGNORE INTO sys_menu (menu_name, route_path, permission_code, component_name, visible, sort_no, status)
VALUES
    ('账号查看', NULL, 'sys:user:read', NULL, 0, 901, 'ENABLED'),
    ('账号创建', NULL, 'sys:user:create', NULL, 0, 902, 'ENABLED'),
    ('账号编辑', NULL, 'sys:user:update', NULL, 0, 903, 'ENABLED'),
    ('账号状态与交接', NULL, 'sys:user:lifecycle', NULL, 0, 904, 'ENABLED'),
    ('密码重置', NULL, 'sys:user:reset-password', NULL, 0, 905, 'ENABLED'),
    ('部门查看', NULL, 'sys:dept:read', NULL, 0, 906, 'ENABLED'),
    ('部门维护', NULL, 'sys:dept:manage', NULL, 0, 907, 'ENABLED'),
    ('岗位维护', NULL, 'sys:post:manage', NULL, 0, 908, 'ENABLED'),
    ('角色查看', NULL, 'sys:role:read', NULL, 0, 909, 'ENABLED'),
    ('角色维护', NULL, 'sys:role:manage', NULL, 0, 910, 'ENABLED'),
    ('角色授权', NULL, 'sys:role:grant', NULL, 0, 911, 'ENABLED'),
    ('权限目录维护', NULL, 'sys:permission:manage', NULL, 0, 912, 'ENABLED'),
    ('授权审计查看', NULL, 'sys:authorization:audit', NULL, 0, 913, 'ENABLED');

-- 每个既有业务权限都会通过规范 nso:* 命名空间暴露。
INSERT IGNORE INTO sys_menu (menu_name, route_path, permission_code, component_name, visible, sort_no, status)
SELECT CONCAT('兼容-', menu_name), NULL, CONCAT('nso:', permission_code), NULL, 0, sort_no + 1000, 'ENABLED'
FROM sys_menu
WHERE permission_code IS NOT NULL
  AND permission_code NOT LIKE 'sys:%' AND permission_code NOT LIKE 'nso:%' AND deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r CROSS JOIN sys_menu m
WHERE r.tenant_id = 1 AND r.role_code IN ('admin', 'superadmin')
  AND m.permission_code LIKE 'sys:%' AND m.status = 'ENABLED' AND m.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.permission_code IN
    ('sys:user:read','sys:user:update','sys:user:lifecycle','sys:user:reset-password','sys:role:read','sys:role:manage','sys:role:grant','sys:permission:manage','sys:authorization:audit')
WHERE r.tenant_id = 1 AND r.role_code = 'system_admin' AND m.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.permission_code IN
    ('sys:user:read','sys:user:create','sys:user:update','sys:user:lifecycle','sys:dept:read','sys:dept:manage','sys:post:manage','sys:authorization:audit')
WHERE r.tenant_id = 1 AND r.role_code = 'hr_admin' AND m.deleted = 0;

-- 过渡期间，规范业务权限会映射每项旧版授权。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT rm.role_id, canonical.id
FROM sys_role_menu rm
JOIN sys_menu legacy ON legacy.id = rm.menu_id
JOIN sys_menu canonical ON canonical.permission_code = CONCAT('nso:', legacy.permission_code)
WHERE legacy.permission_code NOT LIKE 'sys:%' AND legacy.permission_code NOT LIKE 'nso:%';

-- ===== 结束 V23__authorization_governance_and_permission_contract.sql =====

-- ===== 开始 V24__project_ownership_and_external_identity.sql =====
-- 规范项目职责与外部客户身份。

ALTER TABLE nso_project_member_responsibility
    ADD COLUMN owner_slot VARCHAR(160) NULL AFTER primary_flag;

CREATE UNIQUE INDEX uk_project_active_owner_slot ON nso_project_member_responsibility (tenant_id, owner_slot);

-- 旧版职责先成为成员。仅在安全时才推断所有权。
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

-- ===== 结束 V24__project_ownership_and_external_identity.sql =====

-- ===== 开始 V25__retention_policy_and_authorization_indexes.sql =====
-- 保留策略受控、可审计，且绝不静默删除业务历史。

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

-- ===== 结束 V25__retention_policy_and_authorization_indexes.sql =====

-- ===== 开始 V26__external_customer_contact_authorization_closure.sql =====
-- 外部客户闭环：联系人、项目访问、哈希确认令牌与负责人槽位修复。
-- V1-V25 保持不可变。此迁移保留旧版账户、确认链接与审计证据。

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

-- 复用客户联系人表。既有客户主数据成为幂等的初始联系人。
INSERT INTO crm_contact (tenant_id, customer_id, contact_name, phone, email, position_name, preferred_channel, status, deleted)
SELECT c.tenant_id, c.id, c.contact_name, c.phone, NULL, '客户确认联系人', 'LINK', 'ENABLED', 0
FROM nso_customer c
WHERE c.deleted = 0 AND c.contact_name IS NOT NULL AND TRIM(c.contact_name) <> ''
  AND NOT EXISTS (
      SELECT 1 FROM crm_contact cc
      WHERE cc.tenant_id = c.tenant_id AND cc.customer_id = c.id AND cc.deleted = 0
        AND cc.contact_name = c.contact_name
  );

-- 仅当关系明确时，才将旧版外部身份关联到项目客户。
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

-- 为已拥有有效旧版确认链接的客户联系人创建外部身份。
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

-- 当可识别客户联系人时，每条有效旧版链接都会成为哈希外部令牌。
INSERT IGNORE INTO nso_external_project_access (tenant_id, identity_id, project_id, status, granted_at)
SELECT DISTINCT ei.tenant_id, ei.id, p.id, 'ACTIVE', NOW()
FROM nso_external_identity ei
JOIN nso_sample_confirm sc ON sc.tenant_id = ei.tenant_id
JOIN nso_sample s ON s.id = sc.sample_id AND s.tenant_id = sc.tenant_id
JOIN nso_project p ON p.id = s.project_id AND p.tenant_id = s.tenant_id
WHERE ei.status = 'ACTIVE' AND ei.customer_id = p.customer_id;

-- 从内部团队移除前，先将原客户成员的项目关系保留为外部访问。
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

-- 原演示客户仅保留为历史证据；不再授予成员资格或登录访问。
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

-- V24 在 responsibility_code 变更后仍写入旧 owner_slot 值。仅在负责人唯一时重建槽位。
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

-- ===== 结束 V26__external_customer_contact_authorization_closure.sql =====

-- ===== 开始 V27__pilot_closure_state_machine_and_demo_repair.sql =====
-- 仅修复已知历史演示数据不一致。普通项目
-- 会被有意排除：其状态必须继续由服务修改。
SET @demo_project_id := (
    SELECT id FROM nso_project
    WHERE tenant_id = 1 AND project_no = 'NSO-DEMO-202607-003' AND deleted = 0
    LIMIT 1
);
SET @demo_sample_id := (
    SELECT id FROM nso_sample
    WHERE tenant_id = 1 AND project_id = @demo_project_id AND deleted = 0
    ORDER BY id DESC
    LIMIT 1
);
SET @demo_before_status := (
    SELECT status FROM nso_project
    WHERE id = @demo_project_id AND tenant_id = 1
);

UPDATE nso_sample
SET status = 'CONFIRMED',
    confirm_conclusion = 'PASS'
WHERE id = @demo_sample_id
  AND tenant_id = 1
  AND status IN ('CONFIRMED', 'WAIT_CUSTOMER_CONFIRM', 'CHECKED', 'DRAFT');

UPDATE nso_project
SET status = 'CUSTOMER_CONFIRMED',
    stage = 'SAMPLE',
    sample_status = 'CONFIRMED'
WHERE id = @demo_project_id
  AND tenant_id = 1
  AND EXISTS (
      SELECT 1 FROM nso_sample
      WHERE id = @demo_sample_id
        AND tenant_id = 1
        AND status = 'CONFIRMED'
        AND confirm_conclusion = 'PASS'
  );

INSERT INTO nso_project_status_history
    (tenant_id, project_id, before_status, after_status, action_code, reason, operator_id)
SELECT p.tenant_id, p.id, @demo_before_status, 'CUSTOMER_CONFIRMED', 'DEMO_SAMPLE_STATE_REPAIRED',
       'V27 修复历史样品已确认但项目仍停留在打样状态的不一致数据', p.owner_user_id
FROM nso_project p
WHERE p.id = @demo_project_id
  AND NOT EXISTS (
      SELECT 1 FROM nso_project_status_history h
      WHERE h.tenant_id = p.tenant_id AND h.project_id = p.id
        AND h.action_code = 'DEMO_SAMPLE_STATE_REPAIRED'
  );

INSERT INTO nso_business_event
    (tenant_id, project_id, business_type, business_id, action_code, before_summary, after_summary, operator_id, operator_name)
SELECT p.tenant_id, p.id, 'PROJECT', p.id, 'DEMO_SAMPLE_STATE_REPAIRED', @demo_before_status,
       'CUSTOMER_CONFIRMED / CONFIRMED', p.owner_user_id, COALESCE(p.owner_name, 'SYSTEM')
FROM nso_project p
WHERE p.id = @demo_project_id
  AND NOT EXISTS (
      SELECT 1 FROM nso_business_event e
      WHERE e.tenant_id = p.tenant_id AND e.project_id = p.id
        AND e.action_code = 'DEMO_SAMPLE_STATE_REPAIRED'
  );

INSERT INTO nso_timeline_event
    (tenant_id, project_id, business_type, business_id, event_type, title, summary, operator_name)
SELECT p.tenant_id, p.id, 'PROJECT', p.id, 'DEMO_SAMPLE_STATE_REPAIRED', 'DEMO_SAMPLE_STATE_REPAIRED',
       'V27 已将样品确认状态同步回项目；严重风险和受阻任务保持原状，继续作为交付阻断示例。', COALESCE(p.owner_name, 'SYSTEM')
FROM nso_project p
WHERE p.id = @demo_project_id
  AND NOT EXISTS (
      SELECT 1 FROM nso_timeline_event e
      WHERE e.tenant_id = p.tenant_id AND e.project_id = p.id
        AND e.event_type = 'DEMO_SAMPLE_STATE_REPAIRED'
  );

-- ===== 结束 V27__pilot_closure_state_machine_and_demo_repair.sql =====

