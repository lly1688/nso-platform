package com.nso.business.project.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nso.business.core.TenantContext;
import com.nso.business.project.domain.ProjectMember;
import com.nso.business.project.domain.ProjectMemberResponsibility;
import com.nso.business.project.mapper.ProjectMemberMapper;
import com.nso.business.project.mapper.ProjectMemberResponsibilityMapper;
import com.nso.shared.exception.BusinessException;
import com.nso.system.domain.SysUser;
import com.nso.system.security.UserLifecycle;
import com.nso.system.mapper.SysUserMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

// 有效成员决定项目范围，管理员与高管保留全租户可见性。
@Service

// 项目数据权限范围 定义项目相关的数据权限范围规则
public class ProjectDataScope {
    // 项目成员数据映射
    private final ProjectMemberMapper members;
    // 项目成员职责数据映射
    private final ProjectMemberResponsibilityMapper responsibilities;
    // 系统用户数据映射
    private final SysUserMapper users;
    // JDBC模板
    private final JdbcTemplate jdbc;

    public ProjectDataScope(ProjectMemberMapper members, ProjectMemberResponsibilityMapper responsibilities, SysUserMapper users, JdbcTemplate jdbc) {
        this.members = members;
        this.responsibilities = responsibilities;
        this.users = users;
        this.jdbc = jdbc;
    }

    // 判断当前用户是否受项目数据范围限制。
    public boolean isRestricted() {
        return TenantContext.userId() != null && !TenantContext.hasAnyRole("admin", "executive");
    }

    // 校验当前用户可访问项目。
    public void requireAccess(Long projectId) {
        if (!isRestricted()) return;
        long count = members.selectCount(Wrappers.<ProjectMember>lambdaQuery().eq(ProjectMember::getProjectId, projectId).eq(ProjectMember::getUserId, TenantContext.userId()).eq(ProjectMember::getStatus, "ACTIVE"));
        List<Long> visible = visibleProjectIds();
        if (count == 0 && (visible == null || visible.contains(projectId))) return;
        if (count == 0) throw BusinessException.accessDenied("PROJECT_DATA_SCOPE", "无权访问此项目", String.valueOf(projectId), "项目成员", "联系项目经理加入项目");
    }

    // 校验当前用户在项目中的职责。
    public void requireProjectRole(Long projectId, String... projectRoles) {
        if (!isRestricted()) return;
        List<String> expected = java.util.Arrays.stream(projectRoles)
                .filter(role -> role != null && !role.isBlank())
                .flatMap(role -> compatibleResponsibilities(role).stream())
                .distinct().toList();
        long count = responsibilities.selectCount(Wrappers.<ProjectMemberResponsibility>lambdaQuery()
                .eq(ProjectMemberResponsibility::getProjectId, projectId)
                .eq(ProjectMemberResponsibility::getUserId, TenantContext.userId())
                .eq(ProjectMemberResponsibility::getStatus, "ACTIVE")
                .in(!expected.isEmpty(), ProjectMemberResponsibility::getResponsibilityCode, expected));
        if (count == 0 && responsibilities.selectCount(Wrappers.<ProjectMemberResponsibility>lambdaQuery()
                .eq(ProjectMemberResponsibility::getProjectId, projectId)
                .eq(ProjectMemberResponsibility::getUserId, TenantContext.userId())
                .eq(ProjectMemberResponsibility::getStatus, "ACTIVE")) == 0) {
            count = members.selectCount(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, TenantContext.userId())
                .eq(ProjectMember::getStatus, "ACTIVE")
                .in(!expected.isEmpty(), ProjectMember::getProjectRole, expected));
        }
        if (count == 0) {
            throw BusinessException.accessDenied("PROJECT_ROLE_SCOPE", "当前项目职责不允许执行此操作",
                    String.valueOf(projectId), String.join(",", expected), "由项目经理分配相应项目职责");
        }
    }

    // 读取项目中的有效成员。
    public ProjectMember requireActiveMember(Long projectId, Long userId) {
        ProjectMember member = members.selectOne(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId)
                .eq(ProjectMember::getStatus, "ACTIVE")
                .last("LIMIT 1"));
        if (member == null) {
            throw BusinessException.accessDenied("PROJECT_DATA_SCOPE", "任务责任人不是项目有效成员",
                    String.valueOf(projectId), "项目有效成员", "先将责任人加入项目");
        }
        SysUser user = users.selectById(userId);
        if (user == null || !UserLifecycle.isActive(user.getStatus()) || !"INTERNAL".equals(user.getUserType())) {
            throw BusinessException.accessDenied("PROJECT_MEMBER_USER", "任务责任人不是启用的内部账号",
                    String.valueOf(userId), "启用内部项目成员", "重新选择任务责任人");
        }
        return member;
    }

    // 职责记录优先，缺失时兼容旧成员角色。
    public ProjectMember requireActiveMemberForResponsibilities(Long projectId, Long userId, String... projectRoles) {
        ProjectMember member = requireActiveMember(projectId, userId);
        List<String> expected = java.util.Arrays.stream(projectRoles)
                .filter(role -> role != null && !role.isBlank())
                .flatMap(role -> compatibleResponsibilities(role).stream())
                .distinct().toList();
        long matched = responsibilities.selectCount(Wrappers.<ProjectMemberResponsibility>lambdaQuery()
                .eq(ProjectMemberResponsibility::getProjectId, projectId)
                .eq(ProjectMemberResponsibility::getUserId, member.getUserId())
                .eq(ProjectMemberResponsibility::getStatus, "ACTIVE")
                .in(!expected.isEmpty(), ProjectMemberResponsibility::getResponsibilityCode, expected));
        if (matched == 0 && responsibilities.selectCount(Wrappers.<ProjectMemberResponsibility>lambdaQuery()
                .eq(ProjectMemberResponsibility::getProjectId, projectId)
                .eq(ProjectMemberResponsibility::getUserId, member.getUserId())
                .eq(ProjectMemberResponsibility::getStatus, "ACTIVE")) == 0) {
            matched = members.selectCount(Wrappers.<ProjectMember>lambdaQuery()
                    .eq(ProjectMember::getId, member.getId())
                    .eq(ProjectMember::getProjectId, projectId)
                    .eq(ProjectMember::getStatus, "ACTIVE")
                    .in(!expected.isEmpty(), ProjectMember::getProjectRole, expected));
        }
        if (matched == 0) {
            throw BusinessException.ruleBlock("TASK_ASSIGNEE_RESPONSIBILITY", "任务责任人的项目职责与任务类型不匹配",
                    member.getProjectRole(), String.join(",", expected), "请选择匹配岗位的项目成员");
        }
        return member;
    }

    // 按项目职责查找可用受分配人。
    public ProjectMember findActiveMemberForResponsibilities(Long projectId, String... projectRoles) {
        List<String> expected = java.util.Arrays.stream(projectRoles)
                .filter(role -> role != null && !role.isBlank())
                .flatMap(role -> compatibleResponsibilities(role).stream())
                .distinct().toList();
        if (expected.isEmpty()) return null;

        List<ProjectMemberResponsibility> assignments = responsibilities.selectList(
                Wrappers.<ProjectMemberResponsibility>lambdaQuery()
                        .eq(ProjectMemberResponsibility::getProjectId, projectId)
                        .eq(ProjectMemberResponsibility::getStatus, "ACTIVE")
                        .in(ProjectMemberResponsibility::getResponsibilityCode, expected)
                        .orderByDesc(ProjectMemberResponsibility::getPrimaryFlag)
                        .orderByAsc(ProjectMemberResponsibility::getId));
        for (ProjectMemberResponsibility assignment : assignments) {
            ProjectMember member = findUsableMember(projectId, assignment.getProjectMemberId(), assignment.getUserId());
            if (member != null) return member;
        }

        // 旧项目尚无职责记录时，再按历史成员角色查找。
        List<ProjectMember> legacyMembers = members.selectList(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getStatus, "ACTIVE")
                .in(ProjectMember::getProjectRole, expected)
                .orderByAsc(ProjectMember::getId));
        for (ProjectMember member : legacyMembers) {
            ProjectMember usable = findUsableMember(projectId, member.getId(), member.getUserId());
            if (usable != null) return usable;
        }
        return null;
    }

    // 查询当前用户可见项目编号。
    public List<Long> visibleProjectIds() {
        if (!isRestricted()) return null;
        List<Long> direct = members.selectList(Wrappers.<ProjectMember>lambdaQuery().eq(ProjectMember::getUserId, TenantContext.userId()).eq(ProjectMember::getStatus, "ACTIVE")).stream().map(ProjectMember::getProjectId).distinct().toList();
        SysUser user = users.selectById(TenantContext.userId());
        java.util.LinkedHashSet<Long> visible = new java.util.LinkedHashSet<>(direct);
        java.util.LinkedHashSet<Long> scopedDepts = new java.util.LinkedHashSet<>();
        for (String role : TenantContext.roles()) {
            String scope = jdbc.query("SELECT data_scope FROM sys_role WHERE tenant_id=? AND role_code=? AND deleted=0 LIMIT 1", rs -> rs.next() ? rs.getString(1) : null, TenantContext.tenantId(), role);
            if ("ALL".equals(scope)) return null;
            if ("PROJECT_OWNER".equals(scope)) {
                visible.addAll(jdbc.queryForList("SELECT id FROM nso_project WHERE tenant_id=? AND owner_user_id=? AND deleted=0", Long.class, TenantContext.tenantId(), TenantContext.userId()));
            }
            if ("CUSTOM_DEPT".equals(scope)) {
                scopedDepts.addAll(jdbc.queryForList("SELECT rd.dept_id FROM sys_role_dept rd JOIN sys_role r ON r.id=rd.role_id WHERE r.tenant_id=? AND r.role_code=? AND r.deleted=0", Long.class, TenantContext.tenantId(), role));
            }
            if (user != null && user.getDeptId() != null && "DEPT".equals(scope)) scopedDepts.add(user.getDeptId());
            if (user != null && user.getDeptId() != null && "DEPT_AND_CHILD".equals(scope)) {
                scopedDepts.addAll(jdbc.queryForList("SELECT id FROM sys_dept WHERE tenant_id=? AND deleted=0 AND (id=? OR parent_id=? OR ancestors LIKE ?)", Long.class,
                        TenantContext.tenantId(), user.getDeptId(), user.getDeptId(), "%," + user.getDeptId() + ",%"));
            }
        }
        return projectIdsForDepartments(List.copyOf(visible), List.copyOf(scopedDepts));
    }

    private List<String> compatibleResponsibilities(String role) {
        return switch (role.trim().toUpperCase()) {
            case "TECHNICAL" -> List.of("TECHNICAL", "TECH_MEMBER", "TECH_OWNER");
            case "PROCESS" -> List.of("PROCESS", "PROCESS_MEMBER", "PROCESS_OWNER");
            case "PURCHASER" -> List.of("PURCHASER", "PURCHASE_MEMBER", "PURCHASE_OWNER");
            case "PRODUCTION" -> List.of("PRODUCTION", "PRODUCTION_MEMBER", "PRODUCTION_OWNER");
            case "QUALITY" -> List.of("QUALITY", "QUALITY_MEMBER", "QUALITY_OWNER");
            default -> List.of(role.trim().toUpperCase());
        };
    }

    private ProjectMember findUsableMember(Long projectId, Long memberId, Long userId) {
        if (memberId == null || userId == null) return null;
        ProjectMember member = members.selectOne(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getId, memberId)
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, userId)
                .eq(ProjectMember::getStatus, "ACTIVE")
                .last("LIMIT 1"));
        if (member == null) return null;
        SysUser user = users.selectById(userId);
        return user != null && UserLifecycle.isActive(user.getStatus()) && "INTERNAL".equals(user.getUserType()) ? member : null;
    }

    private List<Long> projectIdsForDepartments(List<Long> direct, List<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) return direct;
        List<Long> deptUserIds = jdbc.queryForList("SELECT id FROM sys_user WHERE tenant_id=? AND dept_id IN (" + placeholders(deptIds.size()) + ") AND status='ACTIVE'", Long.class, arguments(deptIds));
        if (deptUserIds.isEmpty()) return direct;
        return java.util.stream.Stream.concat(direct.stream(), members.selectList(Wrappers.<ProjectMember>lambdaQuery().in(ProjectMember::getUserId, deptUserIds).eq(ProjectMember::getStatus, "ACTIVE")).stream().map(ProjectMember::getProjectId)).distinct().toList();
    }

    private String placeholders(int count) {
        return String.join(",", java.util.Collections.nCopies(count, "?"));
    }

    private Object[] arguments(List<Long> ids) {
        Object[] args = new Object[ids.size() + 1];
        args[0] = TenantContext.tenantId();
        for (int i = 0; i < ids.size(); i++) {
            args[i + 1] = ids.get(i);
        }
        return args;
    }
}
