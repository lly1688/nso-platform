package com.nso.scheduler.task;

import java.util.Map;
import com.nso.business.report.service.IReportService;
import org.springframework.stereotype.Component;

// 报表汇总定时任务。
@Component
public class ReportSummaryTask {

    // 报表服务
    private final IReportService reportService;

    public ReportSummaryTask(IReportService reportService) {
        this.reportService = reportService;
    }

    // 汇总经营概览和风险分组指标。
    public Map<String, Object> execute() {
        var overview = reportService.overview();
        return Map.of(
                "task", "reportSummaryTask",
                "metrics", overview.metrics(),
                "riskGroups", overview.riskLevels().size());
    }
}
