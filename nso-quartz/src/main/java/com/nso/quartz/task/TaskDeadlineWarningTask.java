package com.nso.quartz.task;

import java.time.LocalDateTime;
import java.util.Map;

import com.nso.business.message.service.IMessageService;
import com.nso.business.task.service.ITaskService;

import org.springframework.stereotype.Component;

@Component
public class TaskDeadlineWarningTask {

    private final ITaskService taskService;
    private final IMessageService messageService;

    public TaskDeadlineWarningTask(ITaskService taskService, IMessageService messageService) {
        this.taskService = taskService;
        this.messageService = messageService;
    }

    public Map<String, Object> execute() {
        var due = taskService.list(null).list().stream()
                .filter(task -> task.planFinish() != null && !task.planFinish().isAfter(java.time.LocalDate.now().plusDays(2)))
                .filter(task -> !"DONE".equals(task.status()))
                .toList();
        due.forEach(task -> messageService.notify("任务临期提醒", task.taskNo() + " 即将到期，请及时反馈", "TASK_DUE", "TASK", task.id()));
        return Map.of(
                "task", "taskDeadlineWarningTask",
                "summary", "已检查并提醒 " + due.size() + " 个采购、生产、检验或交付任务",
                "notified", due.size(),
                "executedAt", LocalDateTime.now());
    }
}
