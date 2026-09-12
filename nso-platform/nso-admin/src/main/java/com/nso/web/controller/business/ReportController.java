package com.nso.web.controller.business;

import com.nso.shared.core.domain.AjaxResult;
import com.nso.business.report.service.IReportService;
import com.nso.business.core.NsoDtos.PageQuery;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin")

// 报表统计接口 负责数据报表的生成和查询
public class ReportController {

    // 报表服务
    private final IReportService reportService;

    public ReportController(IReportService reportService) {

        this.reportService = reportService;
    }

    // 查询报表概览。
    @GetMapping("/reports/overview")
    @PreAuthorize("hasAnyAuthority('nso:report:view', 'report:view')")
    public AjaxResult<?> reportOverview() {

        return AjaxResult.success(reportService.overview());
    }

    // 查询当前用户工作台。
    @GetMapping("/workbench")
    @PreAuthorize("hasAnyAuthority('nso:dashboard:view', 'dashboard:view')")
    public AjaxResult<?> workbench() {

        return AjaxResult.success(reportService.workbench());
    }

    // 查询仪表盘。
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyAuthority('nso:dashboard:view', 'dashboard:view')")
    public AjaxResult<?> dashboard(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return AjaxResult.success(reportService.dashboard(period, startDate, endDate));
    }

    // 查询仪表盘首屏概览。
    @GetMapping("/dashboard/overview")
    @PreAuthorize("hasAnyAuthority('nso:dashboard:view', 'dashboard:view')")
    public AjaxResult<?> dashboardOverview(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return AjaxResult.success(reportService.dashboardOverview(period, startDate, endDate));
    }

    // 查询仪表盘分析数据。
    @GetMapping("/dashboard/insights")
    @PreAuthorize("hasAnyAuthority('nso:dashboard:view', 'dashboard:view')")
    public AjaxResult<?> dashboardInsights(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return AjaxResult.success(reportService.dashboardInsights(period, startDate, endDate));
    }

    // 查询仪表盘摘要。
    @GetMapping("/dashboard/summary")
    @PreAuthorize("hasAnyAuthority('nso:dashboard:view', 'dashboard:view')")
    public AjaxResult<?> dashboardSummary(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return AjaxResult.success(reportService.dashboardSummary(period, startDate, endDate));
    }

    // 查询仪表盘项目。
    @GetMapping("/dashboard/projects")
    @PreAuthorize("hasAnyAuthority('nso:dashboard:view', 'dashboard:view')")
    public AjaxResult<?> dashboardProjects(@RequestParam(required = false) Integer pageNo,
                                            @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(reportService.dashboardProjects(new PageQuery(pageNo, pageSize)));
    }

    // 查询仪表盘严重风险。
    @GetMapping("/dashboard/actions/risks")
    @PreAuthorize("hasAnyAuthority('nso:dashboard:view', 'dashboard:view')")
    public AjaxResult<?> dashboardRisks(@RequestParam(required = false) Integer pageNo,
                                        @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(reportService.dashboardCriticalRisks(new PageQuery(pageNo, pageSize)));
    }

    // 查询仪表盘待办任务。
    @GetMapping("/dashboard/actions/todos")
    @PreAuthorize("hasAnyAuthority('nso:dashboard:view', 'dashboard:view')")
    public AjaxResult<?> dashboardTodos(@RequestParam(required = false) Integer pageNo,
                                        @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(reportService.dashboardTodos(new PageQuery(pageNo, pageSize)));
    }

    // 查询仪表盘临期任务。
    @GetMapping("/dashboard/actions/due")
    @PreAuthorize("hasAnyAuthority('nso:dashboard:view', 'dashboard:view')")
    public AjaxResult<?> dashboardDueTasks(@RequestParam(required = false) Integer pageNo,
                                           @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(reportService.dashboardDueTasks(new PageQuery(pageNo, pageSize)));
    }

    // 查询仪表盘未读消息。
    @GetMapping("/dashboard/actions/messages")
    @PreAuthorize("hasAnyAuthority('nso:dashboard:view', 'dashboard:view')")
    public AjaxResult<?> dashboardUnreadMessages(@RequestParam(required = false) Integer pageNo,
                                                 @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(reportService.dashboardUnreadMessages(new PageQuery(pageNo, pageSize)));
    }

    // 执行全局搜索。
    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('nso:project:view', 'project:view')")
    public AjaxResult<?> search(@RequestParam(required = false) String keyword,
                                @RequestParam(required = false) Integer pageNo,
                                @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(reportService.search(keyword, new PageQuery(pageNo, pageSize)));
    }

    // 查询项目综合详情。
    @GetMapping("/projects/{id}")
    @PreAuthorize("hasAnyAuthority('nso:project:view', 'project:view')")
    public AjaxResult<?> projectDetail(@PathVariable Long id) {
        return AjaxResult.success(reportService.projectDetail(id));
    }

    // 查询项目工作区。
    @GetMapping("/projects/{id}/workspace")
    @PreAuthorize("hasAnyAuthority('nso:project:view', 'project:view')")
    public AjaxResult<?> projectWorkspace(@PathVariable Long id) {
        return AjaxResult.success(reportService.projectWorkspace(id));
    }

    // 查询项目工作区摘要。
    @GetMapping("/projects/{id}/workspace/summary")
    @PreAuthorize("hasAnyAuthority('nso:project:view', 'project:view')")
    public AjaxResult<?> projectWorkspaceSummary(@PathVariable Long id) {
        return AjaxResult.success(reportService.projectWorkspaceSummary(id));
    }

    // 查询项目脉搏指标。
    @GetMapping("/projects/{id}/workspace/pulse")
    @PreAuthorize("hasAnyAuthority('nso:project:view', 'project:view')")
    public AjaxResult<?> projectPulse(@PathVariable Long id) {
        return AjaxResult.success(reportService.projectPulse(id));
    }

    // 查询项目工作区时间线。
    @GetMapping("/projects/{id}/workspace/timeline")
    @PreAuthorize("hasAnyAuthority('nso:project:view', 'project:view')")
    public AjaxResult<?> projectWorkspaceTimeline(@PathVariable Long id,
                                                  @RequestParam(required = false) Integer pageNo,
                                                  @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(reportService.projectWorkspaceTimeline(id, new PageQuery(pageNo, pageSize)));
    }

    // 查询项目工作区版本冲突。
    @GetMapping("/projects/{id}/workspace/version-conflicts")
    @PreAuthorize("hasAnyAuthority('nso:project:view', 'project:view')")
    public AjaxResult<?> projectWorkspaceVersionConflicts(@PathVariable Long id,
                                                          @RequestParam(required = false) Integer pageNo,
                                                          @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(reportService.projectWorkspaceVersionConflicts(id, new PageQuery(pageNo, pageSize)));
    }

    // 查询项目工作区责任人缺口。
    @GetMapping("/projects/{id}/workspace/owner-gaps")
    @PreAuthorize("hasAnyAuthority('nso:project:view', 'project:view')")
    public AjaxResult<?> projectWorkspaceOwnerGaps(@PathVariable Long id,
                                                   @RequestParam(required = false) Integer pageNo,
                                                   @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(reportService.projectWorkspaceOwnerGaps(id, new PageQuery(pageNo, pageSize)));
    }
}
