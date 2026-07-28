package com.nso.business.customer.service;

import com.nso.business.core.NsoDtos.CustomerDto;
import com.nso.business.core.NsoDtos.CustomerRequest;
import com.nso.business.core.NsoDtos.PageResult;

public interface ICustomerService {
    PageResult<CustomerDto> list(String keyword);
    CustomerDto create(CustomerRequest request);
}
