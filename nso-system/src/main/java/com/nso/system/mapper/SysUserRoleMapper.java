package com.nso.system.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户角色关联数据访问接口。
 */
@Mapper
public interface SysUserRoleMapper {

    /**
     * 新增用户角色关联。
     *
     * @param userId 用户编号
     * @param roleId 角色编号
     * @return 影响行数
     */
    @Insert("INSERT INTO sys_user_role (user_id, role_id) VALUES (#{userId}, #{roleId})")
    int insertRole(@Param("userId") Long userId, @Param("roleId") Long roleId);

    /**
     * 删除用户的全部角色关联。
     *
     * @param userId 用户编号
     * @return 影响行数
     */
    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 查询角色关联的用户编号。
     *
     * @param roleId 角色编号
     * @return 用户编号列表
     */
    @Select("SELECT ur.user_id FROM sys_user_role ur WHERE ur.role_id = #{roleId}")
    List<Long> selectUserIdsByRoleId(@Param("roleId") Long roleId);
}
