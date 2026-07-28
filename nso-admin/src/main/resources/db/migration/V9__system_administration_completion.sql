-- V1.0 system administration completion.  Earlier migrations are immutable.

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
