package com.nso.business.task.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nso.business.core.BusinessLockPort;
import com.nso.business.core.NsoDtos.*;
import com.nso.business.core.PageSupport;
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
import com.nso.business.support.BusinessNumberService;
import com.nso.business.support.capa.ICapaService;
import com.nso.business.task.domain.DeliveryRecord;
import com.nso.business.task.domain.Task;
import com.nso.business.task.mapper.DeliveryRecordMapper;
import com.nso.business.task.mapper.TaskMapper;
import com.nso.business.task.service.ITaskService;
import com.nso.business.task.service.TaskResponsibilityCatalog;
import com.nso.shared.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// 任务与交付服务。
@Service

// 任务管理 服务层处理
public class TaskServiceImpl implements ITaskService {
    // 任务数据映射
    private final TaskMapper tasks;
    // 项目数据映射
    private final ProjectMapper projects;
    // 文档版本数据映射
    private final DocumentVersionMapper documents;
    // 样品数据映射
    private final SampleMapper samples;
    // 风险数据映射
    private final RiskMapper risks;
    // 交付Record数据映射
    private final DeliveryRecordMapper deliveries;
    // 业务锁端口
    private final BusinessLockPort locks;
    // 业务事件服务
    private final BusinessEventService events;
    // JDBC模板
    private final JdbcTemplate jdbc;
    // 项目数据范围
    private final ProjectDataScope dataScope;
    // 业务Number服务
    private final BusinessNumberService numbers;
    // CAPA服务
    private final ICapaService capaService;

    public TaskServiceImpl(
            TaskMapper tasks,
            ProjectMapper projects,
            DocumentVersionMapper documents,
            SampleMapper samples,
            RiskMapper risks,
            DeliveryRecordMapper deliveries,
            BusinessLockPort locks,
            BusinessEventService events,
            JdbcTemplate jdbc,
            ProjectDataScope dataScope,
            BusinessNumberService numbers,
            ICapaService capaService) {
        this.tasks = tasks;
        this.projects = projects;
        this.documents = documents;
        this.samples = samples;
        this.risks = risks;
        this.deliveries = deliveries;
        this.locks = locks;
        this.events = events;
        this.jdbc = jdbc;
        this.dataScope = dataScope;
        this.numbers = numbers;
        this.capaService = capaService;
    }

    // 查询可见任务。
    @Override
    public PageResult<TaskDto> list(Long projectId) {
        List<Long> visibleIds = dataScope.visibleProjectIds();
        if (visibleIds != null && visibleIds.isEmpty()) {
            return new PageResult<>(List.of(), 0);
        }
        List<TaskDto> rows = tasks.selectList(Wrappers.<Task>lambdaQuery().eq(projectId != null, Task::getProjectId, projectId).in(visibleIds != null, Task::getProjectId, visibleIds == null ? List.of() : visibleIds).orderByDesc(Task::getId)).stream().map(this::toDto).toList();
        return new PageResult<>(rows, rows.size());
    }

    // 按条件分页查询项目任务。
    @Override
    public PageResult<TaskDto> list(Long projectId, String status, String taskType, PageQuery pageQuery) {
        PageQuery page = PageSupport.normalize(pageQuery);
        List<Long> visibleIds = dataScope.visibleProjectIds();
        if (visibleIds != null && visibleIds.isEmpty()) {
            return PageResult.empty(page);
        }
        Page<Task> entityPage = tasks.selectPage(PageSupport.page(page), Wrappers.<Task>lambdaQuery()
                .eq(projectId != null, Task::getProjectId, projectId)
                .eq(status != null && !status.isBlank(), Task::getStatus, status)
                .eq(taskType != null && !taskType.isBlank(), Task::getTaskType, taskType)
                .in(visibleIds != null, Task::getProjectId, visibleIds == null ? List.of() : visibleIds)
                .orderByDesc(Task::getId));
        return PageSupport.result(entityPage, page, this::toDto);
    }
    // 查询任务详情。
    @Override
    public TaskDto get(Long taskId) {
        return toDto(requireTask(taskId));
    }

    // 创建执行任务。
    @Override
    @Transactional
    public TaskDto create(ExecutionTaskRequest request) {
        if (request == null || request.projectId() == null || blank(request.taskType()) || blank(request.title())) throw new BusinessException("项目、任务类型和任务名称不能为空");

        Project project = requireProject(request.projectId());

        dataScope.requireProjectRole(project.getId(), "PROJECT_MANAGER", "PRODUCTION");

        if (request.assigneeId() == null) throw new BusinessException("任务必须指定责任人");

        String taskType = request.taskType().trim().toUpperCase();

        ProjectMember assignee = dataScope.requireActiveMemberForResponsibilities(project.getId(), request.assigneeId(),
                TaskResponsibilityCatalog.forTaskType(taskType).toArray(String[]::new));

        DocumentVersion currentVersion = currentPublishedVersion(project);

        if (requiresCurrentTechnicalVersion(taskType) && currentVersion == null) {
            throw BusinessException.ruleBlock("TASK_TECHNICAL_VERSION", "当前项目没有已发布的技术版本，不能创建此类任务",
                    "none", "current published version", "先发布技术包后再创建任务");
        }
        Task task = new Task();
        task.setTenantId(TenantContext.tenantId());
        task.setProjectId(project.getId());
        task.setTaskNo(numbers.next("TASK"));
        task.setTaskType(taskType);
        task.setTitle(request.title().trim());
        task.setReferencedVersion(currentVersion == null ? null : currentVersion.getVersionNo());
        task.setReferencedDocVersionId(currentVersion == null ? null : currentVersion.getId());
        task.setStatus("TODO");
        task.setAssigneeId(assignee.getUserId());
        task.setResponsibleName(assignee.getMemberName());
        task.setPlanStart(request.planStart());
        task.setPlanFinish(request.planFinish());
        task.setVersion(0);
        tasks.insert(task);
        record(task, null, "TODO", "TASK_CREATED", "已创建协同任务");
        return toDto(task);
    }

    // 使用默认操作启动任务。
    @Override
    @Transactional
    public TaskDto start(Long taskId) {
        return start(taskId, null);
    }

    // 根据操作内容启动任务。
    @Override
    @Transactional
    public TaskDto start(Long taskId, TaskActionRequest request) {
        return locks.withLock("task:start:" + taskId, () -> {
            Task task = requireTask(taskId);
            requireTaskExecutionAccess(task);
            assertExpectedVersion(task, request == null ? null : request.version());
            if (!Set.of("TODO", "BLOCKED", "PAUSED").contains(task.getStatus())) throw BusinessException.ruleBlock("TASK_STATUS", "当前任务不能开工", task.getStatus(), "TODO/BLOCKED/PAUSED", "刷新任务状态");
            Project project = requireProject(task.getProjectId());
            if ("PRODUCTION".equals(task.getTaskType())) assertProductionReady(task, project);
            else if (requiresVersionCheck(task)) assertReferencedVersionCurrent(task, project);
            String before = task.getStatus();
            task.setStatus("IN_PROGRESS");
            task.setActualStart(LocalDateTime.now());
            task.setBlockReason(null);
            update(task);
            record(task, before, task.getStatus(), "TASK_STARTED", "任务已开工");
            return toDto(task);
        });
    }

    // 暂停任务。
    @Override
    @Transactional
    public TaskDto pause(Long taskId, TaskActionRequest request) {
        Task task = requireTask(taskId);
        requireTaskExecutionAccess(task);
        assertExpectedVersion(task, request == null ? null : request.version());
        if (!"IN_PROGRESS".equals(task.getStatus())) throw BusinessException.ruleBlock("TASK_STATUS", "只有执行中的任务可以暂停", task.getStatus(), "IN_PROGRESS", "先开工或刷新任务");
        String before = task.getStatus();
        task.setStatus("PAUSED");
        task.setBlockReason(request == null ? null : request.notes());
        update(task);
        record(task, before, task.getStatus(), "TASK_PAUSED", task.getBlockReason());
        return toDto(task);
    }

    // 完成任务。
    @Override
    @Transactional
    public TaskDto complete(Long taskId, TaskActionRequest request) {
        return feedback(taskId, new TaskFeedbackRequest("DONE", request == null ? null : request.notes(), request == null ? null : request.version()));
    }

    // 提交任务反馈。
    @Override
    @Transactional
    public TaskDto feedback(Long taskId, TaskFeedbackRequest request) {
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
            task.setStatus("DONE");
            task.setActualFinish(LocalDateTime.now());
            task.setBlockReason(request.notes());
        } else {
            task.setStatus("BLOCKED");
            task.setBlockReason(request.notes());
        }
        update(task);
        record(task, before, task.getStatus(), "TASK_FEEDBACK", request.notes());
        return toDto(task);
    }

    // 上报执行异常。
    @Override
    @Transactional
    public RiskDto reportException(ExceptionReportRequest request) {
        return capaService.reportLegacy(request);
    }

    // 查询项目交付记录。
    @Override
    public PageResult<DeliveryRecordDto> deliveries(Long projectId) {
        List<Long> visibleIds = dataScope.visibleProjectIds();
        if (visibleIds != null && visibleIds.isEmpty()) {
            return new PageResult<>(List.of(), 0);
        }
        List<DeliveryRecordDto> rows = deliveries.selectList(
                        Wrappers.<DeliveryRecord>lambdaQuery()
                                .eq(projectId != null, DeliveryRecord::getProjectId, projectId)
                                .in(visibleIds != null,
                                        DeliveryRecord::getProjectId,
                                        visibleIds == null ? List.of() : visibleIds)
                                .orderByDesc(DeliveryRecord::getShippedAt))
                .stream()
                .map(this::toDeliveryDto)
                .toList();
        return new PageResult<>(rows, rows.size());
    }

    // 按状态分页查询项目交付记录。
    @Override
    public PageResult<DeliveryRecordDto> deliveries(Long projectId, String status, PageQuery pageQuery) {
        PageQuery page = PageSupport.normalize(pageQuery);
        List<Long> visibleIds = dataScope.visibleProjectIds();
        if (visibleIds != null && visibleIds.isEmpty()) {
            return PageResult.empty(page);
        }
        Page<DeliveryRecord> entityPage = deliveries.selectPage(PageSupport.page(page),
                Wrappers.<DeliveryRecord>lambdaQuery()
                        .eq(projectId != null, DeliveryRecord::getProjectId, projectId)
                        .eq(status != null && !status.isBlank(), DeliveryRecord::getStatus, status)
                        .in(visibleIds != null, DeliveryRecord::getProjectId,
                                visibleIds == null ? List.of() : visibleIds)
                        .orderByDesc(DeliveryRecord::getShippedAt));
        return PageSupport.result(entityPage, page, this::toDeliveryDto);
    }
    // 检查项目交付就绪状态。
    @Override
    public DeliveryReadinessDto deliveryReadiness(Long projectId) {
        Project project = requireProject(projectId);
        List<DeliveryBlockerDto> blockers = new ArrayList<>();
        if (!"EXECUTING".equals(project.getStatus())) {
            blockers.add(new DeliveryBlockerDto("DELIVERY_PROJECT_STATUS", "项目必须处于生产执行中，才能创建交付记录"));
        }
        DocumentVersion current = project.getCurrentDocVersionId() == null ? null : documents.selectById(project.getCurrentDocVersionId());
        if (current == null || !project.getId().equals(current.getProjectId()) || !"PUBLISHED".equals(current.getStatus())) {
            blockers.add(new DeliveryBlockerDto("DELIVERY_TECHNICAL_PACKAGE", "缺少当前有效的已发布技术包"));
        }
        if (samples.selectCount(Wrappers.<Sample>lambdaQuery().eq(Sample::getProjectId, project.getId()).eq(Sample::getStatus, "CONFIRMED")) == 0) {
            blockers.add(new DeliveryBlockerDto("DELIVERY_SAMPLE_CONFIRM", "没有客户确认通过的样品"));
        }
        Integer passedCheckCount = jdbc.queryForObject("SELECT COUNT(*) FROM nso_sample_check c JOIN nso_sample s ON s.id=c.sample_id WHERE c.tenant_id=? AND s.tenant_id=? AND s.project_id=? AND c.result='PASS' AND s.deleted=0", Integer.class, TenantContext.tenantId(), TenantContext.tenantId(), project.getId());
        if (passedCheckCount == null || passedCheckCount == 0) {
            blockers.add(new DeliveryBlockerDto("DELIVERY_QUALITY_CHECK", "缺少质量人员录入的样品合格检验记录"));
        }
        long incompleteTasks = tasks.selectList(Wrappers.<Task>lambdaQuery().eq(Task::getProjectId, project.getId()).ne(Task::getStatus, "CANCELLED"))
                .stream().filter(task -> !Set.of("DONE", "COMPLETED").contains(task.getStatus())).count();
        if (incompleteTasks > 0) {
            blockers.add(new DeliveryBlockerDto("DELIVERY_TASKS_PENDING", "仍有 " + incompleteTasks + " 个未取消任务尚未完成"));
        }
        if (risks.selectCount(Wrappers.<Risk>lambdaQuery().eq(Risk::getProjectId, project.getId()).eq(Risk::getStatus, "OPEN").eq(Risk::getLevel, "SERIOUS")) > 0) {
            blockers.add(new DeliveryBlockerDto("DELIVERY_SERIOUS_RISK", "存在未关闭的严重风险，禁止交付"));
        }
        return new DeliveryReadinessDto(project.getId(), blockers.isEmpty(), List.copyOf(blockers));
    }

    // 创建项目交付记录。
    @Override
    @Transactional
    public DeliveryRecordDto createDelivery(DeliveryRequest request) {
        if (request == null || request.projectId() == null || request.quantity() == null || request.quantity() < 1
                 || blank(request.logisticsNo()) || blank(request.receiver())) {
            throw BusinessException.ruleBlock("DELIVERY_FIELDS", "交付数量、物流单号和收货人必须如实填写", "incomplete", "quantity/logisticsNo/receiver", "补齐交付信息后重新提交");
        }
        Project project = requireProject(request.projectId());
        dataScope.requireProjectRole(project.getId(), "PROJECT_MANAGER", "PRODUCTION");
        if (project.getQuantity() != null && request.quantity() > project.getQuantity()) {
            throw BusinessException.ruleBlock("DELIVERY_QUANTITY", "交付数量不能超过项目数量", String.valueOf(request.quantity()), String.valueOf(project.getQuantity()), "核对实际交付数量");
        }
        DeliveryReadinessDto readiness = deliveryReadiness(project.getId());
        if (!readiness.ready()) {
            DeliveryBlockerDto blocker = readiness.blockers().get(0);
            throw BusinessException.ruleBlock(blocker.code(), blocker.message(), "blocked", "ready", "完成全部交付前置条件后重新预检");
        }
        DeliveryRecord row = new DeliveryRecord();
        row.setTenantId(TenantContext.tenantId());
        row.setProjectId(project.getId());
        row.setQuantity(request.quantity());
        row.setLogisticsNo(request.logisticsNo().trim());
        row.setReceiver(request.receiver().trim());
        row.setCustomerFeedback(blank(request.feedback()) ? null : request.feedback().trim());
        row.setStatus("SHIPPED");
        row.setShippedAt(LocalDateTime.now());
        deliveries.insert(row);
        moveProject(project, "DELIVERY_CREATED", "PENDING_DELIVERY", "DELIVERY", "已创建真实交付记录");
        events.record(project.getId(), "DELIVERY", row.getId(), "DELIVERY_CREATED", null, "SHIPPED", "已创建真实交付记录");
        return toDeliveryDto(row);
    }

    // 同步技术包执行任务。
    @Override
    @Transactional
    public TechnicalPackageSyncResultDto syncTechnicalPackageTasks(Long projectId) {
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
        if (risks.selectCount(Wrappers.<Risk>lambdaQuery().eq(Risk::getProjectId, project.getId()).eq(Risk::getStatus, "OPEN").eq(Risk::getLevel, "SERIOUS")) > 0)
            throw BusinessException.ruleBlock("SERIOUS_RISK_OPEN", "存在未关闭严重风险，禁止投产", "SERIOUS", "无严重风险", "先关闭风险或走特殊放行流程");
        if (tasks.selectCount(Wrappers.<Task>lambdaQuery().eq(Task::getProjectId, project.getId()).eq(Task::getTaskType, "PURCHASE").ne(Task::getStatus, "DONE")) > 0)
            throw BusinessException.ruleBlock("MATERIAL_NOT_READY", "关键采购任务尚未完成，禁止投产", "purchase pending", "purchase done", "确认到料后再投产");
    }

    private void assertReferencedVersionCurrent(Task task, Project project) {
        DocumentVersion current = currentPublishedVersion(project);
        if (current == null || task.getReferencedVersion() == null || !task.getReferencedVersion().equals(current.getVersionNo()))
            throw BusinessException.ruleBlock("VERSION_MISMATCH", "任务引用的技术版本不是当前发布版本", task.getReferencedVersion(), current == null ? "none" : current.getVersionNo(), "更新任务引用版本");
    }

    private void moveProject(Project project, String action, String targetStatus, String targetStage, String reason) {
        String before = project.getStatus();
        project.setStatus(targetStatus);
        project.setStage(targetStage);
        if (projects.updateById(project) != 1)
            throw BusinessException.ruleBlock("OPTIMISTIC_LOCK", "项目已被其他用户修改", "stale", "latest", "刷新项目后重试");
        jdbc.update("INSERT INTO nso_project_status_history (tenant_id,project_id,before_status,after_status,action_code,reason,operator_id) VALUES (?,?,?,?,?,?,?)",
                TenantContext.tenantId(), project.getId(), before, targetStatus, action, reason, TenantContext.userId());
    }

    private boolean requiresVersionCheck(Task task) {
        return requiresCurrentTechnicalVersion(task.getTaskType());
    }

    private void record(Task task, String before, String after, String action, String summary) {
        jdbc.update("INSERT INTO nso_task_log (tenant_id,task_id,before_status,after_status,operation_type,summary,operator_name) VALUES (?,?,?,?,?,?,?)",
                TenantContext.tenantId(),
                task.getId(),
                before,
                after,
                action,
                summary,
                TenantContext.username());
        events.record(task.getProjectId(),
                "TASK",
                task.getId(),
                action,
                before,
                after,
                summary);
    }
    private Task requireTask(Long id) {
        Task row = tasks.selectById(id);
        if (row == null) throw new BusinessException("任务不存在");
        dataScope.requireAccess(row.getProjectId());
        return row;
    }
    private void requireTaskExecutionAccess(Task task) {
        if (TenantContext.hasAnyRole("admin")) return;
        if (TenantContext.userId() == null || task.getAssigneeId() == null || !TenantContext.userId().equals(task.getAssigneeId()))
            throw BusinessException.accessDenied("TASK_ASSIGNEE_SCOPE", "仅任务责任人可以执行或反馈任务", String.valueOf(task.getId()), "任务责任人", "由责任人处理或重新分派任务");
        String type = task.getTaskType() == null ? "" : task.getTaskType().toUpperCase();
        boolean allowed = switch (type) { case "PURCHASE" -> TenantContext.hasAnyRole("purchaser");
            case "PRODUCTION", "SAMPLE_PREPARE", "SAMPLE_MAKE", "SAMPLE_REWORK" -> TenantContext.hasAnyRole("production", "field_user");
            case "QUALITY", "INSPECTION" -> TenantContext.hasAnyRole("quality");
            case "TECHNICAL", "DESIGN" -> TenantContext.hasAnyRole("technical");
            case "PROCESS" -> TenantContext.hasAnyRole("process");
            default -> TenantContext.hasAnyRole("project_manager");
        };
        if (!allowed) throw BusinessException.accessDenied("TASK_ROLE_SCOPE", "当前岗位不能执行此类任务", type, "匹配任务岗位", "由对应岗位责任人执行");
        dataScope.requireProjectRole(task.getProjectId(), TaskResponsibilityCatalog.forTaskType(type).toArray(String[]::new));
    }

    private boolean requiresCurrentTechnicalVersion(String taskType) {
        return Set.of("PURCHASE", "PRODUCTION", "INSPECTION", "DELIVERY", "SAMPLE_PREPARE", "SAMPLE_MAKE", "SAMPLE_REWORK").contains(taskType);
    }
    private Project requireProject(Long id) {
        Project row = projects.selectById(id);
        if (row == null) throw new BusinessException("项目不存在");
        dataScope.requireAccess(id);
        return row; }
    private DocumentVersion currentPublishedVersion(Project project) {
        if (project.getCurrentDocVersionId() == null) return null;
        DocumentVersion current = documents.selectById(project.getCurrentDocVersionId());
        return current != null && Long.valueOf(TenantContext.tenantId()).equals(current.getTenantId()) && project.getId().equals(current.getProjectId()) && "PUBLISHED".equals(current.getStatus()) ? current : null;
    }
    private void update(Task task) {
        if (tasks.updateById(task) != 1) throw BusinessException.ruleBlock("OPTIMISTIC_LOCK", "任务已被其他用户修改", "stale", "latest", "刷新后重试");
    }
    private void assertExpectedVersion(Task task, Integer requestedVersion) {
        if (requestedVersion != null && !requestedVersion.equals(task.getVersion())) throw BusinessException.ruleBlock("OPTIMISTIC_LOCK", "任务已被其他用户更新", String.valueOf(requestedVersion), String.valueOf(task.getVersion()), "刷新任务后重试");
    }
    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
    private String jsonReasons(String reason) {
        return "[\"" + reason.replace("\\", "\\\\").replace("\"", "\\\"") + "\"]";
    }
    private List<String> reasons(Risk row) {
        String raw = row.getReasons();
        return raw == null || raw.length() < 2 ? List.of() : List.of(raw.substring(1, raw.length() - 1).replace("\"", "").split(","));
    }
    private TaskDto toDto(Task row) {
        return new TaskDto(row.getId(),
                row.getProjectId(),
                requireProject(row.getProjectId()).getProjectNo(),
                row.getTaskNo(),
                row.getTaskType(),
                row.getTitle(),
                row.getReferencedVersion(),
                row.getStatus(),
                row.getResponsibleName(),
                row.getPlanStart(),
                row.getPlanFinish(),
                row.getBlockReason(),
                row.getVersion(),
                row.getAssigneeId());
    }
    private RiskDto toRiskDto(Risk row) {
        return new RiskDto(row.getId(),
                row.getProjectId(),
                requireProject(row.getProjectId()).getProjectNo(),
                row.getLevel(),
                row.getScore(),
                reasons(row),
                row.getSuggestion(),
                row.getStatus());
    }
    private DeliveryRecordDto toDeliveryDto(DeliveryRecord row) {
        return new DeliveryRecordDto(
                row.getId(),
                row.getProjectId(),
                requireProject(row.getProjectId()).getProjectNo(),
                row.getQuantity(),
                row.getLogisticsNo(),
                row.getReceiver(),
                row.getCustomerFeedback(),
                row.getStatus(),
                row.getShippedAt());
    }
}
