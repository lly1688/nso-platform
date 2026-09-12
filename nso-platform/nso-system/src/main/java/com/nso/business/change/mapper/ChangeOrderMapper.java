package com.nso.business.change.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.business.change.domain.ChangeOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 变更单数据访问接口。
 */
@Mapper
public interface ChangeOrderMapper extends BaseMapper<ChangeOrder> {
}
