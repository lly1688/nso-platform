package com.nso.business.risk.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.business.risk.domain.Risk;
import org.apache.ibatis.annotations.Mapper;

/**
 * 风险数据访问接口。
 */
@Mapper
public interface RiskMapper extends BaseMapper<Risk> {
}
