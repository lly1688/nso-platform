package com.nso.business.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.business.document.domain.BomItem;
import org.apache.ibatis.annotations.Mapper;


/**
 * BOM物料明细行 数据层
 */
@Mapper
public interface BomItemMapper extends BaseMapper<BomItem> {

}
