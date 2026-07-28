package com.nso.web.controller.business;

import com.nso.common.core.domain.AjaxResult;
import com.nso.business.core.NsoDtos.*;
import com.nso.business.project.service.IProjectService;
import com.nso.common.exception.BusinessException;
import com.nso.system.service.ISysUserService;
import com.nso.business.support.IFlowCodeService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class ProjectController {

    private final IProjectService projectService;
    private final ISysUserService users;
    private final PasswordEncoder passwordEncoder;
    private final IFlowCodeService flowCodes;

    public ProjectController(IProjectService projectService, ISysUserService users, PasswordEncoder passwordEncoder, IFlowCodeService flowCodes) {
        this.projectService = projectService;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.flowCodes = flowCodes;
    }

    @GetMapping("/projects")
    @PreAuthorize("hasAuthority('project:view')")
    public AjaxResult<?> projects(@RequestParam(required = false) String keyword) {
        return AjaxResult.success(projectService.list(keyword));
    }

    @PostMapping("/projects")
    @PreAuthorize("hasAuthority('project:create')")
    public AjaxResult<?> createProject(@RequestBody ProjectRequest request) {
        return AjaxResult.success(projectService.create(request));
    }

    @PostMapping("/projects/{id}/submit-review")
    @PreAuthorize("hasAuthority('project:manage')")
    public AjaxResult<?> submitReview(@PathVariable Long id) {
        return AjaxResult.success(projectService.submitReview(id));
    }

    @GetMapping("/projects/{id}/requirements")
    @PreAuthorize("hasAuthority('project:view')")
    public AjaxResult<?> requirements(@PathVariable Long id) {
        return AjaxResult.success(projectService.requirements(id));
    }

    @PostMapping("/projects/{id}/requirements")
    @PreAuthorize("hasAuthority('project:requirement:manage')")
    public AjaxResult<?> saveRequirement(@PathVariable Long id, @RequestBody RequirementRequest request) {
        return AjaxResult.success(projectService.saveRequirement(id, request));
    }

    @PostMapping("/requirements/{id}/confirm")
    @PreAuthorize("hasAuthority('project:requirement:manage')")
    public AjaxResult<?> confirmRequirement(@PathVariable Long id, @RequestBody RequirementRequest request) {
        return AjaxResult.success(projectService.confirmRequirement(id, request));
    }

    @GetMapping("/projects/{id}/members")
    @PreAuthorize("hasAuthority('project:view')")
    public AjaxResult<?> members(@PathVariable Long id) {
        return AjaxResult.success(projectService.members(id));
    }

    @PostMapping("/projects/{id}/members")
    @PreAuthorize("hasAuthority('project:member:manage')")
    public AjaxResult<?> addMember(@PathVariable Long id, @RequestBody ProjectMemberRequest request) {
        return AjaxResult.success(projectService.addMember(id, request));
    }

    @PostMapping("/projects/{id}/customer-invitations")
    @PreAuthorize("hasAuthority('customer:invite')")
    public AjaxResult<?> inviteCustomer(@PathVariable Long id, @RequestBody CustomerInviteRequest request) {
        if (request == null || request.username() == null || request.username().isBlank() || request.password() == null || request.password().length() < 8) {
            throw new BusinessException("客户邀请需要账号和至少 8 位初始密码");
        }
        var user = users.createUser(request.username(), passwordEncoder.encode(request.password()), request.nickname(), null, java.util.List.of("customer_confirm"));
        return AjaxResult.success(projectService.addMember(id, new ProjectMemberRequest(user.getId(), user.getNickname(), "CUSTOMER_CONFIRM", "外部客户")));
    }

    @PostMapping("/projects/{id}/flow-qr")
    @PreAuthorize("hasAuthority('project:view')")
    public AjaxResult<?> projectFlowQr(@PathVariable Long id) {
        return AjaxResult.success(flowCodes.ensureProjectFlowCode(id));
    }

    public record CustomerInviteRequest(String username, String password, String nickname) { }
}
