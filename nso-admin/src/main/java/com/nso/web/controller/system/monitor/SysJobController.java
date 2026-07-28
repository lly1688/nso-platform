package com.nso.web.controller.system.monitor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.nso.common.core.domain.AjaxResult;
import com.nso.quartz.task.ChangeTimeoutTask;
import com.nso.quartz.task.MessageRetryTask;
import com.nso.quartz.task.ProjectRiskScanTask;
import com.nso.quartz.task.ReportSummaryTask;
import com.nso.quartz.task.SampleConfirmReminderTask;
import com.nso.quartz.task.TaskDeadlineWarningTask;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/api/v1/admin/monitor")
@PreAuthorize("hasRole('ADMIN')")
public class SysJobController {

    private final ProjectRiskScanTask projectRiskScanTask;
    private final SampleConfirmReminderTask sampleConfirmReminderTask;
    private final ChangeTimeoutTask changeTimeoutTask;
    private final TaskDeadlineWarningTask taskDeadlineWarningTask;
    private final MessageRetryTask messageRetryTask;
    private final ReportSummaryTask reportSummaryTask;

    public SysJobController(ProjectRiskScanTask projectRiskScanTask, SampleConfirmReminderTask sampleConfirmReminderTask,
                            ChangeTimeoutTask changeTimeoutTask, TaskDeadlineWarningTask taskDeadlineWarningTask,
                            MessageRetryTask messageRetryTask, ReportSummaryTask reportSummaryTask) {
        this.projectRiskScanTask = projectRiskScanTask;
        this.sampleConfirmReminderTask = sampleConfirmReminderTask;
        this.changeTimeoutTask = changeTimeoutTask;
        this.taskDeadlineWarningTask = taskDeadlineWarningTask;
        this.messageRetryTask = messageRetryTask;
        this.reportSummaryTask = reportSummaryTask;
    }

    private static final List<Map<String, Object>> WHITE_LIST_JOBS = List.of(
            job(1L, "项目风险扫描", "projectRiskScanTask", "0 0/10 * * * ?", "RUNNING"),
            job(2L, "样品确认催办", "sampleConfirmReminderTask", "0 0 9 * * ?", "RUNNING"),
            job(3L, "变更超时升级", "changeTimeoutTask", "0 0/30 * * * ?", "RUNNING"),
            job(4L, "任务临期提醒", "taskDeadlineWarningTask", "0 0 8 * * ?", "RUNNING"),
            job(5L, "消息重试", "messageRetryTask", "0 0/5 * * * ?", "RUNNING"),
            job(6L, "报表汇总", "reportSummaryTask", "0 10 1 * * ?", "RUNNING"));

    @GetMapping("/jobs")
    public AjaxResult<?> jobs() {
        return AjaxResult.success(Map.of("list", WHITE_LIST_JOBS, "total", WHITE_LIST_JOBS.size()));
    }

    @PostMapping("/jobs/{id}/run")
    public ResponseEntity<AjaxResult<?>> run(@PathVariable Long id) {
        return WHITE_LIST_JOBS.stream()
                .filter(job -> id.equals(job.get("id")))
                .findFirst()
                .<ResponseEntity<AjaxResult<?>>>map(job -> ResponseEntity.ok(AjaxResult.success(runJob(id, job))))
                .orElseGet(() -> ResponseEntity.status(404)
                        .body(AjaxResult.error(404, "JOB_NOT_ALLOWED: 任务不存在或不在白名单中")));
    }

    private Map<String, Object> runJob(Long id, Map<String, Object> job) {
        Map<String, Object> result = switch (id.intValue()) {
            case 1 -> projectRiskScanTask.execute();
            case 2 -> sampleConfirmReminderTask.execute();
            case 3 -> changeTimeoutTask.execute();
            case 4 -> taskDeadlineWarningTask.execute();
            case 5 -> messageRetryTask.execute();
            case 6 -> reportSummaryTask.execute();
            default -> throw new IllegalArgumentException("not allowed");
        };
        return Map.of("jobId", id, "jobName", job.get("jobName"), "result", "SUCCESS", "detail", result, "executedAt", LocalDateTime.now());
    }

    private static Map<String, Object> job(Long id, String name, String beanName, String cron, String status) {
        return Map.of(
                "id", id,
                "jobName", name,
                "beanName", beanName,
                "cronExpression", cron,
                "concurrentPolicy", "FORBID",
                "status", status);
    }
}
