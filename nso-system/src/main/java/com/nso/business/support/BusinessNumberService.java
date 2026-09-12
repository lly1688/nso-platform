package com.nso.business.support;

import com.nso.business.core.TenantContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

// 业务编号以数据库行锁为准，跨节点保持确定性。 Redis 不作为业务标识的唯一来源。
@Service

// 业务编号 服务层处理
public class BusinessNumberService {
    // JDBC模板
    private final JdbcTemplate jdbc;

    public BusinessNumberService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // 按业务类型生成流水编号。
    @Transactional
    public String next(String businessType) {
        LocalDate now = LocalDate.now();
        String period = switch (businessType) {
            case "TASK", "RISK" -> now.format(DateTimeFormatter.BASIC_ISO_DATE);
            default -> String.valueOf(now.getYear());
        };
        Long tenantId = TenantContext.tenantId();
        jdbc.update("INSERT IGNORE INTO nso_business_sequence (tenant_id, business_type, period_key, next_value, version) VALUES (?, ?, ?, 1, 0)",
                tenantId,
                businessType,
                period);

        Long next = jdbc.queryForObject("SELECT next_value FROM nso_business_sequence WHERE tenant_id=? AND business_type=? AND period_key=? FOR UPDATE",
                Long.class,
                tenantId,
                businessType,
                period);

        if (next == null) throw new IllegalStateException("业务流水号初始化失败");

        jdbc.update("UPDATE nso_business_sequence SET next_value=next_value+1, version=version+1 WHERE tenant_id=? AND business_type=? AND period_key=?",
                tenantId,
                businessType,
                period);

        return format(businessType, period, next);
    }

    private String format(String type, String period, Long value) {
        return switch (type) {
            case "PROJECT" -> "NSO-" + period + "-" + String.format("%06d", value);
            case "CUSTOMER" -> "CUS-" + period + "-" + String.format("%06d", value);
            case "SAMPLE" -> "SMP-" + period + "-" + String.format("%06d", value);
            case "CHANGE" -> "ECN-" + period + "-" + String.format("%06d", value);
            case "TASK" -> "TSK-" + period + "-" + String.format("%04d", value);
            case "RISK" -> "RSK-" + period + "-" + String.format("%04d", value);
            case "CAPA" -> "CAPA-" + period + "-" + String.format("%06d", value);
            case "SUPPORT" -> "SUP-" + period + "-" + String.format("%06d", value);
            case "DOCUMENT" -> "DOC-" + String.format("%06d", value);
            default -> type + "-" + period + "-" + String.format("%06d", value);
        };
    }
}
