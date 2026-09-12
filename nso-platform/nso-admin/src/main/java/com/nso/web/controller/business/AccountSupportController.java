package com.nso.web.controller.business;

import com.nso.shared.core.domain.AjaxResult;
import com.nso.system.support.service.IAccountSupportService;
import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.web.controller.common.WebFilePayloads;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/support-tickets")

// 账号支持接口 负责账号相关支持操作
public class AccountSupportController {
    // 账户支持服务
    private final IAccountSupportService accountSupport;

    public AccountSupportController(IAccountSupportService accountSupport) {
        this.accountSupport = accountSupport;
    }

    // 创建当前用户支持工单。
    @PostMapping
    public AjaxResult<IAccountSupportService.SupportTicketView> create(@RequestBody(required = false) IAccountSupportService.CurrentUserSupportTicketSubmission request) {
        return AjaxResult.success(accountSupport.createCurrentUserSupportTicket(request));
    }

    // 查询当前用户支持工单。
    @GetMapping("/mine")
    public AjaxResult<?> mine(@RequestParam(required = false) Integer pageNo,
                              @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(accountSupport.listMySupportTickets(new PageQuery(pageNo, pageSize)));
    }

    // 上传支持工单图片。
    @PostMapping(value = "/{ticketId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AjaxResult<IAccountSupportService.SupportTicketAttachmentView> uploadImage(@PathVariable Long ticketId,
                                                                                        @RequestParam("file") MultipartFile file) {
        return AjaxResult.success(accountSupport.uploadSupportTicketImage(ticketId, WebFilePayloads.upload(file)));
    }

    // 下载支持工单图片。
    @GetMapping("/{ticketId}/attachments/{attachmentId}")
    public ResponseEntity<Resource> downloadImage(@PathVariable Long ticketId, @PathVariable Long attachmentId) {
        IAccountSupportService.AttachmentContent content = accountSupport.downloadSupportTicketImage(ticketId, attachmentId);
        return WebFilePayloads.inline(content.content());
    }
}
