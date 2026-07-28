package com.nso.business.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.business.customer.domain.Customer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {
}
