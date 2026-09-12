package com.nso.system.service.impl;

import com.nso.business.core.TenantContext;
import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.business.core.PageSupport;
import com.nso.shared.exception.BusinessException;
import com.nso.system.service.IRetentionGovernanceService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service

// 数据保留治理 服务层处理
public class RetentionGovernanceServiceImpl implements IRetentionGovernanceService {
    private static final Set<String> AUTOMATIC = Set.of("TEMPORARY_SECURITY", "IMPORT_EXPORT_TEMPORARY");
    // JDBC模板
    private final JdbcTemplate jdbc;

    public RetentionGovernanceServiceImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // 查询数据保留策略。
    @Override
    public List<Map<String, Object>> policies() {
        return jdbc.queryForList("SELECT policy_code AS policyCode,retention_days AS retentionDays,archive_after_days AS archiveAfterDays,purge_requires_confirmation AS purgeRequiresConfirmation,enabled,updated_at AS updatedAt FROM nso_retention_policy WHERE tenant_id=? ORDER BY policy_code", TenantContext.tenantId());
    }

    // 分页查询数据保留策略。
    @Override
    public PageResult<Map<String, Object>> policies(PageQuery pageQuery) {
        PageQuery page = PageSupport.normalize(pageQuery);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM nso_retention_policy WHERE tenant_id=?", Long.class, TenantContext.tenantId());
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT policy_code AS policyCode,retention_days AS retentionDays,archive_after_days AS archiveAfterDays,purge_requires_confirmation AS purgeRequiresConfirmation,enabled,updated_at AS updatedAt FROM nso_retention_policy WHERE tenant_id=? ORDER BY policy_code LIMIT ? OFFSET ?",
                TenantContext.tenantId(), page.pageSizeValue(), page.offset());
        return new PageResult<>(rows, total == null ? 0 : total, page.pageNoValue(), page.pageSizeValue());
    }

    // 更新数据保留策略。
    @Override
    @Transactional
    public Map<String, Object> updatePolicy(String policyCode, Integer retentionDays, Integer archiveAfterDays, Boolean enabled) {
        requireSuperadmin();
        String code = normalized(policyCode);
        if (retentionDays == null || retentionDays < 1 || archiveAfterDays == null || archiveAfterDays < 0) throw new BusinessException("保留期和归档期必须为非负有效值");
        int changed = jdbc.update("UPDATE nso_retention_policy SET retention_days=?,archive_after_days=?,enabled=?,updated_by=? WHERE tenant_id=? AND policy_code=?",
                retentionDays, archiveAfterDays, Boolean.FALSE.equals(enabled) ? 0 : 1, TenantContext.userId(), TenantContext.tenantId(), code);
        if (changed == 0) throw new BusinessException("保留策略不存在");
        return Map.of("policyCode", code, "retentionDays", retentionDays, "archiveAfterDays", archiveAfterDays, "enabled", !Boolean.FALSE.equals(enabled));
    }

    // 执行指定数据保留策略。
    @Override
    @Transactional
    public Map<String, Object> runPolicy(String policyCode, String confirmationRef) {
        requireSuperadmin();
        String code = normalized(policyCode);
        if (!AUTOMATIC.contains(code) && (confirmationRef == null || confirmationRef.isBlank())) {
            throw new BusinessException("业务与审计历史清理必须先完成备份导出并提供二次确认编码");
        }
        Integer days = jdbc.query("SELECT retention_days FROM nso_retention_policy WHERE tenant_id=? AND policy_code=? AND enabled=1", rs -> rs.next() ? rs.getInt(1) : null,
                TenantContext.tenantId(), code);
        if (days == null) throw new BusinessException("保留策略不存在或已禁用");
        int affected = automaticCleanup(TenantContext.tenantId(), code, days);
        String status = AUTOMATIC.contains(code) ? "COMPLETED" : "RECORDED_NO_DELETE";
        jdbc.update("INSERT INTO nso_retention_run (tenant_id,policy_code,run_type,status,affected_count,confirmation_ref,operator_id,detail) VALUES (?,?,'MANUAL',?,?,?,?,?)",
                TenantContext.tenantId(), code, status, affected, confirmationRef, TenantContext.userId(),
                AUTOMATIC.contains(code) ? "仅清理过期令牌、验证码和临时记录" : "业务与审计历史默认只读归档；本次仅登记备份确认，不物理删除");
        return Map.of("policyCode", code, "status", status, "affectedCount", affected);
    }

    // 执行全部租户的临时数据策略。
    @Override
    @Transactional
    public int runTemporaryPoliciesForAllTenants() {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT tenant_id AS tenantId,policy_code AS policyCode,retention_days AS retentionDays FROM nso_retention_policy WHERE enabled=1 AND policy_code IN ('TEMPORARY_SECURITY','IMPORT_EXPORT_TEMPORARY')");
        int affected = 0;
        for (Map<String, Object> row : rows) {
            Long tenantId = ((Number) row.get("tenantId")).longValue();
            String code = String.valueOf(row.get("policyCode"));
            int days = ((Number) row.get("retentionDays")).intValue();
            int count = automaticCleanup(tenantId, code, days);
            affected += count;
            jdbc.update("INSERT INTO nso_retention_run (tenant_id,policy_code,run_type,status,affected_count,detail) VALUES (?,?,'SCHEDULED','COMPLETED',?,?)",
                    tenantId, code, count, "仅清理可自动清理的过期临时数据");
        }
        return affected;
    }

    private int automaticCleanup(Long tenantId, String code, int days) {
        if ("TEMPORARY_SECURITY".equals(code)) {
            int total = jdbc.update("DELETE FROM nso_verification_challenge WHERE tenant_id=? AND expires_at < DATE_SUB(NOW(), INTERVAL ? DAY)", tenantId, days);
            total += jdbc.update("DELETE FROM nso_operation_confirmation WHERE tenant_id=? AND expires_at < DATE_SUB(NOW(), INTERVAL ? DAY)", tenantId, days);
            total += jdbc.update("DELETE FROM nso_external_token WHERE tenant_id=? AND (status='REVOKED' OR expire_at < DATE_SUB(NOW(), INTERVAL ? DAY))", tenantId, days);
            return total;
        }
        if ("IMPORT_EXPORT_TEMPORARY".equals(code)) {
            return jdbc.update("UPDATE nso_file_cleanup_task SET status='PENDING',updated_at=NOW() WHERE tenant_id=? AND status='FAILED' AND created_at < DATE_SUB(NOW(), INTERVAL ? DAY)", tenantId, days);
        }
        return 0;
    }

    private String normalized(String code) {
        if (code == null || code.isBlank()) throw new BusinessException("保留策略不能为空");
        return code.trim().toUpperCase();
    }

    private void requireSuperadmin() {
        if (!TenantContext.hasAnyRole("superadmin", "admin")) throw new BusinessException("只有超级管理员可以配置或执行历史清理");
    }
}
