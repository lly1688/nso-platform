package com.nso.business.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.business.document.domain.DocumentVersion;
import org.apache.ibatis.annotations.Mapper;


/**
 * 文档版本 数据层
 */
@Mapper
public interface DocumentVersionMapper extends BaseMapper<DocumentVersion> {

}
