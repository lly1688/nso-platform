package com.nso.web.controller.business;

import com.nso.business.core.NsoDtos.OperationConfirmationRequest;
import com.nso.business.core.NsoDtos.VerificationChallengeRequest;
import com.nso.business.support.OperationConfirmationService;
import com.nso.shared.core.domain.AjaxResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/operation-confirmations")

// 操作确认接口 负责操作确认流程
public class OperationConfirmationController {
    // 操作确认服务
    private final OperationConfirmationService confirmations;



    public OperationConfirmationController(OperationConfirmationService confirmations) {
        this.confirmations = confirmations;
    }

    // 创建敏感操作确认挑战。
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public AjaxResult<?> request(@RequestBody OperationConfirmationRequest request) {
        return AjaxResult.success(confirmations.request(request));
    }



    // 确认敏感操作。
    @PostMapping("/{id}/confirm")
    @PreAuthorize("isAuthenticated()")
    public AjaxResult<?> confirm(@PathVariable Long id) {
        return AjaxResult.success(confirmations.confirm(id));
    }

    // 校验操作确认验证码。
    @PostMapping("/challenges/{id}/verify")
    @PreAuthorize("isAuthenticated()")
    public AjaxResult<?> verify(@PathVariable Long id, @RequestBody VerificationChallengeRequest request) {
        return AjaxResult.success(confirmations.verify(id, request == null ? null : request.code()));
    }
}
