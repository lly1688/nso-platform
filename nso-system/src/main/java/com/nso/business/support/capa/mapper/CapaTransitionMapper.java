package com.nso.business.support.capa.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.business.support.capa.domain.CapaTransition;
import org.apache.ibatis.annotations.Mapper;


/**
 * CAPA 状态流转数据访问接口。
 */
@Mapper
public interface CapaTransitionMapper extends BaseMapper<CapaTransition> {
}
