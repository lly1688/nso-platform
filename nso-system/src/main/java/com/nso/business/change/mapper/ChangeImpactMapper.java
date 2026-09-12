package com.nso.business.change.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.business.change.domain.ChangeImpact;
import org.apache.ibatis.annotations.Mapper;

/**
 * 变更影响数据访问接口。
 */
@Mapper
public interface ChangeImpactMapper extends BaseMapper<ChangeImpact> {
}
