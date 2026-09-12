package com.nso.system.support.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.system.support.domain.SupportTicket;
import org.apache.ibatis.annotations.Mapper;


/**
 * 支持工单 数据层
 */
@Mapper
public interface SupportTicketMapper extends BaseMapper<SupportTicket> {
}
