package com.nso.business.project.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.business.core.PageSupport;
import com.nso.business.core.NsoDtos.CustomerProjectDto;
import com.nso.business.core.NsoDtos.ProjectDto;
import com.nso.business.core.NsoDtos.ProjectMemberDto;
import com.nso.business.core.NsoDtos.ProjectMemberRequest;
import com.nso.business.core.NsoDtos.ProjectMemberUpdateRequest;
import com.nso.business.core.NsoDtos.ProjectManagerTransferRequest;
import com.nso.business.core.NsoDtos.ProjectMemberCandidateDto;
import com.nso.business.core.NsoDtos.ProjectRequest;
import com.nso.business.core.NsoDtos.ProjectActionRequest;
import com.nso.business.core.NsoDtos.ProjectCopyRequest;
import com.nso.business.core.NsoDtos.ProjectStatsDto;
import com.nso.business.core.NsoDtos.ProjectStatusHistoryDto;
import com.nso.business.core.NsoDtos.RequirementDto;
import com.nso.business.core.NsoDtos.RequirementRequest;
import com.nso.business.core.TenantContext;
import com.nso.business.customer.domain.Customer;
import com.nso.business.customer.mapper.CustomerMapper;
import com.nso.business.project.domain.Project;
import com.nso.business.project.domain.ProjectMember;
import com.nso.business.project.domain.ProjectMemberResponsibility;
import com.nso.business.project.domain.ProjectRequirement;
import com.nso.business.project.mapper.ProjectMapper;
import com.nso.business.project.mapper.ProjectMemberMapper;
import com.nso.business.project.mapper.ProjectMemberResponsibilityMapper;
import com.nso.business.project.service.IProjectService;
import com.nso.business.project.service.ProjectDataScope;
import com.nso.business.project.service.ProjectLifecycleCoordinator;
import com.nso.business.project.service.ProjectMembershipCoordinator;
import com.nso.business.project.service.ProjectQueryCoordinator;
import com.nso.business.project.service.ProjectRequirementCoordinator;
import com.nso.business.project.service.ProjectResponsibilityCatalog;
import com.nso.business.support.BusinessEventService;
import com.nso.business.support.BusinessNumberService;
import com.nso.shared.exception.BusinessException;
import com.nso.system.domain.SysUser;
import com.nso.system.security.UserLifecycle;
import com.nso.system.domain.SysDept;
import com.nso.system.mapper.SysDeptMapper;
import com.nso.system.mapper.SysUserMapper;
import com.nso.system.service.ISysUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.List;

// 项目协同服务。
@Service

// 项目协同 服务层处理
public class ProjectServiceImpl implements IProjectService {
    // 项目数据映射
    private final ProjectMapper projectMapper;
    // 客户数据映射
    private final CustomerMapper customerMapper;
    // 项目需求协调器
    private final ProjectRequirementCoordinator requirements;
    // 项目成员数据映射
    private final ProjectMemberMapper memberMapper;
    // 项目成员职责数据映射
    private final ProjectMemberResponsibilityMapper responsibilityMapper;
    // 系统用户数据映射
    private final SysUserMapper userMapper;
    // 系统部门数据映射
    private final SysDeptMapper deptMapper;
    // 系统用户服务
    private final ISysUserService directoryUsers;
    // 项目职责目录
    private final ProjectResponsibilityCatalog responsibilities;
    // 项目数据范围
    private final ProjectDataScope dataScope;
    // 项目生命周期协调器
    private final ProjectLifecycleCoordinator lifecycle;
    // 项目Membership协调器
    private final ProjectMembershipCoordinator membership;
    // 项目查询协调器
    private final ProjectQueryCoordinator queries;
    // 业务事件服务
    private final BusinessEventService events;
    // 业务Number服务
    private final BusinessNumberService numbers;
    // JDBC模板
    private final JdbcTemplate jdbc;

    public ProjectServiceImpl(
            ProjectMapper projectMapper,
            CustomerMapper customerMapper,
            ProjectRequirementCoordinator requirements,
            ProjectMemberMapper memberMapper,
            ProjectMemberResponsibilityMapper responsibilityMapper,
            SysUserMapper userMapper,
            SysDeptMapper deptMapper,
            ISysUserService directoryUsers,
            ProjectResponsibilityCatalog responsibilities,
            ProjectDataScope dataScope,
            ProjectLifecycleCoordinator lifecycle,
            ProjectMembershipCoordinator membership,
            ProjectQueryCoordinator queries,
            BusinessEventService events,
            BusinessNumberService numbers,
            JdbcTemplate jdbc) {
        this.projectMapper = projectMapper;
        this.customerMapper = customerMapper;
        this.requirements = requirements;
        this.memberMapper = memberMapper;
        this.responsibilityMapper = responsibilityMapper;
        this.userMapper = userMapper;
        this.deptMapper = deptMapper;
        this.directoryUsers = directoryUsers;
        this.responsibilities = responsibilities;
        this.dataScope = dataScope;
        this.lifecycle = lifecycle;
        this.membership = membership;
        this.queries = queries;
        this.events = events;
        this.numbers = numbers;
        this.jdbc = jdbc;
    }

    // 查询当前用户可见的项目。
    @Override
    public PageResult<ProjectDto> list(String keyword) {
        return queries.list(keyword);
    }

    // 按条件分页查询项目。
    @Override
    public PageResult<ProjectDto> list(String keyword, String stage, String status, String riskLevel,
                                       String dueState, String quickFilter, PageQuery pageQuery) {
        return queries.list(keyword, stage, status, riskLevel, dueState, quickFilter, pageQuery);
    }

    // 查询客户可见项目。
    @Override
    public PageResult<CustomerProjectDto> customerProjects(String keyword) {
        return queries.customerProjects(keyword);
    }

    // 分页查询客户可见项目。
    @Override
    public PageResult<CustomerProjectDto> customerProjects(String keyword, PageQuery pageQuery) {
        return queries.customerProjects(keyword, pageQuery);
    }

    // 汇总项目列表统计数据。
    @Override
    public ProjectStatsDto stats(String keyword, String stage, String status, String riskLevel, String dueState,
                                 String quickFilter, PageQuery attentionPage) {
        return queries.stats(keyword, stage, status, riskLevel, dueState, quickFilter, attentionPage);
    }

    // 新项目会同步建立成员、需求和职责范围。
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
        Long managerUserId = request.managerUserId() == null ? TenantContext.userId() : request.managerUserId();
        SysUser manager = requireProjectManager(managerUserId);
        Project project = new Project();
        project.setTenantId(TenantContext.tenantId());
        project.setProjectNo(numbers.next("PROJECT"));
        project.setCustomerId(request.customerId());
        project.setCustomerName(customerName.trim());
        project.setProductName(request.productName().trim());
        project.setOwnerUserId(manager.getId());
        project.setQuantity(request.quantity() == null || request.quantity() < 1 ? 1 : request.quantity());
        project.setTargetDate(request.targetDate());
        project.setPlanStartDate(LocalDate.now());
        project.setOwnerName(displayName(manager));
        project.setStatus("DRAFT");
        project.setStage("REQUIREMENT");
        project.setPriority(request.priority() == null || request.priority().isBlank() ? "MEDIUM" : request.priority());
        project.setRiskLevel("LOW");
        project.setRiskScore(0);
        project.setSampleStatus("NONE");
        projectMapper.insert(project);
        ensureMember(project.getId(), manager, List.of("PROJECT_MANAGER"), "PROJECT_MANAGER");
        if (TenantContext.userId() != null) {
            SysUser creator = userMapper.selectById(TenantContext.userId());
            List<String> creatorRoles = creator == null ? List.of() : directoryUsers.roleCodes(creator.getId());
            if (creator != null && creatorRoles.stream().anyMatch(role -> "sales".equalsIgnoreCase(role))) {
                ProjectMember creatorMember = memberMapper.selectOne(Wrappers.<ProjectMember>lambdaQuery()
                        .eq(ProjectMember::getProjectId, project.getId()).eq(ProjectMember::getUserId, creator.getId()).last("LIMIT 1"));
                List<String> duties = creatorMember == null ? List.of("SALES") : activeResponsibilityCodes(creatorMember);
                if (!duties.contains("SALES")) duties = java.util.stream.Stream.concat(duties.stream(), java.util.stream.Stream.of("SALES")).toList();
                ensureMember(project.getId(), creator, duties, duties.contains("PROJECT_MANAGER") ? "PROJECT_MANAGER" : "SALES");
            }
        }
        requirements.createInitial(project.getId(), request.requirements());
        events.record(project.getId(), "PROJECT", project.getId(), "PROJECT_CREATED", null, "DRAFT", "已创建项目 " + project.getProjectNo());
        return toDto(project);
    }

    // 提交项目立项评审。
    @Override
    @Transactional
    public ProjectDto submitReview(Long projectId) {
        Project project = requireProject(projectId);
        dataScope.requireProjectRole(projectId, "PROJECT_MANAGER");
        if (!"DRAFT".equals(project.getStatus())) {
            throw BusinessException.ruleBlock("PROJECT_STATUS", "只有草稿项目可以提交评审", project.getStatus(), "DRAFT", "请刷新项目状态");
        }
        if (!requirements.exists(projectId)) {
            throw BusinessException.ruleBlock("PROJECT_REQUIREMENT", "至少需要一条需求后才能提交评审", "0", ">=1", "补充项目需求");
        }
        transition(project, "SUBMIT_REVIEW", "REVIEWING", "TECHNICAL", null);
        return toDto(project);
    }

    // 执行项目状态动作。
    @Override
    @Transactional
    public ProjectDto action(Long projectId, String action, ProjectActionRequest request) {
        Project project = requireProject(projectId);
        dataScope.requireProjectRole(projectId, "PROJECT_MANAGER");
        if (request != null && request.version() != null && !request.version().equals(project.getVersion())) {
            throw BusinessException.ruleBlock("OPTIMISTIC_LOCK", "项目已被其他用户更新", String.valueOf(request.version()), String.valueOf(project.getVersion()), "刷新项目后重试");
        }
        String normalized = action == null ? "" : action.trim().toUpperCase();
        String reason = request == null ? null : request.reason();
        switch (normalized) {
            case "APPROVE_REVIEW" -> {
                requireStatus(project, normalized, "REVIEWING");
                transition(project, normalized, "TECH_PREPARING", "TECHNICAL", reason);
            }
            case "REJECT_REVIEW" -> {
                requireStatus(project, normalized, "REVIEWING");
                transition(project, normalized, "DRAFT", "REQUIREMENT", requiredReason(reason));
            }
            case "START_SAMPLING" -> throw BusinessException.ruleBlock("PROJECT_ACTION_SOURCE", "打样阶段只能由创建样品单驱动", normalized, "创建样品", "请先创建关联当前技术包的样品单");
            case "WAIT_CUSTOMER_CONFIRM" -> throw BusinessException.ruleBlock("PROJECT_ACTION_SOURCE", "待客户确认只能由提交样品确认驱动", normalized, "提交客户确认", "请在样品页完成独立检验后提交客户确认");
            case "RECONCILE_SAMPLE_STATE" -> reconcileSampleState(project, requiredReason(reason));
            case "PREPARE_PRODUCTION" -> {
                requireStatus(project, normalized, "CUSTOMER_CONFIRMED");
                transition(project, normalized, "PRODUCTION_PREPARING", "EXECUTION", reason);
            }
            case "START_PRODUCTION" -> {
                requireStatus(project, normalized, "PRODUCTION_PREPARING");
                transition(project, normalized, "EXECUTING", "EXECUTION", reason);
            }
            case "PENDING_DELIVERY" -> throw BusinessException.ruleBlock("PROJECT_ACTION_SOURCE", "待交付只能由创建通过校验的交付记录驱动", normalized, "创建交付记录", "请在任务与交付页完成预检并提交真实交付信息");
            case "COMPLETE" -> {
                requireStatus(project, normalized, "PENDING_DELIVERY");
                assertCompletionReady(project);
                transition(project, normalized, "COMPLETED", "DELIVERY", requiredReason(reason));
            }
            case "SUSPEND" -> {
                requireStatus(project, normalized, "DRAFT", "REVIEWING", "TECH_PREPARING", "TECH_PUBLISHED", "SAMPLING", "CUSTOMER_CONFIRMING", "CUSTOMER_CONFIRMED", "PRODUCTION_PREPARING", "EXECUTING", "PENDING_DELIVERY");
                project.setSuspendedReason(requiredReason(reason));
                transition(project, normalized, "SUSPENDED", project.getStage(), reason);
            }
            case "RESUME" -> {
                requireStatus(project, normalized, "SUSPENDED");
                transition(project, normalized, "TECH_PREPARING", "TECHNICAL", requiredReason(reason));
            }
            case "CANCEL" -> {
                requireStatus(project, normalized, "DRAFT", "REVIEWING", "TECH_PREPARING", "TECH_PUBLISHED", "SAMPLING", "CUSTOMER_CONFIRMING", "CUSTOMER_CONFIRMED", "PRODUCTION_PREPARING");
                transition(project, normalized, "CANCELLED", project.getStage(), requiredReason(reason));
            }
            case "ARCHIVE" -> {
                requireConfirmation(request, "PROJECT_ARCHIVE", projectId);
                archive(project, requiredReason(reason));
            }
            case "RESTORE" -> {
                requireConfirmation(request, "PROJECT_RESTORE", projectId);
                restore(project, requiredReason(reason));
            }
            default -> throw new BusinessException("不支持的项目状态动作：" + action);
        }
        return toDto(project);
    }

    // 复制项目时带入需求、成员和职责。
    @Override
    @Transactional
    public ProjectDto copy(Long projectId, ProjectCopyRequest request) {
        Project source = requireProject(projectId);
        dataScope.requireProjectRole(projectId, "PROJECT_MANAGER");
        Project copy = new Project();
        copy.setTenantId(TenantContext.tenantId());
        copy.setProjectNo(numbers.next("PROJECT"));
        copy.setCustomerId(source.getCustomerId());
        copy.setCustomerName(source.getCustomerName());
        copy.setProductName(request != null && request.productName() != null && !request.productName().isBlank() ? request.productName().trim() : source.getProductName() + "-复制");
        SysUser copyManager = requireProjectManager(TenantContext.userId());
        copy.setOwnerUserId(copyManager.getId());
        copy.setQuantity(source.getQuantity());
        copy.setTargetDate(request == null ? null : request.targetDate());
        copy.setPlanStartDate(LocalDate.now());
        copy.setOwnerName(displayName(copyManager));
        copy.setStatus("DRAFT");
        copy.setStage("REQUIREMENT");
        copy.setPriority(source.getPriority());
        copy.setRiskLevel("LOW");
        copy.setRiskScore(0);
        copy.setSampleStatus("NONE");
        projectMapper.insert(copy);
        requirements.copy(projectId, copy.getId());
        for (ProjectMember member : memberMapper.selectList(Wrappers.<ProjectMember>lambdaQuery().eq(ProjectMember::getProjectId, projectId).eq(ProjectMember::getStatus, "ACTIVE"))) {
            ProjectMember cloned = new ProjectMember();
            cloned.setTenantId(TenantContext.tenantId());
            cloned.setProjectId(copy.getId());
            cloned.setUserId(member.getUserId());
            cloned.setMemberName(member.getMemberName());
            cloned.setProjectRole(member.getProjectRole());
            cloned.setDeptId(member.getDeptId());
            cloned.setDepartmentName(member.getDepartmentName());
            cloned.setStatus("ACTIVE");
            memberMapper.insert(cloned);
            syncResponsibilities(cloned, responsibilityCodes(member), member.getProjectRole());
        }
        ensureMember(copy.getId(), copyManager, List.of("PROJECT_MANAGER"), "PROJECT_MANAGER");
        events.record(copy.getId(), "PROJECT", copy.getId(), "PROJECT_COPIED", null, "DRAFT", "复制自项目 " + source.getProjectNo());
        return toDto(copy);
    }

    // 查询项目状态历史。
    @Override
    public PageResult<ProjectStatusHistoryDto> statusHistory(Long projectId) {
        requireProject(projectId);
        List<ProjectStatusHistoryDto> rows = jdbc.query("SELECT before_status,after_status,action_code,reason,occurred_at FROM nso_project_status_history WHERE tenant_id=? AND project_id=? ORDER BY id DESC",
                (rs, row) -> new ProjectStatusHistoryDto(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getTimestamp(5).toLocalDateTime()), TenantContext.tenantId(), projectId);
        return new PageResult<>(rows, rows.size());
    }

    // 分页查询项目状态历史。
    @Override
    public PageResult<ProjectStatusHistoryDto> statusHistory(Long projectId, PageQuery pageQuery) {
        requireProject(projectId);
        PageQuery page = PageSupport.normalize(pageQuery);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM nso_project_status_history WHERE tenant_id=? AND project_id=?",
                Long.class, TenantContext.tenantId(), projectId);
        List<ProjectStatusHistoryDto> rows = jdbc.query("SELECT before_status,after_status,action_code,reason,occurred_at FROM nso_project_status_history WHERE tenant_id=? AND project_id=? ORDER BY id DESC LIMIT ? OFFSET ?",
                (rs, row) -> new ProjectStatusHistoryDto(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getTimestamp(5).toLocalDateTime()),
                TenantContext.tenantId(), projectId, page.pageSizeValue(), page.offset());
        return new PageResult<>(rows, total == null ? 0 : total, page.pageNoValue(), page.pageSizeValue());
    }

    // 查询项目需求。
    @Override
    public PageResult<RequirementDto> requirements(Long projectId) {
        requireProject(projectId);
        return requirements.list(projectId);
    }

    // 分页查询项目需求。
    @Override
    public PageResult<RequirementDto> requirements(Long projectId, PageQuery pageQuery) {
        requireProject(projectId);
        return requirements.list(projectId, pageQuery);
    }

    // 保存项目需求。
    @Override
    @Transactional
    public RequirementDto saveRequirement(Long projectId, RequirementRequest request) {
        requireProject(projectId);
        dataScope.requireProjectRole(projectId, "PROJECT_MANAGER");
        return requirements.save(projectId, request);
    }

    // 确认项目需求。
    @Override
    @Transactional
    public RequirementDto confirmRequirement(Long requirementId, RequirementRequest request) {
        ProjectRequirement requirement = requirements.require(requirementId);
        dataScope.requireProjectRole(requirement.getProjectId(), "PROJECT_MANAGER");
        return requirements.confirm(requirement, request);
    }

    // 查询项目成员。
    @Override
    public PageResult<ProjectMemberDto> members(Long projectId) {
        requireProject(projectId);
        List<ProjectMemberDto> rows = memberMapper.selectList(Wrappers.<ProjectMember>lambdaQuery()
                        .eq(ProjectMember::getProjectId, projectId).orderByAsc(ProjectMember::getId))
                .stream().map(this::toDto).toList();
        return new PageResult<>(rows, rows.size());
    }

    // 分页查询项目成员。
    @Override
    public PageResult<ProjectMemberDto> members(Long projectId, PageQuery pageQuery) {
        requireProject(projectId);
        PageQuery page = PageSupport.normalize(pageQuery);
        Page<ProjectMember> entityPage = memberMapper.selectPage(PageSupport.page(page),
                Wrappers.<ProjectMember>lambdaQuery().eq(ProjectMember::getProjectId, projectId)
                        .orderByAsc(ProjectMember::getId));
        return PageSupport.result(entityPage, page, this::toDto);
    }

    // 添加项目成员。
    @Override
    @Transactional
    public ProjectMemberDto addMember(Long projectId, ProjectMemberRequest request) {
        requireProject(projectId);
        dataScope.requireProjectRole(projectId, "PROJECT_MANAGER");
        if (request == null || request.userId() == null) throw new BusinessException("请选择成员账号");
        SysUser user = requireAssignableUser(request.userId(), false);
        List<String> dutyCodes = responsibilities.normalize(request.responsibilityCodes(), request.projectRole());
        String primary = responsibilities.primary(dutyCodes, request.primaryResponsibilityCode() == null ? request.projectRole() : request.primaryResponsibilityCode());
        validateResponsibilities(user, dutyCodes);
        ProjectMember member = memberMapper.selectOne(Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId).eq(ProjectMember::getUserId, user.getId()).last("LIMIT 1"));
        if (member != null && "ACTIVE".equals(member.getStatus())) throw new BusinessException("该用户已是项目成员，请使用职责调整功能");
        member = ensureMember(projectId, user, dutyCodes, primary);
        events.record(projectId, "PROJECT_MEMBER", member.getId(), "PROJECT_MEMBER_ADDED", null, "ACTIVE", "已增加项目成员 " + member.getMemberName());
        return toDto(member);
    }

    // 更新项目成员。
    @Override
    @Transactional
    public ProjectMemberDto updateMember(Long projectId, Long memberId, ProjectMemberUpdateRequest request) {
        requireProject(projectId);
        dataScope.requireProjectRole(projectId, "PROJECT_MANAGER");
        ProjectMember member = requireMember(projectId, memberId);
        if (!"ACTIVE".equals(member.getStatus())) throw new BusinessException("已移除成员不能调整职责，请先恢复成员");
        SysUser user = requireAssignableUser(member.getUserId(), false);
        if (request == null) throw new BusinessException("成员职责参数不能为空");
        List<String> dutyCodes = responsibilities.normalize(request.responsibilityCodes(), member.getProjectRole());
        String primary = responsibilities.primary(dutyCodes, request == null ? member.getProjectRole() : request.primaryResponsibilityCode());
        validateResponsibilities(user, dutyCodes);
        syncResponsibilities(member, dutyCodes, primary);
        member.setProjectRole(primary);
        memberMapper.updateById(member);
        events.record(projectId, "PROJECT_MEMBER", memberId, "PROJECT_MEMBER_RESPONSIBILITIES_UPDATED", null, "ACTIVE", "已调整项目成员职责");
        return toDto(member);
    }

    // 移除项目成员。
    @Override
    @Transactional
    public ProjectMemberDto removeMember(Long projectId, Long memberId) {
        requireProject(projectId);
        dataScope.requireProjectRole(projectId, "PROJECT_MANAGER");
        ProjectMember member = requireMember(projectId, memberId);
        Project project = projectMapper.selectById(projectId);
        if (member.getUserId().equals(project.getOwnerUserId())) throw new BusinessException("项目经理必须先完成交接后才能移除");
        assertNoOpenAssignment(projectId, member.getUserId());
        member.setStatus("REMOVED");
        memberMapper.updateById(member);
        responsibilityMapper.selectList(Wrappers.<ProjectMemberResponsibility>lambdaQuery()
                        .eq(ProjectMemberResponsibility::getProjectMemberId, memberId))
                .forEach(row -> {
                    row.setStatus("REMOVED");
                    row.setPrimaryFlag(0);
                    row.setOwnerSlot(null);
                    responsibilityMapper.updateById(row);
                });
        events.record(projectId, "PROJECT_MEMBER", memberId, "PROJECT_MEMBER_REMOVED", "ACTIVE", "REMOVED", "已移除项目成员 " + member.getMemberName());
        return toDto(member);
    }

    // 恢复项目成员。
    @Override
    @Transactional
    public ProjectMemberDto restoreMember(Long projectId, Long memberId) {
        requireProject(projectId);
        dataScope.requireProjectRole(projectId, "PROJECT_MANAGER");
        ProjectMember member = requireMember(projectId, memberId);
        if ("ACTIVE".equals(member.getStatus())) return toDto(member);
        SysUser user = requireAssignableUser(member.getUserId(), false);
        List<String> dutyCodes = responsibilityCodes(member);
        if (dutyCodes.isEmpty()) dutyCodes = List.of(responsibilities.normalizeCode(member.getProjectRole()));
        validateResponsibilities(user, dutyCodes);
        member.setStatus("ACTIVE");
        memberMapper.updateById(member);
        syncResponsibilities(member, dutyCodes, member.getProjectRole());
        events.record(projectId, "PROJECT_MEMBER", memberId, "PROJECT_MEMBER_RESTORED", "REMOVED", "ACTIVE", "已恢复项目成员 " + member.getMemberName());
        return toDto(member);
    }

    // 移交项目经理。
    @Override
    @Transactional
    public ProjectDto transferManager(Long projectId, ProjectManagerTransferRequest request) {
        Project project = requireProject(projectId);
        dataScope.requireProjectRole(projectId, "PROJECT_MANAGER");
        if (request == null || request.managerUserId() == null || request.reason() == null || request.reason().isBlank()) throw new BusinessException("请选择新项目经理并填写交接原因");
        SysUser nextManager = requireProjectManager(request.managerUserId());
        if (nextManager.getId().equals(project.getOwnerUserId()))
            return toDto(project);
        ProjectMember previous = memberMapper.selectOne(Wrappers.<ProjectMember>lambdaQuery().eq(ProjectMember::getProjectId, projectId).eq(ProjectMember::getUserId, project.getOwnerUserId()).eq(ProjectMember::getStatus, "ACTIVE").last("LIMIT 1"));
        if (previous != null) {
            assertNoOpenAssignment(projectId, previous.getUserId());
            List<String> remaining = activeResponsibilityCodes(previous).stream().filter(code -> !"PROJECT_MANAGER".equals(code)).toList();
            if (remaining.isEmpty()) {
                previous.setStatus("REMOVED");
                memberMapper.updateById(previous);
                responsibilityMapper.selectList(Wrappers.<ProjectMemberResponsibility>lambdaQuery()
                                .eq(ProjectMemberResponsibility::getProjectMemberId, previous.getId()))
                        .forEach(row -> {
                            row.setStatus("REMOVED");
                            row.setPrimaryFlag(0);
                            row.setOwnerSlot(null);
                            responsibilityMapper.updateById(row);
                        });
            } else {
                String primary = previous.getProjectRole().equals("PROJECT_MANAGER") ? remaining.get(0) : previous.getProjectRole();
                syncResponsibilities(previous, remaining, primary);
                previous.setProjectRole(primary);
                memberMapper.updateById(previous);
            }
        }
        ensureMember(projectId, nextManager, List.of("PROJECT_MANAGER"), "PROJECT_MANAGER");
        Long previousId = project.getOwnerUserId();
        project.setOwnerUserId(nextManager.getId());
        project.setOwnerName(displayName(nextManager));
        ensureUpdated(project, "项目经理交接时项目已被其他用户更新");
        jdbc.update("INSERT INTO nso_project_manager_history (tenant_id,project_id,previous_manager_user_id,next_manager_user_id,reason,operator_id) VALUES (?,?,?,?,?,?)",
                TenantContext.tenantId(), projectId, previousId, nextManager.getId(), request.reason().trim(), TenantContext.userId());
        events.record(projectId, "PROJECT", projectId, "PROJECT_MANAGER_TRANSFERRED", String.valueOf(previousId), String.valueOf(nextManager.getId()), request.reason().trim());
        return toDto(project);
    }

    // 查询项目经理候选人。
    @Override
    public PageResult<ProjectMemberCandidateDto> managerCandidates(String keyword) {
        List<ProjectMemberCandidateDto> rows = userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getTenantId, TenantContext.tenantId()).eq(SysUser::getStatus, UserLifecycle.ACTIVE).eq(SysUser::getUserType, "INTERNAL")
                        .and(keyword != null && !keyword.isBlank(), query -> query.like(SysUser::getUsername, keyword).or().like(SysUser::getNickname, keyword)))
                .stream().map(user -> {
                    List<String> roleCodes = directoryUsers.roleCodes(user.getId());
                    SysDept dept = user.getDeptId() == null ? null : deptMapper.selectById(user.getDeptId());
                    return new ProjectMemberCandidateDto(user.getId(), user.getUsername(), displayName(user), user.getDeptId(),
                            dept == null ? null : dept.getDeptName(), roleCodes, List.of("PROJECT_MANAGER"));
                }).filter(candidate -> candidate.roleCodes().stream()
                        .anyMatch(role -> "project_manager".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role))).toList();
        return new PageResult<>(rows, rows.size());
    }

    // 分页查询项目经理候选人。
    @Override
    public PageResult<ProjectMemberCandidateDto> managerCandidates(String keyword, PageQuery pageQuery) {
        return pageCandidates(managerCandidates(keyword).list(), pageQuery);
    }

    // 查询项目成员候选人。
    @Override
    public PageResult<ProjectMemberCandidateDto> memberCandidates(Long projectId, String keyword) {
        requireProject(projectId);
        dataScope.requireProjectRole(projectId, "PROJECT_MANAGER");
        List<ProjectMemberCandidateDto> rows = userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getTenantId, TenantContext.tenantId()).eq(SysUser::getStatus, UserLifecycle.ACTIVE).eq(SysUser::getUserType, "INTERNAL")
                        .and(keyword != null && !keyword.isBlank(), query -> query.like(SysUser::getUsername, keyword).or().like(SysUser::getNickname, keyword)))
                .stream().map(user -> {
                    List<String> roleCodes = directoryUsers.roleCodes(user.getId());
                    List<String> allowed = responsibilities.allowedResponsibilities(roleCodes, roleCodes.stream().anyMatch(role -> "admin".equalsIgnoreCase(role)), false);
                    SysDept dept = user.getDeptId() == null ? null : deptMapper.selectById(user.getDeptId());
                    return new ProjectMemberCandidateDto(user.getId(), user.getUsername(), displayName(user), user.getDeptId(),
                            dept == null ? null : dept.getDeptName(), roleCodes, allowed);
                }).filter(candidate -> !candidate.allowedResponsibilityCodes().isEmpty()).toList();
        return new PageResult<>(rows, rows.size());
    }

    // 分页查询项目成员候选人。
    @Override
    public PageResult<ProjectMemberCandidateDto> memberCandidates(Long projectId, String keyword, PageQuery pageQuery) {
        return pageCandidates(memberCandidates(projectId, keyword).list(), pageQuery);
    }

    // 读取项目详情。
    @Override
    public ProjectDto get(Long projectId) {
        return toDto(requireProject(projectId));
    }

    private Project requireProject(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException("项目不存在");
        }
        dataScope.requireAccess(projectId);
        return project;
    }

    private PageResult<ProjectMemberCandidateDto> pageCandidates(List<ProjectMemberCandidateDto> rows, PageQuery pageQuery) {
        PageQuery page = PageSupport.normalize(pageQuery);
        int from = Math.min((int) Math.min(page.offset(), Integer.MAX_VALUE), rows.size());
        int to = Math.min(from + page.pageSizeValue(), rows.size());
        return new PageResult<>(rows.subList(from, to), rows.size(), page.pageNoValue(), page.pageSizeValue());
    }

    private void ensureUpdated(Project project, String message) {
        lifecycle.ensureUpdated(project, message);
    }

    private void reconcileSampleState(Project project, String reason) {
        lifecycle.reconcileSampleState(project, reason);
    }

    private void assertCompletionReady(Project project) {
        lifecycle.assertCompletionReady(project);
    }

    private void transition(Project project, String action, String status, String stage, String reason) {
        lifecycle.transition(project, action, status, stage, reason);
    }

    private void requireStatus(Project project, String action, String... allowed) {
        lifecycle.requireStatus(project, action, allowed);
    }

    private void archive(Project project, String reason) {
        lifecycle.archive(project, reason);
    }

    private void restore(Project project, String reason) {
        lifecycle.restore(project, reason);
    }

    private String requiredReason(String reason) {
        return lifecycle.requiredReason(reason);
    }

    private void requireConfirmation(ProjectActionRequest request, String operation, Long projectId) {
        lifecycle.requireConfirmation(request, operation, projectId);
    }

    private ProjectMember ensureMember(Long projectId, SysUser user, List<String> dutyCodes, String primary) {
        return membership.ensureMember(projectId, user, dutyCodes, primary);
    }

    private void syncResponsibilities(ProjectMember member, List<String> dutyCodes, String primary) {
        membership.syncResponsibilities(member, dutyCodes, primary);
    }

    private List<String> responsibilityCodes(ProjectMember member) {
        return membership.responsibilityCodes(member);
    }

    private List<String> activeResponsibilityCodes(ProjectMember member) {
        return membership.activeResponsibilityCodes(member);
    }

    private SysUser requireAssignableUser(Long userId, boolean externalAllowed) {
        return membership.requireAssignableUser(userId, externalAllowed);
    }

    private SysUser requireProjectManager(Long userId) {
        return membership.requireProjectManager(userId);
    }

    private void validateResponsibilities(SysUser user, List<String> dutyCodes) {
        membership.validateResponsibilities(user, dutyCodes);
    }

    private ProjectMember requireMember(Long projectId, Long memberId) {
        return membership.requireMember(projectId, memberId);
    }

    private void assertNoOpenAssignment(Long projectId, Long userId) {
        membership.assertNoOpenAssignment(projectId, userId);
    }

    private String displayName(SysUser user) {
        return membership.displayName(user);
    }

    private ProjectDto toDto(Project source) {
        return queries.toDto(source);
    }

    private ProjectMemberDto toDto(ProjectMember source) {
        SysUser user = source.getUserId() == null ? null : userMapper.selectById(source.getUserId());
        SysDept dept = user == null || user.getDeptId() == null ? null : deptMapper.selectById(user.getDeptId());
        List<ProjectMemberResponsibility> rows = responsibilityMapper.selectList(Wrappers.<ProjectMemberResponsibility>lambdaQuery()
                .eq(ProjectMemberResponsibility::getProjectMemberId, source.getId()).eq(ProjectMemberResponsibility::getStatus, "ACTIVE"));
        List<String> dutyCodes = rows.stream().map(ProjectMemberResponsibility::getResponsibilityCode).toList();
        if (dutyCodes.isEmpty() && source.getProjectRole() != null) dutyCodes = List.of(source.getProjectRole());
        String primary = rows.stream().filter(row -> Integer.valueOf(1).equals(row.getPrimaryFlag())).map(ProjectMemberResponsibility::getResponsibilityCode).findFirst().orElse(source.getProjectRole());
        return new ProjectMemberDto(source.getId(), source.getProjectId(), source.getUserId(),
                user == null ? source.getMemberName() : displayName(user), primary, dept == null ? source.getDepartmentName() : dept.getDeptName(), source.getStatus(),
                user == null ? source.getDeptId() : user.getDeptId(), user == null ? "MISSING" : user.getStatus(), dutyCodes, primary);
    }
}
