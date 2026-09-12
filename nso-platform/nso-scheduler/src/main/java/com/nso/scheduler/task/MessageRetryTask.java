package com.nso.scheduler.task;

import java.util.Map;
import com.nso.business.message.service.IMessageService;
import org.springframework.stereotype.Component;

// 消息重试定时任务。
@Component
public class MessageRetryTask {

    // 消息服务
    private final IMessageService messageService;

    public MessageRetryTask(IMessageService messageService) {
        this.messageService = messageService;
    }

    // 重试待发送的站内通知。
    public Map<String, Object> execute() {
        int delivered = messageService.retryPendingNotifications();
        return Map.of(
                "task", "messageRetryTask",
                "delivered", delivered,
                "channel", "IN_APP");
    }
}
