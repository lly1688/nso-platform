package com.nso.business.sample.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.business.sample.domain.SampleConfirm;
import org.apache.ibatis.annotations.Mapper;


/**
 * 打样确认 数据层
 */
@Mapper
public interface SampleConfirmMapper extends BaseMapper<SampleConfirm> {

}
