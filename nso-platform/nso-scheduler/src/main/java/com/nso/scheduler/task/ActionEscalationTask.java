package com.nso.scheduler.task;

import com.nso.business.support.action.IActionCenterService;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.stereotype.Component;

// 行动项超时升级定时任务。
@Component
public class ActionEscalationTask {
    // 操作中心服务
    private final IActionCenterService actionCenter;

    public ActionEscalationTask(IActionCenterService actionCenter) {
        this.actionCenter = actionCenter;
    }

    // 升级已超时的审批和纠正预防行动。
    public Map<String, Object> execute() {
        int affected = actionCenter.escalateOverdue();
        return Map.of(
                "task", "actionEscalationTask",
                "affected", affected,
                "executedAt", LocalDateTime.now());
    }
}
