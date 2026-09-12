package com.nso.scheduler.task;

import com.nso.system.service.IRetentionGovernanceService;
import org.springframework.stereotype.Component;

import java.util.Map;

// 临时数据清理定时任务。
@Component
public class RetentionCleanupTask {

    // 留存治理服务
    private final IRetentionGovernanceService retention;

    public RetentionCleanupTask(IRetentionGovernanceService retention) {
        this.retention = retention;
    }

    // 执行所有租户的临时数据保留策略。
    public Map<String, Object> execute() {
        return Map.of("task", "retentionCleanupTask", "affected", retention.runTemporaryPoliciesForAllTenants());
    }
}
