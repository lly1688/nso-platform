package com.nso.business.report.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nso.business.change.service.IChangeService;
import com.nso.business.core.NsoDtos.*;
import com.nso.business.core.TenantContext;
import com.nso.business.document.domain.DocumentVersion;
import com.nso.business.document.service.IDocumentService;
import com.nso.business.message.service.IMessageService;
import com.nso.business.project.service.IProjectService;
import com.nso.business.project.service.ProjectDataScope;
import com.nso.business.report.service.IReportService;
import com.nso.business.risk.service.IRiskService;
import com.nso.business.sample.service.ISampleService;
import com.nso.business.support.IFlowCodeService;
import com.nso.business.task.domain.Task;
import com.nso.business.task.mapper.TaskMapper;
import com.nso.business.task.service.ITaskService;
import com.nso.business.document.mapper.DocumentVersionMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class ReportServiceImpl implements IReportService {
    private final IProjectService projects;
    private final IDocumentService documents;
    private final ISampleService samples;
    private final IChangeService changes;
    private final ITaskService tasks;
    private final IRiskService risks;
    private final IMessageService messages;
    private final IFlowCodeService flowCodes;
    private final JdbcTemplate jdbc;
    private final ProjectDataScope dataScope;
    private final TaskMapper taskMapper;
    private final DocumentVersionMapper documentVersionMapper;

    public ReportServiceImpl(IProjectService projects, IDocumentService documents, ISampleService samples,
                             IChangeService changes, ITaskService tasks, IRiskService risks,
                             IMessageService messages, IFlowCodeService flowCodes, JdbcTemplate jdbc,
                             ProjectDataScope dataScope, TaskMapper taskMapper, DocumentVersionMapper documentVersionMapper) {
        this.projects = projects;
        this.documents = documents;
        this.samples = samples;
        this.changes = changes;
        this.tasks = tasks;
        this.risks = risks;
        this.messages = messages;
        this.flowCodes = flowCodes;
        this.jdbc = jdbc;
        this.dataScope = dataScope;
        this.taskMapper = taskMapper;
        this.documentVersionMapper = documentVersionMapper;
    }

    @Override
    public ProjectDetail projectDetail(Long projectId) {
        ProjectDto project = projects.get(projectId);
        return new ProjectDetail(project, projects.requirements(projectId).list(), projects.members(projectId).list(),
            documents.documents(projectId).list(), documents.boms(projectId).list(), documents.processRoutes(projectId).list(),
            documents.inspectionSpecs(projectId).list(), samples.list(projectId).list(), changes.list(projectId).list(),
            tasks.list(projectId).list(), risks.list(projectId).list(), timeline(projectId));
    }

    @Override
    public ProjectWorkspaceDto projectWorkspace(Long projectId) {
        ProjectDetail detail = projectDetail(projectId);
        RiskDto currentRisk = detail.risks().stream().filter(risk -> !"CLOSED".equals(risk.status())).findFirst()
            .orElse(detail.risks().isEmpty() ? null : detail.risks().get(0));
        List<RiskActionDto> actions = currentRisk == null ? List.of() : risks.actions(currentRisk.id()).list();
        DocumentVersionDto currentDocument = detail.documents().stream().filter(document -> Boolean.TRUE.equals(document.currentVersion()))
            .findFirst().orElse(null);
        List<VersionConflictDto> conflicts = currentDocument == null ? List.of() : detail.tasks().stream()
            .filter(task -> !Set.of("DONE", "CANCELLED").contains(task.status()))
            .filter(task -> !Objects.equals(task.referencedVersion(), currentDocument.versionNo()))
            .map(task -> new VersionConflictDto(task.id(), task.taskNo(), task.title(), task.referencedVersion(), currentDocument.versionNo()))
            .toList();
        return new ProjectWorkspaceDto(detail, currentRisk, actions, milestones(detail.project().stage(), detail.timeline()),
            conflicts, flowCodes.findProjectFlowCode(projectId));
    }

    @Override
    public WorkbenchDto workbench() {
        List<ProjectDto> projectRows = projects.list(null).list();
        List<TaskDto> taskRows = tasks.list(null).list();
        List<MessageDto> unread = messages.list("UNREAD").list();
        List<RiskDto> riskRows = risks.list(null).list();
        Map<String, Number> metrics = new LinkedHashMap<>();
        metrics.put("projectCount", projectRows.size());
        metrics.put("todoCount", taskRows.stream().filter(task -> "TODO".equals(task.status()) || "BLOCKED".equals(task.status())).count());
        metrics.put("unreadCount", unread.size());
        metrics.put("highRiskCount", riskRows.stream().filter(risk -> "HIGH".equals(risk.level()) || "SERIOUS".equals(risk.level())).count());
        List<TaskDto> due = taskRows.stream().filter(task -> task.planFinish() != null && !task.planFinish().isAfter(LocalDate.now().plusDays(3)) && !"DONE".equals(task.status())).toList();
        return new WorkbenchDto(metrics, taskRows.stream().filter(task -> "TODO".equals(task.status()) || "BLOCKED".equals(task.status())).toList(),
            due, unread, projectRows, List.of(), riskRows,
            List.of(Map.of("name", "新建项目", "route", "/projects"), Map.of("name", "样品确认", "route", "/samples"), Map.of("name", "发起变更", "route", "/changes")));
    }

    @Override
    public DashboardDto dashboard(String period, LocalDate startDate, LocalDate endDate) {
        DashboardPeriodDto resolvedPeriod = resolvePeriod(period, startDate, endDate);
        List<Long> visibleProjectIds = dataScope.visibleProjectIds();
        List<ProjectDto> projectRows = projects.list(null).list();
        List<TaskDto> taskRows = tasks.list(null).list();
        List<SampleDto> sampleRows = samples.list(null).list();
        List<ChangeOrderDto> changeRows = changes.list(null).list();
        List<RiskDto> riskRows = risks.list(null).list();
        List<MessageDto> unread = messages.list("UNREAD").list();
        List<DocumentVersion> documentRows = visibleDocumentVersions(visibleProjectIds);
        Set<Long> personalTaskIds = personalTodoIds(visibleProjectIds);
        LocalDate today = LocalDate.now();

        Map<String, Number> metrics = new LinkedHashMap<>();
        metrics.put("activeProjects", projectRows.stream().filter(project -> !isTerminalProject(project.status())).count());
        metrics.put("delayedProjects", projectRows.stream().filter(project -> project.targetDate() != null
                && project.targetDate().isBefore(today) && !isTerminalProject(project.status())).count());
        metrics.put("pendingSamples", sampleRows.stream().filter(sample -> waitingSample(sample.status())).count());
        metrics.put("pendingDocuments", documentRows.stream().filter(document -> "DRAFT".equals(document.getStatus())).count());
        metrics.put("openChanges", changeRows.stream().filter(change -> !"CLOSED".equals(change.status())).count());
        metrics.put("reworkQuantity", reworkQuantity(resolvedPeriod, visibleProjectIds));
        metrics.put("highRisks", riskRows.stream().filter(this::isCriticalRisk).count());
        metrics.put("dueTasks", taskRows.stream().filter(task -> isDueTask(task, today)).count());
        metrics.put("unreadMessages", unread.size());
        metrics.put("personalTodos", personalTaskIds.stream()
                .filter(id -> taskRows.stream().anyMatch(task -> task.id().equals(id) && isOpenTask(task.status()))).count());

        List<TaskDto> todos = taskRows.stream()
                .filter(task -> personalTaskIds.contains(task.id()) && isOpenTask(task.status()))
                .sorted(Comparator.comparing(TaskDto::planFinish, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(8)
                .toList();
        List<TaskDto> dueTasks = taskRows.stream().filter(task -> isDueTask(task, today))
                .sorted(Comparator.comparing(TaskDto::planFinish, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(8)
                .toList();
        List<RiskDto> criticalRisks = riskRows.stream().filter(this::isCriticalRisk)
                .sorted(Comparator.comparing(RiskDto::score, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(6)
                .toList();

        return new DashboardDto(
                resolvedPeriod,
                metrics,
                deliveryTrend(resolvedPeriod, visibleProjectIds),
                riskDistribution(riskRows),
                changeTypes(resolvedPeriod, visibleProjectIds),
                departmentLoads(visibleProjectIds),
                projectRows.stream().limit(10).toList(),
                criticalRisks,
                todos,
                dueTasks,
                unread.stream().limit(8).toList(),
                shortcuts());
    }

    @Override
    public PageResult<SearchResultItem> search(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
        List<SearchResultItem> rows = new ArrayList<>();
        for (ProjectDto project : projects.list(normalized).list()) {
            rows.add(new SearchResultItem("PROJECT", project.id(), project.projectNo(), project.customerName() + " / " + project.productName(), "/projects?id=" + project.id()));
        }
        for (TaskDto task : tasks.list(null).list()) {
            if (normalized.isBlank() || task.taskNo().toLowerCase().contains(normalized) || task.title().toLowerCase().contains(normalized)) {
                rows.add(new SearchResultItem("TASK", task.id(), task.taskNo(), task.title(), "/tasks?id=" + task.id()));
            }
        }
        return new PageResult<>(rows, rows.size());
    }

    @Override
    public ReportOverview overview() {
        List<ProjectDto> projectRows = projects.list(null).list();
        List<RiskDto> riskRows = risks.list(null).list();
        List<ChangeOrderDto> changeRows = changes.list(null).list();
        List<SampleDto> sampleRows = samples.list(null).list();
        List<TaskDto> taskRows = tasks.list(null).list();
        List<MessageDto> unread = messages.list("UNREAD").list();
        Map<String, Number> metrics = new LinkedHashMap<>();
        metrics.put("projects", projectRows.size());
        metrics.put("highRisks", riskRows.stream().filter(risk -> "HIGH".equals(risk.level()) || "SERIOUS".equals(risk.level())).count());
        metrics.put("waitingSamples", sampleRows.stream().filter(sample -> "WAITING_CONFIRM".equals(sample.status()) || "PENDING_CONFIRM".equals(sample.status()) || "WAIT_CUSTOMER_CONFIRM".equals(sample.status())).count());
        metrics.put("openChanges", changeRows.stream().filter(change -> !"CLOSED".equals(change.status())).count());
        metrics.put("unreadMessages", unread.size());
        metrics.put("overdueTasks", taskRows.stream().filter(task -> task.planFinish() != null && !task.planFinish().isAfter(LocalDate.now()) && !"DONE".equals(task.status())).count());
        metrics.put("confirmedSamples", sampleRows.stream().filter(sample -> "CONFIRMED".equals(sample.status())).count());
        metrics.put("completedTasks", taskRows.stream().filter(task -> "DONE".equals(task.status())).count());
        return new ReportOverview(metrics, groupRisk(riskRows), groupChange(changeRows), List.of(), List.of(), List.of(), List.of());
    }

    private DashboardPeriodDto resolvePeriod(String period, LocalDate startDate, LocalDate endDate) {
        String normalized = period == null || period.isBlank() ? "30D" : period.trim().toUpperCase();
        LocalDate today = LocalDate.now();
        LocalDate start;
        LocalDate end;
        switch (normalized) {
            case "7D" -> {
                start = today.minusDays(6);
                end = today;
            }
            case "30D" -> {
                start = today.minusDays(29);
                end = today;
            }
            case "MONTH" -> {
                start = today.withDayOfMonth(1);
                end = today;
            }
            case "CUSTOM" -> {
                if (startDate == null || endDate == null) {
                    throw new com.nso.common.exception.BusinessException("自定义统计周期必须包含开始日期和结束日期");
                }
                start = startDate;
                end = endDate;
            }
            default -> throw new com.nso.common.exception.BusinessException("不支持的统计周期：" + normalized);
        }
        if (end.isBefore(start)) {
            throw new com.nso.common.exception.BusinessException("统计结束日期不能早于开始日期");
        }
        if (ChronoUnit.DAYS.between(start, end) > 365) {
            throw new com.nso.common.exception.BusinessException("自定义统计周期不能超过 366 天");
        }
        return new DashboardPeriodDto(normalized, start, end);
    }

    private List<DocumentVersion> visibleDocumentVersions(List<Long> visibleProjectIds) {
        if (visibleProjectIds != null && visibleProjectIds.isEmpty()) {
            return List.of();
        }
        return documentVersionMapper.selectList(Wrappers.<DocumentVersion>lambdaQuery()
                .in(visibleProjectIds != null, DocumentVersion::getProjectId,
                        visibleProjectIds == null ? List.of() : visibleProjectIds));
    }

    private Set<Long> personalTodoIds(List<Long> visibleProjectIds) {
        if (TenantContext.userId() == null || (visibleProjectIds != null && visibleProjectIds.isEmpty())) {
            return Set.of();
        }
        return taskMapper.selectList(Wrappers.<Task>lambdaQuery()
                        .eq(Task::getAssigneeId, TenantContext.userId())
                        .in(visibleProjectIds != null, Task::getProjectId,
                                visibleProjectIds == null ? List.of() : visibleProjectIds))
                .stream()
                .map(Task::getId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private long reworkQuantity(DashboardPeriodDto period, List<Long> visibleProjectIds) {
        StringBuilder sql = new StringBuilder("SELECT COALESCE(SUM(rework_qty), 0) FROM nso_delay_rework "
                + "WHERE tenant_id=? AND recorded_at>=? AND recorded_at<?");
        List<Object> params = new ArrayList<>(List.of(TenantContext.tenantId(), java.sql.Date.valueOf(period.startDate()),
                java.sql.Date.valueOf(period.endDate().plusDays(1))));
        appendProjectScope(sql, params, "project_id", visibleProjectIds);
        Number value = jdbc.queryForObject(sql.toString(), Number.class, params.toArray());
        return value == null ? 0 : value.longValue();
    }

    private List<DashboardTrendPointDto> deliveryTrend(DashboardPeriodDto period, List<Long> visibleProjectIds) {
        Map<LocalDate, long[]> daily = new LinkedHashMap<>();
        for (LocalDate date = period.startDate(); !date.isAfter(period.endDate()); date = date.plusDays(1)) {
            daily.put(date, new long[] { 0, 0 });
        }
        applyDailyCounts(daily, "plan_finish", "status <> 'CANCELLED'", 0, period, visibleProjectIds);
        applyDailyCounts(daily, "DATE(actual_finish)", "status = 'DONE' AND actual_finish IS NOT NULL", 1, period, visibleProjectIds);
        return daily.entrySet().stream()
                .map(entry -> new DashboardTrendPointDto(entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                .toList();
    }

    private void applyDailyCounts(Map<LocalDate, long[]> daily, String dateColumn, String condition, int index,
                                  DashboardPeriodDto period, List<Long> visibleProjectIds) {
        StringBuilder sql = new StringBuilder("SELECT " + dateColumn + " AS stat_date, COUNT(*) AS value "
                + "FROM nso_task WHERE tenant_id=? AND deleted=0 AND " + dateColumn + ">=? AND " + dateColumn + "<? AND " + condition);
        List<Object> params = new ArrayList<>(List.of(TenantContext.tenantId(), java.sql.Date.valueOf(period.startDate()),
                java.sql.Date.valueOf(period.endDate().plusDays(1))));
        appendProjectScope(sql, params, "project_id", visibleProjectIds);
        sql.append(" GROUP BY ").append(dateColumn);
        jdbc.query(sql.toString(), (rs, rowNum) -> {
            java.sql.Date rawDate = rs.getDate("stat_date");
            if (rawDate != null && daily.containsKey(rawDate.toLocalDate())) {
                daily.get(rawDate.toLocalDate())[index] = rs.getLong("value");
            }
            return null;
        }, params.toArray());
    }

    private List<DashboardChartItemDto> riskDistribution(List<RiskDto> riskRows) {
        Map<String, Long> grouped = riskRows.stream()
                .filter(risk -> !"CLOSED".equals(risk.status()))
                .collect(Collectors.groupingBy(RiskDto::level, LinkedHashMap::new, Collectors.counting()));
        return List.of("SERIOUS", "HIGH", "MEDIUM", "LOW").stream()
                .map(level -> new DashboardChartItemDto(level, grouped.getOrDefault(level, 0L)))
                .toList();
    }

    private List<DashboardChartItemDto> changeTypes(DashboardPeriodDto period, List<Long> visibleProjectIds) {
        StringBuilder sql = new StringBuilder("SELECT COALESCE(NULLIF(change_type, ''), '未分类') AS name, COUNT(*) AS value "
                + "FROM nso_change_order WHERE tenant_id=? AND deleted=0 AND created_at>=? AND created_at<?");
        List<Object> params = new ArrayList<>(List.of(TenantContext.tenantId(), java.sql.Date.valueOf(period.startDate()),
                java.sql.Date.valueOf(period.endDate().plusDays(1))));
        appendProjectScope(sql, params, "project_id", visibleProjectIds);
        sql.append(" GROUP BY change_type ORDER BY COUNT(*) DESC, change_type ASC");
        return jdbc.query(sql.toString(), (rs, rowNum) -> new DashboardChartItemDto(rs.getString("name"), rs.getLong("value")), params.toArray());
    }

    private List<DashboardDepartmentLoadDto> departmentLoads(List<Long> visibleProjectIds) {
        Map<String, long[]> rows = new LinkedHashMap<>();
        StringBuilder tasksSql = new StringBuilder("SELECT COALESCE(NULLIF(d.dept_name, ''), '未分配') AS name, "
                + "SUM(CASE WHEN t.status NOT IN ('DONE', 'CANCELLED', 'COMPLETED') THEN 1 ELSE 0 END) AS todo_count, "
                + "SUM(CASE WHEN t.plan_finish < CURDATE() AND t.status NOT IN ('DONE', 'CANCELLED', 'COMPLETED') THEN 1 ELSE 0 END) AS overdue_count "
                + "FROM nso_task t LEFT JOIN sys_user u ON u.id=t.assignee_id AND u.tenant_id=t.tenant_id "
                + "LEFT JOIN sys_dept d ON d.id=u.dept_id AND d.tenant_id=t.tenant_id "
                + "WHERE t.tenant_id=? AND t.deleted=0");
        List<Object> taskParams = new ArrayList<>(List.of(TenantContext.tenantId()));
        appendProjectScope(tasksSql, taskParams, "t.project_id", visibleProjectIds);
        tasksSql.append(" GROUP BY d.dept_name");
        jdbc.query(tasksSql.toString(), (rs, rowNum) -> {
            rows.put(rs.getString("name"), new long[] { rs.getLong("todo_count"), rs.getLong("overdue_count"), 0 });
            return null;
        }, taskParams.toArray());

        StringBuilder impactsSql = new StringBuilder("SELECT COALESCE(NULLIF(i.department_name, ''), '未分配') AS name, COUNT(*) AS impact_count "
                + "FROM nso_change_impact i JOIN nso_change_order c ON c.id=i.change_id AND c.tenant_id=i.tenant_id "
                + "WHERE i.tenant_id=? AND c.deleted=0 AND i.status NOT IN ('COMPLETED', 'CLOSED')");
        List<Object> impactParams = new ArrayList<>(List.of(TenantContext.tenantId()));
        appendProjectScope(impactsSql, impactParams, "c.project_id", visibleProjectIds);
        impactsSql.append(" GROUP BY i.department_name");
        jdbc.query(impactsSql.toString(), (rs, rowNum) -> {
            rows.computeIfAbsent(rs.getString("name"), ignored -> new long[] { 0, 0, 0 })[2] = rs.getLong("impact_count");
            return null;
        }, impactParams.toArray());

        return rows.entrySet().stream()
                .map(entry -> new DashboardDepartmentLoadDto(entry.getKey(), entry.getValue()[0], entry.getValue()[1], entry.getValue()[2]))
                .sorted(Comparator.comparingLong((DashboardDepartmentLoadDto row) -> row.todoTasks() + row.overdueTasks() + row.impactedTasks()).reversed()
                        .thenComparing(DashboardDepartmentLoadDto::name))
                .toList();
    }

    private void appendProjectScope(StringBuilder sql, List<Object> params, String projectColumn, List<Long> visibleProjectIds) {
        if (visibleProjectIds == null) {
            return;
        }
        if (visibleProjectIds.isEmpty()) {
            sql.append(" AND 1=0");
            return;
        }
        sql.append(" AND ").append(projectColumn).append(" IN (");
        for (int index = 0; index < visibleProjectIds.size(); index++) {
            if (index > 0) {
                sql.append(',');
            }
            sql.append('?');
            params.add(visibleProjectIds.get(index));
        }
        sql.append(')');
    }

    private boolean isTerminalProject(String status) {
        return Set.of("COMPLETED", "CLOSED", "CANCELLED").contains(status);
    }

    private boolean waitingSample(String status) {
        return Set.of("WAITING_CONFIRM", "PENDING_CONFIRM", "WAIT_CUSTOMER_CONFIRM").contains(status);
    }

    private boolean isOpenTask(String status) {
        return !Set.of("DONE", "CANCELLED", "COMPLETED").contains(status);
    }

    private boolean isDueTask(TaskDto task, LocalDate today) {
        return task.planFinish() != null && !task.planFinish().isAfter(today.plusDays(3)) && isOpenTask(task.status());
    }

    private boolean isCriticalRisk(RiskDto risk) {
        return !"CLOSED".equals(risk.status()) && Set.of("HIGH", "SERIOUS").contains(risk.level());
    }

    private List<Map<String, Object>> shortcuts() {
        return List.of(
                Map.<String, Object>of("name", "新建项目", "route", "/projects", "permission", "project:create"),
                Map.<String, Object>of("name", "上传图纸", "route", "/documents", "permission", "document:upload"),
                Map.<String, Object>of("name", "样品确认", "route", "/samples", "permission", "sample:submit"),
                Map.<String, Object>of("name", "发起变更", "route", "/changes", "permission", "change:create"));
    }

    private List<ProjectMilestoneDto> milestones(String stage, List<TimelineItem> timeline) {
        List<String> codes = List.of("REQUIREMENT", "TECHNICAL", "SAMPLE", "EXECUTION", "DELIVERY");
        List<String> labels = List.of("需求评审", "图纸发布", "样品确认", "正式投产", "交付");
        int current = stageIndex(stage);
        List<ProjectMilestoneDto> rows = new ArrayList<>();
        for (int index = 0; index < codes.size(); index++) {
            String code = codes.get(index);
            String state = index < current ? "COMPLETED" : index == current ? "CURRENT" : "PENDING";
            LocalDateTime occurred = timeline.stream().filter(item -> item.eventType() != null && item.eventType().contains(code))
                .map(TimelineItem::occurredAt).findFirst().orElse(null);
            rows.add(new ProjectMilestoneDto(code, labels.get(index), state, occurred));
        }
        return rows;
    }

    private int stageIndex(String stage) {
        return switch (stage == null ? "" : stage) {
            case "TECHNICAL" -> 1;
            case "SAMPLE", "CONFIRM" -> 2;
            case "EXECUTION" -> 3;
            case "DELIVERY" -> 4;
            default -> 0;
        };
    }

    private List<Map<String, Object>> groupRisk(List<RiskDto> rows) {
        return rows.stream().collect(java.util.stream.Collectors.groupingBy(RiskDto::level, LinkedHashMap::new, java.util.stream.Collectors.counting()))
            .entrySet().stream().map(item -> Map.<String, Object>of("name", item.getKey(), "value", item.getValue())).toList();
    }

    private List<Map<String, Object>> groupChange(List<ChangeOrderDto> rows) {
        return rows.stream().collect(java.util.stream.Collectors.groupingBy(ChangeOrderDto::changeType, LinkedHashMap::new, java.util.stream.Collectors.counting()))
            .entrySet().stream().map(item -> Map.<String, Object>of("name", item.getKey(), "value", item.getValue())).toList();
    }

    private List<TimelineItem> timeline(Long projectId) {
        return jdbc.query("SELECT id,project_id,event_type,title,summary,operator_name,occurred_at FROM nso_timeline_event WHERE tenant_id=? AND project_id=? ORDER BY occurred_at DESC,id DESC",
            (rs, row) -> new TimelineItem(rs.getLong("id"), rs.getLong("project_id"), rs.getString("event_type"), rs.getString("title"),
                rs.getString("summary"), rs.getString("operator_name"), rs.getTimestamp("occurred_at").toLocalDateTime()),
            TenantContext.tenantId(), projectId);
    }
}
