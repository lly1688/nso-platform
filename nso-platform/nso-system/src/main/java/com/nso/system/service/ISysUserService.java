package com.nso.system.service;

import com.nso.system.domain.SysUser;

import java.util.List;
import java.util.Optional;

public interface ISysUserService {

    Optional<SysUser> findByUsername(String username);

    Optional<SysUser> findByWechatOpenId(String openId);

    Optional<SysUser> findById(Long userId);

    List<String> roleCodes(Long userId);

    List<String> permissionCodes(Long userId);

    List<String> menuRoutes(Long userId);

    int authVersion(Long userId);

    List<SysUser> listUsers();

    SysUser createUser(String username, String passwordHash, String nickname, Long deptId, List<String> roleCodes);

    SysUser ensureBootstrapAdmin(String username, String passwordHash);

    SysUser ensureDemoUser(String username, String passwordHash, String nickname, String roleCode);

    void replaceUserRoles(Long userId, List<String> roleCodes);

    void invalidateUsersByRole(Long roleId);

    void upgradePassword(Long userId, String passwordHash);
}
