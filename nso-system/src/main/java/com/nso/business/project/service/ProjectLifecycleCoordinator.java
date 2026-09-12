package com.nso.business.project.service;

import com.nso.business.core.NsoDtos.ProjectActionRequest;
import com.nso.business.core.TenantContext;
import com.nso.business.project.domain.Project;
import com.nso.business.project.mapper.ProjectMapper;
import com.nso.business.support.BusinessEventService;
import com.nso.business.support.OperationConfirmationService;
import com.nso.shared.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

// 项目状态机与归档生命周期协作组件。 调用方负责授权和事务边界；本组件统一维护状态历史、事件和乐观锁约束。
@Component

// 项目生命周期协调器 协调项目状态流转与生命周期管理
public class ProjectLifecycleCoordinator {
    // 项目数据映射
    private final ProjectMapper projects;
    // JDBC模板
    private final JdbcTemplate jdbc;
    // 业务事件服务
    private final BusinessEventService events;
    // 操作确认服务
    private final OperationConfirmationService confirmations;

    public ProjectLifecycleCoordinator(
            ProjectMapper projects,
            JdbcTemplate jdbc,
            BusinessEventService events,
            OperationConfirmationService confirmations) {
        this.projects = projects;
        this.jdbc = jdbc;
        this.events = events;
        this.confirmations = confirmations;
    }

    // 根据样品结果同步项目状态。
    public void reconcileSampleState(Project project, String reason) {
        if (!Set.of("TECH_PUBLISHED", "SAMPLING", "CUSTOMER_CONFIRMING", "CUSTOMER_CONFIRMED").contains(project.getStatus())) {
            throw BusinessException.ruleBlock("PROJECT_SAMPLE_STATE", "当前项目状态不允许执行样品状态同步",
                    project.getStatus(), "TECH_PUBLISHED/SAMPLING/CUSTOMER_CONFIRMING/CUSTOMER_CONFIRMED", "仅对样品阶段的历史不一致数据执行同步");
        }
        Map<String, Object> latest = jdbc.query("SELECT id,status FROM nso_sample WHERE tenant_id=? AND project_id=? AND deleted=0 ORDER BY id DESC LIMIT 1",
                resultSet -> resultSet.next() ? Map.of("id", resultSet.getLong("id"), "status", resultSet.getString("status")) : null,
                TenantContext.tenantId(), project.getId());
        if (latest == null || latest.get("status") == null) {
            throw BusinessException.ruleBlock("PROJECT_SAMPLE_STATE", "项目没有可同步的样品状态", "none", "latest sample", "先创建样品，或核对历史样品数据");
        }
        String sampleStatus = String.valueOf(latest.get("status"));
        String targetStatus = switch (sampleStatus) {
            case "CONFIRMED" -> "CUSTOMER_CONFIRMED";
            case "WAIT_CUSTOMER_CONFIRM", "WAIT_SUPPLEMENT", "CONDITIONAL_PASS" -> "CUSTOMER_CONFIRMING";
            case "DRAFT", "CHECKED", "REWORKING", "REJECTED" -> "SAMPLING";
            default -> throw BusinessException.ruleBlock("PROJECT_SAMPLE_STATE", "最新样品状态不支持自动同步",
                    sampleStatus, "可识别样品状态", "核对样品状态后重试");
        };
        if (targetStatus.equals(project.getStatus()) && sampleStatus.equals(project.getSampleStatus())) {
            throw BusinessException.ruleBlock("PROJECT_SAMPLE_STATE", "项目状态与最新样品已经一致，无需同步",
                    project.getStatus(), "inconsistent", "刷新项目后继续正常流转");
        }
        project.setSampleStatus(sampleStatus);
        transition(project, "RECONCILE_SAMPLE_STATE", targetStatus, "SAMPLE", reason + "；以最新样品状态 " + sampleStatus + " 同步");
    }

    // 校验项目是否满足完成条件。
    public void assertCompletionReady(Project project) {
        Integer deliveryCount = jdbc.queryForObject("SELECT COUNT(*) FROM nso_delivery_record WHERE tenant_id=? AND project_id=? AND status='SHIPPED'", Integer.class,
                TenantContext.tenantId(), project.getId());
        if (deliveryCount == null || deliveryCount == 0) {
            throw BusinessException.ruleBlock("PROJECT_DELIVERY_RECORD", "项目完成前必须存在有效交付记录", "missing", "SHIPPED delivery", "先通过交付预检并创建交付记录");
        }
        Integer incompleteTaskCount = jdbc.queryForObject("SELECT COUNT(*) FROM nso_task WHERE tenant_id=? AND project_id=? AND deleted=0 AND status NOT IN ('DONE','COMPLETED','CANCELLED')", Integer.class,
                TenantContext.tenantId(), project.getId());
        if (incompleteTaskCount != null && incompleteTaskCount > 0) {
            throw BusinessException.ruleBlock("PROJECT_COMPLETION_TASKS", "仍有未取消任务未完成，不能完成项目",
                    String.valueOf(incompleteTaskCount), "0", "完成或取消剩余任务后重试");
        }
        Integer seriousRiskCount = jdbc.queryForObject("SELECT COUNT(*) FROM nso_risk WHERE tenant_id=? AND project_id=? AND deleted=0 AND status='OPEN' AND level='SERIOUS'", Integer.class,
                TenantContext.tenantId(), project.getId());
        if (seriousRiskCount != null && seriousRiskCount > 0) {
            throw BusinessException.ruleBlock("PROJECT_COMPLETION_RISK", "存在未关闭严重风险，不能完成项目", "SERIOUS", "无未关闭严重风险", "先关闭严重风险或按流程处理");
        }
    }

    // 执行项目状态与阶段迁移。
    public void transition(Project project, String action, String status, String stage, String reason) {
        String before = project.getStatus();
        project.setStatus(status);
        project.setStage(stage);
        ensureUpdated(project, "项目已被其他用户修改");
        jdbc.update("INSERT INTO nso_project_status_history (tenant_id,project_id,before_status,after_status,action_code,reason,operator_id) VALUES (?,?,?,?,?,?,?)",
                TenantContext.tenantId(), project.getId(), before, status, action, reason, TenantContext.userId());
        events.record(project.getId(), "PROJECT", project.getId(), "PROJECT_" + action, before, status, reason == null ? "项目状态已更新" : reason);
    }

    // 校验操作允许的项目状态。
    public void requireStatus(Project project, String action, String... allowed) {
        if (!Set.of(allowed).contains(project.getStatus())) {
            throw BusinessException.ruleBlock("PROJECT_STATUS", "项目当前状态不允许执行 " + action,
                    project.getStatus(), String.join("/", allowed), "刷新项目状态并执行下一合法动作");
        }
    }

    // 归档项目。
    public void archive(Project project, String reason) {
        if (!"COMPLETED".equals(project.getStatus())) {
            throw BusinessException.ruleBlock("PROJECT_ARCHIVE", "仅已完成项目可归档", project.getStatus(), "COMPLETED", "完成交付后归档");
        }
        project.setArchivedAt(LocalDateTime.now());
        project.setArchivedBy(TenantContext.userId());
        project.setArchiveReason(reason);
        transition(project, "ARCHIVE", "ARCHIVED", project.getStage(), reason);
        jdbc.update("INSERT INTO nso_project_archive (tenant_id,project_id,project_no,customer_name,archived_by,archive_reason,status) VALUES (?,?,?,?,?,?,'ARCHIVED') ON DUPLICATE KEY UPDATE archived_by=VALUES(archived_by),archive_reason=VALUES(archive_reason),archived_at=NOW(),status='ARCHIVED'",
                TenantContext.tenantId(), project.getId(), project.getProjectNo(), project.getCustomerName(), TenantContext.userId(), reason);
    }

    // 恢复已归档项目。
    public void restore(Project project, String reason) {
        if (!"ARCHIVED".equals(project.getStatus())) {
            throw BusinessException.ruleBlock("PROJECT_ARCHIVE", "项目当前未归档", project.getStatus(), "ARCHIVED", "刷新项目状态");
        }
        transition(project, "RESTORE", "COMPLETED", project.getStage(), reason);
        jdbc.update("UPDATE nso_project_archive SET status='RESTORED',restored_by=?,restored_at=NOW(),restore_reason=? WHERE tenant_id=? AND project_id=?",
                TenantContext.userId(), reason, TenantContext.tenantId(), project.getId());
    }

    // 校验并返回必填原因。
    public String requiredReason(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("该状态动作必须填写原因");
        }
        return reason.trim();
    }

    // 消费项目敏感操作确认凭证。
    public void requireConfirmation(ProjectActionRequest request, String operation, Long projectId) {
        if (request == null || request.confirmationId() == null) {
            throw new BusinessException("该高风险操作必须先完成二次确认");
        }
        confirmations.consume(request.confirmationId(), operation, "PROJECT", projectId);
    }

    // 校验项目乐观锁更新结果。
    public void ensureUpdated(Project project, String message) {
        if (projects.updateById(project) != 1) {
            throw BusinessException.ruleBlock("OPTIMISTIC_LOCK", message, "stale", "latest", "刷新后重试");
        }
    }
}
