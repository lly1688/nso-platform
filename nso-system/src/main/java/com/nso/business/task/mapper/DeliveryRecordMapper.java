package com.nso.business.task.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.business.task.domain.DeliveryRecord;
import org.apache.ibatis.annotations.Mapper;


/**
 * 交付记录 数据层
 */
@Mapper
public interface DeliveryRecordMapper extends BaseMapper<DeliveryRecord> {

}
