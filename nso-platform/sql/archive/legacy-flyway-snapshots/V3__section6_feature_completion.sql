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
