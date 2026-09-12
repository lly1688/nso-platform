package com.nso.web.controller.business;

import com.nso.shared.core.domain.AjaxResult;
import com.nso.business.message.service.IMessageService;
import com.nso.business.core.NsoDtos.PageQuery;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")

// 消息管理接口 负责消息的推送和查询
public class MessageController {

    // 消息服务
    private final IMessageService messageService;

    public MessageController(IMessageService messageService) {
        this.messageService = messageService;
    }

    // 查询当前用户消息。
    @GetMapping("/messages")
    @PreAuthorize("hasAnyAuthority('nso:message:view', 'message:view')")
    public AjaxResult<?> messages(@RequestParam(required = false) String status,
                                  @RequestParam(required = false) Integer pageNo,
                                  @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(messageService.list(status, new PageQuery(pageNo, pageSize)));
    }

    // 将消息标记为已读。
    @PostMapping("/messages/{id}/read")
    @PreAuthorize("hasAnyAuthority('nso:message:view', 'message:view')")
    public AjaxResult<?> markMessageRead(@PathVariable Long id) {
        return AjaxResult.success(messageService.markRead(id));
    }
}
