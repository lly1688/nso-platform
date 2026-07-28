package com.nso.business.customer.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nso.business.core.NsoDtos.CustomerDto;
import com.nso.business.core.NsoDtos.CustomerRequest;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.business.core.TenantContext;
import com.nso.business.customer.domain.Customer;
import com.nso.business.customer.mapper.CustomerMapper;
import com.nso.business.customer.service.ICustomerService;
import com.nso.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class CustomerServiceImpl implements ICustomerService {
    private final CustomerMapper customerMapper;

    public CustomerServiceImpl(CustomerMapper customerMapper) {
        this.customerMapper = customerMapper;
    }

    @Override
    public PageResult<CustomerDto> list(String keyword) {
        boolean canViewContacts = TenantContext.hasAnyRole("admin", "project_manager");
        List<CustomerDto> rows = customerMapper.selectList(Wrappers.<Customer>lambdaQuery()
                        .and(keyword != null && !keyword.isBlank(), query -> query.like(Customer::getName, keyword)
                                .or().like(Customer::getCustomerCode, keyword)
                                .or(canViewContacts, nested -> nested.like(Customer::getContactName, keyword)))
                        .orderByDesc(Customer::getId))
                .stream().map(this::toDto).toList();
        return new PageResult<>(rows, rows.size());
    }

    @Override
    @Transactional
    public CustomerDto create(CustomerRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new BusinessException("客户名称不能为空");
        }
        Customer customer = new Customer();
        customer.setTenantId(TenantContext.tenantId());
        customer.setCustomerCode("CUS-" + LocalDate.now().toString().replace("-", "") + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        customer.setName(request.name().trim());
        customer.setIndustry(request.industry());
        customer.setContactName(request.contactName());
        customer.setPhone(request.phone());
        customer.setStatus(request.status() == null || request.status().isBlank() ? "ENABLED" : request.status());
        customerMapper.insert(customer);
        return toDto(customer);
    }

    private CustomerDto toDto(Customer source) {
        boolean canViewContacts = TenantContext.hasAnyRole("admin", "project_manager");
        return new CustomerDto(source.getId(), source.getCustomerCode(), source.getName(), source.getIndustry(),
                canViewContacts ? source.getContactName() : null, canViewContacts ? source.getPhone() : null, source.getStatus());
    }
}
