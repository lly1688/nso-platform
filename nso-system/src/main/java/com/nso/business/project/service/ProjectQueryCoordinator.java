package com.nso.business.project.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nso.business.core.NsoDtos.CustomerProjectDto;
import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.business.core.NsoDtos.ProjectDto;
import com.nso.business.core.NsoDtos.ProjectStatsDto;
import com.nso.business.core.ProjectProgressSemantics;
import com.nso.business.core.PageSupport;
import com.nso.business.core.TenantContext;
import com.nso.business.project.domain.Project;
import com.nso.business.project.mapper.ProjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// 项目查询协作组件。 聚合可见范围、统计口径和变更计数，不承担项目状态迁移或成员维护。
@Component

// 项目查询协调器 协调项目的多维度查询与统计
public class ProjectQueryCoordinator {
    // 项目数据映射
    private final ProjectMapper projects;
    // 项目数据范围
    private final ProjectDataScope dataScope;
    // JDBC模板
    private final JdbcTemplate jdbc;

    public ProjectQueryCoordinator(ProjectMapper projects, ProjectDataScope dataScope, JdbcTemplate jdbc) {
        this.projects = projects;
        this.dataScope = dataScope;
        this.jdbc = jdbc;
    }

    // 查询当前用户可见项目。
    public PageResult<ProjectDto> list(String keyword) {
        List<Long> visibleIds = dataScope.visibleProjectIds();
        if (visibleIds != null && visibleIds.isEmpty()) {
            return new PageResult<>(List.of(), 0);
        }
        List<Project> rows = projects.selectList(Wrappers.<Project>lambdaQuery()
                .in(visibleIds != null, Project::getId, visibleIds == null ? List.of() : visibleIds)
                .and(keyword != null && !keyword.isBlank(), query -> query.like(Project::getProjectNo, keyword)
                        .or().like(Project::getCustomerName, keyword)
                        .or().like(Project::getProductName, keyword))
                .orderByDesc(Project::getId));
        List<Long> projectIds = rows.stream().map(Project::getId).toList();
        Map<Long, Integer> changeCounts = projectChangeCounts(projectIds);
        Map<Long, ProjectInsight> insights = projectInsights(projectIds);
        List<ProjectDto> result = rows.stream()
                .map(project -> toDto(project, changeCounts.getOrDefault(project.getId(), 0), insights.getOrDefault(project.getId(), ProjectInsight.EMPTY)))
                .toList();
        return new PageResult<>(result, result.size());
    }

    // 按条件查询当前用户可见项目。
    public PageResult<ProjectDto> list(String keyword, String stage, String status, String riskLevel,
                                       String dueState, String quickFilter, PageQuery pageQuery) {
        PageQuery page = PageSupport.normalize(pageQuery);
        List<Long> visibleIds = dataScope.visibleProjectIds();
        if (visibleIds != null && visibleIds.isEmpty()) {
            return PageResult.empty(page);
        }
        LocalDate dueDate = LocalDate.now();
        boolean dueSoon = "DUE".equalsIgnoreCase(dueState) || "DUE".equalsIgnoreCase(quickFilter);
        boolean overdue = "OVERDUE".equalsIgnoreCase(dueState) || "OVERDUE".equalsIgnoreCase(quickFilter);
        Page<Project> entityPage = projects.selectPage(PageSupport.page(page), Wrappers.<Project>lambdaQuery()
                .in(visibleIds != null, Project::getId, visibleIds == null ? List.of() : visibleIds)
                .and(hasText(keyword), query -> query.like(Project::getProjectNo, keyword)
                        .or().like(Project::getCustomerName, keyword)
                        .or().like(Project::getProductName, keyword))
                .eq(hasText(stage), Project::getStage, stage)
                .eq(hasText(status), Project::getStatus, status)
                .eq(hasText(riskLevel), Project::getRiskLevel, riskLevel)
                .eq("RISK".equalsIgnoreCase(quickFilter), Project::getRiskLevel, "HIGH")
                .notIn("ACTIVE".equalsIgnoreCase(quickFilter), Project::getStatus, List.of("CLOSED", "ARCHIVED"))
                .le(dueSoon, Project::getTargetDate, dueDate.plusDays(3))
                .lt(overdue, Project::getTargetDate, dueDate)
                .orderByDesc(Project::getId));
        List<Long> projectIds = entityPage.getRecords().stream().map(Project::getId).toList();
        Map<Long, Integer> changeCounts = projectChangeCounts(projectIds);
        Map<Long, ProjectInsight> insights = projectInsights(projectIds);
        return PageSupport.result(entityPage, page,
                project -> toDto(project, changeCounts.getOrDefault(project.getId(), 0), insights.getOrDefault(project.getId(), ProjectInsight.EMPTY)));
    }

    // 查询客户可见项目。
    public PageResult<CustomerProjectDto> customerProjects(String keyword) {
        PageResult<ProjectDto> page = list(keyword);
        List<CustomerProjectDto> rows = page.list().stream()
                .map(project -> new CustomerProjectDto(project.id(),
                        project.projectNo(),
                        project.productName(),
                        project.targetDate(),
                        project.status(),
                        project.stage(),
                        project.sampleStatus()))
                .toList();
        return new PageResult<>(rows, rows.size());
    }

    // 分页查询客户可见项目。
    public PageResult<CustomerProjectDto> customerProjects(String keyword, PageQuery pageQuery) {
        PageResult<ProjectDto> page = list(keyword, null, null, null, null, null, pageQuery);
        List<CustomerProjectDto> rows = page.list().stream()
                .map(project -> new CustomerProjectDto(project.id(), project.projectNo(), project.productName(),
                        project.targetDate(), project.status(), project.stage(), project.sampleStatus()))
                .toList();
        return new PageResult<>(rows, page.total(), page.pageNo(), page.pageSize());
    }

    // 汇总项目列表统计数据。
    public ProjectStatsDto stats(String keyword, String stage, String status, String riskLevel, String dueState,
                                 String quickFilter, PageQuery attentionPage) {
        List<ProjectDto> rows = list(keyword).list().stream()
                .filter(project -> !hasText(stage) || stage.equals(project.stage()))
                .filter(project -> !hasText(status) || status.equals(project.status()))
                .filter(project -> !hasText(riskLevel) || riskLevel.equals(project.riskLevel()))
                .filter(project -> matchesQuickFilter(project, dueState, quickFilter))
                .toList();
        long inProgress = rows.stream().filter(project -> !Set.of("COMPLETED", "ARCHIVED", "CANCELLED").contains(project.status())).count();
        long due = rows.stream().filter(project -> inProgress(project) && project.targetDate() != null && project.daysLeft() <= 3).count();
        long highRisk = rows.stream().filter(project -> Set.of("SERIOUS", "HIGH").contains(project.riskLevel())).count();
        Map<String, Long> stageDistribution = rows.stream().collect(Collectors.groupingBy(
                project -> project.stage() == null ? "UNKNOWN" : project.stage(), LinkedHashMap::new, Collectors.counting()));
        List<ProjectDto> attention = rows.stream()
                .filter(this::requiresAttention)
                .sorted(Comparator.comparing(ProjectDto::daysLeft, Comparator.nullsLast(Long::compareTo)))
                .toList();
        return new ProjectStatsDto(rows.size(), inProgress, due, highRisk, stageDistribution,
                PageSupport.slice(attention, PageSupport.normalize(attentionPage)));
    }

    // 转换项目数据。
    public ProjectDto toDto(Project source) {
        return toDto(source, projectChangeCounts(List.of(source.getId())).getOrDefault(source.getId(), 0),
                projectInsights(List.of(source.getId())).getOrDefault(source.getId(), ProjectInsight.EMPTY));
    }

    private boolean matchesQuickFilter(ProjectDto project, String dueState, String quickFilter) {
        String filter = hasText(quickFilter) ? quickFilter : dueState;
        if (!hasText(filter) || "ALL".equalsIgnoreCase(filter)) {
            return true;
        }
        if ("ACTIVE".equalsIgnoreCase(filter)) {
            return inProgress(project);
        }
        if ("RISK".equalsIgnoreCase(filter)) {
            return Set.of("SERIOUS", "HIGH").contains(project.riskLevel());
        }
        if ("DUE".equalsIgnoreCase(filter) || "DUE_SOON".equalsIgnoreCase(filter)) {
            return inProgress(project) && project.targetDate() != null && project.daysLeft() >= 0 && project.daysLeft() <= 3;
        }
        if ("OVERDUE".equalsIgnoreCase(filter)) {
            return inProgress(project) && project.targetDate() != null && project.daysLeft() < 0;
        }
        if ("NO_DATE".equalsIgnoreCase(filter)) {
            return inProgress(project) && project.targetDate() == null;
        }
        return true;
    }

    private boolean inProgress(ProjectDto project) {
        return project != null && !Set.of("COMPLETED", "ARCHIVED", "CANCELLED").contains(project.status());
    }

    private boolean requiresAttention(ProjectDto project) {
        return inProgress(project)
                && ((project.targetDate() != null && project.daysLeft() < 0)
                || Set.of("SERIOUS", "HIGH").contains(project.riskLevel())
                || (project.targetDate() != null && project.daysLeft() <= 3)
                || Set.of("DRAFT", "REVIEWING").contains(project.status())
                || project.targetDate() == null);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private ProjectDto toDto(Project source, int changeCount, ProjectInsight insight) {
        long daysLeft = source.getTargetDate() == null ? 0 : ChronoUnit.DAYS.between(LocalDate.now(), source.getTargetDate());
        ProjectProgressSemantics.Progress progress = ProjectProgressSemantics.describe(source.getStage(), source.getStatus(), source.getTargetDate(), insight.blockerCount(), insight.pendingDecisionCount());
        return new ProjectDto(source.getId(), source.getProjectNo(), source.getCustomerId(), source.getCustomerName(), source.getProductName(),
                source.getQuantity(), source.getTargetDate(), source.getOwnerName(), source.getStatus(), source.getStage(), source.getPriority(),
                source.getRiskLevel(), source.getSampleStatus(), changeCount, daysLeft, source.getVersion(), source.getOwnerUserId(),
                insight.blockerCount(), insight.pendingDecisionCount(), progress.recommendedAction().label(), progress.recommendedAction().route());
    }

    private Map<Long, Integer> projectChangeCounts(List<Long> projectIds) {
        if (projectIds.isEmpty()) {
            return Map.of();
        }
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("tenantId", TenantContext.tenantId())
                .addValue("projectIds", projectIds);
        return new NamedParameterJdbcTemplate(jdbc).query("""
                SELECT project_id, COUNT(*) AS change_count
                FROM nso_change_order
                WHERE tenant_id = :tenantId AND deleted = 0 AND project_id IN (:projectIds)
                GROUP BY project_id
                """, parameters, resultSet -> {
            Map<Long, Integer> result = new LinkedHashMap<>();
            while (resultSet.next()) {
                result.put(resultSet.getLong("project_id"), resultSet.getInt("change_count"));
            }
            return result;
        });
    }

    private Map<Long, ProjectInsight> projectInsights(List<Long> projectIds) {
        if (projectIds.isEmpty()) {
            return Map.of();
        }
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("tenantId", TenantContext.tenantId())
                .addValue("projectIds", projectIds);
        return new NamedParameterJdbcTemplate(jdbc).query("""
                SELECT project_id,
                       SUM(blockers) AS blocker_count,
                       SUM(decisions) AS decision_count
                FROM (
                    SELECT project_id, COUNT(*) AS blockers, 0 AS decisions
                    FROM nso_action_item
                    WHERE tenant_id = :tenantId AND deleted = 0
                      AND action_status IN ('OPEN', 'BLOCKED', 'IN_PROGRESS')
                      AND source_status IN ('BLOCKED', 'OVERDUE')
                      AND project_id IN (:projectIds)
                    GROUP BY project_id
                    UNION ALL
                    SELECT c.project_id, 0 AS blockers, COUNT(*) AS decisions
                    FROM nso_change_approval a
                    JOIN nso_change_order c ON c.id = a.change_id AND c.tenant_id = a.tenant_id
                    WHERE a.tenant_id = :tenantId AND a.decision = 'PENDING'
                      AND c.deleted = 0 AND c.project_id IN (:projectIds)
                    GROUP BY c.project_id
                ) project_signals
                GROUP BY project_id
                """, parameters, resultSet -> {
            Map<Long, ProjectInsight> result = new LinkedHashMap<>();
            while (resultSet.next()) {
                result.put(resultSet.getLong("project_id"), new ProjectInsight(
                        resultSet.getInt("blocker_count"), resultSet.getInt("decision_count")));
            }
            return result;
        });
    }

    private record ProjectInsight(int blockerCount, int pendingDecisionCount) {
        private static final ProjectInsight EMPTY = new ProjectInsight(0, 0);
    }
}
