package com.nso.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.system.domain.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {
    @Select("SELECT r.role_code FROM sys_role r INNER JOIN sys_user_role ur ON ur.role_id = r.id WHERE ur.user_id = #{userId} AND r.status = 'ENABLED' AND r.deleted = 0")
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    @Select("SELECT id FROM sys_role WHERE tenant_id = #{tenantId} AND role_code = #{roleCode} AND status = 'ENABLED' AND deleted = 0 LIMIT 1")
    Long selectIdByRoleCode(@Param("tenantId") Long tenantId, @Param("roleCode") String roleCode);
}
