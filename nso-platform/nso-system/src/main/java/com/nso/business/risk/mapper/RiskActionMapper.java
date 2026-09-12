package com.nso.business.risk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.business.risk.domain.RiskAction;
import org.apache.ibatis.annotations.Mapper;


/**
 * 风险措施 数据层
 */
@Mapper
public interface RiskActionMapper extends BaseMapper<RiskAction> {
}
