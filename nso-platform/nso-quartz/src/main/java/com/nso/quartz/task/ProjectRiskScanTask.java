package com.nso.quartz.task;

import java.time.LocalDateTime;
import java.util.Map;

import com.nso.business.project.service.IProjectService;
import com.nso.business.risk.service.IRiskService;

import org.springframework.stereotype.Component;

@Component
public class ProjectRiskScanTask {

    private final IProjectService projectService;
    private final IRiskService riskService;

    public ProjectRiskScanTask(IProjectService projectService, IRiskService riskService) {
        this.projectService = projectService;
        this.riskService = riskService;
    }

    public Map<String, Object> execute() {
        int scanned = projectService.list(null).list().size();
        projectService.list(null).list().forEach(project -> riskService.calculate(project.id()));
        return Map.of(
                "task", "projectRiskScanTask",
                "summary", "已扫描 " + scanned + " 个项目的交期、样品确认和任务阻断风险",
                "scanned", scanned,
                "executedAt", LocalDateTime.now());
    }
}
