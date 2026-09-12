package com.nso.system.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 角色菜单关联数据访问接口。
 */
@Mapper
public interface SysRoleMenuMapper {

    /**
     * 查询角色拥有的权限标识。
     *
     * @param roleId 角色编号
     * @return 权限标识列表
     */
    @Select("SELECT m.permission_code FROM sys_role_menu rm JOIN sys_menu m ON m.id = rm.menu_id " +
            "WHERE rm.role_id = #{roleId} AND m.deleted = 0 ORDER BY m.sort_no, m.id")
    List<String> selectPermissionCodes(@Param("roleId") Long roleId);

    /**
     * 删除角色的菜单关联。
     *
     * @param roleId 角色编号
     * @return 影响行数
     */
    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 新增角色菜单关联。
     *
     * @param roleId 角色编号
     * @param menuId 菜单编号
     * @return 影响行数
     */
    @Insert("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (#{roleId}, #{menuId})")
    int insert(@Param("roleId") Long roleId, @Param("menuId") Long menuId);
}
