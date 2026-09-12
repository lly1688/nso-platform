package com.nso.business.support.approval.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.business.support.approval.domain.ApprovalTask;
import org.apache.ibatis.annotations.Mapper;


/**
 * 审批任务 数据层
 */
@Mapper
public interface ApprovalTaskMapper extends BaseMapper<ApprovalTask> {
}
