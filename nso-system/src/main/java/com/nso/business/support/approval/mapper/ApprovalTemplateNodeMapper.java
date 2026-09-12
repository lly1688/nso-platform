package com.nso.business.support.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.business.support.approval.domain.ApprovalTemplateNode;
import org.apache.ibatis.annotations.Mapper;


/**
 * 审批模板节点 数据层
 */
@Mapper
public interface ApprovalTemplateNodeMapper extends BaseMapper<ApprovalTemplateNode> {
}
