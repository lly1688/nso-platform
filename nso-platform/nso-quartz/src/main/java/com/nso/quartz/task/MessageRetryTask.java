package com.nso.quartz.task;

import java.util.Map;
import com.nso.business.message.service.IMessageService;
import org.springframework.stereotype.Component;

@Component
public class MessageRetryTask {
    private final IMessageService messageService;

    public MessageRetryTask(IMessageService messageService) {
        this.messageService = messageService;
    }

    public Map<String, Object> execute() {
        int delivered = messageService.retryPendingNotifications();
        return Map.of(
                "task", "messageRetryTask",
                "delivered", delivered,
                "channel", "IN_APP");
    }
}
