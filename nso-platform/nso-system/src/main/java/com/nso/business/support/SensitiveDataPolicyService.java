package com.nso.business.support;

import com.nso.business.core.TenantContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

// 服务层投影保护；不可仅信任界面可见性。
@Service

// 敏感数据策略 服务层处理
public class SensitiveDataPolicyService {
    // JDBC模板
    private final JdbcTemplate jdbc;

    public SensitiveDataPolicyService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // 判断当前用户能否查看敏感字段明文。
    public boolean canReadFull(String fieldCode) {
        if (TenantContext.hasAnyRole("admin")) return true;
        for (String role : TenantContext.roles()) {
            String mode = jdbc.query("SELECT read_mode FROM nso_sensitive_field_policy WHERE tenant_id=? AND role_code=? AND field_code=?",
                    rs -> rs.next() ? rs.getString(1) : null,
                    TenantContext.tenantId(),
                    role,
                    fieldCode);
            if ("FULL".equals(mode))
                return true;
        }
        return false;
    }

    // 判断当前用户能否写入敏感字段。
    public boolean canWrite(String fieldCode) {
        if (TenantContext.hasAnyRole("admin"))
            return true;
        for (String role : TenantContext.roles()) {
            Integer allowed = jdbc.query("SELECT write_allowed FROM nso_sensitive_field_policy WHERE tenant_id=? AND role_code=? AND field_code=?",
                    rs -> rs.next() ? rs.getInt(1) : null,
                    TenantContext.tenantId(),
                    role, fieldCode);
            if (allowed != null && allowed == 1)
                return true;
        }
        return false;
    }

    // 按字段权限投影敏感数据。
    public String project(String fieldCode, String value) {
        if (value == null || value.isBlank() || canReadFull(fieldCode))
            return value;
        if (value.length() <= 2)
            return "**";
        if (value.contains("@")) {
            int at = value.indexOf('@');
            return value.substring(0, 1) + "***" + value.substring(at);
        }
        return value.substring(0, 1) + "***" + value.substring(value.length() - 1);
    }
}
