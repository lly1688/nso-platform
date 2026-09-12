package com.nso.framework.security;

import com.nso.framework.config.NsoDemoProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// 演示业务场景初始化器。
@Component
@Order(30)
public class NsoDemoScenarioInitializer implements ApplicationRunner {

    // NSO演示配置
    private final NsoDemoProperties properties;
    // JDBC模板
    private final JdbcTemplate jdbc;

    public NsoDemoScenarioInitializer(NsoDemoProperties properties, JdbcTemplate jdbc) {
        this.properties = properties;
        this.jdbc = jdbc;
    }

    // 使用稳定业务键初始化可重复执行的演示数据。
    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }
        // 按依赖顺序初始化账号档案、项目关系、外部访问和任务消息。
        seedProfiles();
        seedArchivedWorkflow();
        seedProjectMembers();
        seedCustomerExternalAccess();
        seedTasks();
        seedMessages();
    }

    private void seedProfiles() {
        seedProfile("demo-admin", "平台运营部", "13900000001", "demo.admin@nso.local", "MALE");
        seedProfile("demo-pm", "项目管理部", "13900000002", "demo.pm@nso.local", "MALE");
        seedProfile("demo-tech", "技术设计部", "13900000003", "demo.tech@nso.local", "FEMALE");
        seedProfile("demo-process", "工艺工程部", "13900000004", "demo.process@nso.local", "MALE");
        seedProfile("demo-purchase", "采购供应部", "13900000005", "demo.purchase@nso.local", "FEMALE");
        seedProfile("demo-production", "生产计划部", "13900000006", "demo.production@nso.local", "MALE");
        seedProfile("demo-quality", "质量管理部", "13900000007", "demo.quality@nso.local", "FEMALE");
        seedProfile("demo-executive", "经营管理部", "13900000009", "demo.executive@nso.local", "MALE");
    }

    // 初始化演示项目的完整归档生命周期。 仅在开启开关时使用稳定业务键写入。
    private void seedArchivedWorkflow() {
        jdbc.update("""
                INSERT INTO nso_project (tenant_id, project_no, customer_id, customer_name, product_name, quantity, plan_start_date, target_date, owner_user_id, owner_name, status, stage, priority, risk_level, risk_score, sample_status, create_by, update_by)
                SELECT 1,'NSO-DEMO-CLOSED-001',c.id,c.name,'归档演示：精密检测治具',8,CURDATE()-INTERVAL 45 DAY,CURDATE()-INTERVAL 18 DAY,u.id,u.nickname,'ARCHIVED','ARCHIVE','NORMAL','LOW',0,'CONFIRMED','DEMO','DEMO'
                FROM nso_customer c JOIN sys_user u ON u.tenant_id=1 AND u.username='demo-pm'
                WHERE c.tenant_id=1 AND c.customer_code='CUST-DEMO-002'
                ON DUPLICATE KEY UPDATE status='ARCHIVED',stage='ARCHIVE',sample_status='CONFIRMED',deleted=0
                """);
        member("NSO-DEMO-CLOSED-001", "demo-pm", "PROJECT_MANAGER", "项目管理部");
        member("NSO-DEMO-CLOSED-001", "demo-tech", "TECHNICAL", "技术设计部");
        member("NSO-DEMO-CLOSED-001", "demo-production", "PRODUCTION", "生产计划部");
        member("NSO-DEMO-CLOSED-001", "demo-quality", "QUALITY", "质量管理部");
        jdbc.update("""
                INSERT INTO nso_sample (tenant_id, project_id, sample_no, purpose, quantity, plan_finish_date, referenced_version, status, confirm_conclusion, responsible_name, issue_summary, created_by, quality_confirmed_by)
                SELECT 1,p.id,'SMP-DEMO-CLOSED-001','完整闭环客户确认样品',1,CURDATE()-INTERVAL 32 DAY,'V1.0','CONFIRMED','PASS','演示生产人员','客户通过公共确认链接完成确认',u.id,q.id
                FROM nso_project p JOIN sys_user u ON u.tenant_id=1 AND u.username='demo-production' JOIN sys_user q ON q.tenant_id=1 AND q.username='demo-quality'
                WHERE p.tenant_id=1 AND p.project_no='NSO-DEMO-CLOSED-001'
                    AND NOT EXISTS (SELECT 1 FROM nso_sample s WHERE s.tenant_id=1 AND s.sample_no='SMP-DEMO-CLOSED-001')
                """);
        jdbc.update("""
                INSERT INTO nso_delivery_record (tenant_id, project_id, quantity, logistics_no, receiver, customer_feedback, status, shipped_at, signed_at)
                SELECT 1,p.id,8,'DEMO-FULL-001','客户收货人','交付验收完成','SIGNED',NOW()-INTERVAL 22 DAY,NOW()-INTERVAL 20 DAY
                FROM nso_project p WHERE p.tenant_id=1 AND p.project_no='NSO-DEMO-CLOSED-001'
                    AND NOT EXISTS (SELECT 1 FROM nso_delivery_record d WHERE d.tenant_id=1 AND d.logistics_no='DEMO-FULL-001')
                """);
        jdbc.update("""
                INSERT INTO nso_project_archive (tenant_id, project_id, project_no, customer_name, archived_by, archive_reason, archived_at, status)
                SELECT 1,p.id,p.project_no,p.customer_name,u.id,'开发演示：立项、技术发布、样品确认、投产、交付完成后归档',NOW()-INTERVAL 15 DAY,'ARCHIVED'
                FROM nso_project p JOIN sys_user u ON u.tenant_id=1 AND u.username='demo-pm'
                WHERE p.tenant_id=1 AND p.project_no='NSO-DEMO-CLOSED-001'
                ON DUPLICATE KEY UPDATE archive_reason=VALUES(archive_reason),status='ARCHIVED'
                """);
        history("NSO-DEMO-CLOSED-001", null, "INITIATED", "PROJECT_CREATED", "演示完整流程立项", 44);
        history("NSO-DEMO-CLOSED-001", "INITIATED", "TECH_PUBLISHED", "TECH_PUBLISHED", "技术包已发布", 40);
        history("NSO-DEMO-CLOSED-001", "TECH_PUBLISHED", "CUSTOMER_CONFIRMING", "SAMPLE_SUBMITTED_CONFIRM", "样品已发送客户确认", 35);
        history("NSO-DEMO-CLOSED-001", "CUSTOMER_CONFIRMING", "CUSTOMER_CONFIRMED", "SAMPLE_CONFIRMED", "客户通过确认链接", 32);
        history("NSO-DEMO-CLOSED-001", "CUSTOMER_CONFIRMED", "PRODUCING", "PRODUCTION_STARTED", "投产", 29);
        history("NSO-DEMO-CLOSED-001", "PRODUCING", "DELIVERED", "DELIVERY_SIGNED", "交付签收", 20);
        history("NSO-DEMO-CLOSED-001", "DELIVERED", "COMPLETED", "PROJECT_COMPLETED", "项目完成", 17);
        history("NSO-DEMO-CLOSED-001", "COMPLETED", "ARCHIVED", "PROJECT_ARCHIVED", "项目归档", 15);
    }

    private void history(String projectNo, String before, String after, String action, String reason, int daysAgo) {
        jdbc.update("""
                INSERT INTO nso_project_status_history (tenant_id, project_id, before_status, after_status, action_code, reason, operator_id, occurred_at)
                SELECT 1, p.id, ?, ?, ?, ?, u.id, NOW()-INTERVAL ? DAY
                FROM nso_project p JOIN sys_user u ON u.tenant_id=1 AND u.username='demo-pm'
                WHERE p.tenant_id = 1 AND p.project_no = ?
                    AND NOT EXISTS (SELECT 1 FROM nso_project_status_history h WHERE h.tenant_id = 1 AND h.project_id = p.id AND h.action_code = ?)
                """, before, after, action, reason, daysAgo, projectNo, action);
    }

    private void seedCustomerExternalAccess() {
        jdbc.update("""
                INSERT INTO crm_contact (tenant_id, customer_id, contact_name, phone, email, position_name, preferred_channel, status, deleted)
                SELECT 1,c.id,'演示确认联系人A','13800001101','demo.contact.a@nso.local','项目接口人','LINK','ENABLED',0
                FROM nso_customer c WHERE c.tenant_id=1 AND c.customer_code='CUST-DEMO-001'
                    AND NOT EXISTS (SELECT 1 FROM crm_contact cc WHERE cc.tenant_id=1 AND cc.customer_id=c.id AND cc.contact_name='演示确认联系人A' AND cc.deleted=0)
                """);
        jdbc.update("""
                INSERT INTO crm_contact (tenant_id, customer_id, contact_name, phone, email, position_name, preferred_channel, status, deleted)
                SELECT 1,c.id,'演示确认联系人B','13800001102','demo.contact.b@nso.local','质量接口人','LINK','ENABLED',0
                FROM nso_customer c WHERE c.tenant_id=1 AND c.customer_code='CUST-DEMO-002'
                    AND NOT EXISTS (SELECT 1 FROM crm_contact cc WHERE cc.tenant_id=1 AND cc.customer_id=c.id AND cc.contact_name='演示确认联系人B' AND cc.deleted=0)
                """);
        jdbc.update("""
                INSERT IGNORE INTO nso_external_identity (tenant_id, customer_id, contact_id, name, mobile, email, status)
                SELECT cc.tenant_id,cc.customer_id,cc.id,cc.contact_name,cc.phone,cc.email,'ACTIVE'
                FROM crm_contact cc WHERE cc.tenant_id=1 AND cc.deleted=0 AND cc.status='ENABLED'
                """);
        jdbc.update("""
                INSERT IGNORE INTO nso_external_project_access (tenant_id, identity_id, project_id, status, granted_by, granted_at)
                SELECT 1,ei.id,p.id,'ACTIVE',u.id,NOW()
                FROM nso_external_identity ei JOIN nso_project p ON p.tenant_id = ei.tenant_id AND p.customer_id = ei.customer_id
                    JOIN sys_user u ON u.tenant_id=1 AND u.username='demo-pm'
                WHERE ei.tenant_id=1 AND ei.status='ACTIVE' AND p.deleted=0
                """);
        jdbc.update("""
                INSERT INTO nso_sample_confirm (tenant_id, sample_id, token, confirmer, company_name, contact, expire_at, used_flag, max_use_count, used_count)
                SELECT 1,s.id,'demo-public-confirm-2026','演示确认联系人A',p.customer_name,'演示确认联系人A',NOW()+INTERVAL 14 DAY,0,1,0
                FROM nso_sample s JOIN nso_project p ON p.id = s.project_id AND p.tenant_id = s.tenant_id
                WHERE s.tenant_id=1 AND s.sample_no='SMP-DEMO-001'
                    AND NOT EXISTS (SELECT 1 FROM nso_sample_confirm sc WHERE sc.tenant_id=1 AND sc.token='demo-public-confirm-2026')
                """);
        jdbc.update("""
                INSERT IGNORE INTO nso_external_token (tenant_id, identity_id, project_id, sample_id, legacy_confirmation_id, token_hash, expire_at, max_uses, used_count, status, created_by)
                SELECT sc.tenant_id,ei.id,p.id,s.id,sc.id,SHA2(sc.token,256),sc.expire_at,sc.max_use_count,sc.used_count,'ACTIVE',u.id
                FROM nso_sample_confirm sc JOIN nso_sample s ON s.id = sc.sample_id AND s.tenant_id = sc.tenant_id
                    JOIN nso_project p ON p.id = s.project_id AND p.tenant_id = s.tenant_id
                    JOIN crm_contact cc ON cc.tenant_id=p.tenant_id AND cc.customer_id=p.customer_id AND cc.contact_name='演示确认联系人A' AND cc.deleted=0
                    JOIN nso_external_identity ei ON ei.tenant_id = cc.tenant_id AND ei.contact_id = cc.id
                    JOIN sys_user u ON u.tenant_id=1 AND u.username='demo-pm'
                WHERE sc.tenant_id=1 AND sc.token='demo-public-confirm-2026'
                """);
    }

    private void seedProfile(String username, String department, String phone, String email, String gender) {
        jdbc.update("""
                UPDATE sys_user u JOIN sys_dept d ON d.tenant_id = u.tenant_id AND d.dept_name = ? AND d.deleted = 0
                SET u.dept_id = COALESCE(u.dept_id, d.id),
                    u.phone = COALESCE(NULLIF(u.phone, ''), ?),
                    u.email = COALESCE(NULLIF(u.email, ''), ?),
                    u.gender = COALESCE(NULLIF(u.gender, ''), ?)
                WHERE u.tenant_id = 1 AND u.username = ?
                """, department, phone, email, gender, username);
    }

    private void seedProjectMembers() {
        member("NSO-DEMO-202607-001", "demo-admin", "ADMIN", "平台运营部");
        member("NSO-DEMO-202607-001", "demo-pm", "PROJECT_MANAGER", "项目管理部");
        member("NSO-DEMO-202607-002", "demo-pm", "PROJECT_MANAGER", "项目管理部");
        member("NSO-DEMO-202607-003", "demo-pm", "PROJECT_MANAGER", "项目管理部");
        member("NSO-DEMO-202607-004", "demo-pm", "PROJECT_MANAGER", "项目管理部");
        member("NSO-DEMO-202607-001", "demo-tech", "TECHNICAL", "技术设计部");
        member("NSO-DEMO-202607-002", "demo-tech", "TECHNICAL", "技术设计部");
        member("NSO-DEMO-202607-003", "demo-tech", "TECHNICAL", "技术设计部");
        member("NSO-DEMO-202607-003", "demo-process", "PROCESS", "工艺工程部");
        member("NSO-DEMO-202607-001", "demo-purchase", "PURCHASER", "采购供应部");
        member("NSO-DEMO-202607-003", "demo-production", "PRODUCTION", "生产计划部");
        member("NSO-DEMO-202607-001", "demo-quality", "QUALITY", "质量管理部");
        member("NSO-DEMO-202607-003", "demo-quality", "QUALITY", "质量管理部");
        member("NSO-DEMO-202607-003", "demo-executive", "EXECUTIVE", "经营管理部");
    }

    private void member(String projectNo, String username, String projectRole, String department) {
        jdbc.update("""
                INSERT INTO nso_project_member (tenant_id, project_id, user_id, member_name, project_role, department_name, status)
                SELECT 1, p.id, u.id, u.nickname, ?, ?, 'ACTIVE'
                FROM nso_project p JOIN sys_user u ON u.tenant_id = 1 AND u.username = ?
                WHERE p.tenant_id = 1 AND p.project_no = ?
                ON DUPLICATE KEY UPDATE member_name = VALUES(member_name), department_name = VALUES(department_name),
                    project_role = VALUES(project_role), status = 'ACTIVE', deleted = 0
                """, projectRole, department, username, projectNo);
    }

    private void seedTasks() {
        task("TSK-DEMO-ADM-001", "NSO-DEMO-202607-001", "demo-admin", "COORDINATION", "演示项目风险协调", "TODO", 2, 3, null);
        task("TSK-DEMO-PM-001", "NSO-DEMO-202607-004", "demo-pm", "PLAN", "冻结客户需求并安排评审", "TODO", 0, 2, null);
        task("TSK-DEMO-TECH-002", "NSO-DEMO-202607-002", "demo-tech", "TECHNICAL", "确认测量基准与图纸修订", "TODO", 0, 2, null);
        task("TSK-DEMO-PROC-001", "NSO-DEMO-202607-003", "demo-process", "PROCESS", "反馈喷涂工艺变更影响", "TODO", 0, 2, null);
        task("TSK-DEMO-PUR-002", "NSO-DEMO-202607-001", "demo-purchase", "PURCHASE", "确认关键定位销到料承诺", "TODO", -4, -1, "供应商交期待确认");
        task("TSK-DEMO-PROD-002", "NSO-DEMO-202607-003", "demo-production", "PRODUCTION", "处理试制排程阻塞", "BLOCKED", -2, 1, "等待工艺变更影响评估完成");
        task("TSK-DEMO-QA-002", "NSO-DEMO-202607-001", "demo-quality", "INSPECTION", "复核样品定位尺寸报告", "TODO", 0, 2, null);
        task("TSK-DEMO-EXEC-001", "NSO-DEMO-202607-003", "demo-executive", "RISK_REVIEW", "审阅严重交付风险处置方案", "TODO", 0, 3, null);
    }

    private void task(String taskNo, String projectNo, String username, String taskType, String title, String status,
                      int startOffset, int finishOffset, String blockReason) {
        jdbc.update("""
                INSERT INTO nso_task (tenant_id, project_id, task_no, task_type, title, status, assignee_id, responsible_name,
                    plan_start, plan_finish, block_reason, version)
                SELECT 1, p.id, ?, ?, ?, ?, u.id, u.nickname,
                    CURDATE() + INTERVAL ? DAY, CURDATE() + INTERVAL ? DAY, ?, 0
                FROM nso_project p JOIN sys_user u ON u.tenant_id = 1 AND u.username = ?
                WHERE p.tenant_id = 1 AND p.project_no = ?
                    AND NOT EXISTS (SELECT 1 FROM nso_task t WHERE t.tenant_id = 1 AND t.task_no = ?)
                """, taskNo, taskType, title, status, startOffset, finishOffset, blockReason, username, projectNo, taskNo);
    }

    private void seedMessages() {
        message("demo-admin", "NSO-DEMO-202607-001", "项目风险协调待办", "请跟进样品确认与采购超期风险", "RISK_ALERT");
        message("demo-pm", "NSO-DEMO-202607-004", "需求冻结待推进", "客户需求尚未冻结，请安排评审", "PROJECT_TODO");
        message("demo-tech", "NSO-DEMO-202607-002", "图纸修订待完成", "测量基准确认任务已分派", "TASK_DUE");
        message("demo-process", "NSO-DEMO-202607-003", "工艺影响待反馈", "喷涂工艺变更等待反馈", "CHANGE_PENDING");
        message("demo-purchase", "NSO-DEMO-202607-001", "采购任务已逾期", "关键定位销交期需要立即确认", "TASK_DUE");
        message("demo-production", "NSO-DEMO-202607-003", "生产任务受阻", "工艺影响未完成，试制排程已阻塞", "TASK_BLOCKED");
        message("demo-quality", "NSO-DEMO-202607-001", "样品复核待处理", "请复核定位尺寸与检验记录", "SAMPLE_CONFIRM");
        message("demo-executive", "NSO-DEMO-202607-003", "严重交付风险待审阅", "请审阅风险处置方案和项目统计报表", "RISK_ALERT");
    }

    private void message(String username, String projectNo, String title, String content, String type) {
        jdbc.update("""
                INSERT INTO nso_message (tenant_id, receiver_id, title, content, type, status, business_type, business_id, channel, send_result)
                SELECT 1, u.id, ?, ?, ?, 'UNREAD', 'PROJECT', p.id, 'IN_APP', 'SENT'
                FROM sys_user u JOIN nso_project p ON p.tenant_id = 1 AND p.project_no = ?
                WHERE u.tenant_id = 1 AND u.username = ?
                    AND NOT EXISTS (
                        SELECT 1 FROM nso_message m WHERE m.tenant_id = 1 AND m.receiver_id = u.id
                            AND m.title = ? AND m.business_type = 'PROJECT' AND m.business_id = p.id)
                """, title, content, type, projectNo, username, title);
    }
}
