package com.nso.business.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.business.message.domain.NotificationOutbox;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotificationOutboxMapper extends BaseMapper<NotificationOutbox> {
}
