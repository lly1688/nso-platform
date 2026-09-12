package com.nso.business.project.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.business.core.NsoDtos.RequirementDto;
import com.nso.business.core.NsoDtos.RequirementRequest;
import com.nso.business.core.PageSupport;
import com.nso.business.core.TenantContext;
import com.nso.business.project.domain.ProjectRequirement;
import com.nso.business.project.mapper.ProjectRequirementMapper;
import com.nso.business.support.BusinessEventService;
import com.nso.shared.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;

// 项目需求的持久化、确认和事件协作组件。
@Component

// 项目需求协调器 协调项目需求的管理与确认
public class ProjectRequirementCoordinator {
    // 项目需求数据映射
    private final ProjectRequirementMapper requirements;
    // 业务事件服务
    private final BusinessEventService events;

    public ProjectRequirementCoordinator(ProjectRequirementMapper requirements, BusinessEventService events) {
        this.requirements = requirements;
        this.events = events;
    }

    // 为新项目创建初始需求。
    public void createInitial(Long projectId, List<String> contents) {
        if (contents == null) {
            return;
        }
        for (String content : contents) {
            if (content == null || content.isBlank()) {
                continue;
            }
            ProjectRequirement requirement = new ProjectRequirement();
            requirement.setTenantId(TenantContext.tenantId());
            requirement.setProjectId(projectId);
            requirement.setCategory("GENERAL");
            requirement.setContent(content.trim());
            requirement.setConfirmStatus("UNCONFIRMED");
            requirements.insert(requirement);
        }
    }

    // 判断项目是否已有需求。
    public boolean exists(Long projectId) {
        return requirements.selectCount(Wrappers.<ProjectRequirement>lambdaQuery()
                .eq(ProjectRequirement::getProjectId, projectId)) > 0;
    }

    // 复制项目需求。
    public void copy(Long sourceProjectId, Long targetProjectId) {
        for (ProjectRequirement requirement : requirements.selectList(Wrappers.<ProjectRequirement>lambdaQuery()
                .eq(ProjectRequirement::getProjectId, sourceProjectId))) {
            ProjectRequirement cloned = new ProjectRequirement();
            cloned.setTenantId(TenantContext.tenantId());
            cloned.setProjectId(targetProjectId);
            cloned.setCategory(requirement.getCategory());
            cloned.setContent(requirement.getContent());
            cloned.setConfirmStatus("UNCONFIRMED");
            requirements.insert(cloned);
        }
    }

    // 查询项目需求。
    public PageResult<RequirementDto> list(Long projectId) {
        List<RequirementDto> rows = requirements.selectList(Wrappers.<ProjectRequirement>lambdaQuery()
                        .eq(ProjectRequirement::getProjectId, projectId)
                        .orderByAsc(ProjectRequirement::getId))
                .stream()
                .map(this::toDto)
                .toList();
        return new PageResult<>(rows, rows.size());
    }

    // 分页查询项目需求。
    public PageResult<RequirementDto> list(Long projectId, PageQuery pageQuery) {
        PageQuery page = PageSupport.normalize(pageQuery);
        Page<ProjectRequirement> entityPage = requirements.selectPage(PageSupport.page(page),
                Wrappers.<ProjectRequirement>lambdaQuery()
                        .eq(ProjectRequirement::getProjectId, projectId)
                        .orderByAsc(ProjectRequirement::getId));
        return PageSupport.result(entityPage, page, this::toDto);
    }

    // 保存项目需求。
    public RequirementDto save(Long projectId, RequirementRequest request) {
        if (request == null || request.content() == null || request.content().isBlank()) {
            throw new BusinessException("需求内容不能为空");
        }
        ProjectRequirement requirement = new ProjectRequirement();
        requirement.setTenantId(TenantContext.tenantId());
        requirement.setProjectId(projectId);
        requirement.setCategory(request.category() == null || request.category().isBlank() ? "GENERAL" : request.category());
        requirement.setContent(request.content().trim());
        requirement.setConfirmStatus(request.confirmStatus() == null || request.confirmStatus().isBlank() ? "UNCONFIRMED" : request.confirmStatus());
        requirement.setLastReason(request.reason());
        requirements.insert(requirement);
        events.record(projectId, "REQUIREMENT", requirement.getId(), "REQUIREMENT_CREATED", null, requirement.getConfirmStatus(), "已新增项目需求");
        return toDto(requirement);
    }

    // 校验并读取项目需求。
    public ProjectRequirement require(Long requirementId) {
        ProjectRequirement requirement = requirements.selectById(requirementId);
        if (requirement == null) {
            throw new BusinessException("需求不存在");
        }
        return requirement;
    }

    // 确认项目需求。
    public RequirementDto confirm(ProjectRequirement requirement, RequirementRequest request) {
        requirement.setConfirmStatus(request == null || request.confirmStatus() == null || request.confirmStatus().isBlank()
                ? "INTERNAL_CONFIRMED"
                : request.confirmStatus());
        requirement.setLastReason(request == null ? null : request.reason());
        if (requirements.updateById(requirement) != 1) {
            throw BusinessException.ruleBlock("OPTIMISTIC_LOCK", "需求已被其他用户修改", "stale", "latest", "刷新后重试");
        }
        events.record(requirement.getProjectId(), "REQUIREMENT", requirement.getId(), "REQUIREMENT_CONFIRMED", null,
                requirement.getConfirmStatus(), "需求确认状态已更新");
        return toDto(requirement);
    }

    private RequirementDto toDto(ProjectRequirement source) {
        return new RequirementDto(source.getId(), source.getProjectId(), source.getCategory(), source.getContent(),
                source.getConfirmStatus(), source.getLastReason());
    }
}
