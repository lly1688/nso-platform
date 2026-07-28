package com.nso.business.support;

import com.nso.business.core.TenantContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Writes immutable domain history and the project timeline in the same transaction as a business action. */
@Service
public class BusinessEventService {
    private final JdbcTemplate jdbc;

    public BusinessEventService(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public void record(Long projectId, String businessType, Long businessId, String action,
                       String before, String after, String summary) {
        long tenantId = TenantContext.tenantId();
        jdbc.update("INSERT INTO nso_business_event (tenant_id,project_id,business_type,business_id,action_code,before_summary,after_summary,operator_id,operator_name) VALUES (?,?,?,?,?,?,?,?,?)",
                tenantId, projectId, businessType, businessId, action, before, after, TenantContext.userId(), TenantContext.username());
        if (projectId != null) {
            jdbc.update("INSERT INTO nso_timeline_event (tenant_id,project_id,business_type,business_id,event_type,title,summary,operator_name) VALUES (?,?,?,?,?,?,?,?)",
                    tenantId, projectId, businessType, businessId, action, action, summary, TenantContext.username());
        }
    }
}
