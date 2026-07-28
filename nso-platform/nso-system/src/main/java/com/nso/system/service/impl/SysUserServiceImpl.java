package com.nso.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nso.system.domain.SysUser;
import com.nso.system.mapper.SysRoleMapper;
import com.nso.system.mapper.SysMenuMapper;
import com.nso.system.mapper.SysUserMapper;
import com.nso.system.mapper.SysUserRoleMapper;
import com.nso.system.service.ISysUserService;
import com.nso.common.exception.BusinessException;
import com.nso.business.core.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SysUserServiceImpl implements ISysUserService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysUserRoleMapper userRoleMapper;

    public SysUserServiceImpl(SysUserMapper userMapper, SysRoleMapper roleMapper, SysMenuMapper menuMapper, SysUserRoleMapper userRoleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.menuMapper = menuMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public Optional<SysUser> findByUsername(String username) {
        return Optional.ofNullable(userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getStatus, "ENABLED")));
    }

    @Override
    public Optional<SysUser> findByWechatOpenId(String openId) {
        return Optional.ofNullable(userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getWechatOpenId, openId)
                .eq(SysUser::getStatus, "ENABLED")));
    }

    @Override
    public Optional<SysUser> findById(Long userId) {
        return Optional.ofNullable(userMapper.selectById(userId));
    }

    @Override
    public List<String> roleCodes(Long userId) {
        return roleMapper.selectRoleCodesByUserId(userId);
    }

    @Override
    public List<String> permissionCodes(Long userId) {
        SysUser user = findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
        return menuMapper.selectPermissionCodesByUserId(userId, user.getTenantId());
    }

    @Override
    public List<String> menuRoutes(Long userId) {
        SysUser user = findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
        return menuMapper.selectMenuRoutesByUserId(userId, user.getTenantId());
    }

    @Override
    public int authVersion(Long userId) {
        return findById(userId).map(user -> user.getAuthVersion() == null ? 0 : user.getAuthVersion()).orElse(-1);
    }

    @Override
    public List<SysUser> listUsers() {
        return userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getTenantId, TenantContext.tenantId()).orderByAsc(SysUser::getId));
    }

    @Override
    @Transactional
    public SysUser createUser(String username, String passwordHash, String nickname, Long deptId, List<String> roleCodes) {
        if (username == null || username.isBlank() || passwordHash == null || passwordHash.isBlank()) {
            throw new BusinessException("用户名和密码不能为空");
        }
        if (findByUsername(username.trim()).isPresent()) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(username.trim());
        user.setTenantId(TenantContext.tenantId());
        user.setDeptId(deptId);
        user.setPasswordHash(passwordHash);
        user.setNickname(nickname == null || nickname.isBlank() ? username.trim() : nickname.trim());
        user.setStatus("ENABLED");
        userMapper.insert(user);
        for (String roleCode : roleCodes == null || roleCodes.isEmpty() ? List.of("field_user") : roleCodes) {
            Long roleId = roleMapper.selectIdByRoleCode(user.getTenantId(), roleCode.trim().toLowerCase());
            if (roleId == null) throw new BusinessException("角色不存在：" + roleCode);
            userRoleMapper.insertRole(user.getId(), roleId);
        }
        return user;
    }

    @Override
    @Transactional
    public SysUser ensureBootstrapAdmin(String username, String passwordHash) {
        SysUser existing = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
        if (existing != null) {
            return existing;
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setTenantId(1L);
        user.setPasswordHash(passwordHash);
        user.setNickname("系统管理员");
        user.setStatus("ENABLED");
        userMapper.insert(user);
        Long adminRoleId = roleMapper.selectIdByRoleCode(1L, "admin");
        if (adminRoleId != null) {
            userRoleMapper.insertRole(user.getId(), adminRoleId);
        }
        return user;
    }

    @Override
    public void upgradePassword(Long userId, String passwordHash) {
        userMapper.updatePassword(userId, passwordHash);
        userMapper.incrementAuthVersion(userId);
    }

    @Override
    @Transactional
    public SysUser ensureDemoUser(String username, String passwordHash, String nickname, String roleCode) {
        SysUser existing = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUsername, username));
        if (existing != null) {
            return existing;
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setTenantId(1L);
        user.setPasswordHash(passwordHash);
        user.setNickname(nickname);
        user.setStatus("ENABLED");
        userMapper.insert(user);
        Long roleId = roleMapper.selectIdByRoleCode(1L, roleCode);
        if (roleId == null) {
            throw new BusinessException("演示角色不存在：" + roleCode);
        }
        userRoleMapper.insertRole(user.getId(), roleId);
        return user;
    }

    @Override
    @Transactional
    public void replaceUserRoles(Long userId, List<String> roleCodes) {
        SysUser user = findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
        if (TenantContext.tenantId() != user.getTenantId()) {
            throw new BusinessException("不能修改其他企业的用户角色");
        }
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new BusinessException("至少需要选择一个角色");
        }
        userRoleMapper.deleteByUserId(userId);
        for (String roleCode : roleCodes.stream().filter(code -> code != null && !code.isBlank()).map(code -> code.trim().toLowerCase()).distinct().toList()) {
            Long roleId = roleMapper.selectIdByRoleCode(user.getTenantId(), roleCode);
            if (roleId == null) {
                throw new BusinessException("角色不存在：" + roleCode);
            }
            userRoleMapper.insertRole(userId, roleId);
        }
        userMapper.incrementAuthVersion(userId);
    }

    @Override
    @Transactional
    public void invalidateUsersByRole(Long roleId) {
        for (Long userId : userRoleMapper.selectUserIdsByRoleId(roleId)) {
            userMapper.incrementAuthVersion(userId);
        }
    }
}
