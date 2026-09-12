package com.nso.business.support.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.business.support.approval.domain.ApprovalTemplate;
import org.apache.ibatis.annotations.Mapper;


/**
 * 审批模板 数据层
 */
@Mapper
public interface ApprovalTemplateMapper extends BaseMapper<ApprovalTemplate> {
}
