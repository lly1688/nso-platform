package com.nso.business.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.business.document.domain.ProcessStep;
import org.apache.ibatis.annotations.Mapper;


/**
 * 工艺步骤 数据层
 */
@Mapper
public interface ProcessStepMapper extends BaseMapper<ProcessStep> {

}
