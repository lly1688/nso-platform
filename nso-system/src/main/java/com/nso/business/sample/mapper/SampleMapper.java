package com.nso.business.sample.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.business.sample.domain.Sample;
import org.apache.ibatis.annotations.Mapper;


/**
 * 打样 数据层
 */
@Mapper
public interface SampleMapper extends BaseMapper<Sample> {

}
