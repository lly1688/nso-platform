package com.nso.business.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.business.project.domain.Project;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
}
