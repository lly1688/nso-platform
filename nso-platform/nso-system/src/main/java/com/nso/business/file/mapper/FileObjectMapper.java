package com.nso.business.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.business.file.domain.FileObject;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文件对象数据访问接口。
 */
@Mapper
public interface FileObjectMapper extends BaseMapper<FileObject> {
}
