package com.nso.business.change.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nso.business.change.domain.ChangeImpact;
import com.nso.business.change.domain.ChangeOrder;
import com.nso.business.change.mapper.ChangeImpactMapper;
import com.nso.business.core.NsoDtos.ApprovalInstanceDto;
import com.nso.business.core.NsoDtos.ApprovalTodoDto;
import com.nso.business.core.TenantContext;
import com.nso.business.document.domain.Bom;
import com.nso.business.document.domain.DocumentVersion;
import com.nso.business.document.domain.InspectionSpec;
import com.nso.business.document.mapper.BomMapper;
import com.nso.business.document.mapper.DocumentVersionMapper;
import com.nso.business.document.mapper.InspectionSpecMapper;
import com.nso.business.sample.domain.Sample;
import com.nso.business.sample.mapper.SampleMapper;
import com.nso.business.task.domain.Task;
import com.nso.business.task.mapper.TaskMapper;
import com.nso.business.support.approval.IApprovalService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

// 变更影响分析的领域协作组件。 只负责从项目事实生成影响项、审批节点和任务阻断，不承担变更状态迁移与权限判断。
@Component

// 变更影响分析协调器 协调变更影响的自动分析与分配
public class ChangeImpactAnalysisCoordinator {
    // 变更影响数据映射
    private final ChangeImpactMapper impacts;
    // 任务数据映射
    private final TaskMapper tasks;
    // 文档版本数据映射
    private final DocumentVersionMapper documents;
    // BOM数据映射
    private final BomMapper boms;
    // 检验Spec数据映射
    private final InspectionSpecMapper inspections;
    // 样品数据映射
    private final SampleMapper samples;
    // JDBC模板
    private final JdbcTemplate jdbc;
    // 审批服务
    private final IApprovalService approvals;

    public ChangeImpactAnalysisCoordinator(
            ChangeImpactMapper impacts,
            TaskMapper tasks,
            DocumentVersionMapper documents,
            BomMapper boms,
            InspectionSpecMapper inspections,
            SampleMapper samples,
            JdbcTemplate jdbc,
            IApprovalService approvals) {
        this.impacts = impacts;
        this.tasks = tasks;
        this.documents = documents;
        this.boms = boms;
        this.inspections = inspections;
        this.samples = samples;
        this.jdbc = jdbc;
        this.approvals = approvals;
    }

    // 生成变更影响项。
    public void generate(ChangeOrder change) {
        documents.selectList(Wrappers.<DocumentVersion>lambdaQuery()
                        .eq(DocumentVersion::getProjectId, change.getProjectId())
                        .eq(DocumentVersion::getCurrentVersion, 1))
                .forEach(row -> addImpact(
                        change,
                        "DOCUMENT",
                        row.getId(),
                        row.getVersionNo(),
                        row.getFileName(),
                        "TECHNICAL",
                        "发布更正版本并重新确认",
                        null));
        boms.selectList(Wrappers.<Bom>lambdaQuery()
                        .eq(Bom::getProjectId, change.getProjectId())
                        .ne(Bom::getStatus, "VOID"))
                .forEach(row -> addImpact(
                        change,
                        "BOM",
                        row.getId(),
                        row.getVersionNo(),
                        row.getBomNo(),
                        "TECHNICAL",
                        "核对受影响物料并更新 BOM",
                        null));
        inspections.selectList(Wrappers.<InspectionSpec>lambdaQuery()
                        .eq(InspectionSpec::getProjectId, change.getProjectId())
                        .ne(InspectionSpec::getStatus, "VOID"))
                .forEach(row -> addImpact(
                        change,
                        "INSPECTION",
                        row.getId(),
                        row.getVersionNo(),
                        row.getSpecNo(),
                        "QUALITY",
                        "更新检验标准并确认执行",
                        null));
        samples.selectList(Wrappers.<Sample>lambdaQuery()
                        .eq(Sample::getProjectId, change.getProjectId()))
                .forEach(row -> addImpact(
                        change,
                        "SAMPLE",
                        row.getId(),
                        row.getReferencedVersion(),
                        row.getSampleNo(),
                        "QUALITY",
                        "评估是否重新打样或复检",
                        row.getResponsibleName()));
        tasks.selectList(Wrappers.<Task>lambdaQuery()
                        .eq(Task::getProjectId, change.getProjectId())
                        .notIn(Task::getStatus, List.of("DONE", "CANCELLED")))
                .forEach(row -> {
                    String type = "PURCHASE".equalsIgnoreCase(row.getTaskType())
                            ? "PROCUREMENT"
                            : "PRODUCTION".equalsIgnoreCase(row.getTaskType()) ? "PRODUCTION" : null;
                    if (type != null) {
                        addImpact(
                                change,
                                type,
                                row.getId(),
                                row.getReferencedVersion(),
                                row.getTaskNo() + " " + row.getTitle(),
                                type,
                                "核对生效版本并反馈执行计划",
                                row.getResponsibleName());
                    }
                });
    }

    // 初始化变更审批节点。
    public void seedApprovals(ChangeOrder change) {
        Set<String> nodes = new LinkedHashSet<>(List.of("PROJECT_MANAGER", "TECHNICAL"));
        boolean production = impacts.selectCount(Wrappers.<ChangeImpact>lambdaQuery()
                .eq(ChangeImpact::getChangeId, change.getId())
                .eq(ChangeImpact::getObjectType, "PRODUCTION")) > 0;
        boolean procurement = impacts.selectCount(Wrappers.<ChangeImpact>lambdaQuery()
                .eq(ChangeImpact::getChangeId, change.getId())
                .eq(ChangeImpact::getObjectType, "PROCUREMENT")) > 0;
        if (production || isHighRisk(change)) {
            nodes.addAll(List.of("PROCESS", "PRODUCTION", "QUALITY"));
        }
        if (procurement) {
            nodes.add("PURCHASER");
        }
        ApprovalInstanceDto instance = approvals.createInstance("CHANGE", change.getId(), change.getProjectId(), nodes);
        for (ApprovalTodoDto todo : instance.todos()) {
            jdbc.update(
                    "INSERT IGNORE INTO nso_change_approval (tenant_id,change_id,node_code,decision) VALUES (?,?,?,'PENDING')",
                    TenantContext.tenantId(),
                    change.getId(),
                    todo.responsibilityCode());
        }
    }

    // 阻塞受变更影响的执行任务。
    public void blockAffectedTasks(Long changeId) {
        List<ChangeImpact> affectedImpacts = impacts.selectList(
                Wrappers.<ChangeImpact>lambdaQuery()
                        .eq(ChangeImpact::getChangeId, changeId)
                        .in(ChangeImpact::getObjectType, List.of("PROCUREMENT", "PRODUCTION")));
        for (ChangeImpact impact : affectedImpacts) {
            if (impact.getObjectId() == null) {
                continue;
            }
            Task task = tasks.selectById(impact.getObjectId());
            if (task != null && !Set.of("DONE", "CANCELLED").contains(task.getStatus())) {
                task.setStatus("BLOCKED");
                task.setBlockReason("变更 " + changeId + " 已批准，待确认新版本");
                tasks.updateById(task);
            }
        }
    }

    private void addImpact(
            ChangeOrder change,
            String type,
            Long objectId,
            String version,
            String name,
            String department,
            String action,
            String responsible) {
        ChangeImpact row = new ChangeImpact();
        row.setTenantId(TenantContext.tenantId());
        row.setChangeId(change.getId());
        row.setObjectType(type);
        row.setObjectId(objectId);
        row.setObjectVersion(version);
        row.setObjectName(name);
        row.setDepartmentName(department);
        row.setSuggestedAction(action);
        row.setStatus("PENDING_FEEDBACK");
        row.setResponsibleName(responsible);
        row.setDelayDays(0);
        row.setReworkQty(BigDecimal.ZERO);
        row.setVerifiedFlag(0);
        impacts.insert(row);
    }

    private boolean isHighRisk(ChangeOrder change) {
        return Set.of("HIGH", "URGENT", "CRITICAL").contains(change.getUrgency());
    }
}
