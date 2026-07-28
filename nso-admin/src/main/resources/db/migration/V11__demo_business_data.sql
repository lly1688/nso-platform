-- Development demonstration data for the NSO workbench.
-- All records use the DEMO prefix so they are identifiable and safe to re-run.

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
