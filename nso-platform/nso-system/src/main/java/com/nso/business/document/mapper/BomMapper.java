package com.nso.business.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.business.document.domain.Bom;
import org.apache.ibatis.annotations.Mapper;


/**
 * BOM物料清单 数据层
 */
@Mapper
public interface BomMapper extends BaseMapper<Bom> {

}
