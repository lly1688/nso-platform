package com.nso.web.controller.system.monitor;

import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.PageSupport;
import com.nso.scheduler.task.ChangeTimeoutTask;
import com.nso.scheduler.task.MessageRetryTask;
import com.nso.scheduler.task.ProjectRiskScanTask;
import com.nso.scheduler.task.ReportSummaryTask;
import com.nso.scheduler.task.RetentionCleanupTask;
import com.nso.scheduler.task.SampleConfirmReminderTask;
import com.nso.scheduler.task.TaskDeadlineWarningTask;
import com.nso.shared.core.domain.AjaxResult;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// 定时任务管理接口。
@RestController
@RequestMapping("/api/v1/admin/monitor")
@PreAuthorize("hasRole('ADMIN')")
public class SysJobController {
    private static final List<Map<String, Object>> WHITE_LIST_JOBS = List.of(
            job(1L, "项目风险扫描", "projectRiskScanTask", "0 0/10 * * * ?", "RUNNING"),
            job(2L, "样品确认催办", "sampleConfirmReminderTask", "0 0 9 * * ?", "RUNNING"),
            job(3L, "变更超时升级", "changeTimeoutTask", "0 0/30 * * * ?", "RUNNING"),
            job(4L, "任务临期提醒", "taskDeadlineWarningTask", "0 0 8 * * ?", "RUNNING"),
            job(5L, "消息重试", "messageRetryTask", "0 0/5 * * * ?", "RUNNING"),
            job(6L, "报表汇总", "reportSummaryTask", "0 10 1 * * ?", "RUNNING"),
            job(7L, "临时数据保留清理", "retentionCleanupTask", "0 20 2 * * ?", "RUNNING"));

    // 项目风险Scan任务
    private final ProjectRiskScanTask projectRiskScanTask;
    // 样品ConfirmReminder任务
    private final SampleConfirmReminderTask sampleConfirmReminderTask;
    // 变更超时任务
    private final ChangeTimeoutTask changeTimeoutTask;
    // 任务截止预警任务
    private final TaskDeadlineWarningTask taskDeadlineWarningTask;
    // 消息Retry任务
    private final MessageRetryTask messageRetryTask;
    // 报表Summary任务
    private final ReportSummaryTask reportSummaryTask;
    // 留存清理任务
    private final RetentionCleanupTask retentionCleanupTask;

    public SysJobController(ProjectRiskScanTask projectRiskScanTask,
                            SampleConfirmReminderTask sampleConfirmReminderTask,
                            ChangeTimeoutTask changeTimeoutTask,
                            TaskDeadlineWarningTask taskDeadlineWarningTask,
                            MessageRetryTask messageRetryTask,
                            ReportSummaryTask reportSummaryTask,
                            RetentionCleanupTask retentionCleanupTask) {
        this.projectRiskScanTask = projectRiskScanTask;
        this.sampleConfirmReminderTask = sampleConfirmReminderTask;
        this.changeTimeoutTask = changeTimeoutTask;
        this.taskDeadlineWarningTask = taskDeadlineWarningTask;
        this.messageRetryTask = messageRetryTask;
        this.reportSummaryTask = reportSummaryTask;
        this.retentionCleanupTask = retentionCleanupTask;
    }

    // 查询允许手动执行的定时任务。
    @GetMapping("/jobs")
    public AjaxResult<?> jobs(@RequestParam(required = false) Integer pageNo,
                              @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(PageSupport.slice(WHITE_LIST_JOBS, new PageQuery(pageNo, pageSize)));
    }

    // 手动执行白名单中的定时任务。
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
            case 7 -> retentionCleanupTask.execute();
            default -> throw new IllegalArgumentException("not allowed");
        };
        return Map.of(
                "jobId", id,
                "jobName", job.get("jobName"),
                "result", "SUCCESS",
                "detail", result,
                "executedAt", LocalDateTime.now());
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
