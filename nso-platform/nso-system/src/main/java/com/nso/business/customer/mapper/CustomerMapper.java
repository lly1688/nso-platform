package com.nso.business.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.business.customer.domain.Customer;
import org.apache.ibatis.annotations.Mapper;


/**
 * 客户 数据层
 */
@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {
}
