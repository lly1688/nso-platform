package com.nso.business.support.capa;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nso.business.core.NsoDtos.CapaActionDto;
import com.nso.business.core.NsoDtos.CapaCaseCreateRequest;
import com.nso.business.core.NsoDtos.CapaCaseDto;
import com.nso.business.core.NsoDtos.CapaCorrectiveTaskRequest;
import com.nso.business.core.NsoDtos.CapaEvidenceDto;
import com.nso.business.core.NsoDtos.CapaEvidenceRequest;
import com.nso.business.core.NsoDtos.CapaTransitionRequest;
import com.nso.business.core.NsoDtos.ExceptionReportRequest;
import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.business.core.NsoDtos.RiskDto;
import com.nso.business.core.PageSupport;
import com.nso.business.core.TenantContext;
import com.nso.business.message.service.IMessageService;
import com.nso.business.project.domain.Project;
import com.nso.business.project.domain.ProjectMember;
import com.nso.business.project.mapper.ProjectMapper;
import com.nso.business.project.service.ProjectDataScope;
import com.nso.business.risk.domain.Risk;
import com.nso.business.risk.domain.RiskAction;
import com.nso.business.risk.mapper.RiskActionMapper;
import com.nso.business.risk.mapper.RiskMapper;
import com.nso.business.support.BusinessEventService;
import com.nso.business.support.BusinessNumberService;
import com.nso.business.support.approval.IApprovalService;
import com.nso.business.support.capa.domain.CapaTransition;
import com.nso.business.support.capa.domain.ExceptionCase;
import com.nso.business.support.capa.domain.ExceptionEvidence;
import com.nso.business.support.capa.mapper.CapaTransitionMapper;
import com.nso.business.support.capa.mapper.ExceptionCaseMapper;
import com.nso.business.support.capa.mapper.ExceptionEvidenceMapper;
import com.nso.business.task.domain.Task;
import com.nso.business.task.mapper.TaskMapper;
import com.nso.shared.exception.BusinessException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// CAPA 异常闭环服务实现。 基于现有任务和风险处置事实驱动异常单工作流。
@Service
public class CapaServiceImpl implements ICapaService {
    private static final Set<String> TRANSITIONS = Set.of(
            "OPEN->CONTAINED", "CONTAINED->ANALYZING", "ANALYZING->ACTIONING",
            "ACTIONING->VERIFYING", "VERIFYING->CLOSED");

    // 异常案例数据映射
    private final ExceptionCaseMapper cases;
    // 异常证据数据映射
    private final ExceptionEvidenceMapper evidences;
    // CAPA流转数据映射
    private final CapaTransitionMapper transitions;
    // 任务数据映射
    private final TaskMapper tasks;
    // 项目数据映射
    private final ProjectMapper projects;
    // 风险数据映射
    private final RiskMapper risks;
    // 风险操作数据映射
    private final RiskActionMapper riskActions;
    // 项目数据范围
    private final ProjectDataScope dataScope;
    // 业务事件服务
    private final BusinessEventService events;
    // 业务Number服务
    private final BusinessNumberService numbers;
    // 消息服务
    private final IMessageService messages;
    // 审批服务
    private final IApprovalService approvals;

    public CapaServiceImpl(
            ExceptionCaseMapper cases,
            ExceptionEvidenceMapper evidences,
            CapaTransitionMapper transitions,
            TaskMapper tasks,
            ProjectMapper projects,
            RiskMapper risks,
            RiskActionMapper riskActions,
            ProjectDataScope dataScope,
            BusinessEventService events,
            BusinessNumberService numbers,
            IMessageService messages,
            IApprovalService approvals) {
        this.cases = cases;
        this.evidences = evidences;
        this.transitions = transitions;
        this.tasks = tasks;
        this.projects = projects;
        this.risks = risks;
        this.riskActions = riskActions;
        this.dataScope = dataScope;
        this.events = events;
        this.numbers = numbers;
        this.messages = messages;
        this.approvals = approvals;
    }

    // 分页查询异常单。
    @Override
    public PageResult<CapaCaseDto> list(Long projectId, String status, PageQuery pageQuery) {
        if (projectId != null) {
            dataScope.requireAccess(projectId);
        }
        PageQuery page = pageQuery == null ? new PageQuery() : pageQuery;
        List<Long> visible = dataScope.visibleProjectIds();
        if (visible != null && visible.isEmpty()) {
            return PageResult.empty(page);
        }
        Page<ExceptionCase> rows = cases.selectPage(PageSupport.page(page), Wrappers.<ExceptionCase>lambdaQuery()
                .eq(ExceptionCase::getTenantId, TenantContext.tenantId())
                .eq(projectId != null, ExceptionCase::getProjectId, projectId)
                .eq(notBlank(status), ExceptionCase::getStatus, upper(status))
                .in(visible != null, ExceptionCase::getProjectId, visible == null ? List.of() : visible)
                .orderByAsc(ExceptionCase::getDueAt)
                .orderByDesc(ExceptionCase::getId));
        return PageSupport.result(rows, page, this::toDto);
    }

    // 查询项目未关闭异常单。
    @Override
    public List<CapaCaseDto> openCases(Long projectId) {
        dataScope.requireAccess(projectId);
        return cases.selectList(Wrappers.<ExceptionCase>lambdaQuery()
                        .eq(ExceptionCase::getTenantId, TenantContext.tenantId())
                        .eq(ExceptionCase::getProjectId, projectId)
                        .ne(ExceptionCase::getStatus, "CLOSED")
                        .orderByAsc(ExceptionCase::getDueAt))
                .stream().map(this::toDto).toList();
    }

    // 查询异常单详情。
    @Override
    public CapaCaseDto get(Long caseId) {
        return toDto(requireCase(caseId));
    }

    // 创建异常单。
    @Override
    @Transactional
    public CapaCaseDto create(CapaCaseCreateRequest request) {
        validateCreate(request);
        if (notBlank(request.idempotencyKey())) {
            ExceptionCase existing = cases.selectOne(Wrappers.<ExceptionCase>lambdaQuery()
                    .eq(ExceptionCase::getTenantId, TenantContext.tenantId())
                    .eq(ExceptionCase::getIdempotencyKey, request.idempotencyKey().trim()).last("LIMIT 1"));
            if (existing != null) {
                return toDto(existing);
            }
        }
        Project project = requireProject(request.projectId());
        requireCreateAccess(project.getId());
        Task task = request.taskId() == null ? null : requireTaskForProject(request.taskId(), project.getId());
        ProjectMember owner = dataScope.requireActiveMember(project.getId(), request.ownerUserId());

        Risk risk = new Risk();
        risk.setTenantId(TenantContext.tenantId());
        risk.setProjectId(project.getId());
        risk.setRiskNo(numbers.next("RISK"));
        risk.setLevel("HIGH");
        risk.setScore(70);
        // 在 CAPA 接管新生命周期的同时兼容旧异常上报契约。
        risk.setReasons(jsonArray("现场异常:" + request.summary().trim()));
        risk.setSuggestion("立即完成 CAPA 遏制、分析、整改和验证闭环");
        risk.setStatus("OPEN");
        risk.setRuleCode("EXECUTION_EXCEPTION");
        risk.setRuleVersion("V2");
        risk.setCalculatedAt(LocalDateTime.now());
        risk.setVersion(0);
        risks.insert(risk);

        ExceptionCase row = new ExceptionCase();
        row.setTenantId(TenantContext.tenantId());
        row.setCaseNo(numbers.next("CAPA"));
        row.setProjectId(project.getId());
        row.setTaskId(task == null ? null : task.getId());
        row.setRiskId(risk.getId());
        row.setExceptionType(upper(request.exceptionType()));
        row.setSummary(request.summary().trim());
        row.setReporterName(trimToNull(request.reporterName()));
        row.setOwnerUserId(owner.getUserId());
        row.setOwnerName(owner.getMemberName());
        row.setDueAt(request.dueAt());
        row.setStatus("OPEN");
        row.setIdempotencyKey(trimToNull(request.idempotencyKey()));
        row.setCreatedBy(TenantContext.userId());
        row.setVersion(0);
        cases.insert(row);
        if (task != null && !Set.of("DONE", "CANCELLED").contains(task.getStatus())) {
            task.setStatus("BLOCKED");
            task.setBlockReason("CAPA " + row.getCaseNo() + " 阻塞整改");
            tasks.updateById(task);
        }
        events.record(project.getId(), "EXCEPTION_CASE", row.getId(), "CAPA_CREATED", null, "OPEN", "已创建异常 CAPA " + row.getCaseNo());
        events.record(project.getId(), "RISK", risk.getId(), "EXECUTION_EXCEPTION_REPORTED", null, "OPEN", row.getSummary());
        messages.notifyProject(project.getId(), "EXCEPTION_REPORTED", "现场异常待处理", row.getCaseNo() + " 已创建，请明确遏制措施和责任人", "EXCEPTION", row.getId());
        return toDto(row);
    }

    // 兼容旧入口上报异常风险。
    @Override
    @Transactional
    public RiskDto reportLegacy(ExceptionReportRequest request) {
        if (request == null || request.projectId() == null || !notBlank(request.summary())) {
            throw new BusinessException("项目和异常说明不能为空");
        }
        Project project = requireProject(request.projectId());
        Task task = request.taskId() == null ? null : requireTaskForProject(request.taskId(), project.getId());
        Long ownerId = task == null ? TenantContext.userId() : task.getAssigneeId();
        ProjectMember owner = ownerId == null ? null : findActiveMember(project.getId(), ownerId);
        if (owner == null) {
            owner = dataScope.findActiveMemberForResponsibilities(project.getId(), "PROJECT_MANAGER");
        }
        if (owner == null) {
            throw BusinessException.ruleBlock("CAPA_OWNER_MISSING", "异常上报无法确定项目责任人", String.valueOf(project.getId()), "有效项目成员", "先补充项目经理或任务责任人");
        }
        CapaCaseDto created = create(new CapaCaseCreateRequest(project.getId(), request.taskId(),
                notBlank(request.exceptionType()) ? request.exceptionType() : "EXECUTION_EXCEPTION", request.summary(), request.reporterName(),
                owner.getUserId(), LocalDateTime.now().plusHours(24), null));
        Risk risk = risks.selectById(created.riskId());
        return toRiskDto(risk, project);
    }

    // 执行异常单状态迁移。
    @Override
    @Transactional
    public CapaCaseDto transition(Long caseId, CapaTransitionRequest request) {
        ExceptionCase row = requireCase(caseId);
        requireManageAccess(row);
        if (request == null || !notBlank(request.toStatus()) || !notBlank(request.idempotencyKey())) {
            throw new BusinessException("CAPA 状态、幂等键不能为空");
        }
        CapaTransition replay = transitions.selectOne(Wrappers.<CapaTransition>lambdaQuery()
                .eq(CapaTransition::getTenantId, TenantContext.tenantId())
                .eq(CapaTransition::getExceptionCaseId, caseId)
                .eq(CapaTransition::getIdempotencyKey, request.idempotencyKey().trim()).last("LIMIT 1"));
        if (replay != null) {
            return toDto(requireCase(caseId));
        }
        if (request.version() != null && !request.version().equals(row.getVersion())) {
            throw BusinessException.ruleBlock("OPTIMISTIC_LOCK", "CAPA 案例已被其他用户更新", String.valueOf(request.version()), String.valueOf(row.getVersion()), "刷新后重试");
        }
        String target = upper(request.toStatus());
        if (!TRANSITIONS.contains(row.getStatus() + "->" + target)) {
            throw BusinessException.ruleBlock("CAPA_STATUS", "CAPA 状态迁移不合法", row.getStatus(), target, "按 OPEN→CONTAINED→ANALYZING→ACTIONING→VERIFYING→CLOSED 推进");
        }
        if (notBlank(request.evidenceRef())) {
            storeEvidence(row, request.evidenceRef(), request.evidenceSummary(), "TRANSITION");
        }
        validateTransition(row, target, request);
        String before = row.getStatus();
        applyTransitionValues(row, target, request);
        row.setStatus(target);
        if ("CLOSED".equals(target)) {
            row.setClosedAt(LocalDateTime.now());
        }
        if (cases.updateById(row) != 1) {
            throw BusinessException.ruleBlock("OPTIMISTIC_LOCK", "CAPA 案例已被其他用户更新", "stale", "latest", "刷新后重试");
        }
        CapaTransition transition = new CapaTransition();
        transition.setTenantId(TenantContext.tenantId());
        transition.setExceptionCaseId(row.getId());
        transition.setFromStatus(before);
        transition.setToStatus(target);
        transition.setIdempotencyKey(request.idempotencyKey().trim());
        transition.setComment(trimToNull(request.comment()));
        transition.setOperatorId(TenantContext.userId());
        transitions.insert(transition);
        if ("VERIFYING".equals(target)) {
            approvals.createInstance("EXCEPTION", row.getId(), row.getProjectId(), null);
        }
        if ("CLOSED".equals(target)) {
            closeLinkedFacts(row);
        }
        events.record(row.getProjectId(), "EXCEPTION_CASE", row.getId(), "CAPA_" + target, before, target,
                "CAPA " + row.getCaseNo() + " 已进入" + target);
        return toDto(row);
    }

    // 添加异常证据。
    @Override
    @Transactional
    public CapaEvidenceDto addEvidence(Long caseId, CapaEvidenceRequest request) {
        ExceptionCase row = requireCase(caseId);
        requireManageAccess(row);
        if (request == null || !notBlank(request.evidenceRef())) {
            throw new BusinessException("证据引用不能为空");
        }
        return toEvidenceDto(storeEvidence(row, request.evidenceRef(), request.summary(), request.evidenceType()));
    }

    // 创建纠正任务。
    @Override
    @Transactional
    public CapaActionDto createCorrectiveTask(Long caseId, CapaCorrectiveTaskRequest request) {
        ExceptionCase row = requireCase(caseId);
        requireManageAccess(row);
        if (!"ACTIONING".equals(row.getStatus())) {
            throw BusinessException.ruleBlock("CAPA_STATUS", "仅整改执行阶段可以创建整改任务", row.getStatus(), "ACTIONING", "先完成根因分析");
        }
        if (request == null || !notBlank(request.title()) || request.assigneeUserId() == null || request.planFinish() == null || !notBlank(request.idempotencyKey())) {
            throw new BusinessException("整改任务名称、责任人、计划完成日期和幂等键不能为空");
        }
        LocalDate planStart = request.planStart() == null ? LocalDate.now() : request.planStart();
        if (request.planFinish().isBefore(planStart)) {
            throw new BusinessException("整改任务计划完成日期不能早于开始日期");
        }
        Task existing = tasks.selectOne(Wrappers.<Task>lambdaQuery()
                .eq(Task::getTenantId, TenantContext.tenantId())
                .eq(Task::getCapaCaseId, caseId)
                .eq(Task::getCapaIdempotencyKey, request.idempotencyKey().trim()).last("LIMIT 1"));
        if (existing != null) {
            return toTaskAction(existing);
        }
        ProjectMember assignee = dataScope.requireActiveMember(row.getProjectId(), request.assigneeUserId());
        Task task = new Task();
        task.setTenantId(TenantContext.tenantId());
        task.setProjectId(row.getProjectId());
        task.setCapaCaseId(row.getId());
        task.setCapaIdempotencyKey(request.idempotencyKey().trim());
        task.setTaskNo(numbers.next("TASK"));
        task.setTaskType("CAPA_ACTION");
        task.setTitle(request.title().trim());
        task.setStatus("TODO");
        task.setAssigneeId(assignee.getUserId());
        task.setResponsibleName(assignee.getMemberName());
        task.setPlanStart(planStart);
        task.setPlanFinish(request.planFinish());
        task.setBlockReason(trimToNull(request.actionPlan()));
        task.setVersion(0);
        tasks.insert(task);
        events.record(row.getProjectId(), "TASK", task.getId(), "CAPA_ACTION_CREATED", null, "TODO", "CAPA " + row.getCaseNo() + " 已创建整改任务");
        return toTaskAction(task);
    }

    // 查询异常处置行动。
    @Override
    public List<CapaActionDto> actions(Long caseId) {
        ExceptionCase row = requireCase(caseId);
        List<CapaActionDto> correctiveTasks = tasks.selectList(Wrappers.<Task>lambdaQuery()
                        .eq(Task::getTenantId, TenantContext.tenantId())
                        .eq(Task::getCapaCaseId, row.getId())
                        .orderByAsc(Task::getPlanFinish))
                .stream().map(this::toTaskAction).toList();
        List<CapaActionDto> riskRows = row.getRiskId() == null ? List.of() : riskActions.selectList(Wrappers.<RiskAction>lambdaQuery()
                        .eq(RiskAction::getTenantId, TenantContext.tenantId())
                        .eq(RiskAction::getRiskId, row.getRiskId())
                        .orderByAsc(RiskAction::getPlanFinishTime))
                .stream().map(this::toRiskAction).toList();
        return java.util.stream.Stream.concat(correctiveTasks.stream(), riskRows.stream())
                .sorted(Comparator.comparing(CapaActionDto::dueAt, Comparator.nullsLast(Comparator.naturalOrder()))).toList();
    }

    // 在审批驳回后重新打开异常单。
    @Override
    @Transactional
    public CapaCaseDto reopenAfterApprovalRejection(Long caseId) {
        ExceptionCase row = requireCase(caseId);
        if (!TenantContext.hasAnyRole("admin", "executive")) {
            try {
                dataScope.requireProjectRole(row.getProjectId(), "PROJECT_MANAGER", "QUALITY");
            } catch (BusinessException denied) {
                requireManageAccess(row);
            }
        }
        if (!"VERIFYING".equals(row.getStatus()) || !"REJECTED".equals(approvals.instance("EXCEPTION", row.getId()).status())) {
            throw BusinessException.ruleBlock("CAPA_APPROVAL_STATE", "仅被驳回的关闭审批可以退回整改", row.getStatus(), "VERIFYING with rejected approval", "刷新审批待办");
        }
        approvals.reopenRejectedExceptionApproval(row.getId());
        String before = row.getStatus();
        row.setStatus("ACTIONING");
        row.setVerificationSummary(null);
        if (cases.updateById(row) != 1) {
            throw BusinessException.ruleBlock("OPTIMISTIC_LOCK", "CAPA 案例已被其他用户更新", "stale", "latest", "刷新后重试");
        }
        events.record(row.getProjectId(), "EXCEPTION_CASE", row.getId(), "CAPA_REOPENED", before, "ACTIONING", "关闭审批驳回，已退回整改阶段");
        return toDto(row);
    }

    // 升级逾期异常单。
    @Override
    @Transactional
    public int escalateOverdue() {
        int count = 0;
        for (ExceptionCase row : cases.selectList(Wrappers.<ExceptionCase>lambdaQuery()
                .eq(ExceptionCase::getTenantId, TenantContext.tenantId())
                .ne(ExceptionCase::getStatus, "CLOSED")
                .lt(ExceptionCase::getDueAt, LocalDateTime.now())
                .isNull(ExceptionCase::getEscalatedAt))) {
            row.setEscalatedAt(LocalDateTime.now());
            if (cases.updateById(row) == 1) {
                messages.notifyProject(row.getProjectId(), "CAPA_OVERDUE", "高风险异常已超时", row.getCaseNo() + " 超过 SLA，请协调处置", "EXCEPTION", row.getId());
                events.record(row.getProjectId(), "EXCEPTION_CASE", row.getId(), "CAPA_ESCALATED", row.getStatus(), row.getStatus(), "CAPA 已超时 SLA");
                count++;
            }
        }
        return count;
    }

    private void validateCreate(CapaCaseCreateRequest request) {
        if (request == null || request.projectId() == null || !notBlank(request.exceptionType()) || !notBlank(request.summary())
                || request.ownerUserId() == null || request.dueAt() == null) {
            throw new BusinessException("项目、异常类型、说明、责任人和处置时限不能为空");
        }
        if (!request.dueAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException("CAPA 处置时限必须晚于当前时间");
        }
    }

    private void requireCreateAccess(Long projectId) {
        dataScope.requireProjectRole(projectId, "PROJECT_MANAGER", "TECHNICAL", "PROCESS", "PURCHASER", "PRODUCTION", "QUALITY", "FIELD_USER");
    }

    private void requireManageAccess(ExceptionCase row) {
        dataScope.requireAccess(row.getProjectId());
        if (TenantContext.hasAnyRole("admin", "executive")) {
            return;
        }
        if (TenantContext.userId() != null && TenantContext.userId().equals(row.getOwnerUserId())) {
            return;
        }
        try {
            dataScope.requireProjectRole(row.getProjectId(), "PROJECT_MANAGER");
        } catch (BusinessException denied) {
            throw BusinessException.accessDenied("CAPA_SCOPE", "仅 CAPA 责任人或项目经理可处置案例", String.valueOf(row.getId()), "责任人/项目经理", "联系责任人或项目经理处理");
        }
    }

    private void validateTransition(ExceptionCase row, String target, CapaTransitionRequest request) {
        if ("CONTAINED".equals(target) && !notBlank(request.comment())) {
            throw new BusinessException("进入遏制阶段必须填写临时遏制措施");
        }
        if ("ACTIONING".equals(target)) {
            if (!notBlank(request.rootCause()) || !notBlank(request.correctivePlan())) {
                throw new BusinessException("进入整改阶段必须填写根因与整改计划");
            }
        }
        if ("VERIFYING".equals(target)) {
            if (!notBlank(request.verificationSummary())) {
                throw new BusinessException("进入验证阶段必须填写验证说明");
            }
            requireEvidence(row);
            requireCorrectiveActionsClosed(row);
        }
        if ("CLOSED".equals(target)) {
            if (!notBlank(request.closeConclusion())) {
                throw new BusinessException("关闭 CAPA 必须填写关闭结论");
            }
            requireEvidence(row);
            if (!approvals.isApproved("EXCEPTION", row.getId())) {
                throw BusinessException.ruleBlock("CAPA_CLOSE_APPROVAL", "异常关闭审批尚未完成", "PENDING", "APPROVED", "先完成关闭审批");
            }
        }
    }

    private void applyTransitionValues(ExceptionCase row, String target, CapaTransitionRequest request) {
        if ("CONTAINED".equals(target)) {
            row.setContainmentPlan(request.comment().trim());
        }
        if ("ACTIONING".equals(target)) {
            row.setRootCause(request.rootCause().trim());
            row.setCorrectivePlan(request.correctivePlan().trim());
        }
        if ("VERIFYING".equals(target)) {
            row.setVerificationSummary(request.verificationSummary().trim());
        }
        if ("CLOSED".equals(target)) {
            row.setCloseConclusion(request.closeConclusion().trim());
        }
    }

    private void requireCorrectiveActionsClosed(ExceptionCase row) {
        List<Task> correctiveTasks = tasks.selectList(Wrappers.<Task>lambdaQuery()
                .eq(Task::getTenantId, TenantContext.tenantId())
                .eq(Task::getCapaCaseId, row.getId()));
        if (correctiveTasks.isEmpty()) {
            throw BusinessException.ruleBlock("CAPA_ACTION_REQUIRED", "进入验证前必须创建至少一个整改任务", "0", ">0", "创建并完成整改任务");
        }
        long unfinishedTasks = correctiveTasks.stream().filter(task -> !"DONE".equals(task.getStatus())).count();
        long unfinishedRiskActions = row.getRiskId() == null ? 0 : riskActions.selectCount(Wrappers.<RiskAction>lambdaQuery()
                .eq(RiskAction::getTenantId, TenantContext.tenantId()).eq(RiskAction::getRiskId, row.getRiskId())
                .ne(RiskAction::getStatus, "CLOSED"));
        if (unfinishedTasks > 0 || unfinishedRiskActions > 0) {
            throw BusinessException.ruleBlock("CAPA_ACTION_INCOMPLETE", "整改任务或风险处置尚未关闭", String.valueOf(unfinishedTasks + unfinishedRiskActions), "0", "完成全部整改和风险处置后再验证");

        }
    }

    private void requireEvidence(ExceptionCase row) {
        long count = evidences.selectCount(Wrappers.<ExceptionEvidence>lambdaQuery()
                .eq(ExceptionEvidence::getTenantId, TenantContext.tenantId())
                .eq(ExceptionEvidence::getExceptionCaseId, row.getId()));
        if (count == 0) {
            throw BusinessException.ruleBlock("CAPA_EVIDENCE_REQUIRED", "进入验证或关闭前必须提交证据", "0", ">0", "上传文件后引用，或补充可追溯证据编号");
        }
    }

    private ExceptionEvidence storeEvidence(ExceptionCase row, String evidenceRef, String summary, String evidenceType) {
        if (!notBlank(evidenceRef)) {
            throw new BusinessException("证据引用不能为空");
        }
        ExceptionEvidence evidence = new ExceptionEvidence();
        evidence.setTenantId(TenantContext.tenantId());
        evidence.setExceptionCaseId(row.getId());
        evidence.setEvidenceType(notBlank(evidenceType) ? upper(evidenceType) : "REFERENCE");
        evidence.setEvidenceRef(evidenceRef.trim());
        evidence.setSummary(trimToNull(summary));
        evidence.setSubmittedBy(TenantContext.userId());
        evidences.insert(evidence);
        events.record(row.getProjectId(), "EXCEPTION_EVIDENCE", evidence.getId(), "CAPA_EVIDENCE_ADDED", null, "ADDED", "CAPA 已补充处置证据");
        return evidence;
    }

    private void closeLinkedFacts(ExceptionCase row) {
        if (row.getRiskId() != null) {
            Risk risk = risks.selectById(row.getRiskId());
            if (risk != null && !"CLOSED".equals(risk.getStatus())) {
                risk.setStatus("CLOSED");
                risks.updateById(risk);
            }
        }
        if (row.getTaskId() != null) {
            Task task = tasks.selectById(row.getTaskId());
            if (task != null && "BLOCKED".equals(task.getStatus()) && task.getBlockReason() != null
                    && task.getBlockReason().startsWith("CAPA " + row.getCaseNo())) {
                task.setStatus("TODO");
                task.setBlockReason(null);
                tasks.updateById(task);
            }
        }
    }

    private ExceptionCase requireCase(Long id) {
        ExceptionCase row = cases.selectById(id);
        if (row == null || !Long.valueOf(TenantContext.tenantId()).equals(row.getTenantId())) {
            throw new BusinessException("CAPA 异常案例不存在");
        }
        dataScope.requireAccess(row.getProjectId());
        return row;
    }

    private Project requireProject(Long id) {
        Project project = projects.selectById(id);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        dataScope.requireAccess(id);
        return project;
    }

    private Task requireTaskForProject(Long taskId, Long projectId) {
        Task task = tasks.selectById(taskId);
        if (task == null || !projectId.equals(task.getProjectId())) {
            throw new BusinessException("关联任务不存在或不属于当前项目");
        }
        return task;
    }

    private ProjectMember findActiveMember(Long projectId, Long userId) {
        try {
            return dataScope.requireActiveMember(projectId, userId);
        } catch (BusinessException ignored) {
            return null;
        }
    }

    private CapaCaseDto toDto(ExceptionCase row) {
        return new CapaCaseDto(row.getId(), row.getCaseNo(), row.getProjectId(), row.getTaskId(), row.getRiskId(), row.getExceptionType(),
                row.getSummary(), row.getReporterName(), row.getOwnerUserId(), row.getOwnerName(), row.getDueAt(), row.getStatus(),
                row.getContainmentPlan(), row.getRootCause(), row.getCorrectivePlan(), row.getVerificationSummary(), row.getCloseConclusion(),
                evidences.selectList(Wrappers.<ExceptionEvidence>lambdaQuery().eq(ExceptionEvidence::getTenantId, TenantContext.tenantId())
                        .eq(ExceptionEvidence::getExceptionCaseId, row.getId()).orderByDesc(ExceptionEvidence::getCreatedAt))
                        .stream().map(this::toEvidenceDto).toList(), row.getVersion(), row.getCreatedAt(), row.getClosedAt());
    }

    private CapaEvidenceDto toEvidenceDto(ExceptionEvidence row) {
        return new CapaEvidenceDto(row.getId(), row.getEvidenceType(), row.getEvidenceRef(), row.getSummary(), row.getCreatedAt());
    }

    private CapaActionDto toTaskAction(Task row) {
        return new CapaActionDto("TASK", row.getId(), row.getTitle(), row.getStatus(), row.getAssigneeId(), row.getResponsibleName(),
                row.getPlanFinish() == null ? null : row.getPlanFinish().atTime(23, 59, 59), row.getBlockReason(), row.getVersion());
    }

    private CapaActionDto toRiskAction(RiskAction row) {
        return new CapaActionDto("RISK_ACTION", row.getId(), row.getActionPlan(), row.getStatus(), row.getResponsibleUserId(), row.getResponsibleName(),
                row.getPlanFinishTime(), row.getCloseSummary(), row.getVersion());
    }

    private RiskDto toRiskDto(Risk risk, Project project) {
        if (risk == null) {
            throw new BusinessException("CAPA 关联风险不存在");
        }
        String raw = risk.getReasons();
        List<String> reasons = raw == null || raw.length() < 2 ? List.of()
                : List.of(raw.substring(1, raw.length() - 1).replace("\"", "").split(","));
        return new RiskDto(risk.getId(), risk.getProjectId(), project.getProjectNo(), risk.getLevel(), risk.getScore(),
                reasons, risk.getSuggestion(), risk.getStatus());
    }

    private String jsonArray(String value) {
        return "[\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"]";
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        return notBlank(value) ? value.trim() : null;
    }

    private String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
