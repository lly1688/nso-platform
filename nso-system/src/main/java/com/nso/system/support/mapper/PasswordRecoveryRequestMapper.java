package com.nso.system.support.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.system.support.domain.PasswordRecoveryRequest;
import org.apache.ibatis.annotations.Mapper;


/**
 * 密码找回申请 数据层
 */
@Mapper
public interface PasswordRecoveryRequestMapper extends BaseMapper<PasswordRecoveryRequest> {
}
