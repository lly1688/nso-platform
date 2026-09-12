package com.nso.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.system.domain.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 用户数据访问接口。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 更新用户密码散列。
     *
     * @param userId 用户编号
     * @param passwordHash 密码散列
     * @return 影响行数
     */
    @Update("UPDATE sys_user SET password_hash = #{passwordHash} WHERE id = #{userId}")
    int updatePassword(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);

    /**
     * 更新强制改密标志。
     *
     * @param userId 用户编号
     * @param forceChangePassword 强制改密标志
     * @return 影响行数
     */
    @Update("UPDATE sys_user SET force_change_password = #{forceChangePassword} WHERE id = #{userId}")
    int updateForceChangePassword(@Param("userId") Long userId, @Param("forceChangePassword") Integer forceChangePassword);

    /**
     * 递增授权版本，使旧会话失效。
     *
     * @param userId 用户编号
     * @return 影响行数
     */
    @Update("UPDATE sys_user SET auth_version = auth_version + 1 WHERE id = #{userId}")
    int incrementAuthVersion(@Param("userId") Long userId);

    /**
     * 递增角色关联用户的授权版本，使角色权限调整即时失效。
     *
     * @param tenantId 租户编号
     * @param roleId 角色编号
     * @return 影响行数
     */
    @Update("UPDATE sys_user u JOIN sys_user_role ur ON ur.user_id = u.id " +
            "SET u.auth_version = u.auth_version + 1 WHERE u.tenant_id = #{tenantId} AND ur.role_id = #{roleId}")
    int incrementAuthVersionByRoleId(@Param("tenantId") Long tenantId, @Param("roleId") Long roleId);

    /**
     * 按版本号更新用户资料。
     *
     * @param userId 用户编号
     * @param tenantId 租户编号
     * @param nickname 用户昵称
     * @param phone 手机号码
     * @param email 邮箱地址
     * @param gender 用户性别
     * @param version 当前版本号
     * @return 影响行数
     */
    @Update("UPDATE sys_user SET nickname = #{nickname}, phone = #{phone}, email = #{email}, gender = #{gender}, " +
            "version = version + 1 WHERE id = #{userId} AND tenant_id = #{tenantId} AND version = #{version}")
    int updateProfile(@Param("userId") Long userId, @Param("tenantId") Long tenantId,
            @Param("nickname") String nickname, @Param("phone") String phone,
            @Param("email") String email, @Param("gender") String gender,
            @Param("version") Integer version);

    /**
     * 更新用户头像文件。
     *
     * @param userId 用户编号
     * @param tenantId 租户编号
     * @param fileId 文件编号
     * @return 影响行数
     */
    @Update("UPDATE sys_user SET avatar_file_id = #{fileId}, version = version + 1 " +
            "WHERE id = #{userId} AND tenant_id = #{tenantId}")
    int updateAvatar(@Param("userId") Long userId, @Param("tenantId") Long tenantId, @Param("fileId") Long fileId);
}
