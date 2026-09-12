package com.nso.web.controller.business;

import com.nso.shared.core.domain.AjaxResult;
import com.nso.business.core.NsoDtos.*;
import com.nso.business.change.service.IChangeService;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")

// 变更管理接口 负责变更单的创建、审批、反馈和关闭
public class ChangeController {

    // 变更服务
    private final IChangeService changeService;

    public ChangeController(IChangeService changeService) {
        this.changeService = changeService;
    }

    // 按条件查询变更单。
    @GetMapping("/changes")
    @PreAuthorize("hasAnyAuthority('nso:change:view', 'change:view')")
    public AjaxResult<?> changes(@RequestParam(required = false) Long projectId,
                                 @RequestParam(required = false) String status,
                                 @RequestParam(required = false) String changeType,
                                 @RequestParam(required = false) Integer pageNo,
                                 @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(changeService.list(projectId, status, changeType, new PageQuery(pageNo, pageSize)));
    }

    // 创建变更单。
    @PostMapping("/changes")
    @PreAuthorize("hasAnyAuthority('nso:change:create', 'change:create')")
    public AjaxResult<?> createChange(@RequestBody ChangeRequest request) {
        return AjaxResult.success(changeService.create(request));
    }

    // 查询变更单详情。
    @GetMapping("/changes/{id}")
    @PreAuthorize("hasAnyAuthority('nso:change:view', 'change:view')")
    public AjaxResult<?> change(@PathVariable Long id) {
        return AjaxResult.success(changeService.get(id));
    }

    // 分析变更影响。
    @PostMapping("/changes/{id}/analyze-impact")
    @PreAuthorize("hasAnyAuthority('nso:change:analyze', 'change:analyze')")
    public AjaxResult<?> analyzeChange(@PathVariable Long id) {
        return AjaxResult.success(changeService.analyze(id));
    }

    // 查询变更影响项。
    @GetMapping("/changes/{id}/impacts")
    @PreAuthorize("hasAnyAuthority('nso:change:view', 'change:view')")
    public AjaxResult<?> changeImpacts(@PathVariable Long id, @RequestParam(required = false) Integer pageNo,
                                       @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(changeService.impactsPage(id, new PageQuery(pageNo, pageSize)));
    }

    // 提交变更审批结论。
    @PostMapping("/changes/{id}/approve")
    @PreAuthorize("hasAnyAuthority('nso:change:approve', 'change:approve')")
    public AjaxResult<?> approveChange(@PathVariable Long id, @RequestBody(required = false) ChangeApprovalRequest request) {
        return AjaxResult.success(changeService.approve(id, request));
    }

    // 提交变更整体执行反馈。
    @PostMapping("/changes/{id}/feedback")
    @PreAuthorize("hasAnyAuthority('nso:change:feedback', 'change:feedback')")
    public AjaxResult<?> feedbackChange(@PathVariable Long id, @RequestBody ChangeFeedbackRequest request) {
        return AjaxResult.success(changeService.feedbackChange(id, request));
    }

    // 提交单项变更执行反馈。
    @PostMapping("/change-impacts/{id}/feedback")
    @PreAuthorize("hasAnyAuthority('nso:change:feedback', 'change:feedback')")
    public AjaxResult<?> feedbackImpact(@PathVariable Long id, @RequestBody ChangeFeedbackRequest request) {
        return AjaxResult.success(changeService.feedbackImpact(id, request));
    }

    // 关闭变更单。
    @PostMapping("/changes/{id}/close")
    @PreAuthorize("hasAnyAuthority('nso:change:close', 'change:close')")
    public AjaxResult<?> closeChange(@PathVariable Long id) {
        return AjaxResult.success(changeService.close(id));
    }

    // 验证变更执行结果。
    @PostMapping("/changes/{id}/verify")
    @PreAuthorize("hasAnyAuthority('nso:change:close', 'change:close')")
    public AjaxResult<?> verifyChange(@PathVariable Long id, @RequestBody(required = false) ChangeVerifyRequest request) {
        return AjaxResult.success(changeService.verify(id, request));
    }

    // 撤销变更单。
    @PostMapping("/changes/{id}/revoke")
    @PreAuthorize("hasAnyAuthority('nso:change:approve', 'change:approve')")
    public AjaxResult<?> revokeChange(@PathVariable Long id, @RequestBody ChangeRevokeRequest request) {
        return AjaxResult.success(changeService.revoke(id, request));
    }

    // 补充变更申请内容。
    @PostMapping("/changes/{id}/supplements")
    @PreAuthorize("hasAnyAuthority('nso:change:create', 'change:create')")
    public AjaxResult<?> supplementChange(@PathVariable Long id, @RequestBody(required = false) ChangeRequest request) {
        return AjaxResult.success(changeService.supplement(id, request));
    }
}
