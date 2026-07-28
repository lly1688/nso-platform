package com.nso.business.change.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nso.business.change.domain.ChangeImpact;
import com.nso.business.change.domain.ChangeOrder;
import com.nso.business.change.mapper.ChangeImpactMapper;
import com.nso.business.change.mapper.ChangeOrderMapper;
import com.nso.business.core.BusinessLockPort;
import com.nso.business.core.NsoDtos.*;
import com.nso.business.core.TenantContext;
import com.nso.business.document.domain.Bom;
import com.nso.business.document.domain.DocumentVersion;
import com.nso.business.document.domain.InspectionSpec;
import com.nso.business.document.mapper.BomMapper;
import com.nso.business.document.mapper.DocumentVersionMapper;
import com.nso.business.document.mapper.InspectionSpecMapper;
import com.nso.business.message.service.IMessageService;
import com.nso.business.project.domain.Project;
import com.nso.business.project.mapper.ProjectMapper;
import com.nso.business.project.service.ProjectDataScope;
import com.nso.business.sample.domain.Sample;
import com.nso.business.sample.mapper.SampleMapper;
import com.nso.business.support.BusinessEventService;
import com.nso.business.task.domain.Task;
import com.nso.business.task.mapper.TaskMapper;
import com.nso.business.change.service.IChangeService;
import com.nso.common.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ChangeServiceImpl implements IChangeService {
    private final ChangeOrderMapper changes;
    private final ChangeImpactMapper impacts;
    private final ProjectMapper projects;
    private final TaskMapper tasks;
    private final DocumentVersionMapper documents;
    private final BomMapper boms;
    private final InspectionSpecMapper inspections;
    private final SampleMapper samples;
    private final IMessageService messages;
    private final BusinessEventService events;
    private final JdbcTemplate jdbc;
    private final BusinessLockPort locks;
    private final ProjectDataScope dataScope;

    public ChangeServiceImpl(ChangeOrderMapper changes, ChangeImpactMapper impacts, ProjectMapper projects, TaskMapper tasks,
                             DocumentVersionMapper documents, BomMapper boms, InspectionSpecMapper inspections, SampleMapper samples,
                             IMessageService messages, BusinessEventService events, JdbcTemplate jdbc, BusinessLockPort locks, ProjectDataScope dataScope) {
        this.changes = changes; this.impacts = impacts; this.projects = projects; this.tasks = tasks; this.documents = documents;
        this.boms = boms; this.inspections = inspections; this.samples = samples; this.messages = messages; this.events = events;
        this.jdbc = jdbc; this.locks = locks;
        this.dataScope = dataScope;
    }

    @Override public PageResult<ChangeOrderDto> list(Long projectId) {
        List<Long> visibleIds = dataScope.visibleProjectIds();
        if (visibleIds != null && visibleIds.isEmpty()) return new PageResult<>(List.of(), 0);
        List<ChangeOrderDto> rows = changes.selectList(Wrappers.<ChangeOrder>lambdaQuery().eq(projectId != null, ChangeOrder::getProjectId, projectId)
                .in(visibleIds != null, ChangeOrder::getProjectId, visibleIds == null ? List.of() : visibleIds).orderByDesc(ChangeOrder::getId)).stream().map(this::toDto).toList();
        return new PageResult<>(rows, rows.size());
    }

    @Override public ChangeOrderDto get(Long changeId) { return toDto(requireChange(changeId)); }

    @Override @Transactional public ChangeOrderDto create(ChangeRequest request) {
        if (request == null || request.projectId() == null || blank(request.changeType()) || blank(request.afterContent())) throw new BusinessException("项目、变更类型和变更后内容不能为空");
        Project project = requireProject(request.projectId());
        dataScope.requireProjectRole(project.getId(), "PROJECT_MANAGER", "TECHNICAL", "PROCESS", "PURCHASER", "PRODUCTION");
        if ("CLOSED".equals(project.getStatus()) || "COMPLETED".equals(project.getStatus())) throw BusinessException.ruleBlock("PROJECT_STATUS", "已关闭项目不能发起变更", project.getStatus(), "进行中", "新建补充项目或恢复后处理");
        ChangeOrder row = new ChangeOrder();
        row.setTenantId(TenantContext.tenantId()); row.setProjectId(project.getId()); row.setApplicantUserId(TenantContext.userId()); row.setChangeNo("ECN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        row.setChangeType(request.changeType()); row.setUrgency(blank(request.urgency()) ? "NORMAL" : request.urgency());
        row.setBeforeContent(request.beforeContent()); row.setAfterContent(request.afterContent()); row.setReason(request.reason());
        row.setStatus("WAIT_IMPACT"); row.setDelayDays(0); row.setReworkQty(0); changes.insert(row);
        events.record(project.getId(), "CHANGE", row.getId(), "CHANGE_CREATED", null, row.getStatus(), "已发起变更 " + row.getChangeNo());
        return toDto(row);
    }

    @Override @Transactional public List<ChangeImpactDto> analyze(Long changeId) {
        ChangeOrder change = requireChange(changeId);
        dataScope.requireProjectRole(change.getProjectId(), "TECHNICAL", "PROCESS");
        if (!Set.of("WAIT_IMPACT", "ANALYZED").contains(change.getStatus())) throw BusinessException.ruleBlock("CHANGE_STATUS", "当前变更不能执行影响分析", change.getStatus(), "WAIT_IMPACT/ANALYZED", "刷新状态后重试");
        if (impacts.selectCount(Wrappers.<ChangeImpact>lambdaQuery().eq(ChangeImpact::getChangeId, changeId)) == 0) generateImpacts(change);
        if (impacts.selectCount(Wrappers.<ChangeImpact>lambdaQuery().eq(ChangeImpact::getChangeId, changeId)) == 0) throw BusinessException.ruleBlock("CHANGE_IMPACT", "未识别到可执行影响项", "0", ">0", "补充关联技术或任务后重新分析");
        seedApprovals(change);
        String before = change.getStatus(); change.setStatus("WAIT_APPROVAL"); update(change);
        events.record(change.getProjectId(), "CHANGE", changeId, "CHANGE_ANALYZED", before, change.getStatus(), "已生成图纸、BOM、采购、生产、样品和检验影响项");
        return impacts(changeId);
    }

    @Override public List<ChangeImpactDto> impacts(Long changeId) {
        requireChange(changeId);
        return impacts.selectList(Wrappers.<ChangeImpact>lambdaQuery().eq(ChangeImpact::getChangeId, changeId).orderByAsc(ChangeImpact::getId)).stream().map(this::toDto).toList();
    }

    @Override public ChangeOrderDto approve(Long changeId) { return approve(changeId, new ChangeApprovalRequest("APPROVED", null)); }

    @Override @Transactional public ChangeOrderDto approve(Long changeId, ChangeApprovalRequest request) {
        return locks.withLock("change:approve:" + changeId, () -> {
            ChangeOrder change = requireChange(changeId);
            if (!"WAIT_APPROVAL".equals(change.getStatus())) throw BusinessException.ruleBlock("CHANGE_STATUS", "必须先完成影响分析才能审批", change.getStatus(), "WAIT_APPROVAL", "先执行影响分析");
            String decision = request == null || blank(request.decision()) ? "APPROVED" : request.decision().toUpperCase();
            if (!Set.of("APPROVED", "REJECTED").contains(decision)) throw new BusinessException("审批结论只能是 APPROVED 或 REJECTED");
            if (isHighRisk(change) && change.getApplicantUserId() != null && change.getApplicantUserId().equals(TenantContext.userId())) {
                throw BusinessException.ruleBlock("CHANGE_DUTY_SEPARATION", "高风险变更申请人不能参与本变更审批", String.valueOf(TenantContext.userId()), "独立审批人", "由其他审批节点处理");
            }
            String node = resolveApprovalNode(changeId);
            dataScope.requireProjectRole(change.getProjectId(), node);
            if ("REJECTED".equals(decision)) {
                jdbc.update("UPDATE nso_change_approval SET decision='REJECTED', opinion=?, approver_id=?, approver_name=?, approved_at=NOW() WHERE tenant_id=? AND change_id=? AND node_code=? AND decision='PENDING'",
                        request == null ? null : request.opinion(), TenantContext.userId(), TenantContext.username(), TenantContext.tenantId(), changeId, node);
                change.setStatus("DRAFT"); update(change);
                events.record(change.getProjectId(), "CHANGE", changeId, "CHANGE_REJECTED", "WAIT_APPROVAL", "DRAFT", "变更被 " + node + " 驳回");
                return toDto(change);
            }
            if (TenantContext.hasAnyRole("admin")) {
                jdbc.update("UPDATE nso_change_approval SET decision='APPROVED', opinion=?, approver_id=?, approver_name=?, approved_at=NOW() WHERE tenant_id=? AND change_id=? AND decision='PENDING'",
                        request == null ? null : request.opinion(), TenantContext.userId(), TenantContext.username(), TenantContext.tenantId(), changeId);
            } else {
                jdbc.update("UPDATE nso_change_approval SET decision='APPROVED', opinion=?, approver_id=?, approver_name=?, approved_at=NOW() WHERE tenant_id=? AND change_id=? AND node_code=? AND decision='PENDING'",
                        request == null ? null : request.opinion(), TenantContext.userId(), TenantContext.username(), TenantContext.tenantId(), changeId, node);
            }
            Integer pending = jdbc.queryForObject("SELECT COUNT(*) FROM nso_change_approval WHERE tenant_id=? AND change_id=? AND decision='PENDING'", Integer.class, TenantContext.tenantId(), changeId);
            if (pending != null && pending > 0) return toDto(change);
            String before = change.getStatus(); change.setStatus("EXECUTING"); change.setApprovalNode("ALL_APPROVED"); update(change);
            blockAffectedTasks(changeId);
            messages.notifyOnce("变更已批准", change.getChangeNo() + " 已批准，请处理影响项", "CHANGE_APPROVED", "CHANGE", changeId);
            events.record(change.getProjectId(), "CHANGE", changeId, "CHANGE_APPROVED", before, change.getStatus(), "变更审批完成并已通知责任人");
            return toDto(change);
        });
    }

    @Override @Transactional public ChangeImpactDto feedbackImpact(Long impactId, ChangeFeedbackRequest request) {
        ChangeImpact impact = impacts.selectById(impactId);
        if (impact == null) throw new BusinessException("变更影响项不存在");
        ChangeOrder change = requireChange(impact.getChangeId());
        dataScope.requireProjectRole(change.getProjectId(), projectRoleForImpact(impact.getDepartmentName()));
        if (!"EXECUTING".equals(change.getStatus())) throw BusinessException.ruleBlock("CHANGE_STATUS", "变更未批准，不能提交执行反馈", change.getStatus(), "EXECUTING", "完成审批后再反馈");
        if (request == null || blank(request.result())) throw new BusinessException("执行反馈不能为空");
        impact.setFeedbackResult(request.result()); impact.setFeedbackPlan(request.plan()); impact.setDelayDays(request.delayDays() == null ? 0 : request.delayDays());
        impact.setReworkQty(BigDecimal.valueOf(request.reworkQty() == null ? 0 : request.reworkQty())); impact.setResponsibleName(request.responsibleName()); impact.setStatus("DONE"); impact.setVerifiedFlag(1);
        if (request.version() != null && !request.version().equals(impact.getVersion())) throw BusinessException.ruleBlock("OPTIMISTIC_LOCK", "影响项已被其他用户更新", String.valueOf(request.version()), String.valueOf(impact.getVersion()), "刷新影响项后重新填写反馈");
        if (impacts.updateById(impact) != 1) throw BusinessException.ruleBlock("OPTIMISTIC_LOCK", "影响项已被其他用户更新", "stale", "latest", "刷新影响项后重新填写反馈");
        events.record(change.getProjectId(), "CHANGE_IMPACT", impactId, "CHANGE_IMPACT_FEEDBACK", "PENDING_FEEDBACK", "DONE", impact.getObjectType() + " 影响项已反馈");
        return toDto(impact);
    }

    @Override @Transactional public ChangeOrderDto feedbackChange(Long changeId, ChangeFeedbackRequest request) {
        ChangeOrder change = requireChange(changeId);
        if (request == null || blank(request.result())) throw new BusinessException("执行反馈不能为空");
        for (ChangeImpact impact : impacts.selectList(Wrappers.<ChangeImpact>lambdaQuery().eq(ChangeImpact::getChangeId, changeId).eq(ChangeImpact::getStatus, "PENDING_FEEDBACK"))) feedbackImpact(impact.getId(), new ChangeFeedbackRequest(impact.getId(), request.result(), request.plan(), request.delayDays(), request.reworkQty(), request.responsibleName(), impact.getVersion()));
        return toDto(change);
    }

    @Override @Transactional public ChangeOrderDto close(Long changeId) {
        ChangeOrder change = requireChange(changeId);
        if (!"EXECUTING".equals(change.getStatus())) throw BusinessException.ruleBlock("CHANGE_STATUS", "只有执行中的变更可以关闭", change.getStatus(), "EXECUTING", "先完成审批");
        List<ChangeImpact> rows = impacts.selectList(Wrappers.<ChangeImpact>lambdaQuery().eq(ChangeImpact::getChangeId, changeId));
        long unfinished = rows.stream().filter(row -> !"DONE".equals(row.getStatus())).count();
        if (unfinished > 0) throw BusinessException.ruleBlock("CHANGE_IMPACT_INCOMPLETE", "仍有未完成影响项", String.valueOf(unfinished), "0", "完成全部影响项反馈");
        int delay = rows.stream().map(ChangeImpact::getDelayDays).filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).sum();
        int rework = rows.stream().map(ChangeImpact::getReworkQty).filter(java.util.Objects::nonNull).mapToInt(BigDecimal::intValue).sum();
        for (ChangeImpact item : rows) jdbc.update("INSERT INTO nso_delay_rework (tenant_id,project_id,change_id,reason_type,delay_days,rework_qty,responsibility_stage) VALUES (?,?,?,?,?,?,?)",
                TenantContext.tenantId(), change.getProjectId(), changeId, change.getChangeType(), item.getDelayDays() == null ? 0 : item.getDelayDays(), item.getReworkQty() == null ? BigDecimal.ZERO : item.getReworkQty(), item.getDepartmentName());
        change.setDelayDays(delay); change.setReworkQty(rework); String before = change.getStatus(); change.setStatus("CLOSED"); update(change);
        events.record(change.getProjectId(), "CHANGE", changeId, "CHANGE_CLOSED", before, "CLOSED", "变更关闭，累计延期 " + delay + " 天、返工 " + rework + " 件");
        return toDto(change);
    }

    private void generateImpacts(ChangeOrder change) {
        long tenant = TenantContext.tenantId();
        documents.selectList(Wrappers.<DocumentVersion>lambdaQuery().eq(DocumentVersion::getProjectId, change.getProjectId()).eq(DocumentVersion::getCurrentVersion, 1)).forEach(row -> addImpact(change, "DOCUMENT", row.getId(), row.getVersionNo(), row.getFileName(), "TECHNICAL", "发布更正版本并重新确认", null));
        boms.selectList(Wrappers.<Bom>lambdaQuery().eq(Bom::getProjectId, change.getProjectId()).ne(Bom::getStatus, "VOID")).forEach(row -> addImpact(change, "BOM", row.getId(), row.getVersionNo(), row.getBomNo(), "TECHNICAL", "核对受影响物料并更新 BOM", null));
        inspections.selectList(Wrappers.<InspectionSpec>lambdaQuery().eq(InspectionSpec::getProjectId, change.getProjectId()).ne(InspectionSpec::getStatus, "VOID")).forEach(row -> addImpact(change, "INSPECTION", row.getId(), row.getVersionNo(), row.getSpecNo(), "QUALITY", "更新检验标准并确认执行", null));
        samples.selectList(Wrappers.<Sample>lambdaQuery().eq(Sample::getProjectId, change.getProjectId())).forEach(row -> addImpact(change, "SAMPLE", row.getId(), row.getReferencedVersion(), row.getSampleNo(), "QUALITY", "评估是否重新打样或复检", row.getResponsibleName()));
        tasks.selectList(Wrappers.<Task>lambdaQuery().eq(Task::getProjectId, change.getProjectId()).notIn(Task::getStatus, List.of("DONE", "CANCELLED"))).forEach(row -> {
            String type = "PURCHASE".equalsIgnoreCase(row.getTaskType()) ? "PROCUREMENT" : "PRODUCTION".equalsIgnoreCase(row.getTaskType()) ? "PRODUCTION" : null;
            if (type != null) addImpact(change, type, row.getId(), row.getReferencedVersion(), row.getTaskNo() + " " + row.getTitle(), type, "核对生效版本并反馈执行计划", row.getResponsibleName());
        });
        // A technical change with no operational task still has explicit document/BOM/inspection entries above.
    }

    private void addImpact(ChangeOrder change, String type, Long objectId, String version, String name, String department, String action, String responsible) {
        ChangeImpact row = new ChangeImpact(); row.setTenantId(TenantContext.tenantId()); row.setChangeId(change.getId()); row.setObjectType(type); row.setObjectId(objectId); row.setObjectVersion(version);
        row.setObjectName(name); row.setDepartmentName(department); row.setSuggestedAction(action); row.setStatus("PENDING_FEEDBACK"); row.setResponsibleName(responsible);
        row.setDelayDays(0); row.setReworkQty(BigDecimal.ZERO); row.setVerifiedFlag(0); impacts.insert(row);
    }

    private void seedApprovals(ChangeOrder change) {
        Set<String> nodes = new LinkedHashSet<>(List.of("PROJECT_MANAGER", "TECHNICAL"));
        boolean production = impacts.selectCount(Wrappers.<ChangeImpact>lambdaQuery().eq(ChangeImpact::getChangeId, change.getId()).eq(ChangeImpact::getObjectType, "PRODUCTION")) > 0;
        boolean highRisk = isHighRisk(change);
        if (production || highRisk) nodes.addAll(List.of("PRODUCTION", "QUALITY"));
        for (String node : nodes) jdbc.update("INSERT IGNORE INTO nso_change_approval (tenant_id,change_id,node_code,decision) VALUES (?,?,?,'PENDING')", TenantContext.tenantId(), change.getId(), node);
    }

    private String resolveApprovalNode(Long changeId) {
        if (TenantContext.hasAnyRole("admin")) return "ADMIN";
        String node = TenantContext.hasAnyRole("project_manager") ? "PROJECT_MANAGER" : TenantContext.hasAnyRole("technical", "designer") ? "TECHNICAL" : TenantContext.hasAnyRole("production") ? "PRODUCTION" : TenantContext.hasAnyRole("quality") ? "QUALITY" : null;
        if (node == null) throw BusinessException.ruleBlock("CHANGE_APPROVAL_PERMISSION", "当前角色无变更审批权限", String.join(",", TenantContext.roles()), "项目/技术/生产/质量审批人", "联系项目经理处理");
        Integer pending = jdbc.queryForObject("SELECT COUNT(*) FROM nso_change_approval WHERE tenant_id=? AND change_id=? AND node_code=? AND decision='PENDING'", Integer.class, TenantContext.tenantId(), changeId, node);
        if (pending == null || pending == 0) throw BusinessException.ruleBlock("CHANGE_APPROVAL_NODE", "当前审批节点无需处理", node, "待办节点", "刷新审批列表");
        return node;
    }

    private void blockAffectedTasks(Long changeId) {
        for (ChangeImpact impact : impacts.selectList(Wrappers.<ChangeImpact>lambdaQuery().eq(ChangeImpact::getChangeId, changeId).in(ChangeImpact::getObjectType, List.of("PROCUREMENT", "PRODUCTION")))) {
            if (impact.getObjectId() == null) continue;
            Task task = tasks.selectById(impact.getObjectId());
            if (task != null && !Set.of("DONE", "CANCELLED").contains(task.getStatus())) { task.setStatus("BLOCKED"); task.setBlockReason("变更 " + changeId + " 已批准，待确认新版本"); tasks.updateById(task); }
        }
    }

    private boolean isHighRisk(ChangeOrder change) { return Set.of("HIGH", "URGENT", "CRITICAL").contains(change.getUrgency()); }
    private String projectRoleForImpact(String department) { return switch (department == null ? "" : department.toUpperCase()) { case "PROCUREMENT", "PURCHASE" -> "PURCHASER"; case "PRODUCTION" -> "PRODUCTION"; case "QUALITY", "INSPECTION" -> "QUALITY"; case "PROCESS" -> "PROCESS"; default -> "TECHNICAL"; }; }
    private ChangeOrder requireChange(Long id) { ChangeOrder row = changes.selectById(id); if (row == null) throw new BusinessException("变更单不存在"); dataScope.requireAccess(row.getProjectId()); return row; }
    private Project requireProject(Long id) { Project row = projects.selectById(id); if (row == null) throw new BusinessException("项目不存在"); dataScope.requireAccess(id); return row; }
    private void update(ChangeOrder row) { if (changes.updateById(row) != 1) throw BusinessException.ruleBlock("OPTIMISTIC_LOCK", "变更单已被其他用户修改", "stale", "latest", "刷新后重试"); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private ChangeOrderDto toDto(ChangeOrder row) { int total = impacts.selectCount(Wrappers.<ChangeImpact>lambdaQuery().eq(ChangeImpact::getChangeId, row.getId())).intValue(); int completed = impacts.selectCount(Wrappers.<ChangeImpact>lambdaQuery().eq(ChangeImpact::getChangeId, row.getId()).eq(ChangeImpact::getStatus, "DONE")).intValue(); return new ChangeOrderDto(row.getId(), row.getProjectId(), requireProject(row.getProjectId()).getProjectNo(), row.getChangeNo(), row.getChangeType(), row.getUrgency(), row.getBeforeContent(), row.getAfterContent(), row.getReason(), row.getStatus(), row.getDelayDays(), row.getReworkQty(), total, completed); }
    private ChangeImpactDto toDto(ChangeImpact row) { return new ChangeImpactDto(row.getId(), row.getChangeId(), row.getObjectType(), row.getObjectName(), row.getDepartmentName(), row.getSuggestedAction(), row.getStatus(), row.getFeedbackResult(), row.getResponsibleName(), row.getVersion()); }
}
