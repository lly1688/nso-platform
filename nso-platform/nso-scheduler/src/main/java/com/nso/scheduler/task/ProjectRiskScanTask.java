package com.nso.scheduler.task;

import java.time.LocalDateTime;
import java.util.Map;

import com.nso.business.project.service.IProjectService;
import com.nso.business.risk.service.IRiskService;

import org.springframework.stereotype.Component;

// 项目风险扫描定时任务。
@Component
public class ProjectRiskScanTask {

    // 项目服务
    private final IProjectService projectService;
    // 风险服务
    private final IRiskService riskService;

    public ProjectRiskScanTask(IProjectService projectService, IRiskService riskService) {
        this.projectService = projectService;
        this.riskService = riskService;
    }

    // 恢复过期风险覆盖并重新计算项目风险。
    public Map<String, Object> execute() {
        int restoredOverrides = riskService.restoreExpiredOverrides();
        int scanned = projectService.list(null).list().size();
        projectService.list(null).list().forEach(project -> riskService.calculate(project.id()));
        return Map.of(
                "task", "projectRiskScanTask",
                "summary", "已扫描 " + scanned + " 个项目的交期、样品确认和任务阻断风险",
                "scanned", scanned,
                "restoredOverrides", restoredOverrides,
                "executedAt", LocalDateTime.now());
    }
}
