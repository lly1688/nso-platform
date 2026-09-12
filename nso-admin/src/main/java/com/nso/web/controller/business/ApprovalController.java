package com.nso.web.controller.business;

import com.nso.business.change.service.IChangeService;
import com.nso.business.core.NsoDtos.ApprovalDecisionRequest;
import com.nso.business.core.NsoDtos.ApprovalTemplateRequest;
import com.nso.business.core.NsoDtos.ApprovalTodoDto;
import com.nso.business.core.NsoDtos.ChangeApprovalRequest;
import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.support.approval.IApprovalService;
import com.nso.business.support.capa.ICapaService;
import com.nso.shared.core.domain.AjaxResult;
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

// 审批管理接口 负责审批流程的发起和处理
public class ApprovalController {
    // 审批服务
    private final IApprovalService approvals;
    // 变更服务
    private final IChangeService changes;
    // CAPA服务
    private final ICapaService capaService;

    public ApprovalController(IApprovalService approvals, IChangeService changes, ICapaService capaService) {
        this.approvals = approvals;
        this.changes = changes;
        this.capaService = capaService;
    }

    // 查询审批模板。
    @GetMapping("/approval-templates")
    @PreAuthorize("hasAnyAuthority('nso:approval:template:manage', 'approval:template:manage')")
    public AjaxResult<?> templates(@RequestParam(required = false) String businessType,
                                   @RequestParam(required = false) Integer pageNo,
                                   @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(approvals.templates(businessType, new PageQuery(pageNo, pageSize)));
    }

    // 创建审批模板。
    @PostMapping("/approval-templates")
    @PreAuthorize("hasAnyAuthority('nso:approval:template:manage', 'approval:template:manage')")
    public AjaxResult<?> createTemplate(@RequestBody ApprovalTemplateRequest request) {
        return AjaxResult.success(approvals.createTemplate(request));
    }

    // 发布审批模板。
    @PostMapping("/approval-templates/{id}/publish")
    @PreAuthorize("hasAnyAuthority('nso:approval:template:manage', 'approval:template:manage')")
    public AjaxResult<?> publishTemplate(@PathVariable Long id) {
        return AjaxResult.success(approvals.publishTemplate(id));
    }

    // 查询当前用户待审批项。
    @GetMapping("/approval-todos")
    @PreAuthorize("hasAnyAuthority('nso:approval:view', 'approval:view')")
    public AjaxResult<?> todos(@RequestParam(required = false) String businessType,
                               @RequestParam(required = false) Integer pageNo,
                               @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(approvals.pending(businessType, new PageQuery(pageNo, pageSize)));
    }

    // 查询业务审批实例。
    @GetMapping("/approval-instances/{businessType}/{businessId}")
    @PreAuthorize("hasAnyAuthority('nso:approval:view', 'approval:view')")
    public AjaxResult<?> instance(@PathVariable String businessType, @PathVariable Long businessId) {
        return AjaxResult.success(approvals.instance(businessType, businessId));
    }

    // 提交审批结论。
    @PostMapping("/approval-todos/{id}/decision")
    @PreAuthorize("hasAnyAuthority('nso:approval:decide', 'approval:decide')")
    public AjaxResult<?> decide(@PathVariable Long id, @RequestBody ApprovalDecisionRequest request) {
        ApprovalTodoDto todo = approvals.todo(id);
        return switch (todo.businessType()) {
            case "CHANGE" -> AjaxResult.success(decideChange(todo, request));
            case "SPECIAL_RELEASE" -> AjaxResult.success(approvals.decideSpecialRelease(id, request));
            case "EXCEPTION" -> AjaxResult.success(decideException(id, todo, request));
            default -> throw new IllegalStateException("Unsupported approval business type: " + todo.businessType());
        };
    }

    private Object decideChange(ApprovalTodoDto todo, ApprovalDecisionRequest request) {
        Object result = changes.approve(todo.businessId(), new ChangeApprovalRequest(request == null ? null : request.decision(), request == null ? null : request.opinion()));
        approvals.syncChangeApprovals(todo.businessId());
        return result;
    }

    private Object decideException(Long todoId, ApprovalTodoDto todo, ApprovalDecisionRequest request) {
        ApprovalTodoDto result = approvals.decide(todoId, request);
        if ("REJECTED".equals(result.decision())) {
            capaService.reopenAfterApprovalRejection(todo.businessId());
        }
        return result;
    }
}
