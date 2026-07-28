package com.nso.business.report.service;

import com.nso.business.core.NsoDtos.*;
import java.time.LocalDate;

public interface IReportService {
    ProjectDetail projectDetail(Long projectId);
    ProjectWorkspaceDto projectWorkspace(Long projectId);
    WorkbenchDto workbench();
    DashboardDto dashboard(String period, LocalDate startDate, LocalDate endDate);
    PageResult<SearchResultItem> search(String keyword);
    ReportOverview overview();
}
