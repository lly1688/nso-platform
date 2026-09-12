package com.nso.web.controller.business;

import com.nso.business.core.NsoDtos.CapaCaseCreateRequest;
import com.nso.business.core.NsoDtos.CapaCorrectiveTaskRequest;
import com.nso.business.core.NsoDtos.CapaEvidenceRequest;
import com.nso.business.core.NsoDtos.CapaTransitionRequest;
import com.nso.business.core.NsoDtos.PageQuery;
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

// CAPA 管理接口。 负责纠正与预防措施的管理
public class CapaController {
    // CAPA服务
    private final ICapaService capaService;

    public CapaController(ICapaService capaService) {
        this.capaService = capaService;
    }

    // 查询异常单。
    @GetMapping("/exceptions")
    @PreAuthorize("hasAnyAuthority('nso:exception:view', 'exception:view', 'nso:task:feedback', 'task:feedback')")
    public AjaxResult<?> list(
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(capaService.list(projectId, status, new PageQuery(pageNo, pageSize)));
    }

    // 创建异常单。
    @PostMapping("/exceptions")
    @PreAuthorize("hasAnyAuthority('nso:exception:manage', 'exception:manage', 'nso:task:feedback', 'task:feedback')")
    public AjaxResult<?> create(@RequestBody CapaCaseCreateRequest request) {
        return AjaxResult.success(capaService.create(request));
    }

    // 查询异常单详情。
    @GetMapping("/exceptions/{id}")
    @PreAuthorize("hasAnyAuthority('nso:exception:view', 'exception:view', 'nso:task:feedback', 'task:feedback')")
    public AjaxResult<?> get(@PathVariable Long id) {
        return AjaxResult.success(capaService.get(id));
    }

    // 执行异常单状态迁移。
    @PostMapping("/exceptions/{id}/transitions")
    @PreAuthorize("hasAnyAuthority('nso:exception:manage', 'exception:manage', 'nso:task:feedback', 'task:feedback')")
    public AjaxResult<?> transition(@PathVariable Long id, @RequestBody CapaTransitionRequest request) {
        return AjaxResult.success(capaService.transition(id, request));
    }

    // 添加异常证据。
    @PostMapping("/exceptions/{id}/evidence")
    @PreAuthorize("hasAnyAuthority('nso:exception:manage', 'exception:manage', 'nso:task:feedback', 'task:feedback')")
    public AjaxResult<?> evidence(@PathVariable Long id, @RequestBody CapaEvidenceRequest request) {
        return AjaxResult.success(capaService.addEvidence(id, request));
    }

    // 查询异常处置行动。
    @GetMapping("/exceptions/{id}/actions")
    @PreAuthorize("hasAnyAuthority('nso:exception:view', 'exception:view', 'nso:task:feedback', 'task:feedback')")
    public AjaxResult<?> actions(@PathVariable Long id) {
        return AjaxResult.success(capaService.actions(id));
    }

    // 创建纠正任务。
    @PostMapping("/exceptions/{id}/actions")
    @PreAuthorize("hasAnyAuthority('nso:exception:manage', 'exception:manage', 'nso:task:feedback', 'task:feedback')")
    public AjaxResult<?> createAction(@PathVariable Long id, @RequestBody CapaCorrectiveTaskRequest request) {
        return AjaxResult.success(capaService.createCorrectiveTask(id, request));
    }

    // 重新打开异常单。
    @PostMapping("/exceptions/{id}/reopen")
    @PreAuthorize("hasAnyAuthority('nso:exception:manage', 'exception:manage', 'nso:approval:decide', 'approval:decide')")
    public AjaxResult<?> reopen(@PathVariable Long id) {
        return AjaxResult.success(capaService.reopenAfterApprovalRejection(id));
    }
}
