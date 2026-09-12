package com.nso.business.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.business.message.domain.Message;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息数据访问接口。
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
