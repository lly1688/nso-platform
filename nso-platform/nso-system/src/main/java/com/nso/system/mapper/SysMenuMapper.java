package com.nso.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.system.domain.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜单数据访问接口。
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 查询用户拥有的权限标识。
     *
     * @param userId 用户编号
     * @param tenantId 租户编号
     * @return 权限标识列表
     */
    @Select("SELECT DISTINCT m.permission_code FROM sys_menu m " +
            "JOIN sys_role_menu rm ON rm.menu_id = m.id " +
            "JOIN sys_user_role ur ON ur.role_id = rm.role_id " +
            "JOIN sys_role r ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.tenant_id = #{tenantId} " +
            "AND r.status = 'ENABLED' AND r.deleted = 0 AND m.status = 'ENABLED' AND m.deleted = 0 " +
            "AND m.permission_code IS NOT NULL ORDER BY m.permission_code")
    List<String> selectPermissionCodesByUserId(@Param("userId") Long userId, @Param("tenantId") Long tenantId);

    /**
     * 查询用户可访问的菜单路由。
     *
     * @param userId 用户编号
     * @param tenantId 租户编号
     * @return 菜单路由列表
     */
    @Select("SELECT DISTINCT m.route_path FROM sys_menu m " +
            "JOIN sys_role_menu rm ON rm.menu_id = m.id " +
            "JOIN sys_user_role ur ON ur.role_id = rm.role_id " +
            "JOIN sys_role r ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.tenant_id = #{tenantId} " +
            "AND r.status = 'ENABLED' AND r.deleted = 0 AND m.status = 'ENABLED' AND m.deleted = 0 " +
            "AND m.visible = 1 AND m.route_path IS NOT NULL ORDER BY m.route_path")
    List<String> selectMenuRoutesByUserId(@Param("userId") Long userId, @Param("tenantId") Long tenantId);
}
