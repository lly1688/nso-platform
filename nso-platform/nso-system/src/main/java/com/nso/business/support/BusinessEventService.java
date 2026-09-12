package com.nso.business.support;

import com.nso.business.core.TenantContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

// 业务动作会在同一事务中写入不可变领域历史和项目时间线。
@Service

// 业务事件 服务层处理
public class BusinessEventService {
    // JDBC模板
    private final JdbcTemplate jdbc;

    public BusinessEventService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // 记录项目业务事件。
    public void record(Long projectId,
                        String businessType,
                        Long businessId,
                        String action,
                        String before,
                        String after,
                        String summary) {
        long tenantId = TenantContext.tenantId();
        jdbc.update("INSERT INTO nso_business_event (tenant_id,project_id,business_type,business_id,action_code,before_summary,after_summary,operator_id,operator_name) VALUES (?,?,?,?,?,?,?,?,?)",
                tenantId,
                projectId,
                businessType,
                businessId,
                action,
                before,
                after,
                TenantContext.userId(),
                TenantContext.username());
        if (projectId != null) {
            jdbc.update("INSERT INTO nso_timeline_event (tenant_id,project_id,business_type,business_id,event_type,title,summary,operator_name) VALUES (?,?,?,?,?,?,?,?)",
                    tenantId,
                    projectId,
                    businessType,
                    businessId,
                    action,
                    action,
                    summary,
                    TenantContext.username());
        }
    }
}
