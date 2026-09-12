package com.nso.system.support.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.system.support.domain.SupportTicketAttachment;
import org.apache.ibatis.annotations.Mapper;


/**
 * 工单附件 数据层
 */
@Mapper
public interface SupportTicketAttachmentMapper extends BaseMapper<SupportTicketAttachment> {
}
