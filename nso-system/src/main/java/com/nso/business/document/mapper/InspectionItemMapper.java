package com.nso.business.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.business.document.domain.InspectionItem;
import org.apache.ibatis.annotations.Mapper;


/**
 * 检验项目明细 数据层
 */
@Mapper
public interface InspectionItemMapper extends BaseMapper<InspectionItem> {

}
