package com.nso.web.controller.business;

import com.nso.shared.core.domain.AjaxResult;
import com.nso.business.core.NsoDtos.CustomerContactRequest;
import com.nso.business.core.NsoDtos.CustomerRequest;
import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.customer.service.ICustomerService;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")

// 客户管理接口 负责客户信息和项目授权管理
public class CustomerController {

    // 客户服务
    private final ICustomerService customerService;

    public CustomerController(ICustomerService customerService) {
        this.customerService = customerService;
    }

    // 查询客户列表。
    @GetMapping("/customers")
    @PreAuthorize("hasAnyAuthority('nso:customer:view', 'customer:view')")
    public AjaxResult<?> customers(@RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) Integer pageNo,
                                   @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(customerService.list(keyword, new PageQuery(pageNo, pageSize)));
    }

    // 创建客户。
    @PostMapping("/customers")
    @PreAuthorize("hasAnyAuthority('nso:customer:manage', 'customer:manage')")
    public AjaxResult<?> createCustomer(@RequestBody CustomerRequest request) {
        return AjaxResult.success(customerService.create(request));
    }

    // 查询客户联系人。
    @GetMapping("/customers/{customerId}/contacts")
    @PreAuthorize("hasAnyAuthority('nso:customer:view', 'customer:view')")
    public AjaxResult<?> contacts(@PathVariable Long customerId, @RequestParam(required = false) Integer pageNo,
                                  @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(customerService.contacts(customerId, new PageQuery(pageNo, pageSize)));

    }

    // 创建客户联系人。
    @PostMapping("/customers/{customerId}/contacts")
    @PreAuthorize("hasAnyAuthority('nso:customer:manage', 'customer:manage')")
    public AjaxResult<?> createContact(@PathVariable Long customerId, @RequestBody CustomerContactRequest request) {
        return AjaxResult.success(customerService.saveContact(customerId, null, request));
    }

    // 更新客户联系人。
    @PutMapping("/customers/{customerId}/contacts/{contactId}")
    @PreAuthorize("hasAnyAuthority('nso:customer:manage', 'customer:manage')")
    public AjaxResult<?> updateContact(@PathVariable Long customerId, @PathVariable Long contactId, @RequestBody CustomerContactRequest request) {
        return AjaxResult.success(customerService.saveContact(customerId, contactId, request));
    }

    // 更新客户联系人状态。
    @PutMapping("/customers/{customerId}/contacts/{contactId}/status")
    @PreAuthorize("hasAnyAuthority('nso:customer:manage', 'customer:manage')")
    public AjaxResult<?> updateContactStatus(@PathVariable Long customerId, @PathVariable Long contactId, @RequestBody java.util.Map<String, String> request) {
        return AjaxResult.success(customerService.updateContactStatus(customerId, contactId, request == null ? null : request.get("status")));
    }
}
