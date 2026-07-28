package com.nso.framework.security;

import com.nso.framework.config.NsoDemoProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Development-only fixture data. Every lookup uses the stable demo username rather than database IDs.
 */
@Component
@Order(30)
public class NsoDemoScenarioInitializer implements ApplicationRunner {
    private final NsoDemoProperties properties;
    private final JdbcTemplate jdbc;

    public NsoDemoScenarioInitializer(NsoDemoProperties properties, JdbcTemplate jdbc) {
        this.properties = properties;
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }
        seedProfiles();
        seedProjectMembers();
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
        seedProfile("demo-customer", "客户协同组", "13900000008", "demo.customer@nso.local", "UNSPECIFIED");
        seedProfile("demo-executive", "经营管理部", "13900000009", "demo.executive@nso.local", "MALE");
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
        member("NSO-DEMO-202607-001", "demo-customer", "CUSTOMER", "客户协同组");
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
        message("demo-admin", "NSO-DEMO-202607-001", "项目风险协调待办", "请跟进样品确认与采购超期风险。", "RISK_ALERT");
        message("demo-pm", "NSO-DEMO-202607-004", "需求冻结待推进", "客户需求尚未冻结，请安排评审。", "PROJECT_TODO");
        message("demo-tech", "NSO-DEMO-202607-002", "图纸修订待完成", "测量基准确认任务已分派。", "TASK_DUE");
        message("demo-process", "NSO-DEMO-202607-003", "工艺影响待反馈", "喷涂工艺变更等待反馈。", "CHANGE_PENDING");
        message("demo-purchase", "NSO-DEMO-202607-001", "采购任务已逾期", "关键定位销交期需要立即确认。", "TASK_DUE");
        message("demo-production", "NSO-DEMO-202607-003", "生产任务受阻", "工艺影响未完成，试制排程已阻塞。", "TASK_BLOCKED");
        message("demo-quality", "NSO-DEMO-202607-001", "样品复核待处理", "请复核定位尺寸与检验记录。", "SAMPLE_CONFIRM");
        message("demo-customer", "NSO-DEMO-202607-001", "样品等待客户确认", "请通过收到的客户确认链接完成样品确认。", "SAMPLE_CONFIRM");
        message("demo-executive", "NSO-DEMO-202607-003", "严重交付风险待审阅", "请审阅风险处置方案和项目统计报表。", "RISK_ALERT");
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
