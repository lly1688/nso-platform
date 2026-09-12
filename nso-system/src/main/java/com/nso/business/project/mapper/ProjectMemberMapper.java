package com.nso.business.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.business.project.domain.ProjectMember;
import org.apache.ibatis.annotations.Mapper;


/**
 * 项目成员 数据层
 */
@Mapper
public interface ProjectMemberMapper extends BaseMapper<ProjectMember> {

}
