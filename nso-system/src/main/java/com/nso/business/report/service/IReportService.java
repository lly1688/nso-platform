package com.nso.business.report.service;

import com.nso.business.core.NsoDtos.*;

import java.time.LocalDate;

/**
 * 报表与工作台查询服务接口。
 */
public interface IReportService {

    /**
     * 查询项目综合详情。
     *
     * @param projectId 项目编号
     * @return 项目综合详情
     */
    ProjectDetail projectDetail(Long projectId);

    /**
     * 查询项目工作区。
     *
     * @param projectId 项目编号
     * @return 项目工作区数据
     */
    ProjectWorkspaceDto projectWorkspace(Long projectId);

    /**
     * 查询项目工作区摘要。
     *
     * @param projectId 项目编号
     * @return 工作区摘要
     */
    ProjectWorkspaceSummaryDto projectWorkspaceSummary(Long projectId);

    /**
     * 查询项目脉搏指标。
     *
     * @param projectId 项目编号
     * @return 项目脉搏指标
     */
    ProjectPulseDto projectPulse(Long projectId);

    /**
     * 分页查询项目时间线。
     *
     * @param projectId 项目编号
     * @param pageQuery 分页参数
     * @return 时间线分页结果
     */
    PageResult<TimelineItem> projectWorkspaceTimeline(Long projectId, PageQuery pageQuery);

    /**
     * 分页查询项目版本冲突。
     *
     * @param projectId 项目编号
     * @param pageQuery 分页参数
     * @return 版本冲突分页结果
     */
    PageResult<VersionConflictDto> projectWorkspaceVersionConflicts(Long projectId, PageQuery pageQuery);

    /**
     * 分页查询项目责任人缺口。
     *
     * @param projectId 项目编号
     * @param pageQuery 分页参数
     * @return 责任人缺口分页结果
     */
    PageResult<ProjectOwnerGapDto> projectWorkspaceOwnerGaps(Long projectId, PageQuery pageQuery);

    /**
     * 查询当前用户工作台。
     *
     * @return 工作台数据
     */
    WorkbenchDto workbench();

    /**
     * 查询仪表盘。
     *
     * @param period 统计周期
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 仪表盘数据
     */
    DashboardDto dashboard(String period, LocalDate startDate, LocalDate endDate);

    /**
     * 查询仪表盘首屏概览。
     *
     * @param period 统计周期
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 轻量概览指标
     */
    DashboardOverviewDto dashboardOverview(String period, LocalDate startDate, LocalDate endDate);

    /**
     * 查询仪表盘分析数据。
     *
     * @param period 统计周期
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 趋势与分布数据
     */
    DashboardInsightsDto dashboardInsights(String period, LocalDate startDate, LocalDate endDate);

    /**
     * 查询仪表盘摘要。
     *
     * @param period 统计周期
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 仪表盘摘要
     */
    DashboardSummaryDto dashboardSummary(String period, LocalDate startDate, LocalDate endDate);

    /**
     * 分页查询仪表盘项目。
     *
     * @param pageQuery 分页参数
     * @return 项目分页结果
     */
    PageResult<ProjectDto> dashboardProjects(PageQuery pageQuery);

    /**
     * 分页查询仪表盘严重风险。
     *
     * @param pageQuery 分页参数
     * @return 风险分页结果
     */
    PageResult<RiskDto> dashboardCriticalRisks(PageQuery pageQuery);

    /**
     * 分页查询仪表盘待办任务。
     *
     * @param pageQuery 分页参数
     * @return 任务分页结果
     */
    PageResult<TaskDto> dashboardTodos(PageQuery pageQuery);

    /**
     * 分页查询仪表盘临期任务。
     *
     * @param pageQuery 分页参数
     * @return 任务分页结果
     */
    PageResult<TaskDto> dashboardDueTasks(PageQuery pageQuery);

    /**
     * 分页查询仪表盘未读消息。
     *
     * @param pageQuery 分页参数
     * @return 消息分页结果
     */
    PageResult<MessageDto> dashboardUnreadMessages(PageQuery pageQuery);

    /**
     * 全局搜索业务数据。
     *
     * @param keyword 搜索关键字
     * @return 搜索结果
     */
    PageResult<SearchResultItem> search(String keyword);

    /**
     * 分页执行全局搜索。
     *
     * @param keyword 搜索关键字
     * @param pageQuery 分页参数
     * @return 搜索结果
     */
    PageResult<SearchResultItem> search(String keyword, PageQuery pageQuery);

    /**
     * 查询报表概览。
     *
     * @return 报表概览
     */
    ReportOverview overview();
}
