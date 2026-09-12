package com.nso.web.controller.system;

import com.nso.shared.core.domain.AjaxResult;
import com.nso.system.support.service.IAccountSupportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.nso.business.core.NsoDtos.PageQuery;

@RestController
@RequestMapping("/api/v1/admin/system/account-support")
@PreAuthorize("hasAuthority('sys:account-support:manage')")

// 账号支持管理接口 负责管理员对账号支持工单的管理
public class AccountSupportAdminController {
    // 账户支持服务
    private final IAccountSupportService accountSupport;

    public AccountSupportAdminController(IAccountSupportService accountSupport) {
        this.accountSupport = accountSupport;
    }

    // 查询密码恢复申请。
    @GetMapping("/password-recovery-requests")
    public AjaxResult<?> passwordRecoveryRequests(@RequestParam(required = false) String status,
                                                  @RequestParam(required = false) Integer pageNo,
                                                  @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(accountSupport.listPasswordRecoveryRequests(status, new PageQuery(pageNo, pageSize)));
    }

    // 审核密码恢复申请。
    @PutMapping("/password-recovery-requests/{requestId}")
    public AjaxResult<IAccountSupportService.PasswordRecoveryView> reviewPasswordRecovery(@PathVariable Long requestId,
                                                                                             @RequestBody(required = false) IAccountSupportService.RecoveryReviewSubmission request) {
        return AjaxResult.success(accountSupport.reviewPasswordRecovery(requestId, request));
    }

    // 重置已核验申请的密码。
    @PostMapping("/password-recovery-requests/{requestId}/reset")
    public AjaxResult<IAccountSupportService.PasswordResetResult> resetPassword(@PathVariable Long requestId, @RequestBody(required = false) IAccountSupportService.RecoveryResetSubmission request) {
        return AjaxResult.success(accountSupport.resetRecoveredPassword(requestId, request));
    }

    // 查询支持工单。
    @GetMapping("/support-tickets")
    public AjaxResult<?> supportTickets(@RequestParam(required = false) String status,
                                        @RequestParam(required = false) Integer pageNo,
                                        @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(accountSupport.listSupportTickets(status, new PageQuery(pageNo, pageSize)));
    }

    // 更新支持工单处理状态。
    @PutMapping("/support-tickets/{ticketId}")
    public AjaxResult<IAccountSupportService.SupportTicketView> updateSupportTicket(@PathVariable Long ticketId,
                                                                                      @RequestBody(required = false) IAccountSupportService.SupportTicketUpdateSubmission request) {
        return AjaxResult.success(accountSupport.updateSupportTicket(ticketId, request));
    }
}
