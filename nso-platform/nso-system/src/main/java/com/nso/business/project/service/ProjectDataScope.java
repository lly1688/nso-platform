package com.nso.business.project.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nso.business.core.TenantContext;
import com.nso.business.project.domain.ProjectMember;
import com.nso.business.project.mapper.ProjectMemberMapper;
import com.nso.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.util.List;

/** Project-level scope is granted by explicit active membership. Administrators and read-only executives retain tenant-wide visibility. */
@Service
public class ProjectDataScope {
    private final ProjectMemberMapper members;

    public ProjectDataScope(ProjectMemberMapper members) { this.members = members; }

    public boolean isRestricted() { return TenantContext.userId() != null && !TenantContext.hasAnyRole("admin", "executive"); }

    public void requireAccess(Long projectId) {
        if (!isRestricted()) return;
        long count = members.selectCount(Wrappers.<ProjectMember>lambdaQuery().eq(ProjectMember::getProjectId, projectId).eq(ProjectMember::getUserId, TenantContext.userId()).eq(ProjectMember::getStatus, "ACTIVE"));
        if (count == 0) throw BusinessException.accessDenied("PROJECT_DATA_SCOPE", "无权访问此项目", String.valueOf(projectId), "项目成员", "联系项目经理加入项目");
    }

    public void requireProjectRole(Long projectId, String... projectRoles) {
        if (!isRestricted()) return;
        List<String> expected = java.util.Arrays.stream(projectRoles)
                .filter(role -> role != null && !role.isBlank())
                .map(role -> role.trim().toUpperCase())
                .toList();
        long count = members.selectCount(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getUserId, TenantContext.userId())
                .eq(ProjectMember::getStatus, "ACTIVE")
                .in(!expected.isEmpty(), ProjectMember::getProjectRole, expected));
        if (count == 0) {
            throw BusinessException.accessDenied("PROJECT_ROLE_SCOPE", "当前项目职责不允许执行此操作",
                    String.valueOf(projectId), String.join(",", expected), "由项目经理分配相应项目职责");
        }
    }

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
        return member;
    }

    public List<Long> visibleProjectIds() {
        if (!isRestricted()) return null;
        return members.selectList(Wrappers.<ProjectMember>lambdaQuery().eq(ProjectMember::getUserId, TenantContext.userId()).eq(ProjectMember::getStatus, "ACTIVE")).stream().map(ProjectMember::getProjectId).distinct().toList();
    }
}
