package com.nso.web.controller.business;

import com.nso.common.core.domain.AjaxResult;
import com.nso.business.core.NsoDtos.CustomerRequest;
import com.nso.business.customer.service.ICustomerService;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class CustomerController {

    private final ICustomerService customerService;

    public CustomerController(ICustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping("/customers")
    @PreAuthorize("hasAuthority('customer:view')")
    public AjaxResult<?> customers(@RequestParam(required = false) String keyword) {
        return AjaxResult.success(customerService.list(keyword));
    }

    @PostMapping("/customers")
    @PreAuthorize("hasAuthority('customer:manage')")
    public AjaxResult<?> createCustomer(@RequestBody CustomerRequest request) {
        return AjaxResult.success(customerService.create(request));
    }
}
