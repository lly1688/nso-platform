package com.nso.business.project.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nso.business.core.TenantContext;
import com.nso.business.project.domain.ProjectMember;
import com.nso.business.project.domain.ProjectMemberResponsibility;
import com.nso.business.project.mapper.ProjectMemberMapper;
import com.nso.business.project.mapper.ProjectMemberResponsibilityMapper;
import com.nso.shared.exception.BusinessException;
import com.nso.system.domain.SysDept;
import com.nso.system.domain.SysUser;
import com.nso.system.mapper.SysDeptMapper;
import com.nso.system.mapper.SysUserMapper;
import com.nso.system.security.UserLifecycle;
import com.nso.system.service.ISysUserService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 项目成员、职责槽位和交接约束协作组件。
@Component

// 项目成员协调器 协调项目成员的增删改查与权限
public class ProjectMembershipCoordinator {
    // 项目成员数据映射
    private final ProjectMemberMapper members;
    // 项目成员职责数据映射
    private final ProjectMemberResponsibilityMapper memberResponsibilities;
    // 系统用户数据映射
    private final SysUserMapper users;
    // 系统部门数据映射
    private final SysDeptMapper departments;
    // 系统用户服务
    private final ISysUserService directoryUsers;
    // 项目职责目录
    private final ProjectResponsibilityCatalog responsibilities;
    // JDBC模板
    private final JdbcTemplate jdbc;

    public ProjectMembershipCoordinator(
            ProjectMemberMapper members,
            ProjectMemberResponsibilityMapper memberResponsibilities,
            SysUserMapper users,
            SysDeptMapper departments,
            ISysUserService directoryUsers,
            ProjectResponsibilityCatalog responsibilities,
            JdbcTemplate jdbc) {
        this.members = members;
        this.memberResponsibilities = memberResponsibilities;
        this.users = users;
        this.departments = departments;
        this.directoryUsers = directoryUsers;
        this.responsibilities = responsibilities;
        this.jdbc = jdbc;
    }

    // 确保项目成员存在并同步职责。
    public ProjectMember ensureMember(Long projectId, SysUser user, List<String> dutyCodes, String primary) {
        ProjectMember member = members.selectOne(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, user.getId())
                .last("LIMIT 1"));
        SysDept department = user.getDeptId() == null ? null : departments.selectById(user.getDeptId());
        String memberName = displayName(user);
        String departmentName = department == null ? null : department.getDeptName();
        if (member == null) {
            member = new ProjectMember();
            member.setTenantId(TenantContext.tenantId());
            member.setProjectId(projectId);
            member.setUserId(user.getId());
            member.setMemberName(memberName);
            member.setDeptId(user.getDeptId());
            member.setDepartmentName(departmentName);
            member.setProjectRole(primary);
            member.setStatus("ACTIVE");
            members.insert(member);
        } else {
            member.setMemberName(memberName);
            member.setDeptId(user.getDeptId());
            member.setDepartmentName(departmentName);
            member.setProjectRole(primary);
            member.setStatus("ACTIVE");
            members.updateById(member);
        }
        syncResponsibilities(member, dutyCodes, primary);
        return member;
    }

    // 同步项目成员职责。
    public void syncResponsibilities(ProjectMember member, List<String> dutyCodes, String primary) {
        if (dutyCodes.stream().anyMatch(responsibilities::isOwner)) {
            jdbc.queryForObject("SELECT id FROM nso_project WHERE tenant_id=? AND id=? FOR UPDATE", Long.class,
                    TenantContext.tenantId(), member.getProjectId());
        }
        List<ProjectMemberResponsibility> existing = memberResponsibilities.selectList(Wrappers.<ProjectMemberResponsibility>lambdaQuery()
                .eq(ProjectMemberResponsibility::getProjectMemberId, member.getId()));
        Map<String, ProjectMemberResponsibility> byCode = new LinkedHashMap<>();
        for (ProjectMemberResponsibility row : existing) {
            row.setStatus("REMOVED");
            row.setPrimaryFlag(0);
            row.setOwnerSlot(null);
            memberResponsibilities.updateById(row);
            byCode.put(row.getResponsibilityCode(), row);
        }
        for (String code : dutyCodes) {
            ProjectMemberResponsibility row = byCode.get(code);
            if (row == null) {
                row = new ProjectMemberResponsibility();
                row.setTenantId(TenantContext.tenantId());
                row.setProjectMemberId(member.getId());
                row.setProjectId(member.getProjectId());
                row.setUserId(member.getUserId());
                row.setResponsibilityCode(code);
                row.setVersion(0);
                row.setDeleted(0);
                row.setStatus("ACTIVE");
                row.setPrimaryFlag(code.equals(primary) ? 1 : 0);
                relinquishOwner(member, code);
                row.setOwnerSlot(responsibilities.isOwner(code) ? ownerSlot(member.getProjectId(), code) : null);
                memberResponsibilities.insert(row);
                resolveOwnerGap(member.getProjectId(), code);
            } else {
                row.setStatus("ACTIVE");
                row.setPrimaryFlag(code.equals(primary) ? 1 : 0);
                relinquishOwner(member, code);
                row.setOwnerSlot(responsibilities.isOwner(code) ? ownerSlot(member.getProjectId(), code) : null);
                memberResponsibilities.updateById(row);
                resolveOwnerGap(member.getProjectId(), code);
            }
        }
    }

    // 查询成员的全部职责标识。
    public List<String> responsibilityCodes(ProjectMember member) {
        List<String> codes = memberResponsibilities.selectList(Wrappers.<ProjectMemberResponsibility>lambdaQuery()
                        .eq(ProjectMemberResponsibility::getProjectMemberId, member.getId()))
                .stream()
                .filter(row -> "ACTIVE".equals(row.getStatus()) || "REMOVED".equals(row.getStatus()))
                .map(ProjectMemberResponsibility::getResponsibilityCode)
                .distinct()
                .toList();
        return codes.isEmpty() && member.getProjectRole() != null
                ? List.of(responsibilities.normalizeCode(member.getProjectRole()))
                : codes;
    }

    // 查询成员的有效职责标识。
    public List<String> activeResponsibilityCodes(ProjectMember member) {
        List<String> codes = memberResponsibilities.selectList(Wrappers.<ProjectMemberResponsibility>lambdaQuery()
                        .eq(ProjectMemberResponsibility::getProjectMemberId, member.getId())
                        .eq(ProjectMemberResponsibility::getStatus, "ACTIVE"))
                .stream()
                .map(ProjectMemberResponsibility::getResponsibilityCode)
                .distinct()
                .toList();
        return codes.isEmpty() && member.getProjectRole() != null
                ? List.of(responsibilities.normalizeCode(member.getProjectRole()))
                : codes;
    }

    // 校验并读取可分配用户。
    public SysUser requireAssignableUser(Long userId, boolean externalAllowed) {
        SysUser user = users.selectById(userId);
        if (user == null || !Long.valueOf(TenantContext.tenantId()).equals(user.getTenantId()) || !UserLifecycle.isActive(user.getStatus())) {
            throw BusinessException.accessDenied("PROJECT_MEMBER_USER", "成员账号不存在、已停用或不属于当前企业",
                    String.valueOf(userId), "当前企业启用账号", "选择有效成员账号");
        }
        if (!externalAllowed && !"INTERNAL".equals(user.getUserType())) {
            throw new BusinessException("项目团队只能选择内部账号；客户请通过联系人项目授权参与确认");
        }
        return user;
    }

    // 校验并读取项目经理。
    public SysUser requireProjectManager(Long userId) {
        if (userId == null) {
            throw new BusinessException("请选择项目经理账号");
        }
        SysUser user = requireAssignableUser(userId, false);
        List<String> roleCodes = directoryUsers.roleCodes(userId);
        if (!roleCodes.stream().anyMatch(role -> "project_manager".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role))) {
            throw new BusinessException("所选账号未配置项目经理角色");
        }
        return user;
    }

    // 校验用户可承担的项目职责。
    public void validateResponsibilities(SysUser user, List<String> dutyCodes) {
        List<String> roleCodes = directoryUsers.roleCodes(user.getId());
        for (String code : dutyCodes) {
            if (!responsibilities.isAllowedForRoles(code, roleCodes,
                    roleCodes.stream().anyMatch(role -> "admin".equalsIgnoreCase(role)))) {
                throw new BusinessException("账号未配置与项目职责匹配的系统角色：" + code);
            }
        }
    }

    // 校验并读取项目成员。
    public ProjectMember requireMember(Long projectId, Long memberId) {
        ProjectMember member = members.selectById(memberId);
        if (member == null || !Long.valueOf(TenantContext.tenantId()).equals(member.getTenantId())
                || !projectId.equals(member.getProjectId())) {
            throw new BusinessException("项目成员不存在");
        }
        return member;
    }

    // 校验用户没有未完成的项目分配。
    public void assertNoOpenAssignment(Long projectId, Long userId) {
        Integer tasks = jdbc.queryForObject("SELECT COUNT(*) FROM nso_task WHERE tenant_id=? AND project_id=? AND assignee_id=? AND status NOT IN ('DONE','CANCELLED')", Integer.class,
                TenantContext.tenantId(), projectId, userId);
        Integer actions = jdbc.queryForObject("SELECT COUNT(*) FROM nso_risk_action a JOIN nso_risk r ON r.id=a.risk_id AND r.tenant_id=a.tenant_id WHERE a.tenant_id=? AND r.project_id=? AND a.responsible_user_id=? AND a.status <> 'CLOSED' AND a.deleted=0", Integer.class,
                TenantContext.tenantId(), projectId, userId);
        Integer approvals = jdbc.queryForObject("SELECT COUNT(*) FROM nso_change_approval a JOIN nso_change_order c ON c.id=a.change_id AND c.tenant_id=a.tenant_id WHERE a.tenant_id=? AND c.project_id=? AND a.approver_id=? AND a.decision='PENDING'", Integer.class,
                TenantContext.tenantId(), projectId, userId);
        Integer pendingMessages = jdbc.queryForObject("SELECT COUNT(*) FROM nso_message m WHERE m.tenant_id=? AND m.receiver_id=? AND m.status='UNREAD' AND m.deleted=0 AND ((m.business_type='PROJECT' AND m.business_id=?) OR (m.business_type='TASK' AND EXISTS (SELECT 1 FROM nso_task t WHERE t.id=m.business_id AND t.tenant_id=m.tenant_id AND t.project_id=?)) OR (m.business_type='CHANGE' AND EXISTS (SELECT 1 FROM nso_change_order c WHERE c.id=m.business_id AND c.tenant_id=m.tenant_id AND c.project_id=?)))", Integer.class,
                TenantContext.tenantId(), userId, projectId, projectId, projectId);
        if ((tasks != null && tasks > 0) || (actions != null && actions > 0)
                || (approvals != null && approvals > 0) || (pendingMessages != null && pendingMessages > 0)) {
            throw new BusinessException("成员仍有未交接的任务、待办、审批或风险处置项，不能移除或交接项目经理");
        }
    }

    // 获取用户展示名称。
    public String displayName(SysUser user) {
        return user.getNickname() == null || user.getNickname().isBlank()
                ? user.getUsername()
                : user.getNickname();
    }

    private String ownerSlot(Long projectId, String responsibility) {
        return projectId + ":" + responsibilities.normalizeCode(responsibility);
    }

    private void relinquishOwner(ProjectMember incoming, String responsibility) {
        if (!responsibilities.isOwner(responsibility)) {
            return;
        }
        String slot = ownerSlot(incoming.getProjectId(), responsibility);
        String memberResponsibility = responsibilities.normalizeCode(responsibility).replace("_OWNER", "_MEMBER");
        jdbc.update("UPDATE nso_project_member_responsibility "
                        + "SET owner_slot=NULL, primary_flag=0, responsibility_code=?, version=version+1 "
                        + "WHERE tenant_id=? AND project_id=? AND owner_slot=? "
                        + "AND project_member_id<>? AND status='ACTIVE' AND deleted=0",
                memberResponsibility, TenantContext.tenantId(), incoming.getProjectId(), slot, incoming.getId());
    }

    private void resolveOwnerGap(Long projectId, String responsibility) {
        if (!responsibilities.isOwner(responsibility)) {
            return;
        }
        jdbc.update("UPDATE nso_project_responsibility_reconciliation SET status='RESOLVED',resolved_by=?,resolved_at=NOW() WHERE tenant_id=? AND project_id=? AND responsibility_code=? AND status='OPEN'",
                TenantContext.userId(), TenantContext.tenantId(), projectId, responsibilities.normalizeCode(responsibility));
    }
}
