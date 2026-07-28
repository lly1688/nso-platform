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
