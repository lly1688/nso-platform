package com.nso.quartz.task;

import com.nso.business.change.service.IChangeService;
import com.nso.business.core.NsoDtos.ChangeOrderDto;
import com.nso.business.message.service.IMessageService;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class ChangeTimeoutTask {
    private final IChangeService changeService;
    private final IMessageService messageService;

    public ChangeTimeoutTask(IChangeService changeService, IMessageService messageService) {
        this.changeService = changeService;
        this.messageService = messageService;
    }

    public Map<String, Object> execute() {
        var pending = changeService.list(null).list().stream()
                .filter(change -> "WAIT_IMPACT".equals(change.status()) || "ANALYZED".equals(change.status()) || "EXECUTING".equals(change.status()))
                .toList();
        for (ChangeOrderDto change : pending) {
            messageService.notifyOnce("变更协同待处理", "变更单 " + change.changeNo() + " 当前处于 " + change.status() + "，请处理影响分析、审批或执行反馈", "CHANGE_TIMEOUT", "CHANGE", change.id());
        }
        return Map.of(
                "task", "changeTimeoutTask",
                "notified", pending.size(),
                "statuses", "WAIT_IMPACT,ANALYZED,EXECUTING");
    }
}
