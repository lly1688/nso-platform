package com.nso.business.support.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.business.support.approval.domain.ApprovalInstance;
import org.apache.ibatis.annotations.Mapper;


/**
 * 审批实例 数据层
 */
@Mapper
public interface ApprovalInstanceMapper extends BaseMapper<ApprovalInstance> {
}
