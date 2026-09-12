package com.nso.business.support.action;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nso.business.core.NsoDtos.ActionCenterSummaryDto;
import com.nso.business.core.NsoDtos.ActionItemDto;
import com.nso.business.core.NsoDtos.ApprovalTodoDto;
import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.business.core.PageSupport;
import com.nso.business.core.BusinessLockPort;
import com.nso.business.core.TenantContext;
import com.nso.business.message.domain.Message;
import com.nso.business.message.mapper.MessageMapper;
import com.nso.business.project.service.ProjectDataScope;
import com.nso.business.risk.domain.Risk;
import com.nso.business.risk.domain.RiskAction;
import com.nso.business.risk.mapper.RiskActionMapper;
import com.nso.business.risk.mapper.RiskMapper;
import com.nso.business.support.approval.IApprovalService;
import com.nso.business.support.action.domain.ActionItem;
import com.nso.business.support.action.mapper.ActionItemMapper;
import com.nso.business.support.capa.ICapaService;
import com.nso.business.support.capa.domain.ExceptionCase;
import com.nso.business.support.capa.mapper.ExceptionCaseMapper;
import com.nso.business.task.domain.Task;
import com.nso.business.task.mapper.TaskMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 行动中心服务实现。 基于任务、风险、审批、CAPA 和消息事实重建个人行动项投影。
@Service
public class ActionCenterServiceImpl implements IActionCenterService {
    private static final Set<String> ACTIVE_TASK_STATUSES = Set.of("TODO", "IN_PROGRESS", "PAUSED", "BLOCKED");

    // 操作Item数据映射
    private final ActionItemMapper actionItems;
    // 任务数据映射
    private final TaskMapper tasks;
    // 风险数据映射
    private final RiskMapper risks;
    // 风险操作数据映射
    private final RiskActionMapper riskActions;
    // 消息数据映射
    private final MessageMapper messages;
    // 异常案例数据映射
    private final ExceptionCaseMapper cases;
    // 审批服务
    private final IApprovalService approvals;
    // CAPA服务
    private final ICapaService capaService;
    // 项目数据范围
    private final ProjectDataScope dataScope;
    // JDBC模板
    private final JdbcTemplate jdbc;
    private final BusinessLockPort locks;

    public ActionCenterServiceImpl(
            ActionItemMapper actionItems,
            TaskMapper tasks,
            RiskMapper risks,
            RiskActionMapper riskActions,
            MessageMapper messages,
            ExceptionCaseMapper cases,
            IApprovalService approvals,
            ICapaService capaService,
            ProjectDataScope dataScope,
            JdbcTemplate jdbc,
            BusinessLockPort locks) {
        this.actionItems = actionItems;
        this.tasks = tasks;
        this.risks = risks;
        this.riskActions = riskActions;
        this.messages = messages;
        this.cases = cases;
        this.approvals = approvals;
        this.capaService = capaService;
        this.dataScope = dataScope;
        this.jdbc = jdbc;
        this.locks = locks;
    }

    // 分页查询当前用户行动项。
    @Override
    public PageResult<ActionItemDto> myActions(String sourceType, String priority, Long projectId, String dueState, PageQuery pageQuery) {
        PageQuery page = pageQuery == null ? new PageQuery() : pageQuery;
        Long actorId = TenantContext.userId();
        if (actorId == null) {
            return PageResult.empty(page);
        }
        List<Long> currentApprovalIds = approvals.pendingForCurrentUser().stream().map(ApprovalTodoDto::id).toList();
        List<ActionItemDto> visible = actionItems.selectList(visibleOpenQuery(actorId, currentApprovalIds, sourceType, priority))
                .stream().map(this::toDto)
                .filter(item -> projectId == null || projectId.equals(item.projectId()))
                .filter(item -> dueState == null || dueState.isBlank() || matchesDueState(item, dueState))
                .sorted(Comparator.comparingInt((ActionItemDto item) -> priorityRank(item.priority())).reversed()
                        .thenComparing(ActionItemDto::slaDueAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        return PageSupport.slice(visible, page);
    }

    private boolean matchesDueState(ActionItemDto item, String dueState) {
        if ("OVERDUE".equalsIgnoreCase(dueState)) return item.overdue();
        if ("DUE_SOON".equalsIgnoreCase(dueState)) {
            return item.slaDueAt() != null && !item.overdue() && !item.slaDueAt().isAfter(LocalDateTime.now().plusDays(3));
        }
        if ("NO_DATE".equalsIgnoreCase(dueState)) return item.slaDueAt() == null;
        return true;
    }

    // 查询行动中心摘要。
    @Override
    public ActionCenterSummaryDto summary() {
        List<ActionItem> open = activeForCurrentUser();
        Map<String, Number> metrics = new LinkedHashMap<>();
        metrics.put("openCount", open.size());
        metrics.put("overdueCount", open.stream().filter(this::overdue).count());
        metrics.put("criticalCount", open.stream().filter(item -> "CRITICAL".equals(item.getPriority())).count());
        metrics.put("approvalCount", open.stream().filter(item -> "APPROVAL_TASK".equals(item.getSourceType())).count());
        metrics.put("exceptionCount", open.stream().filter(item -> "CAPA_CASE".equals(item.getSourceType())).count());
        List<ActionItemDto> priority = open.stream().sorted(actionOrder()).limit(8).map(this::toDto).toList();
        return new ActionCenterSummaryDto(metrics, priority);
    }

    // 查询项目阻塞项。
    @Override
    public List<ActionItemDto> projectBlockers(Long projectId) {
        dataScope.requireAccess(projectId);
        return actionItems.selectList(Wrappers.<ActionItem>lambdaQuery()
                        .eq(ActionItem::getTenantId, TenantContext.tenantId())
                        .eq(ActionItem::getProjectId, projectId)
                        .eq(ActionItem::getActionStatus, "OPEN"))
                .stream().sorted(actionOrder()).map(this::toDto).limit(20).toList();
    }

    // 刷新行动项投影。
    @Override
    @Transactional
    public int refreshProjection() {
        return locks.withLock("action-projection:tenant:" + TenantContext.tenantId(), this::refreshProjectionUnderLock);
    }

    private int refreshProjectionUnderLock() {
        reconcileClosedSources();
        jdbc.queryForList("SELECT id FROM nso_change_order WHERE tenant_id=? AND deleted=0 AND status='WAIT_APPROVAL'", Long.class,
                TenantContext.tenantId()).forEach(approvals::syncChangeApprovals);
        Long actorId = TenantContext.userId();
        if (actorId == null) {
            return 0;
        }
        List<Long> visible = dataScope.visibleProjectIds();
        if (visible != null && visible.isEmpty()) {
            return 0;
        }
        List<Candidate> candidates = new ArrayList<>();
        tasks.selectList(Wrappers.<Task>lambdaQuery()
                        .eq(Task::getTenantId, TenantContext.tenantId())
                        .eq(Task::getAssigneeId, actorId)
                        .in(Task::getStatus, ACTIVE_TASK_STATUSES)
                        .in(visible != null, Task::getProjectId, visible == null ? List.of() : visible))
                .forEach(task -> candidates.add(taskCandidate(task)));
        riskActions.selectList(Wrappers.<RiskAction>lambdaQuery()
                        .eq(RiskAction::getTenantId, TenantContext.tenantId())
                        .eq(RiskAction::getResponsibleUserId, actorId)
                        .ne(RiskAction::getStatus, "CLOSED"))
                .forEach(action -> riskCandidate(action, visible).ifPresent(candidates::add));
        approvals.pendingForCurrentUser().forEach(todo -> candidates.add(approvalCandidate(todo)));
        cases.selectList(Wrappers.<ExceptionCase>lambdaQuery()
                        .eq(ExceptionCase::getTenantId, TenantContext.tenantId())
                        .eq(ExceptionCase::getOwnerUserId, actorId)
                        .ne(ExceptionCase::getStatus, "CLOSED")
                        .in(visible != null, ExceptionCase::getProjectId, visible == null ? List.of() : visible))
                .forEach(row -> candidates.add(capaCandidate(row)));
        messages.selectList(Wrappers.<Message>lambdaQuery()
                        .eq(Message::getTenantId, TenantContext.tenantId())
                        .eq(Message::getReceiverId, actorId)
                        .eq(Message::getStatus, "UNREAD"))
                .forEach(message -> candidates.add(messageCandidate(message)));
        candidates.forEach(this::upsert);
        return candidates.size();
    }

    // 升级逾期行动项。
    @Override
    @Transactional
    public int escalateOverdue() {
        int escalated = approvals.escalateOverdue() + capaService.escalateOverdue();
        int marked = jdbc.update("UPDATE nso_action_item SET priority='CRITICAL',updated_at=NOW() WHERE tenant_id=? AND action_status='OPEN' AND sla_due_at IS NOT NULL AND sla_due_at<NOW() AND priority<>'CRITICAL'",
                TenantContext.tenantId());
        return escalated + marked;
    }

    private void refreshProject(Long projectId) {
        reconcileClosedSources();
        jdbc.queryForList("SELECT id FROM nso_change_order WHERE tenant_id=? AND project_id=? AND deleted=0 AND status='WAIT_APPROVAL'", Long.class,
                TenantContext.tenantId(), projectId).forEach(approvals::syncChangeApprovals);
        List<Candidate> candidates = new ArrayList<>();
        tasks.selectList(Wrappers.<Task>lambdaQuery().eq(Task::getTenantId, TenantContext.tenantId())
                        .eq(Task::getProjectId, projectId).in(Task::getStatus, ACTIVE_TASK_STATUSES))
                .forEach(task -> candidates.add(taskCandidate(task)));
        riskActions.selectList(Wrappers.<RiskAction>lambdaQuery().eq(RiskAction::getTenantId, TenantContext.tenantId())
                        .ne(RiskAction::getStatus, "CLOSED"))
                .forEach(action -> riskCandidate(action, List.of(projectId)).ifPresent(candidates::add));
        approvals.pendingForProject(projectId).forEach(todo -> candidates.add(approvalCandidate(todo)));
        cases.selectList(Wrappers.<ExceptionCase>lambdaQuery().eq(ExceptionCase::getTenantId, TenantContext.tenantId())
                        .eq(ExceptionCase::getProjectId, projectId).ne(ExceptionCase::getStatus, "CLOSED"))
                .forEach(row -> candidates.add(capaCandidate(row)));
        candidates.forEach(this::upsert);
    }

    private java.util.Optional<Candidate> riskCandidate(RiskAction action, List<Long> visible) {
        Risk risk = risks.selectById(action.getRiskId());
        if (risk == null || (visible != null && !visible.contains(risk.getProjectId()))) {
            return java.util.Optional.empty();
        }
        String priority = riskPriority(risk.getLevel(), action.getPlanFinishTime());
        return java.util.Optional.of(new Candidate("RISK_ACTION", action.getId(), "DISPOSE", risk.getProjectId(), action.getResponsibleUserId(),
                action.getResponsibleName(), "风险处置：" + abbreviate(action.getActionPlan()), action.getActionPlan(), priority,
                action.getPlanFinishTime(), "/tasks/risks?projectId=" + risk.getProjectId(), action.getStatus(), action.getVersion()));
    }

    private Candidate taskCandidate(Task task) {
        LocalDateTime due = task.getPlanFinish() == null ? null : task.getPlanFinish().atTime(23, 59, 59);
        String priority = "BLOCKED".equals(task.getStatus()) ? "CRITICAL" : due != null && due.isBefore(LocalDateTime.now()) ? "HIGH" : "NORMAL";
        return new Candidate("TASK", task.getId(), "EXECUTE", task.getProjectId(), task.getAssigneeId(), task.getResponsibleName(),
                "任务：" + abbreviate(task.getTitle()), task.getBlockReason(), priority, due,
                "/tasks/execution?taskId=" + task.getId(), task.getStatus(), task.getVersion());
    }

    private Candidate approvalCandidate(ApprovalTodoDto todo) {
        String priority = todo.overdue() ? "CRITICAL" : "HIGH";
        return new Candidate("APPROVAL_TASK", todo.id(), "DECIDE", todo.projectId(), todo.assigneeUserId(), todo.assigneeName(),
                "待审批：" + todo.nodeName(), todo.businessType() + " · " + todo.nodeCode(), priority, todo.dueAt(),
                "/actions?tab=approvals&approvalId=" + todo.id(), todo.decision(), todo.version());
    }

    private Candidate capaCandidate(ExceptionCase row) {
        String priority = row.getDueAt() != null && row.getDueAt().isBefore(LocalDateTime.now()) ? "CRITICAL" : "HIGH";
        return new Candidate("CAPA_CASE", row.getId(), "DISPOSE", row.getProjectId(), row.getOwnerUserId(), row.getOwnerName(),
                "异常 CAPA：" + row.getCaseNo(), row.getSummary(), priority, row.getDueAt(),
                "/actions?tab=exceptions&exceptionId=" + row.getId(), row.getStatus(), row.getVersion());
    }

    private Candidate messageCandidate(Message message) {
        return new Candidate("MESSAGE", message.getId(), "READ", null, message.getReceiverId(), null, "未读消息：" + abbreviate(message.getTitle()),
                message.getContent(), "NORMAL", message.getCreatedAt(), "/dashboard", message.getStatus(), message.getVersion());
    }

    private void upsert(Candidate candidate) {
        jdbc.update("INSERT INTO nso_action_item (tenant_id,source_type,source_id,action_code,project_id,assignee_user_id,assignee_name,title,summary,priority,sla_due_at,route,source_status,action_status,source_version) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,'OPEN',?) "
                        + "ON DUPLICATE KEY UPDATE project_id=VALUES(project_id),assignee_user_id=VALUES(assignee_user_id),assignee_name=VALUES(assignee_name),title=VALUES(title),summary=VALUES(summary),priority=VALUES(priority),sla_due_at=VALUES(sla_due_at),route=VALUES(route),source_status=VALUES(source_status),action_status='OPEN',source_version=VALUES(source_version),closed_at=NULL,deleted=0,updated_at=NOW()",
                TenantContext.tenantId(), candidate.sourceType(), candidate.sourceId(), candidate.actionCode(), candidate.projectId(),
                candidate.assigneeUserId(), candidate.assigneeName(), candidate.title(), candidate.summary(), candidate.priority(),
                candidate.dueAt(), candidate.route(), candidate.sourceStatus(), candidate.sourceVersion());
    }

    private void reconcileClosedSources() {
        long tenantId = TenantContext.tenantId();
        jdbc.update("UPDATE nso_action_item a LEFT JOIN nso_task s ON s.id=a.source_id AND s.tenant_id=a.tenant_id AND s.deleted=0 SET a.action_status='RESOLVED',a.closed_at=COALESCE(a.closed_at,NOW()),a.updated_at=NOW() WHERE a.tenant_id=? AND a.action_status='OPEN' AND a.source_type='TASK' AND (s.id IS NULL OR s.status IN ('DONE','CANCELLED'))", tenantId);
        jdbc.update("UPDATE nso_action_item a LEFT JOIN nso_risk_action s ON s.id=a.source_id AND s.tenant_id=a.tenant_id AND s.deleted=0 SET a.action_status='RESOLVED',a.closed_at=COALESCE(a.closed_at,NOW()),a.updated_at=NOW() WHERE a.tenant_id=? AND a.action_status='OPEN' AND a.source_type='RISK_ACTION' AND (s.id IS NULL OR s.status='CLOSED')", tenantId);
        jdbc.update("UPDATE nso_action_item a LEFT JOIN nso_approval_task t ON t.id=a.source_id AND t.tenant_id=a.tenant_id AND t.deleted=0 LEFT JOIN nso_approval_instance i ON i.id=t.instance_id AND i.tenant_id=a.tenant_id AND i.deleted=0 SET a.action_status='RESOLVED',a.closed_at=COALESCE(a.closed_at,NOW()),a.updated_at=NOW() WHERE a.tenant_id=? AND a.action_status='OPEN' AND a.source_type='APPROVAL_TASK' AND (t.id IS NULL OR t.decision<>'PENDING' OR i.status<>'PENDING')", tenantId);
        jdbc.update("UPDATE nso_action_item a LEFT JOIN nso_exception_case s ON s.id=a.source_id AND s.tenant_id=a.tenant_id AND s.deleted=0 SET a.action_status='RESOLVED',a.closed_at=COALESCE(a.closed_at,NOW()),a.updated_at=NOW() WHERE a.tenant_id=? AND a.action_status='OPEN' AND a.source_type='CAPA_CASE' AND (s.id IS NULL OR s.status='CLOSED')", tenantId);
        jdbc.update("UPDATE nso_action_item a LEFT JOIN nso_message s ON s.id=a.source_id AND s.tenant_id=a.tenant_id AND s.deleted=0 SET a.action_status='RESOLVED',a.closed_at=COALESCE(a.closed_at,NOW()),a.updated_at=NOW() WHERE a.tenant_id=? AND a.action_status='OPEN' AND a.source_type='MESSAGE' AND (s.id IS NULL OR s.status<>'UNREAD')", tenantId);
    }

    private List<ActionItem> activeForCurrentUser() {
        Long actorId = TenantContext.userId();
        if (actorId == null) {
            return List.of();
        }
        List<Long> approvalsForUser = approvals.pendingForCurrentUser().stream().map(ApprovalTodoDto::id).toList();
        return actionItems.selectList(visibleOpenQuery(actorId, approvalsForUser, null, null));
    }

    // 审批任务按实时项目职责授权可见，不能只依赖固化的处理人字段。 必须在分页前过滤，避免数量与内容暴露其他用户待办。
    private com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ActionItem> visibleOpenQuery(
            Long actorId, List<Long> approvalTodoIds, String sourceType, String priority) {
        var query = Wrappers.<ActionItem>lambdaQuery()
                .eq(ActionItem::getTenantId, TenantContext.tenantId())
                .eq(ActionItem::getActionStatus, "OPEN")
                .eq(notBlank(sourceType), ActionItem::getSourceType, upper(sourceType))
                .eq(notBlank(priority), ActionItem::getPriority, upper(priority));
        List<Long> visibleProjects = dataScope.visibleProjectIds();
        if (visibleProjects != null) {
            if (visibleProjects.isEmpty()) {
                query.isNull(ActionItem::getProjectId);
            } else {
                query.and(scope -> scope.isNull(ActionItem::getProjectId)
                        .or().in(ActionItem::getProjectId, visibleProjects));
            }
        }
        if (approvalTodoIds == null || approvalTodoIds.isEmpty()) {
            return query.eq(ActionItem::getAssigneeUserId, actorId);
        }
        return query.and(group -> group.eq(ActionItem::getAssigneeUserId, actorId)
                .or().eq(ActionItem::getSourceType, "APPROVAL_TASK")
                .in(ActionItem::getSourceId, approvalTodoIds));
    }

    private Comparator<ActionItem> actionOrder() {
        return Comparator.comparingInt((ActionItem item) -> priorityRank(item.getPriority())).reversed()
                .thenComparing(ActionItem::getSlaDueAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ActionItem::getId, Comparator.reverseOrder());
    }

    private ActionItemDto toDto(ActionItem item) {
        return new ActionItemDto(item.getId(), item.getSourceType(), item.getSourceId(), item.getActionCode(), item.getProjectId(),
                item.getAssigneeUserId(), item.getAssigneeName(), item.getTitle(), item.getSummary(), item.getPriority(), item.getSlaDueAt(),
                item.getRoute(), item.getSourceStatus(), item.getActionStatus(), overdue(item));
    }

    private boolean overdue(ActionItem item) {
        return "OPEN".equals(item.getActionStatus()) && item.getSlaDueAt() != null && item.getSlaDueAt().isBefore(LocalDateTime.now());
    }

    private String riskPriority(String level, LocalDateTime due) {
        if ("SERIOUS".equals(level) || due != null && due.isBefore(LocalDateTime.now())) {
            return "CRITICAL";
        }
        return "HIGH".equals(level) ? "HIGH" : "NORMAL";
    }

    private int priorityRank(String value) {
        return switch (value == null ? "" : value) {
            case "CRITICAL" -> 4;
            case "HIGH" -> 3;
            case "NORMAL" -> 2;
            default -> 1;
        };
    }

    private String abbreviate(String value) {
        if (value == null || value.isBlank()) {
            return "待处理事项";
        }
        String trimmed = value.trim();
        return trimmed.length() <= 72 ? trimmed : trimmed.substring(0, 72) + "...";
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    // 行动项候选数据。
    private record Candidate(
            String sourceType,
            Long sourceId,
            String actionCode,
            Long projectId,
            Long assigneeUserId,
            String assigneeName,
            String title,
            String summary,
            String priority,
            LocalDateTime dueAt,
            String route,
            String sourceStatus,
            Integer sourceVersion) {
    }
}
