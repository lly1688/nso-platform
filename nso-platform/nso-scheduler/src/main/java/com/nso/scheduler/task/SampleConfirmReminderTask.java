package com.nso.scheduler.task;

import java.time.LocalDateTime;
import java.util.Map;

import com.nso.business.message.service.IMessageService;
import com.nso.business.sample.service.ISampleService;

import org.springframework.stereotype.Component;

// 样品确认提醒定时任务。
@Component
public class SampleConfirmReminderTask {

    // 样品服务
    private final ISampleService sampleService;
    // 消息服务
    private final IMessageService messageService;

    public SampleConfirmReminderTask(ISampleService sampleService, IMessageService messageService) {
        this.sampleService = sampleService;
        this.messageService = messageService;
    }

    // 扫描临期样品并发送客户确认提醒。
    public Map<String, Object> execute() {
        var due = sampleService.list(null).list().stream()
                .filter(sample -> "WAIT_CUSTOMER_CONFIRM".equals(sample.status()))
                .filter(sample -> sample.planFinishDate() != null
                        && !sample.planFinishDate().isAfter(java.time.LocalDate.now().plusDays(2)))
                .toList();
        due.forEach(sample -> messageService.notifyProject(
                sample.projectId(),
                "SAMPLE_CONFIRM_DUE",
                "样品确认催办",
                sample.sampleNo() + " 即将到期，请跟进客户确认",
                "SAMPLE",
                sample.id()));
        return Map.of(
                "task", "sampleConfirmReminderTask",
                "summary", "已生成 " + due.size() + " 条待客户确认样品临期提醒",
                "notified", due.size(),
                "executedAt", LocalDateTime.now());
    }
}
