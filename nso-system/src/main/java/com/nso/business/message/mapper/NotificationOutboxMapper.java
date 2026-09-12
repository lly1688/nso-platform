package com.nso.business.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.business.message.domain.NotificationOutbox;
import org.apache.ibatis.annotations.Mapper;


/**
 * 通知发件箱 数据层
 */
@Mapper
public interface NotificationOutboxMapper extends BaseMapper<NotificationOutbox> {
}
