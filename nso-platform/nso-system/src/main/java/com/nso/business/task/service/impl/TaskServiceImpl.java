package com.nso.business.task.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nso.business.core.BusinessLockPort;
import com.nso.business.core.NsoDtos.*;
import com.nso.business.core.TenantContext;
import com.nso.business.document.domain.DocumentVersion;
import com.nso.business.document.mapper.DocumentVersionMapper;
import com.nso.business.project.domain.Project;
import com.nso.business.project.domain.ProjectMember;
import com.nso.business.project.mapper.ProjectMapper;
import com.nso.business.project.service.ProjectDataScope;
import com.nso.business.risk.domain.Risk;
import com.nso.business.risk.mapper.RiskMapper;
import com.nso.business.sample.domain.Sample;
import com.nso.business.sample.mapper.SampleMapper;
import com.nso.business.support.BusinessEventService;
import com.nso.business.task.domain.DeliveryRecord;
import com.nso.business.task.domain.Task;
import com.nso.business.task.mapper.DeliveryRecordMapper;
import com.nso.business.task.mapper.TaskMapper;
import com.nso.business.task.service.ITaskService;
import com.nso.common.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TaskServiceImpl implements ITaskService {
    private final TaskMapper tasks; private final ProjectMapper projects; private final DocumentVersionMapper documents;
    private final SampleMapper samples; private final RiskMapper risks; private final DeliveryRecordMapper deliveries;
    private final BusinessLockPort locks; private final BusinessEventService events; private final JdbcTemplate jdbc;
    private final ProjectDataScope dataScope;

    public TaskServiceImpl(TaskMapper tasks, ProjectMapper projects, DocumentVersionMapper documents, SampleMapper samples, RiskMapper risks,
                           DeliveryRecordMapper deliveries, BusinessLockPort locks, BusinessEventService events, JdbcTemplate jdbc, ProjectDataScope dataScope) {
        this.tasks = tasks; this.projects = projects; this.documents = documents; this.samples = samples; this.risks = risks; this.deliveries = deliveries;
        this.locks = locks; this.events = events; this.jdbc = jdbc;
        this.dataScope = dataScope;
    }

    @Override public PageResult<TaskDto> list(Long projectId) { List<Long> visibleIds = dataScope.visibleProjectIds(); if (visibleIds != null && visibleIds.isEmpty()) return new PageResult<>(List.of(), 0); List<TaskDto> rows = tasks.selectList(Wrappers.<Task>lambdaQuery().eq(projectId != null, Task::getProjectId, projectId).in(visibleIds != null, Task::getProjectId, visibleIds == null ? List.of() : visibleIds).orderByDesc(Task::getId)).stream().map(this::toDto).toList(); return new PageResult<>(rows, rows.size()); }
    @Override public TaskDto get(Long taskId) { return toDto(requireTask(taskId)); }

    @Override @Transactional public TaskDto create(ExecutionTaskRequest request) {
        if (request == null || request.projectId() == null || blank(request.taskType()) || blank(request.title())) throw new BusinessException("项目、任务类型和任务名称不能为空");
        Project project = requireProject(request.projectId());
        dataScope.requireProjectRole(project.getId(), "PROJECT_MANAGER", "PRODUCTION");
        if (request.assigneeId() == null) throw new BusinessException("任务必须指定责任人");
        ProjectMember assignee = dataScope.requireActiveMember(project.getId(), request.assigneeId());
        Task task = new Task(); task.setTenantId(TenantContext.tenantId()); task.setProjectId(project.getId()); task.setTaskNo("TSK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        task.setTaskType(request.taskType().toUpperCase()); task.setTitle(request.title()); task.setReferencedVersion(request.referencedVersion());
        DocumentVersion version = resolveVersion(project, request.referencedVersion()); task.setReferencedDocVersionId(version == null ? null : version.getId());
        task.setStatus("TODO"); task.setAssigneeId(assignee.getUserId()); task.setResponsibleName(blank(request.responsibleName()) ? assignee.getMemberName() : request.responsibleName()); task.setPlanStart(request.planStart()); task.setPlanFinish(request.planFinish()); task.setVersion(0); tasks.insert(task);
        record(task, null, "TODO", "TASK_CREATED", "已创建协同任务"); return toDto(task);
    }

    @Override @Transactional public TaskDto start(Long taskId) { return start(taskId, null); }

    @Override @Transactional public TaskDto start(Long taskId, TaskActionRequest request) {
        return locks.withLock("task:start:" + taskId, () -> {
            Task task = requireTask(taskId);
            requireTaskExecutionAccess(task);
            assertExpectedVersion(task, request == null ? null : request.version());
            if (!Set.of("TODO", "BLOCKED", "PAUSED").contains(task.getStatus())) throw BusinessException.ruleBlock("TASK_STATUS", "当前任务不能开工", task.getStatus(), "TODO/BLOCKED/PAUSED", "刷新任务状态");
            Project project = requireProject(task.getProjectId());
            if ("PRODUCTION".equals(task.getTaskType())) assertProductionReady(task, project);
            else if (requiresVersionCheck(task)) assertReferencedVersionCurrent(task, project);
            String before = task.getStatus(); task.setStatus("IN_PROGRESS"); task.setActualStart(LocalDateTime.now()); task.setBlockReason(null); update(task); record(task, before, task.getStatus(), "TASK_STARTED", "任务已开工"); return toDto(task);
        });
    }

    @Override @Transactional public TaskDto pause(Long taskId, TaskActionRequest request) {
        Task task = requireTask(taskId);
        requireTaskExecutionAccess(task);
        assertExpectedVersion(task, request == null ? null : request.version());
        if (!"IN_PROGRESS".equals(task.getStatus())) throw BusinessException.ruleBlock("TASK_STATUS", "只有执行中的任务可以暂停", task.getStatus(), "IN_PROGRESS", "先开工或刷新任务");
        String before = task.getStatus(); task.setStatus("PAUSED"); task.setBlockReason(request == null ? null : request.notes()); update(task); record(task, before, task.getStatus(), "TASK_PAUSED", task.getBlockReason()); return toDto(task);
    }

    @Override @Transactional public TaskDto complete(Long taskId, TaskActionRequest request) { return feedback(taskId, new TaskFeedbackRequest("DONE", request == null ? null : request.notes(), request == null ? null : request.version())); }

    @Override @Transactional public TaskDto feedback(Long taskId, TaskFeedbackRequest request) {
        Task task = requireTask(taskId);
        requireTaskExecutionAccess(task);
        if (request == null || blank(request.result())) throw new BusinessException("任务反馈结果不能为空");
        assertExpectedVersion(task, request.version());
        if (!Set.of("IN_PROGRESS", "BLOCKED").contains(task.getStatus())) throw BusinessException.ruleBlock("TASK_STATUS", "任务尚未开工，不能反馈", task.getStatus(), "IN_PROGRESS/BLOCKED", "先开工");
        String before = task.getStatus();
        if ("DONE".equalsIgnoreCase(request.result())) {
            Project project = requireProject(task.getProjectId());
            if ("PRODUCTION".equals(task.getTaskType())) assertProductionReady(task, project);
            else if (requiresVersionCheck(task)) assertReferencedVersionCurrent(task, project);
            task.setStatus("DONE"); task.setActualFinish(LocalDateTime.now()); task.setBlockReason(request.notes());
        } else { task.setStatus("BLOCKED"); task.setBlockReason(request.notes()); }
        update(task); record(task, before, task.getStatus(), "TASK_FEEDBACK", request.notes()); return toDto(task);
    }

    @Override @Transactional public RiskDto reportException(ExceptionReportRequest request) {
        if (request == null || request.projectId() == null || blank(request.summary())) throw new BusinessException("项目和异常说明不能为空"); requireProject(request.projectId());
        if (request.taskId() != null) { Task task = requireTask(request.taskId()); if (!request.projectId().equals(task.getProjectId())) throw new BusinessException("异常任务不属于当前项目"); requireTaskExecutionAccess(task); }
        else dataScope.requireProjectRole(request.projectId(), "PURCHASER", "PRODUCTION", "QUALITY", "FIELD_USER");
        Risk risk = new Risk(); risk.setTenantId(TenantContext.tenantId()); risk.setProjectId(request.projectId()); risk.setLevel("HIGH"); risk.setScore(70); risk.setReasons(jsonReasons("现场异常:" + request.summary())); risk.setSuggestion("立即处理异常并更新关联任务"); risk.setStatus("OPEN"); risk.setRuleCode("EXECUTION_EXCEPTION"); risk.setRuleVersion("V1"); risk.setCalculatedAt(LocalDateTime.now()); risks.insert(risk);
        events.record(request.projectId(), "RISK", risk.getId(), "EXECUTION_EXCEPTION_REPORTED", null, "OPEN", request.summary()); return toRiskDto(risk);
    }

    @Override public PageResult<DeliveryRecordDto> deliveries(Long projectId) { List<Long> visibleIds = dataScope.visibleProjectIds(); if (visibleIds != null && visibleIds.isEmpty()) return new PageResult<>(List.of(), 0); List<DeliveryRecordDto> rows = deliveries.selectList(Wrappers.<DeliveryRecord>lambdaQuery().eq(projectId != null, DeliveryRecord::getProjectId, projectId).in(visibleIds != null, DeliveryRecord::getProjectId, visibleIds == null ? List.of() : visibleIds).orderByDesc(DeliveryRecord::getShippedAt)).stream().map(this::toDeliveryDto).toList(); return new PageResult<>(rows, rows.size()); }
    @Override @Transactional public DeliveryRecordDto createDelivery(DeliveryRequest request) { if (request == null || request.projectId() == null || request.quantity() == null || request.quantity() < 1) throw new BusinessException("项目和交付数量不能为空"); Project project = requireProject(request.projectId()); DeliveryRecord row = new DeliveryRecord(); row.setTenantId(TenantContext.tenantId()); row.setProjectId(project.getId()); row.setQuantity(request.quantity()); row.setLogisticsNo(request.logisticsNo()); row.setReceiver(request.receiver()); row.setCustomerFeedback(request.feedback()); row.setStatus("SHIPPED"); row.setShippedAt(LocalDateTime.now()); deliveries.insert(row); events.record(project.getId(), "DELIVERY", row.getId(), "DELIVERY_CREATED", null, "SHIPPED", "已创建交付记录"); return toDeliveryDto(row); }

    @Override @Transactional public TechnicalPackageSyncResultDto syncTechnicalPackageTasks(Long projectId) {
        return locks.withLock("project:technical-package-sync:" + projectId, () -> {
            Project project = requireProject(projectId);
            dataScope.requireProjectRole(projectId, "PROJECT_MANAGER", "TECHNICAL");
            if (project.getCurrentDocVersionId() == null) throw new BusinessException("当前项目没有生效的技术版本");
            DocumentVersion current = documents.selectById(project.getCurrentDocVersionId());
            if (current == null || !Long.valueOf(TenantContext.tenantId()).equals(current.getTenantId())) throw new BusinessException("当前生效技术版本不存在");
            List<Task> affected = tasks.selectList(Wrappers.<Task>lambdaQuery()
                .eq(Task::getProjectId, projectId)
                .notIn(Task::getStatus, List.of("DONE", "CANCELLED"))
                .and(query -> query.ne(Task::getReferencedVersion, current.getVersionNo())
                    .or().ne(Task::getReferencedDocVersionId, current.getId())));
            for (Task task : affected) {
                String previous = task.getReferencedVersion();
                task.setReferencedVersion(current.getVersionNo());
                task.setReferencedDocVersionId(current.getId());
                update(task);
                record(task, task.getStatus(), task.getStatus(), "TASK_VERSION_SYNCED",
                    "任务引用版本已从 " + (previous == null ? "未设置" : previous) + " 同步为 " + current.getVersionNo());
            }
            events.record(projectId, "TECHNICAL_PACKAGE", current.getId(), "TECHNICAL_PACKAGE_TASKS_SYNCED", null,
                current.getVersionNo(), "已同步 " + affected.size() + " 个未完成任务的技术版本");
            return new TechnicalPackageSyncResultDto(projectId, current.getId(), current.getVersionNo(), affected.size());
        });
    }

    private void assertProductionReady(Task task, Project project) {
        assertReferencedVersionCurrent(task, project);
        if (samples.selectCount(Wrappers.<Sample>lambdaQuery().eq(Sample::getProjectId, project.getId()).eq(Sample::getStatus, "CONFIRMED")) == 0) {
            Integer activeRelease = jdbc.queryForObject("SELECT COUNT(*) FROM nso_special_release WHERE tenant_id=? AND project_id=? AND status='APPROVED' AND valid_until > NOW()", Integer.class, TenantContext.tenantId(), project.getId());
            if (activeRelease == null || activeRelease == 0) throw BusinessException.ruleBlock("SAMPLE_NOT_CONFIRMED", "样品未完成客户确认，禁止投产", "unconfirmed", "confirmed or approved special release", "完成样品客户确认或走特殊放行流程");
        }
        if (risks.selectCount(Wrappers.<Risk>lambdaQuery().eq(Risk::getProjectId, project.getId()).eq(Risk::getStatus, "OPEN").eq(Risk::getLevel, "SERIOUS")) > 0) throw BusinessException.ruleBlock("SERIOUS_RISK_OPEN", "存在未关闭严重风险，禁止投产", "SERIOUS", "无严重风险", "先关闭风险或走特殊放行流程");
        if (tasks.selectCount(Wrappers.<Task>lambdaQuery().eq(Task::getProjectId, project.getId()).eq(Task::getTaskType, "PURCHASE").ne(Task::getStatus, "DONE")) > 0) throw BusinessException.ruleBlock("MATERIAL_NOT_READY", "关键采购任务尚未完成，禁止投产", "purchase pending", "purchase done", "确认到料后再投产");
    }

    private void assertReferencedVersionCurrent(Task task, Project project) {
        DocumentVersion current = project.getCurrentDocVersionId() == null ? null : documents.selectById(project.getCurrentDocVersionId());
        if (current == null || task.getReferencedVersion() == null || !task.getReferencedVersion().equals(current.getVersionNo())) throw BusinessException.ruleBlock("VERSION_MISMATCH", "任务引用的技术版本不是当前发布版本", task.getReferencedVersion(), current == null ? "none" : current.getVersionNo(), "更新任务引用版本");
    }

    private boolean requiresVersionCheck(Task task) {
        return Set.of("PURCHASE", "INSPECTION", "DELIVERY", "SAMPLE_PREPARE", "SAMPLE_MAKE", "SAMPLE_REWORK").contains(task.getTaskType());
    }

    private void record(Task task, String before, String after, String action, String summary) { jdbc.update("INSERT INTO nso_task_log (tenant_id,task_id,before_status,after_status,operation_type,summary,operator_name) VALUES (?,?,?,?,?,?,?)", TenantContext.tenantId(), task.getId(), before, after, action, summary, TenantContext.username()); events.record(task.getProjectId(), "TASK", task.getId(), action, before, after, summary); }
    private Task requireTask(Long id) { Task row = tasks.selectById(id); if (row == null) throw new BusinessException("任务不存在"); dataScope.requireAccess(row.getProjectId()); return row; }
    private void requireTaskExecutionAccess(Task task) { if (TenantContext.hasAnyRole("admin")) return; if (TenantContext.userId() == null || task.getAssigneeId() == null || !TenantContext.userId().equals(task.getAssigneeId())) throw BusinessException.accessDenied("TASK_ASSIGNEE_SCOPE", "仅任务责任人可以执行或反馈任务", String.valueOf(task.getId()), "任务责任人", "由责任人处理或重新分派任务"); String type = task.getTaskType() == null ? "" : task.getTaskType().toUpperCase(); boolean allowed = switch (type) { case "PURCHASE" -> TenantContext.hasAnyRole("purchaser"); case "PRODUCTION", "SAMPLE_PREPARE", "SAMPLE_MAKE", "SAMPLE_REWORK" -> TenantContext.hasAnyRole("production", "field_user"); case "QUALITY", "INSPECTION" -> TenantContext.hasAnyRole("quality"); case "TECHNICAL", "DESIGN" -> TenantContext.hasAnyRole("technical"); case "PROCESS" -> TenantContext.hasAnyRole("process"); default -> TenantContext.hasAnyRole("project_manager"); }; if (!allowed) throw BusinessException.accessDenied("TASK_ROLE_SCOPE", "当前岗位不能执行此类任务", type, "匹配任务岗位", "由对应岗位责任人执行"); }
    private Project requireProject(Long id) { Project row = projects.selectById(id); if (row == null) throw new BusinessException("项目不存在"); dataScope.requireAccess(id); return row; }
    private DocumentVersion resolveVersion(Project project, String versionNo) { if (blank(versionNo)) return project.getCurrentDocVersionId() == null ? null : documents.selectById(project.getCurrentDocVersionId()); return documents.selectOne(Wrappers.<DocumentVersion>lambdaQuery().eq(DocumentVersion::getProjectId, project.getId()).eq(DocumentVersion::getVersionNo, versionNo).last("LIMIT 1")); }
    private void update(Task task) { if (tasks.updateById(task) != 1) throw BusinessException.ruleBlock("OPTIMISTIC_LOCK", "任务已被其他用户修改", "stale", "latest", "刷新后重试"); }
    private void assertExpectedVersion(Task task, Integer requestedVersion) { if (requestedVersion != null && !requestedVersion.equals(task.getVersion())) throw BusinessException.ruleBlock("OPTIMISTIC_LOCK", "任务已被其他用户更新", String.valueOf(requestedVersion), String.valueOf(task.getVersion()), "刷新任务后重试"); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String jsonReasons(String reason) { return "[\"" + reason.replace("\\", "\\\\").replace("\"", "\\\"") + "\"]"; }
    private List<String> reasons(Risk row) { String raw = row.getReasons(); return raw == null || raw.length() < 2 ? List.of() : List.of(raw.substring(1, raw.length() - 1).replace("\"", "").split(",")); }
    private TaskDto toDto(Task row) { return new TaskDto(row.getId(), row.getProjectId(), requireProject(row.getProjectId()).getProjectNo(), row.getTaskNo(), row.getTaskType(), row.getTitle(), row.getReferencedVersion(), row.getStatus(), row.getResponsibleName(), row.getPlanStart(), row.getPlanFinish(), row.getBlockReason(), row.getVersion()); }
    private RiskDto toRiskDto(Risk row) { return new RiskDto(row.getId(), row.getProjectId(), requireProject(row.getProjectId()).getProjectNo(), row.getLevel(), row.getScore(), reasons(row), row.getSuggestion(), row.getStatus()); }
    private DeliveryRecordDto toDeliveryDto(DeliveryRecord row) { return new DeliveryRecordDto(row.getId(), row.getProjectId(), requireProject(row.getProjectId()).getProjectNo(), row.getQuantity(), row.getLogisticsNo(), row.getReceiver(), row.getCustomerFeedback(), row.getStatus(), row.getShippedAt()); }
}
