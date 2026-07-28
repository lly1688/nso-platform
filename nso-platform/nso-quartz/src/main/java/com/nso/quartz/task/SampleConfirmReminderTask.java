package com.nso.quartz.task;

import java.time.LocalDateTime;
import java.util.Map;

import com.nso.business.message.service.IMessageService;
import com.nso.business.sample.service.ISampleService;

import org.springframework.stereotype.Component;

@Component
public class SampleConfirmReminderTask {

    private final ISampleService sampleService;
    private final IMessageService messageService;

    public SampleConfirmReminderTask(ISampleService sampleService, IMessageService messageService) {
        this.sampleService = sampleService;
        this.messageService = messageService;
    }

    public Map<String, Object> execute() {
        var due = sampleService.list(null).list().stream()
                .filter(sample -> "WAIT_CUSTOMER_CONFIRM".equals(sample.status()))
                .filter(sample -> sample.planFinishDate() != null && !sample.planFinishDate().isAfter(java.time.LocalDate.now().plusDays(2)))
                .toList();
        due.forEach(sample -> messageService.notify("样品确认催办", sample.sampleNo() + " 即将到期，请跟进客户确认", "SAMPLE_CONFIRM", "SAMPLE", sample.id()));
        return Map.of(
                "task", "sampleConfirmReminderTask",
                "summary", "已生成 " + due.size() + " 条待客户确认样品临期提醒",
                "notified", due.size(),
                "executedAt", LocalDateTime.now());
    }
}
