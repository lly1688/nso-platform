package com.nso.system.service.impl;

import com.nso.business.core.TenantContext;
import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.business.core.PageSupport;
import com.nso.shared.exception.BusinessException;
import com.nso.system.domain.SysUser;
import com.nso.system.mapper.SysRoleMapper;
import com.nso.system.mapper.SysUserMapper;
import com.nso.system.mapper.SysUserRoleMapper;
import com.nso.system.service.IAuthorizationGovernanceService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service

// 授权治理 服务层处理
public class AuthorizationGovernanceServiceImpl implements IAuthorizationGovernanceService {
    private static final Set<String> PROTECTED = Set.of("superadmin", "system_admin", "hr_admin");
    // JDBC模板
    private final JdbcTemplate jdbc;
    // 系统用户数据映射
    private final SysUserMapper users;
    // 系统角色数据映射
    private final SysRoleMapper roles;
    // 系统用户角色数据映射
    private final SysUserRoleMapper userRoles;

    public AuthorizationGovernanceServiceImpl(JdbcTemplate jdbc, SysUserMapper users, SysRoleMapper roles, SysUserRoleMapper userRoles) {
        this.jdbc = jdbc;
        this.users = users;
        this.roles = roles;
        this.userRoles = userRoles;
    }

    // 申请受保护角色。
    @Override
    @Transactional
    public Map<String, Object> requestProtectedRole(Long userId, String roleCode, String reason) {
        requireSuperadmin();
        if (userId == null || roleCode == null || !PROTECTED.contains(roleCode.trim().toLowerCase()) || reason == null || reason.isBlank()) {
            throw new BusinessException("受保护角色申请必须指定目标账号、角色和申请理由");
        }
        SysUser target = users.selectById(userId);
        if (target == null || !Long.valueOf(TenantContext.tenantId()).equals(target.getTenantId()) || !"INTERNAL".equals(target.getUserType())) {
            throw new BusinessException("目标必须是当前企业的内部账号");
        }
        String normalized = roleCode.trim().toLowerCase();
        jdbc.update("INSERT INTO nso_authorization_change_request (tenant_id,target_user_id,requested_role_code,request_type,status,reason,requested_by) VALUES (?,?,?,'GRANT_PROTECTED_ROLE','PENDING',?,?)",
                TenantContext.tenantId(), userId, normalized, reason.trim(), TenantContext.userId());
        Long requestId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        audit(userId, "PROTECTED_ROLE_REQUESTED", null, normalized, reason.trim());
        return Map.of("id", requestId, "status", "PENDING", "requestedRoleCode", normalized);
    }

    // 审批受保护角色申请。
    @Override
    @Transactional
    public Map<String, Object> approveProtectedRole(Long requestId) {
        requireSuperadmin();
        Map<String, Object> request = jdbc.query("SELECT id,target_user_id,requested_role_code,requested_by,status,reason FROM nso_authorization_change_request WHERE tenant_id=? AND id=? FOR UPDATE",
                rs -> rs.next() ? Map.of("id",
                        rs.getLong("id"),
                        "targetUserId",
                        rs.getLong("target_user_id"),
                        "roleCode",
                        rs.getString("requested_role_code"),
                        "requestedBy",
                        rs.getLong("requested_by"),
                        "status",
                        rs.getString("status"),
                        "reason",
                        rs.getString("reason")) : null,
                TenantContext.tenantId(), requestId);
        if (request == null || !"PENDING".equals(request.get("status"))) throw new BusinessException("授权申请不存在或已处理");
        if (TenantContext.userId().equals(request.get("requestedBy"))) throw new BusinessException("受保护角色申请必须由另一名超级管理员复核");
        Long targetId = ((Number) request.get("targetUserId")).longValue();
        String roleCode = String.valueOf(request.get("roleCode"));
        Long roleId = roles.selectIdByRoleCode(TenantContext.tenantId(), roleCode);
        if (roleId == null) throw new BusinessException("受保护角色不存在或已停用");
        if (!userRoles.selectUserIdsByRoleId(roleId).contains(targetId)) userRoles.insertRole(targetId, roleId);
        users.incrementAuthVersion(targetId);
        jdbc.update("UPDATE nso_authorization_change_request SET status='APPROVED',approved_by=?,approved_at=NOW() WHERE id=? AND tenant_id=? AND status='PENDING'",
                TenantContext.userId(), requestId, TenantContext.tenantId());
        audit(targetId, "PROTECTED_ROLE_GRANTED", null, roleCode, String.valueOf(request.get("reason")));
        return Map.of("id", requestId, "status", "APPROVED", "targetUserId", targetId, "roleCode", roleCode);
    }

    // 查询授权申请。
    @Override
    public List<Map<String, Object>> requests() {
        return jdbc.queryForList("SELECT id,target_user_id AS targetUserId,requested_role_code AS requestedRoleCode,status,reason,requested_by AS requestedBy,approved_by AS approvedBy,created_at AS createdAt,approved_at AS approvedAt FROM nso_authorization_change_request WHERE tenant_id=? ORDER BY id DESC",
                TenantContext.tenantId());
    }

    // 分页查询授权申请。
    @Override
    public PageResult<Map<String, Object>> requests(PageQuery pageQuery) {
        PageQuery page = PageSupport.normalize(pageQuery);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM nso_authorization_change_request WHERE tenant_id=?",
                Long.class, TenantContext.tenantId());
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT id,target_user_id AS targetUserId,requested_role_code AS requestedRoleCode,status,reason,requested_by AS requestedBy,approved_by AS approvedBy,created_at AS createdAt,approved_at AS approvedAt FROM nso_authorization_change_request WHERE tenant_id=? ORDER BY id DESC LIMIT ? OFFSET ?",
                TenantContext.tenantId(), page.pageSizeValue(), page.offset());
        return new PageResult<>(rows, total == null ? 0 : total, page.pageNoValue(), page.pageSizeValue());
    }

    private void requireSuperadmin() {
        if (!TenantContext.hasAnyRole("superadmin")) throw new BusinessException("只有超级管理员可以发起或复核受保护角色申请");
    }

    private void audit(Long targetId, String action, String before, String after, String reason) {
        jdbc.update("INSERT INTO nso_authorization_audit (tenant_id,subject_type,subject_id,change_type,before_json,after_json,reason,operator_id) VALUES (?,?,?, ?, JSON_OBJECT('value', ?), JSON_OBJECT('value', ?), ?, ?)",
                TenantContext.tenantId(), "USER", targetId, action, before, after, reason, TenantContext.userId());
    }
}
