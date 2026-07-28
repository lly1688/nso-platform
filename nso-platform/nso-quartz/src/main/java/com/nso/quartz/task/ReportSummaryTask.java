package com.nso.quartz.task;

import java.util.Map;
import com.nso.business.report.service.IReportService;
import org.springframework.stereotype.Component;

@Component
public class ReportSummaryTask {
    private final IReportService reportService;

    public ReportSummaryTask(IReportService reportService) {
        this.reportService = reportService;
    }

    public Map<String, Object> execute() {
        var overview = reportService.overview();
        return Map.of(
                "task", "reportSummaryTask",
                "metrics", overview.metrics(),
                "riskGroups", overview.riskLevels().size());
    }
}
