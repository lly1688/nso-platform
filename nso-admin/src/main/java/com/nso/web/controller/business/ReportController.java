package com.nso.web.controller.business;

import com.nso.common.core.domain.AjaxResult;
import com.nso.business.report.service.IReportService;
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
public class ReportController {

    private final IReportService reportService;

    public ReportController(IReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/reports/overview")
    @PreAuthorize("hasAuthority('report:view')")
    public AjaxResult<?> reportOverview() {
        return AjaxResult.success(reportService.overview());
    }

    @GetMapping("/workbench")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public AjaxResult<?> workbench() {
        return AjaxResult.success(reportService.workbench());
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public AjaxResult<?> dashboard(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return AjaxResult.success(reportService.dashboard(period, startDate, endDate));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('project:view')")
    public AjaxResult<?> search(@RequestParam(required = false) String keyword) {
        return AjaxResult.success(reportService.search(keyword));
    }

    @GetMapping("/projects/{id}")
    @PreAuthorize("hasAuthority('project:view')")
    public AjaxResult<?> projectDetail(@PathVariable Long id) {
        return AjaxResult.success(reportService.projectDetail(id));
    }

    @GetMapping("/projects/{id}/workspace")
    @PreAuthorize("hasAuthority('project:view')")
    public AjaxResult<?> projectWorkspace(@PathVariable Long id) {
        return AjaxResult.success(reportService.projectWorkspace(id));
    }
}
