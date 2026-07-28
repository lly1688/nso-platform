package com.nso.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nso.business.core.TenantContext;
import com.nso.common.exception.BusinessException;
import com.nso.system.domain.SysRole;
import com.nso.system.domain.SysMenu;
import com.nso.system.mapper.SysMenuMapper;
import com.nso.system.mapper.SysRoleMenuMapper;
import com.nso.system.mapper.SysRoleMapper;
import com.nso.system.service.ISysRoleService;
import com.nso.system.service.ISysUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SysRoleServiceImpl implements ISysRoleService {
    private final SysRoleMapper roles;
    private final SysMenuMapper menus;
    private final SysRoleMenuMapper roleMenus;
    private final ISysUserService users;

    public SysRoleServiceImpl(SysRoleMapper roles, SysMenuMapper menus, SysRoleMenuMapper roleMenus, ISysUserService users) {
        this.roles = roles;
        this.menus = menus;
        this.roleMenus = roleMenus;
        this.users = users;
    }

    @Override
    public List<SysRole> list() {
        return roles.selectList(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getTenantId, TenantContext.tenantId()).orderByAsc(SysRole::getId));
    }

    @Override
    @Transactional
    public SysRole create(SysRole role) {
        if (role == null || role.getRoleCode() == null || role.getRoleCode().isBlank()
                || role.getRoleName() == null || role.getRoleName().isBlank()) {
            throw new BusinessException("角色编码和角色名称不能为空");
        }
        role.setTenantId(TenantContext.tenantId());
        role.setRoleCode(role.getRoleCode().trim().toLowerCase());
        role.setRoleName(role.getRoleName().trim());
        role.setStatus(role.getStatus() == null || role.getStatus().isBlank() ? "ENABLED" : role.getStatus());
        roles.insert(role);
        return role;
    }

    @Override
    public RolePermissions permissions(Long roleId) {
        return view(requireRole(roleId));
    }

    @Override
    @Transactional
    public RolePermissions replacePermissions(Long roleId, List<String> permissionCodes) {
        SysRole role = requireRole(roleId);
        List<String> requested = permissionCodes == null ? List.of() : permissionCodes.stream()
                .filter(code -> code != null && !code.isBlank()).map(String::trim).distinct().toList();
        List<SysMenu> catalog = menus.selectList(Wrappers.<SysMenu>lambdaQuery()
                .eq(SysMenu::getStatus, "ENABLED").orderByAsc(SysMenu::getSortNo).orderByAsc(SysMenu::getId));
        Map<String, SysMenu> byCode = catalog.stream().filter(menu -> menu.getPermissionCode() != null)
                .collect(Collectors.toMap(SysMenu::getPermissionCode, menu -> menu, (left, right) -> left));
        for (String code : requested) {
            if (!byCode.containsKey(code)) {
                throw new BusinessException("权限编码不存在或已禁用：" + code);
            }
        }
        roleMenus.deleteByRoleId(roleId);
        for (String code : requested) {
            roleMenus.insert(roleId, byCode.get(code).getId());
        }
        users.invalidateUsersByRole(roleId);
        return view(role);
    }

    private SysRole requireRole(Long roleId) {
        SysRole role = roles.selectById(roleId);
        if (role == null || TenantContext.tenantId() != role.getTenantId() || Integer.valueOf(1).equals(role.getDeleted())) {
            throw new BusinessException("角色不存在或不属于当前企业");
        }
        return role;
    }

    private RolePermissions view(SysRole role) {
        List<SysMenu> catalog = menus.selectList(Wrappers.<SysMenu>lambdaQuery()
                .eq(SysMenu::getStatus, "ENABLED").orderByAsc(SysMenu::getSortNo).orderByAsc(SysMenu::getId));
        return new RolePermissions(role, roleMenus.selectPermissionCodes(role.getId()), catalog);
    }
}
