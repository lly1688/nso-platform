package com.nso.business.project.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.business.core.NsoDtos.CustomerProjectDto;
import com.nso.business.core.NsoDtos.ProjectDto;
import com.nso.business.core.NsoDtos.ProjectMemberDto;
import com.nso.business.core.NsoDtos.ProjectMemberRequest;
import com.nso.business.core.NsoDtos.ProjectRequest;
import com.nso.business.core.NsoDtos.RequirementDto;
import com.nso.business.core.NsoDtos.RequirementRequest;
import com.nso.business.core.TenantContext;
import com.nso.business.customer.domain.Customer;
import com.nso.business.customer.mapper.CustomerMapper;
import com.nso.business.project.domain.Project;
import com.nso.business.project.domain.ProjectMember;
import com.nso.business.project.domain.ProjectRequirement;
import com.nso.business.project.mapper.ProjectMapper;
import com.nso.business.project.mapper.ProjectMemberMapper;
import com.nso.business.project.mapper.ProjectRequirementMapper;
import com.nso.business.project.service.IProjectService;
import com.nso.business.project.service.ProjectDataScope;
import com.nso.business.support.BusinessEventService;
import com.nso.common.exception.BusinessException;
import com.nso.system.domain.SysUser;
import com.nso.system.mapper.SysUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class ProjectServiceImpl implements IProjectService {
    private final ProjectMapper projectMapper;
    private final CustomerMapper customerMapper;
    private final ProjectRequirementMapper requirementMapper;
    private final ProjectMemberMapper memberMapper;
    private final SysUserMapper userMapper;
    private final ProjectDataScope dataScope;
    private final BusinessEventService events;

    public ProjectServiceImpl(ProjectMapper projectMapper, CustomerMapper customerMapper,
                               ProjectRequirementMapper requirementMapper, ProjectMemberMapper memberMapper, SysUserMapper userMapper, ProjectDataScope dataScope, BusinessEventService events) {
        this.projectMapper = projectMapper;
        this.customerMapper = customerMapper;
        this.requirementMapper = requirementMapper;
        this.memberMapper = memberMapper;
        this.userMapper = userMapper;
        this.dataScope = dataScope;
        this.events = events;
    }

    @Override
    public PageResult<ProjectDto> list(String keyword) {
        List<Long> visibleIds = dataScope.visibleProjectIds();
        if (visibleIds != null && visibleIds.isEmpty()) return new PageResult<>(List.of(), 0);
        List<ProjectDto> rows = projectMapper.selectList(Wrappers.<Project>lambdaQuery()
                        .in(visibleIds != null, Project::getId, visibleIds == null ? List.of() : visibleIds)
                        .and(keyword != null && !keyword.isBlank(), query -> query.like(Project::getProjectNo, keyword)
                                .or().like(Project::getCustomerName, keyword)
                                .or().like(Project::getProductName, keyword))
                        .orderByDesc(Project::getId))
                .stream().map(this::toDto).toList();
        return new PageResult<>(rows, rows.size());
    }

    @Override
    public PageResult<CustomerProjectDto> customerProjects(String keyword) {
        PageResult<ProjectDto> page = list(keyword);
        List<CustomerProjectDto> rows = page.list().stream()
                .map(project -> new CustomerProjectDto(project.id(), project.projectNo(), project.productName(),
                        project.targetDate(), project.status(), project.stage(), project.sampleStatus()))
                .toList();
        return new PageResult<>(rows, rows.size());
    }

    @Override
    @Transactional
    public ProjectDto create(ProjectRequest request) {
        if (request == null || request.productName() == null || request.productName().isBlank()) {
            throw new BusinessException("产品名称不能为空");
        }
        String customerName = request.customerName();
        if (request.customerId() != null) {
            Customer customer = customerMapper.selectById(request.customerId());
            if (customer == null || !"ENABLED".equals(customer.getStatus())) {
                throw new BusinessException("客户不存在或已禁用");
            }
            customerName = customer.getName();
        }
        if (customerName == null || customerName.isBlank()) {
            throw new BusinessException("客户名称不能为空");
        }
        Project project = new Project();
        project.setTenantId(TenantContext.tenantId());
        project.setProjectNo("NSO-" + LocalDate.now().toString().replace("-", "") + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        project.setCustomerId(request.customerId());
        project.setCustomerName(customerName.trim());
        project.setProductName(request.productName().trim());
        project.setOwnerUserId(TenantContext.userId());
        project.setQuantity(request.quantity() == null || request.quantity() < 1 ? 1 : request.quantity());
        project.setTargetDate(request.targetDate());
        project.setPlanStartDate(LocalDate.now());
        project.setOwnerName(request.ownerName());
        project.setStatus("DRAFT");
        project.setStage("REQUIREMENT");
        project.setPriority(request.priority() == null || request.priority().isBlank() ? "MEDIUM" : request.priority());
        project.setRiskLevel("LOW");
        project.setRiskScore(0);
        project.setSampleStatus("NONE");
        projectMapper.insert(project);
        if (TenantContext.userId() != null) {
            ProjectMember owner = new ProjectMember();
            owner.setTenantId(TenantContext.tenantId());
            owner.setProjectId(project.getId());
            owner.setUserId(TenantContext.userId());
            owner.setMemberName(TenantContext.username());
            owner.setProjectRole("PROJECT_MANAGER");
            owner.setStatus("ACTIVE");
            memberMapper.insert(owner);
        }
        if (request.requirements() != null) {
            for (String content : request.requirements()) {
                if (content != null && !content.isBlank()) {
                    ProjectRequirement requirement = new ProjectRequirement();
                    requirement.setTenantId(TenantContext.tenantId());
                    requirement.setProjectId(project.getId());
                    requirement.setCategory("GENERAL");
                    requirement.setContent(content.trim());
                    requirement.setConfirmStatus("UNCONFIRMED");
                    requirementMapper.insert(requirement);
                }
            }
        }
        events.record(project.getId(), "PROJECT", project.getId(), "PROJECT_CREATED", null, "DRAFT", "已创建项目 " + project.getProjectNo());
        return toDto(project);
    }

    @Override
    @Transactional
    public ProjectDto submitReview(Long projectId) {
        Project project = requireProject(projectId);
        dataScope.requireProjectRole(projectId, "PROJECT_MANAGER");
        if (!"DRAFT".equals(project.getStatus())) {
            throw BusinessException.ruleBlock("PROJECT_STATUS", "只有草稿项目可以提交评审", project.getStatus(), "DRAFT", "请刷新项目状态");
        }
        if (requirementMapper.selectCount(Wrappers.<ProjectRequirement>lambdaQuery().eq(ProjectRequirement::getProjectId, projectId)) == 0) {
            throw BusinessException.ruleBlock("PROJECT_REQUIREMENT", "至少需要一条需求后才能提交评审", "0", ">=1", "补充项目需求");
        }
        project.setStatus("REVIEWING");
        project.setStage("TECHNICAL");
        ensureUpdated(project, "项目已被其他用户修改");
        events.record(projectId, "PROJECT", projectId, "PROJECT_SUBMITTED_REVIEW", "DRAFT", "REVIEWING", "项目已提交需求评审");
        return toDto(project);
    }

    @Override
    public PageResult<RequirementDto> requirements(Long projectId) {
        requireProject(projectId);
        List<RequirementDto> rows = requirementMapper.selectList(Wrappers.<ProjectRequirement>lambdaQuery()
                        .eq(ProjectRequirement::getProjectId, projectId).orderByAsc(ProjectRequirement::getId))
                .stream().map(this::toDto).toList();
        return new PageResult<>(rows, rows.size());
    }

    @Override
    @Transactional
    public RequirementDto saveRequirement(Long projectId, RequirementRequest request) {
        requireProject(projectId);
        dataScope.requireProjectRole(projectId, "PROJECT_MANAGER");
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
        requirementMapper.insert(requirement);
        events.record(projectId, "REQUIREMENT", requirement.getId(), "REQUIREMENT_CREATED", null, requirement.getConfirmStatus(), "已新增项目需求");
        return toDto(requirement);
    }

    @Override
    @Transactional
    public RequirementDto confirmRequirement(Long requirementId, RequirementRequest request) {
        ProjectRequirement requirement = requirementMapper.selectById(requirementId);
        if (requirement == null) {
            throw new BusinessException("需求不存在");
        }
        dataScope.requireProjectRole(requirement.getProjectId(), "PROJECT_MANAGER");
        requirement.setConfirmStatus(request == null || request.confirmStatus() == null || request.confirmStatus().isBlank()
                ? "INTERNAL_CONFIRMED" : request.confirmStatus());
        requirement.setLastReason(request == null ? null : request.reason());
        if (requirementMapper.updateById(requirement) != 1) {
            throw BusinessException.ruleBlock("OPTIMISTIC_LOCK", "需求已被其他用户修改", "stale", "latest", "刷新后重试");
        }
        events.record(requirement.getProjectId(), "REQUIREMENT", requirementId, "REQUIREMENT_CONFIRMED", null, requirement.getConfirmStatus(), "需求确认状态已更新");
        return toDto(requirement);
    }

    @Override
    public PageResult<ProjectMemberDto> members(Long projectId) {
        requireProject(projectId);
        List<ProjectMemberDto> rows = memberMapper.selectList(Wrappers.<ProjectMember>lambdaQuery()
                        .eq(ProjectMember::getProjectId, projectId).orderByAsc(ProjectMember::getId))
                .stream().map(this::toDto).toList();
        return new PageResult<>(rows, rows.size());
    }

    @Override
    @Transactional
    public ProjectMemberDto addMember(Long projectId, ProjectMemberRequest request) {
        requireProject(projectId);
        dataScope.requireProjectRole(projectId, "PROJECT_MANAGER");
        if (request == null || request.userId() == null || request.memberName() == null || request.memberName().isBlank() || request.projectRole() == null || request.projectRole().isBlank()) {
            throw new BusinessException("成员用户、姓名和项目角色不能为空");
        }
        SysUser user = userMapper.selectById(request.userId());
        if (user == null || user.getTenantId() == null || user.getTenantId() != TenantContext.tenantId() || !"ENABLED".equals(user.getStatus())) {
            throw BusinessException.accessDenied("PROJECT_MEMBER_USER", "成员用户不存在、已停用或不属于当前租户", String.valueOf(request.userId()), "当前租户启用用户", "选择有效系统用户");
        }
        long existing = memberMapper.selectCount(Wrappers.<ProjectMember>lambdaQuery().eq(ProjectMember::getProjectId, projectId).eq(ProjectMember::getUserId, request.userId()));
        if (existing > 0) throw new BusinessException("该用户已是项目成员");
        ProjectMember member = new ProjectMember();
        member.setTenantId(TenantContext.tenantId());
        member.setProjectId(projectId);
        member.setUserId(request.userId());
        member.setMemberName(request.memberName().trim());
        member.setProjectRole(request.projectRole().trim());
        member.setDepartmentName(request.departmentName());
        member.setStatus("ACTIVE");
        memberMapper.insert(member);
        events.record(projectId, "PROJECT_MEMBER", member.getId(), "PROJECT_MEMBER_ADDED", null, "ACTIVE", "已增加项目成员 " + member.getMemberName());
        return toDto(member);
    }

    @Override
    public ProjectDto get(Long projectId) { return toDto(requireProject(projectId)); }

    private Project requireProject(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) { throw new BusinessException("项目不存在"); }
        dataScope.requireAccess(projectId);
        return project;
    }

    private void ensureUpdated(Project project, String message) {
        if (projectMapper.updateById(project) != 1) {
            throw BusinessException.ruleBlock("OPTIMISTIC_LOCK", message, "stale", "latest", "刷新后重试");
        }
    }

    private ProjectDto toDto(Project source) {
        long daysLeft = source.getTargetDate() == null ? 0 : ChronoUnit.DAYS.between(LocalDate.now(), source.getTargetDate());
        return new ProjectDto(source.getId(), source.getProjectNo(), source.getCustomerId(), source.getCustomerName(), source.getProductName(),
                source.getQuantity(), source.getTargetDate(), source.getOwnerName(), source.getStatus(), source.getStage(), source.getPriority(),
                source.getRiskLevel(), source.getSampleStatus(), 0, daysLeft);
    }

    private RequirementDto toDto(ProjectRequirement source) {
        return new RequirementDto(source.getId(), source.getProjectId(), source.getCategory(), source.getContent(), source.getConfirmStatus(), source.getLastReason());
    }

    private ProjectMemberDto toDto(ProjectMember source) {
        return new ProjectMemberDto(source.getId(), source.getProjectId(), source.getUserId(), source.getMemberName(), source.getProjectRole(), source.getDepartmentName(), source.getStatus());
    }
}
