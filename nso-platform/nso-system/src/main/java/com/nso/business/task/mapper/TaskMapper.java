package com.nso.business.task.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper; import com.nso.business.task.domain.Task; import org.apache.ibatis.annotations.Mapper;
@Mapper public interface TaskMapper extends BaseMapper<Task> { }
