package com.nso.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.nso.system.domain.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    @Update("UPDATE sys_user SET password_hash = #{passwordHash} WHERE id = #{userId}")
    int updatePassword(@Param("userId") Long userId, @Param("passwordHash") String passwordHash);

    @Update("UPDATE sys_user SET auth_version = auth_version + 1 WHERE id = #{userId}")
    int incrementAuthVersion(@Param("userId") Long userId);

    @Update("UPDATE sys_user SET nickname = #{nickname}, phone = #{phone}, email = #{email}, gender = #{gender}, " +
            "version = version + 1 WHERE id = #{userId} AND tenant_id = #{tenantId} AND version = #{version}")
    int updateProfile(@Param("userId") Long userId, @Param("tenantId") Long tenantId,
                      @Param("nickname") String nickname, @Param("phone") String phone,
                      @Param("email") String email, @Param("gender") String gender,
                      @Param("version") Integer version);

    @Update("UPDATE sys_user SET avatar_file_id = #{fileId}, version = version + 1 " +
            "WHERE id = #{userId} AND tenant_id = #{tenantId}")
    int updateAvatar(@Param("userId") Long userId, @Param("tenantId") Long tenantId, @Param("fileId") Long fileId);
}
