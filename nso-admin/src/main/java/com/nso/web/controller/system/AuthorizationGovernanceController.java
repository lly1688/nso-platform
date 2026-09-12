package com.nso.web.controller.system;

import com.nso.shared.core.domain.AjaxResult;
import com.nso.system.service.IAuthorizationGovernanceService;
import com.nso.business.core.NsoDtos.PageQuery;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/admin/system/authorization-requests")
@PreAuthorize("hasAuthority('sys:authorization:audit')")

// 授权治理接口 负责受保护角色授权的审批治理
public class AuthorizationGovernanceController {
    // 授权治理服务
    private final IAuthorizationGovernanceService governance;

    public AuthorizationGovernanceController(IAuthorizationGovernanceService governance) {
        this.governance = governance;
    }

    // 查询受保护角色授权申请。
    @GetMapping
    public AjaxResult<?> list(@RequestParam(required = false) Integer pageNo,
                              @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(governance.requests(new PageQuery(pageNo, pageSize)));
    }

    // 申请受保护角色。
    @PostMapping
    public AjaxResult<?> request(@RequestBody ProtectedRoleRequest request) {
        return AjaxResult.success(governance.requestProtectedRole(request.userId(), request.roleCode(), request.reason()));
    }

    // 审批受保护角色申请。
    @PostMapping("/{requestId}/approve")
    public AjaxResult<?> approve(@PathVariable Long requestId) {
        return AjaxResult.success(governance.approveProtectedRole(requestId));
    }

    public record ProtectedRoleRequest(
        // 目标用户编号
        Long userId,
        // 角色标识
        String roleCode,
        // 申请原因
        String reason
    ) {
    }
}
