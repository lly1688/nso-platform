package com.nso.web.controller.business;

import com.nso.common.core.domain.AjaxResult;
import com.nso.business.message.service.IMessageService;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class MessageController {

    private final IMessageService messageService;

    public MessageController(IMessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/messages")
    @PreAuthorize("hasAuthority('message:view')")
    public AjaxResult<?> messages(@RequestParam(required = false) String status) {
        return AjaxResult.success(messageService.list(status));
    }

    @PostMapping("/messages/{id}/read")
    @PreAuthorize("hasAuthority('message:view')")
    public AjaxResult<?> markMessageRead(@PathVariable Long id) {
        return AjaxResult.success(messageService.markRead(id));
    }
}
