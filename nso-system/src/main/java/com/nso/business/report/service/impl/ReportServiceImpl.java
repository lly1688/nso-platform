package com.nso.business.report.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nso.business.change.service.IChangeService;
import com.nso.business.core.NsoDtos.*;
import com.nso.business.core.ProjectProgressSemantics;
import com.nso.business.core.PageSupport;
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
import com.nso.business.support.action.IActionCenterService;
import com.nso.business.support.approval.IApprovalService;
import com.nso.business.support.capa.ICapaService;
import com.nso.business.task.service.ITaskService;
import com.nso.business.document.mapper.DocumentVersionMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service

// 报表统计 服务层处理
public class ReportServiceImpl implements IReportService {
    // 项目服务
    private final IProjectService projects;
    // 文档服务
    private final IDocumentService documents;
    // 样品服务
    private final ISampleService samples;
    // 变更服务
    private final IChangeService changes;
    // 任务服务
    private final ITaskService tasks;
    // 风险服务
    private final IRiskService risks;
    // 消息服务
    private final IMessageService messages;
    // 流程编码服务
    private final IFlowCodeService flowCodes;
    // JDBC模板
    private final JdbcTemplate jdbc;
    // 项目数据范围
    private final ProjectDataScope dataScope;
    // 文档版本数据映射
    private final DocumentVersionMapper documentVersionMapper;
    // 操作中心服务
    private final IActionCenterService actionCenter;
    // 审批服务
    private final IApprovalService approvals;
    // CAPA服务
    private final ICapaService capaService;

    public ReportServiceImpl(IProjectService projects,
                                IDocumentService documents,
                                ISampleService samples,
                                IChangeService changes,
                                ITaskService tasks,
                                IRiskService risks,
                                IMessageService messages,
                                IFlowCodeService flowCodes,
                                JdbcTemplate jdbc,
                                ProjectDataScope dataScope,
                                DocumentVersionMapper documentVersionMapper,
                                IActionCenterService actionCenter,
                                IApprovalService approvals,
                                ICapaService capaService) {
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
        this.documentVersionMapper = documentVersionMapper;
        this.actionCenter = actionCenter;
        this.approvals = approvals;
        this.capaService = capaService;
    }

    // 查询项目综合详情。
    @Override
    public ProjectDetail projectDetail(Long projectId) {
        ProjectDto project = projects.get(projectId);
        return new ProjectDetail(project,
                projects.requirements(projectId).list(),
                projects.members(projectId).list(),
            documents.documents(projectId).list(),
                documents.boms(projectId).list(),
                documents.processRoutes(projectId).list(),
            documents.inspectionSpecs(projectId).list(),
                samples.list(projectId).list(),
                changes.list(projectId).list(),
            tasks.list(projectId).list(),
                risks.list(projectId).list(),
                timeline(projectId));
    }

    // 查询项目工作区。
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
        List<ProjectOwnerGapDto> ownerGaps = jdbc.query("SELECT project_id,responsibility_code,status,detail FROM nso_project_responsibility_reconciliation WHERE tenant_id=? AND project_id=? AND status='OPEN' ORDER BY responsibility_code",
                (rs, rowNum) -> new ProjectOwnerGapDto(rs.getLong("project_id"), rs.getString("responsibility_code"), rs.getString("status"), rs.getString("detail")),
                TenantContext.tenantId(), projectId);
        List<ApprovalTodoDto> pendingDecisions = approvals.pendingForProject(projectId);
        ProjectProgressSemantics.Progress progress = ProjectProgressSemantics.describe(detail.project().stage(), detail.project().status(),
                detail.project().targetDate(), actions.size() + conflicts.size() + ownerGaps.size(), pendingDecisions.size());
        ProjectProgressDto progressDto = new ProjectProgressDto(progress.stage(), progress.stageLabel(), progress.status(),
                progress.completionCriteria(), progress.dueState(), progress.riskLevel(), progress.blockerCount(),
                progress.pendingDecisionCount(), progress.recommendedAction().code(), progress.recommendedAction().label(),
                progress.recommendedAction().reason(), progress.recommendedAction().route());
        return new ProjectWorkspaceDto(detail, currentRisk, actions, milestones(detail.project().stage(), detail.project().status(), detail.timeline()),
            conflicts, flowCodes.findProjectFlowCode(projectId), ownerGaps, progressDto);
    }

    // 查询项目工作区摘要。
    @Override
    public ProjectWorkspaceSummaryDto projectWorkspaceSummary(Long projectId) {
        ProjectDto project = projects.get(projectId);
        PageResult<RiskDto> riskPage = risks.list(projectId, null, null, new PageQuery(1, 1));
        RiskDto currentRisk = riskPage.list().stream()
                .filter(risk -> !"CLOSED".equals(risk.status()))
                .findFirst()
                .orElse(riskPage.list().isEmpty() ? null : riskPage.list().get(0));
        List<TimelineItem> timeline = timeline(projectId);
        Long ownerGapCount = jdbc.queryForObject("SELECT COUNT(*) FROM nso_project_responsibility_reconciliation WHERE tenant_id=? AND project_id=? AND status='OPEN'",
                Long.class, TenantContext.tenantId(), projectId);
        return new ProjectWorkspaceSummaryDto(project, currentRisk,
                milestones(project.stage(), project.status(), timeline), flowCodes.findProjectFlowCode(projectId),
                ownerGapCount == null ? 0 : ownerGapCount);
    }

    // 查询项目脉搏指标。
    @Override
    public ProjectPulseDto projectPulse(Long projectId) {
        ProjectDto project = projects.get(projectId);
        List<ActionItemDto> blockers = actionCenter.projectBlockers(projectId);
        List<ApprovalTodoDto> pendingDecisions = approvals.pendingForProject(projectId);
        List<VersionConflictDto> conflicts = projectWorkspaceVersionConflicts(projectId, new PageQuery(1, 100)).list();
        List<ProjectOwnerGapDto> ownerGaps = projectWorkspaceOwnerGaps(projectId, new PageQuery(1, 100)).list();
        List<CapaCaseDto> openExceptions = capaService.openCases(projectId);
        Map<String, Number> metrics = new LinkedHashMap<>();
        metrics.put("blockerCount", blockers.size());
        metrics.put("decisionCount", pendingDecisions.size());
        metrics.put("versionConflictCount", conflicts.size());
        metrics.put("openExceptionCount", openExceptions.size());
        metrics.put("ownerGapCount", ownerGaps.size());
        ProjectProgressSemantics.Progress progress = ProjectProgressSemantics.describe(project.stage(), project.status(),
                project.targetDate(), blockers.size() + conflicts.size() + ownerGaps.size() + openExceptions.size(), pendingDecisions.size());
        metrics.put("riskScore", progress.riskLevel().equals("HIGH") ? 2 : progress.riskLevel().equals("MEDIUM") ? 1 : 0);
        return new ProjectPulseDto(project.id(), project.projectNo(), project.stage(), project.status(), blockers,
                pendingDecisions, conflicts, openExceptions, ownerGaps, metrics);
    }

    // 分页查询项目时间线。
    @Override
    public PageResult<TimelineItem> projectWorkspaceTimeline(Long projectId, PageQuery pageQuery) {
        projects.get(projectId);
        PageQuery page = PageSupport.normalize(pageQuery);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM nso_timeline_event WHERE tenant_id=? AND project_id=?",
                Long.class, TenantContext.tenantId(), projectId);
        List<TimelineItem> rows = jdbc.query("SELECT id,project_id,event_type,title,summary,operator_name,occurred_at FROM nso_timeline_event WHERE tenant_id=? AND project_id=? ORDER BY occurred_at DESC,id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new TimelineItem(rs.getLong("id"), rs.getLong("project_id"), rs.getString("event_type"),
                        rs.getString("title"), rs.getString("summary"), rs.getString("operator_name"),
                        rs.getTimestamp("occurred_at").toLocalDateTime()),
                TenantContext.tenantId(), projectId, page.pageSizeValue(), page.offset());
        return new PageResult<>(rows, total == null ? 0 : total, page.pageNoValue(), page.pageSizeValue());
    }

    // 分页查询项目版本冲突。
    @Override
    public PageResult<VersionConflictDto> projectWorkspaceVersionConflicts(Long projectId, PageQuery pageQuery) {
        projects.get(projectId);
        PageQuery page = PageSupport.normalize(pageQuery);
        DocumentVersion current = documentVersionMapper.selectOne(Wrappers.<DocumentVersion>lambdaQuery()
                .eq(DocumentVersion::getProjectId, projectId).eq(DocumentVersion::getCurrentVersion, 1)
                .orderByDesc(DocumentVersion::getId).last("LIMIT 1"));
        if (current == null) return PageResult.empty(page);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM nso_task WHERE tenant_id=? AND project_id=? AND deleted=0 AND status NOT IN ('DONE','CANCELLED') AND (referenced_version IS NULL OR referenced_version<>?)",
                Long.class, TenantContext.tenantId(), projectId, current.getVersionNo());
        List<VersionConflictDto> rows = jdbc.query("SELECT id,task_no,title,referenced_version FROM nso_task WHERE tenant_id=? AND project_id=? AND deleted=0 AND status NOT IN ('DONE','CANCELLED') AND (referenced_version IS NULL OR referenced_version<>?) ORDER BY id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new VersionConflictDto(rs.getLong("id"), rs.getString("task_no"), rs.getString("title"),
                        rs.getString("referenced_version"), current.getVersionNo()),
                TenantContext.tenantId(), projectId, current.getVersionNo(), page.pageSizeValue(), page.offset());
        return new PageResult<>(rows, total == null ? 0 : total, page.pageNoValue(), page.pageSizeValue());
    }

    // 分页查询项目责任人缺口。
    @Override
    public PageResult<ProjectOwnerGapDto> projectWorkspaceOwnerGaps(Long projectId, PageQuery pageQuery) {
        projects.get(projectId);
        PageQuery page = PageSupport.normalize(pageQuery);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM nso_project_responsibility_reconciliation WHERE tenant_id=? AND project_id=? AND status='OPEN'",
                Long.class, TenantContext.tenantId(), projectId);
        List<ProjectOwnerGapDto> rows = jdbc.query("SELECT project_id,responsibility_code,status,detail FROM nso_project_responsibility_reconciliation WHERE tenant_id=? AND project_id=? AND status='OPEN' ORDER BY responsibility_code LIMIT ? OFFSET ?",
                (rs, rowNum) -> new ProjectOwnerGapDto(rs.getLong("project_id"), rs.getString("responsibility_code"),
                        rs.getString("status"), rs.getString("detail")),
                TenantContext.tenantId(), projectId, page.pageSizeValue(), page.offset());
        return new PageResult<>(rows, total == null ? 0 : total, page.pageNoValue(), page.pageSizeValue());
    }

    // 查询当前用户工作台。
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
            List.of(Map.of("name", "新建项目", "route", "/projects"), Map.of("name", "样品确认", "route", "/samples"), Map.of("name", "发起变更", "route", "/changes"), Map.of("name", "我的行动", "route", "/actions")));
    }

    // 查询仪表盘。
    @Override
    public DashboardDto dashboard(String period, LocalDate startDate, LocalDate endDate) {
        DashboardPeriodDto resolvedPeriod = resolvePeriod(period, startDate, endDate);
        List<Long> visibleProjectIds = dataScope.visibleProjectIds();
        DashboardOverviewDto overview = dashboardOverview(resolvedPeriod, visibleProjectIds);
        DashboardInsightsDto insights = dashboardInsights(resolvedPeriod, visibleProjectIds);

        return new DashboardDto(
                overview.period(),
                overview.metrics(),
                insights.deliveryTrend(),
                insights.riskDistribution(),
                insights.changeTypes(),
                insights.departmentLoads(),
                dashboardProjects(new PageQuery(1, 10)).list(),
                dashboardCriticalRisks(new PageQuery(1, 6)).list(),
                dashboardTodos(new PageQuery(1, 8)).list(),
                dashboardDueTasks(new PageQuery(1, 8)).list(),
                dashboardUnreadMessages(new PageQuery(1, 8)).list(),
                overview.shortcuts());
    }

    // 查询仪表盘首屏概览。
    @Override
    public DashboardOverviewDto dashboardOverview(String period, LocalDate startDate, LocalDate endDate) {
        DashboardPeriodDto resolvedPeriod = resolvePeriod(period, startDate, endDate);
        return dashboardOverview(resolvedPeriod, dataScope.visibleProjectIds());
    }

    // 查询仪表盘分析数据。
    @Override
    public DashboardInsightsDto dashboardInsights(String period, LocalDate startDate, LocalDate endDate) {
        DashboardPeriodDto resolvedPeriod = resolvePeriod(period, startDate, endDate);
        return dashboardInsights(resolvedPeriod, dataScope.visibleProjectIds());
    }

    // 查询仪表盘摘要。
    @Override
    public DashboardSummaryDto dashboardSummary(String period, LocalDate startDate, LocalDate endDate) {
        DashboardPeriodDto resolvedPeriod = resolvePeriod(period, startDate, endDate);
        List<Long> visibleProjectIds = dataScope.visibleProjectIds();
        DashboardOverviewDto overview = dashboardOverview(resolvedPeriod, visibleProjectIds);
        DashboardInsightsDto insights = dashboardInsights(resolvedPeriod, visibleProjectIds);
        return new DashboardSummaryDto(overview.period(), overview.metrics(), insights.deliveryTrend(),
                insights.riskDistribution(), insights.changeTypes(), insights.departmentLoads(), overview.shortcuts());
    }

    // 分页查询仪表盘项目。
    @Override
    public PageResult<ProjectDto> dashboardProjects(PageQuery pageQuery) {
        return projects.list(null, null, null, null, null, null, pageQuery);
    }

    // 分页查询仪表盘严重风险。
    @Override
    public PageResult<RiskDto> dashboardCriticalRisks(PageQuery pageQuery) {
        PageQuery page = PageSupport.normalize(pageQuery);
        List<Long> visibleProjectIds = dataScope.visibleProjectIds();
        if (visibleProjectIds != null && visibleProjectIds.isEmpty()) {
            return PageResult.empty(page);
        }
        StringBuilder from = new StringBuilder(" FROM nso_risk r JOIN nso_project p ON p.id=r.project_id "
                + "AND p.tenant_id=r.tenant_id AND p.deleted=0 WHERE r.tenant_id=? AND r.deleted=0 "
                + "AND r.status<>'CLOSED' AND r.level IN ('SERIOUS','HIGH')");
        List<Object> params = new ArrayList<>(List.of(TenantContext.tenantId()));
        appendProjectScope(from, params, "r.project_id", visibleProjectIds);
        long total = countRows("SELECT COUNT(*)" + from, params);
        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(page.pageSizeValue());
        pageParams.add(page.offset());
        List<RiskDto> rows = jdbc.query("SELECT r.id,r.project_id,p.project_no,r.level,r.score,r.reasons,r.suggestion,r.status"
                        + from + " ORDER BY r.score DESC,r.calculated_at DESC,r.id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new RiskDto(rs.getLong("id"), rs.getLong("project_id"), rs.getString("project_no"),
                        rs.getString("level"), (Integer) rs.getObject("score"), riskReasons(rs.getString("reasons")),
                        rs.getString("suggestion"), rs.getString("status")), pageParams.toArray());
        return new PageResult<>(rows, total, page.pageNoValue(), page.pageSizeValue());
    }

    // 分页查询仪表盘待办任务。
    @Override
    public PageResult<TaskDto> dashboardTodos(PageQuery pageQuery) {
        if (TenantContext.userId() == null) {
            return PageResult.empty(PageSupport.normalize(pageQuery));
        }
        return dashboardTasks(pageQuery, "t.assignee_id=? AND t.status NOT IN ('DONE','CANCELLED','COMPLETED')",
                List.of(TenantContext.userId()));
    }

    // 分页查询仪表盘临期任务。
    @Override
    public PageResult<TaskDto> dashboardDueTasks(PageQuery pageQuery) {
        return dashboardTasks(pageQuery, "p.status NOT IN ('COMPLETED','CLOSED','ARCHIVED','CANCELLED') "
                        + "AND t.plan_finish IS NOT NULL AND t.plan_finish<=? "
                        + "AND t.status NOT IN ('DONE','CANCELLED','COMPLETED')",
                List.of(java.sql.Date.valueOf(LocalDate.now().plusDays(3))));
    }

    // 分页查询仪表盘未读消息。
    @Override
    public PageResult<MessageDto> dashboardUnreadMessages(PageQuery pageQuery) {
        return messages.list("UNREAD", pageQuery);
    }

    // 执行全局搜索。
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

    // 分页执行全局搜索。
    @Override
    public PageResult<SearchResultItem> search(String keyword, PageQuery pageQuery) {
        return PageSupport.slice(search(keyword).list(), pageQuery);
    }

    // 查询报表概览。
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
                    throw new com.nso.shared.exception.BusinessException("自定义统计周期必须包含开始日期和结束日期");
                }
                start = startDate;
                end = endDate;
            }
            default -> throw new com.nso.shared.exception.BusinessException("不支持的统计周期：" + normalized);
        }
        if (end.isBefore(start)) {
            throw new com.nso.shared.exception.BusinessException("统计结束日期不能早于开始日期");
        }
        if (ChronoUnit.DAYS.between(start, end) > 365) {
            throw new com.nso.shared.exception.BusinessException("自定义统计周期不能超过 366 天");
        }
        return new DashboardPeriodDto(normalized, start, end);
    }

    private DashboardOverviewDto dashboardOverview(DashboardPeriodDto period, List<Long> visibleProjectIds) {
        LocalDate today = LocalDate.now();
        Map<String, Number> metrics = new LinkedHashMap<>();
        metrics.put("activeProjects", scopedCount("nso_project", "id",
                "status NOT IN ('COMPLETED','CLOSED','ARCHIVED','CANCELLED')", List.of(), visibleProjectIds));
        metrics.put("delayedProjects", scopedCount("nso_project", "id",
                "target_date IS NOT NULL AND target_date<? AND status NOT IN ('COMPLETED','CLOSED','ARCHIVED','CANCELLED')",
                List.of(java.sql.Date.valueOf(today)), visibleProjectIds));
        metrics.put("pendingSamples", scopedCount("nso_sample", "project_id",
                "status IN ('WAITING_CONFIRM','PENDING_CONFIRM','WAIT_CUSTOMER_CONFIRM')", List.of(), visibleProjectIds));
        metrics.put("pendingDocuments", scopedCount("nso_document_version", "project_id", "status='DRAFT'", List.of(), visibleProjectIds));
        metrics.put("openChanges", scopedCount("nso_change_order", "project_id", "status<>'CLOSED'", List.of(), visibleProjectIds));
        metrics.put("reworkQuantity", reworkQuantity(period, visibleProjectIds));
        metrics.put("highRisks", scopedCount("nso_risk", "project_id",
                "status<>'CLOSED' AND level IN ('HIGH','SERIOUS')", List.of(), visibleProjectIds));
        metrics.put("dueTasks", scopedCount("nso_task", "project_id",
                "plan_finish IS NOT NULL AND plan_finish<=? AND status NOT IN ('DONE','CANCELLED','COMPLETED')",
                List.of(java.sql.Date.valueOf(today.plusDays(3))), visibleProjectIds));
        metrics.put("unreadMessages", unreadMessageCount());
        metrics.put("personalTodos", TenantContext.userId() == null ? 0
                : scopedCount("nso_task", "project_id", "assignee_id=? AND status NOT IN ('DONE','CANCELLED','COMPLETED')",
                        List.of(TenantContext.userId()), visibleProjectIds));
        return new DashboardOverviewDto(period, metrics, shortcuts());
    }

    private DashboardInsightsDto dashboardInsights(DashboardPeriodDto period, List<Long> visibleProjectIds) {
        return new DashboardInsightsDto(period, deliveryTrend(period, visibleProjectIds), riskDistribution(visibleProjectIds),
                changeTypes(period, visibleProjectIds), departmentLoads(visibleProjectIds));
    }

    private long scopedCount(String table, String projectColumn, String condition, List<Object> conditionParams,
                             List<Long> visibleProjectIds) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ").append(table)
                .append(" WHERE tenant_id=? AND deleted=0");
        List<Object> params = new ArrayList<>(List.of(TenantContext.tenantId()));
        if (condition != null && !condition.isBlank()) {
            sql.append(" AND ").append(condition);
            params.addAll(conditionParams);
        }
        appendProjectScope(sql, params, projectColumn, visibleProjectIds);
        return countRows(sql.toString(), params);
    }

    private long unreadMessageCount() {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM nso_message WHERE tenant_id=? AND deleted=0 AND status='UNREAD'");
        List<Object> params = new ArrayList<>(List.of(TenantContext.tenantId()));
        if (TenantContext.userId() != null) {
            sql.append(" AND receiver_id=?");
            params.add(TenantContext.userId());
        }
        return countRows(sql.toString(), params);
    }

    private PageResult<TaskDto> dashboardTasks(PageQuery pageQuery, String condition, List<Object> conditionParams) {
        PageQuery page = PageSupport.normalize(pageQuery);
        List<Long> visibleProjectIds = dataScope.visibleProjectIds();
        if (visibleProjectIds != null && visibleProjectIds.isEmpty()) {
            return PageResult.empty(page);
        }
        StringBuilder from = new StringBuilder(" FROM nso_task t JOIN nso_project p ON p.id=t.project_id "
                + "AND p.tenant_id=t.tenant_id AND p.deleted=0 WHERE t.tenant_id=? AND t.deleted=0 AND ")
                .append(condition);
        List<Object> params = new ArrayList<>(List.of(TenantContext.tenantId()));
        params.addAll(conditionParams);
        appendProjectScope(from, params, "t.project_id", visibleProjectIds);
        long total = countRows("SELECT COUNT(*)" + from, params);
        List<Object> pageParams = new ArrayList<>(params);
        pageParams.add(page.pageSizeValue());
        pageParams.add(page.offset());
        List<TaskDto> rows = jdbc.query("SELECT t.id,t.project_id,p.project_no,t.task_no,t.task_type,t.title,t.referenced_version,"
                        + "t.status,t.responsible_name,t.plan_start,t.plan_finish,t.block_reason,t.version,t.assignee_id" + from
                        + " ORDER BY t.plan_finish ASC,t.id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> new TaskDto(rs.getLong("id"), rs.getLong("project_id"), rs.getString("project_no"),
                        rs.getString("task_no"), rs.getString("task_type"), rs.getString("title"),
                        rs.getString("referenced_version"), rs.getString("status"), rs.getString("responsible_name"),
                        localDate(rs.getDate("plan_start")), localDate(rs.getDate("plan_finish")),
                        rs.getString("block_reason"), (Integer) rs.getObject("version"),
                        rs.getObject("assignee_id", Long.class)), pageParams.toArray());
        return new PageResult<>(rows, total, page.pageNoValue(), page.pageSizeValue());
    }

    private long countRows(String sql, List<Object> params) {
        Number value = jdbc.queryForObject(sql, Number.class, params.toArray());
        return value == null ? 0 : value.longValue();
    }

    private LocalDate localDate(java.sql.Date value) {
        return value == null ? null : value.toLocalDate();
    }

    private List<String> riskReasons(String raw) {
        return raw == null || raw.length() < 2 ? List.of()
                : List.of(raw.substring(1, raw.length() - 1).replace("\"", "").split(","));
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
        applyDailyCounts(daily, "actual_finish", "status = 'DONE' AND actual_finish IS NOT NULL", 1, period, visibleProjectIds);
        return daily.entrySet().stream()
                .map(entry -> new DashboardTrendPointDto(entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                .toList();
    }

    private void applyDailyCounts(Map<LocalDate, long[]> daily, String sourceDateColumn, String condition, int index,
                                    DashboardPeriodDto period, List<Long> visibleProjectIds) {
        StringBuilder sql = new StringBuilder("SELECT DATE(" + sourceDateColumn + ") AS stat_date, COUNT(*) AS value "
                + "FROM nso_task WHERE tenant_id=? AND deleted=0 AND " + sourceDateColumn + ">=? AND " + sourceDateColumn + "<? AND " + condition);
        List<Object> params = new ArrayList<>(List.of(TenantContext.tenantId(), java.sql.Date.valueOf(period.startDate()),
                java.sql.Date.valueOf(period.endDate().plusDays(1))));
        appendProjectScope(sql, params, "project_id", visibleProjectIds);
        sql.append(" GROUP BY DATE(").append(sourceDateColumn).append(')');
        jdbc.query(sql.toString(), (rs, rowNum) -> {
            java.sql.Date rawDate = rs.getDate("stat_date");
            if (rawDate != null && daily.containsKey(rawDate.toLocalDate())) {
                daily.get(rawDate.toLocalDate())[index] = rs.getLong("value");
            }
            return null;
        }, params.toArray());
    }

    private List<DashboardChartItemDto> riskDistribution(List<Long> visibleProjectIds) {
        StringBuilder sql = new StringBuilder("SELECT level,COUNT(*) AS value FROM nso_risk "
                + "WHERE tenant_id=? AND deleted=0 AND status<>'CLOSED'");
        List<Object> params = new ArrayList<>(List.of(TenantContext.tenantId()));
        appendProjectScope(sql, params, "project_id", visibleProjectIds);
        sql.append(" GROUP BY level");
        Map<String, Long> grouped = new LinkedHashMap<>();
        jdbc.query(sql.toString(), (rs, rowNum) -> {
            grouped.put(rs.getString("level"), rs.getLong("value"));
            return null;
        }, params.toArray());
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

    private List<Map<String, Object>> shortcuts() {
        return List.of(
                Map.<String, Object>of("name", "新建项目", "route", "/projects", "permission", "project:create"),
                Map.<String, Object>of("name", "上传图纸", "route", "/documents", "permission", "document:upload"),
                Map.<String, Object>of("name", "样品确认", "route", "/samples", "permission", "sample:submit"),
                Map.<String, Object>of("name", "发起变更", "route", "/changes", "permission", "change:create"));
    }

    private List<ProjectMilestoneDto> milestones(String stage, String projectStatus, List<TimelineItem> timeline) {
        List<String> codes = List.of("REQUIREMENT", "TECHNICAL", "SAMPLE", "EXECUTION", "DELIVERY");
         List<String> labels = List.of("需求评审", "图纸发布", "样品确认", "正式投产", "交付");
        int current = stageIndex(stage);
        if (Set.of("COMPLETED", "ARCHIVED").contains(projectStatus)) current = codes.size();
        List<ProjectMilestoneDto> rows = new ArrayList<>();
        for (int index = 0; index < codes.size(); index++) {
            String code = codes.get(index);
            String state = index < current ? "COMPLETED" : index == current ? "CURRENT" : "PENDING";
            LocalDateTime occurred = "COMPLETED".equals(state) ? completedMilestoneTime(code, timeline) : null;
            rows.add(new ProjectMilestoneDto(code, labels.get(index), state, occurred));
        }
        return rows;
    }

    private LocalDateTime completedMilestoneTime(String code, List<TimelineItem> timeline) {
        Set<String> completionEvents = switch (code) {
            case "REQUIREMENT" -> Set.of("PROJECT_APPROVE_REVIEW");
            case "TECHNICAL" -> Set.of("TECHNICAL_PACKAGE_PUBLISHED", "PROJECT_TECHNICAL_PACKAGE_PUBLISHED");
            case "SAMPLE" -> Set.of("SAMPLE_CONFIRMED");
            case "EXECUTION" -> Set.of("DELIVERY_CREATED", "PROJECT_DELIVERY_CREATED");
            case "DELIVERY" -> Set.of("PROJECT_COMPLETE");
            default -> Set.of();
        };
        return timeline.stream().filter(item -> completionEvents.contains(item.eventType()))
                .map(TimelineItem::occurredAt).filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo).orElse(null);
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
