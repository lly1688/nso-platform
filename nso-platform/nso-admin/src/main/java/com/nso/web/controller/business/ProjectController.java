package com.nso.web.controller.business;

import com.nso.shared.core.domain.AjaxResult;
import com.nso.business.core.NsoDtos.*;
import com.nso.business.customer.service.ICustomerService;
import com.nso.business.project.service.IProjectService;
import com.nso.business.support.IFlowCodeService;
import com.nso.business.task.service.ITaskService;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")

// 项目协同接口 负责项目、成员和状态操作入口
public class ProjectController {

    // 项目服务
    private final IProjectService projectService;
    // 客户服务
    private final ICustomerService customerService;
    // 流程编码服务
    private final IFlowCodeService flowCodes;
    // 任务服务
    private final ITaskService taskService;

    public ProjectController(IProjectService projectService, ICustomerService customerService, IFlowCodeService flowCodes, ITaskService taskService) {
        this.projectService = projectService;
        this.customerService = customerService;
        this.flowCodes = flowCodes;
        this.taskService = taskService;
    }

    // 按条件查询项目。
    @GetMapping("/projects")
    @PreAuthorize("hasAnyAuthority('nso:project:view', 'project:view')")
    public AjaxResult<?> projects(@RequestParam(required = false) String keyword,
                                  @RequestParam(required = false) String stage,
                                  @RequestParam(required = false) String status,
                                  @RequestParam(required = false) String riskLevel,
                                  @RequestParam(required = false) String dueState,
                                  @RequestParam(required = false) String quickFilter,
                                  @RequestParam(required = false) Integer pageNo,
                                  @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(projectService.list(keyword, stage, status, riskLevel, dueState, quickFilter,
                new PageQuery(pageNo, pageSize)));
    }

    // 查询项目统计数据。
    @GetMapping("/projects/stats")
    @PreAuthorize("hasAnyAuthority('nso:project:view', 'project:view')")
    public AjaxResult<?> projectStats(@RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String stage,
                                      @RequestParam(required = false) String status,
                                      @RequestParam(required = false) String riskLevel,
                                      @RequestParam(required = false) String dueState,
                                      @RequestParam(required = false) String quickFilter,
                                      @RequestParam(required = false) Integer pageNo,
                                      @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(projectService.stats(keyword, stage, status, riskLevel, dueState, quickFilter,
                new PageQuery(pageNo, pageSize)));
    }

    // 创建项目。
    @PostMapping("/projects")
    @PreAuthorize("hasAnyAuthority('nso:project:create', 'project:create')")
    public AjaxResult<?> createProject(@RequestBody ProjectRequest request) {
        return AjaxResult.success(projectService.create(request));
    }

    // 查询项目经理候选人。
    @GetMapping("/projects/manager-candidates")
    @PreAuthorize("hasAnyAuthority('nso:project:create', 'project:create')")
    public AjaxResult<?> managerCandidates(@RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Integer pageNo,
                                           @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(projectService.managerCandidates(keyword, new PageQuery(pageNo, pageSize)));
    }

    // 提交项目立项评审。
    @PostMapping("/projects/{id}/submit-review")
    @PreAuthorize("hasAnyAuthority('nso:project:manage', 'project:manage')")
    public AjaxResult<?> submitReview(@PathVariable Long id) {
        return AjaxResult.success(projectService.submitReview(id));
    }

    // 执行项目状态操作。
    @PostMapping("/projects/{id}/actions/{action}")
    @PreAuthorize("hasAnyAuthority('nso:project:manage', 'project:manage')")
    public AjaxResult<?> projectAction(@PathVariable Long id, @PathVariable String action, @RequestBody(required = false) ProjectActionRequest request) {
        return AjaxResult.success(projectService.action(id, action, request));
    }

    // 复制项目。
    @PostMapping("/projects/{id}/copy")
    @PreAuthorize("hasAnyAuthority('nso:project:create', 'project:create')")
    public AjaxResult<?> copyProject(@PathVariable Long id, @RequestBody(required = false) ProjectCopyRequest request) {
        return AjaxResult.success(projectService.copy(id, request));
    }

    // 查询项目状态历史。
    @GetMapping("/projects/{id}/status-history")
    @PreAuthorize("hasAnyAuthority('nso:project:view', 'project:view')")
    public AjaxResult<?> statusHistory(@PathVariable Long id, @RequestParam(required = false) Integer pageNo,
                                       @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(projectService.statusHistory(id, new PageQuery(pageNo, pageSize)));
    }

    // 查询项目需求。
    @GetMapping("/projects/{id}/requirements")
    @PreAuthorize("hasAnyAuthority('nso:project:view', 'project:view')")
    public AjaxResult<?> requirements(@PathVariable Long id, @RequestParam(required = false) Integer pageNo,
                                      @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(projectService.requirements(id, new PageQuery(pageNo, pageSize)));
    }

    // 保存项目需求。
    @PostMapping("/projects/{id}/requirements")
    @PreAuthorize("hasAnyAuthority('nso:project:requirement:manage', 'project:requirement:manage')")
    public AjaxResult<?> saveRequirement(@PathVariable Long id, @RequestBody RequirementRequest request) {
        return AjaxResult.success(projectService.saveRequirement(id, request));
    }

    // 确认项目需求。
    @PostMapping("/requirements/{id}/confirm")
    @PreAuthorize("hasAnyAuthority('nso:project:requirement:manage', 'project:requirement:manage')")
    public AjaxResult<?> confirmRequirement(@PathVariable Long id, @RequestBody RequirementRequest request) {
        return AjaxResult.success(projectService.confirmRequirement(id, request));
    }

    // 查询项目成员。
    @GetMapping("/projects/{id}/members")
    @PreAuthorize("hasAnyAuthority('nso:project:view', 'project:view')")
    public AjaxResult<?> members(@PathVariable Long id, @RequestParam(required = false) Integer pageNo,
                                 @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(projectService.members(id, new PageQuery(pageNo, pageSize)));
    }

    // 添加项目成员。
    @PostMapping("/projects/{id}/members")
    @PreAuthorize("hasAnyAuthority('nso:project:member:manage', 'project:member:manage')")
    public AjaxResult<?> addMember(@PathVariable Long id, @RequestBody ProjectMemberRequest request) {
        return AjaxResult.success(projectService.addMember(id, request));
    }

    // 查询项目成员候选人。
    @GetMapping("/projects/{id}/member-candidates")
    @PreAuthorize("hasAnyAuthority('nso:project:member:manage', 'project:member:manage')")
    public AjaxResult<?> memberCandidates(@PathVariable Long id, @RequestParam(required = false) String keyword,
                                          @RequestParam(required = false) Integer pageNo,
                                          @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(projectService.memberCandidates(id, keyword, new PageQuery(pageNo, pageSize)));
    }

    // 更新项目成员。
    @PutMapping("/projects/{projectId}/members/{memberId}")
    @PreAuthorize("hasAnyAuthority('nso:project:member:manage', 'project:member:manage')")
    public AjaxResult<?> updateMember(@PathVariable Long projectId, @PathVariable Long memberId, @RequestBody ProjectMemberUpdateRequest request) {
        return AjaxResult.success(projectService.updateMember(projectId, memberId, request));
    }

    // 移除项目成员。
    @PostMapping("/projects/{projectId}/members/{memberId}/remove")
    @PreAuthorize("hasAnyAuthority('nso:project:member:manage', 'project:member:manage')")
    public AjaxResult<?> removeMember(@PathVariable Long projectId, @PathVariable Long memberId) {
        return AjaxResult.success(projectService.removeMember(projectId, memberId));
    }

    // 恢复项目成员。
    @PostMapping("/projects/{projectId}/members/{memberId}/restore")
    @PreAuthorize("hasAnyAuthority('nso:project:member:manage', 'project:member:manage')")
    public AjaxResult<?> restoreMember(@PathVariable Long projectId, @PathVariable Long memberId) {
        return AjaxResult.success(projectService.restoreMember(projectId, memberId));
    }

    // 移交项目经理。
    @PostMapping("/projects/{id}/manager-transfer")
    @PreAuthorize("hasAnyAuthority('nso:project:member:manage', 'project:member:manage')")
    public AjaxResult<?> transferManager(@PathVariable Long id, @RequestBody ProjectManagerTransferRequest request) {
        return AjaxResult.success(projectService.transferManager(id, request));
    }

    // 查询项目外部联系人授权。
    @GetMapping("/projects/{id}/customer-authorizations")
    @PreAuthorize("hasAnyAuthority('nso:customer:invite', 'customer:invite')")
    public AjaxResult<?> customerAuthorizations(@PathVariable Long id, @RequestParam(required = false) Integer pageNo,
                                                @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(customerService.projectAuthorizations(id, new PageQuery(pageNo, pageSize)));
    }

    // 授予联系人项目访问权限。
    @PostMapping("/projects/{id}/customer-authorizations")
    @PreAuthorize("hasAnyAuthority('nso:customer:invite', 'customer:invite')")
    public AjaxResult<?> authorizeCustomer(@PathVariable Long id, @RequestBody ExternalProjectAccessRequest request) {
        return AjaxResult.success(customerService.authorizeProject(id, request));
    }

    // 撤销联系人项目访问权限。
    @PostMapping("/projects/{id}/customer-authorizations/{authorizationId}/revoke")
    @PreAuthorize("hasAnyAuthority('nso:customer:invite', 'customer:invite')")
    public AjaxResult<?> revokeCustomerAuthorization(@PathVariable Long id, @PathVariable Long authorizationId,
                                                        @RequestBody(required = false) RevokeProjectAccessRequest request) {
        customerService.revokeProjectAuthorization(id, authorizationId, request == null ? null : request.reason());
        return AjaxResult.success();
    }

    // 生成项目流转码。
    @PostMapping("/projects/{id}/flow-qr")
    @PreAuthorize("hasAnyAuthority('nso:project:view', 'project:view')")
    public AjaxResult<?> projectFlowQr(@PathVariable Long id) {
        return AjaxResult.success(flowCodes.ensureProjectFlowCode(id));
    }

    // 查询项目交付就绪状态。
    @GetMapping("/projects/{id}/delivery-readiness")
    @PreAuthorize("hasAnyAuthority('nso:project:view', 'project:view', 'nso:task:view', 'task:view')")
    public AjaxResult<?> deliveryReadiness(@PathVariable Long id) {
        return AjaxResult.success(taskService.deliveryReadiness(id));
    }
}
