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
