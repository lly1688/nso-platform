package com.nso.framework.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// 试点环境运维监控指标。
@Component
public class PilotOperationalMetrics {

    // JDBC模板
    private final JdbcTemplate jdbc;
    // 指标计数器
    private final Counter queryFailures;

    public PilotOperationalMetrics(JdbcTemplate jdbc, MeterRegistry registry) {
        this.jdbc = jdbc;
        this.queryFailures = Counter.builder("nso.operational.metric.query.failures")
                .description("Operational gauge queries that could not read MySQL")
                .register(registry);
        Gauge.builder("nso.message.backlog", this, metrics -> metrics.countUnreadMessages())
                .description("Unread in-app messages awaiting a recipient")
                .register(registry);
        Gauge.builder("nso.scheduler.failures.24h", this, metrics -> metrics.countRecentSchedulerFailures())
                .description("Scheduler execution failures recorded in the last 24 hours")
                .register(registry);
    }

    private double countUnreadMessages() {
        return queryCount("SELECT COUNT(*) FROM nso_message WHERE deleted=0 AND status='UNREAD'");
    }

    private double countRecentSchedulerFailures() {
        return queryCount("SELECT COUNT(*) FROM sys_job_log WHERE started_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR) "
                + "AND result NOT IN ('SUCCESS','DONE')");
    }

    private double queryCount(String sql) {
        try {
            Long count = jdbc.queryForObject(sql, Long.class);
            return count == null ? 0 : count;
        } catch (DataAccessException exception) {
            queryFailures.increment();
            return Double.NaN;
        }
    }
}
