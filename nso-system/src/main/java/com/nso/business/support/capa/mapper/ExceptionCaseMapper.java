package com.nso.business.support.capa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.business.support.capa.domain.ExceptionCase;
import org.apache.ibatis.annotations.Mapper;


/**
 * 异常案例 数据层
 */
@Mapper
public interface ExceptionCaseMapper extends BaseMapper<ExceptionCase> {
}
