package com.nso.business.sample.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.business.sample.domain.SampleCheck;
import org.apache.ibatis.annotations.Mapper;


/**
 * 打样检查 数据层
 */
@Mapper
public interface SampleCheckMapper extends BaseMapper<SampleCheck> {

}
