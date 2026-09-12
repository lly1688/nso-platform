package com.nso.web.controller.auth;

import com.nso.shared.core.domain.AjaxResult;
import com.nso.system.support.service.IAccountSupportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auth")

// 认证接口 负责用户登录、登出和令牌刷新
public class AuthController {
    // 账户支持服务
    private final IAccountSupportService accountSupport;

    public AuthController(IAccountSupportService accountSupport) {
        this.accountSupport = accountSupport;
    }

    // 提交密码恢复申请。
    @PostMapping("/password-recovery-requests")
    public ResponseEntity<AjaxResult<Void>> requestPasswordRecovery(@RequestBody(required = false) IAccountSupportService.PasswordRecoverySubmission request,
                                                                      HttpServletRequest servletRequest) {
        accountSupport.requestPasswordRecovery(request, servletRequest.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(AjaxResult.success());
    }

    // 创建访客支持工单。
    @PostMapping("/support-tickets")
    public ResponseEntity<AjaxResult<IAccountSupportService.SupportTicketView>> createGuestSupportTicket(
            @RequestBody(required = false) IAccountSupportService.GuestSupportTicketSubmission request,
            HttpServletRequest servletRequest) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(AjaxResult.success(accountSupport.createGuestSupportTicket(request, servletRequest.getRemoteAddr())));
    }

}
