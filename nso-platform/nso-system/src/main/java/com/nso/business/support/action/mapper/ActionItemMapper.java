package com.nso.business.support.action.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.business.support.action.domain.ActionItem;
import org.apache.ibatis.annotations.Mapper;


/**
 * 待办行动项 数据层
 */
@Mapper
public interface ActionItemMapper extends BaseMapper<ActionItem> {
}
