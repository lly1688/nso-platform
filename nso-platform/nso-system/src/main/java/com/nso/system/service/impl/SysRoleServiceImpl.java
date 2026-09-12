package com.nso.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nso.business.core.TenantContext;
import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.business.core.PageSupport;
import com.nso.shared.exception.BusinessException;
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

// 角色管理 服务层处理
public class SysRoleServiceImpl implements ISysRoleService {
    // 系统角色数据映射
    private final SysRoleMapper roles;
    // 系统菜单数据映射
    private final SysMenuMapper menus;
    // 系统角色菜单数据映射
    private final SysRoleMenuMapper roleMenus;
    // 系统用户服务
    private final ISysUserService users;

    public SysRoleServiceImpl(SysRoleMapper roles, SysMenuMapper menus, SysRoleMenuMapper roleMenus, ISysUserService users) {
        this.roles = roles;
        this.menus = menus;
        this.roleMenus = roleMenus;
        this.users = users;
    }

    // 查询角色列表。
    @Override
    public List<SysRole> list() {
        return roles.selectList(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getTenantId, TenantContext.tenantId()).orderByAsc(SysRole::getId));
    }

    // 分页查询角色列表。
    @Override
    public PageResult<SysRole> list(PageQuery pageQuery) {
        PageQuery page = PageSupport.normalize(pageQuery);
        Page<SysRole> entityPage = roles.selectPage(PageSupport.page(page), Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getTenantId, TenantContext.tenantId()).orderByAsc(SysRole::getId));
        return new PageResult<>(entityPage.getRecords(), entityPage.getTotal(), page.pageNoValue(), page.pageSizeValue());
    }

    // 创建角色。
    @Override
    @Transactional
    public SysRole create(SysRole role) {
        if (role == null || role.getRoleCode() == null || role.getRoleCode().isBlank()
                 || role.getRoleName() == null || role.getRoleName().isBlank()) {
            throw new BusinessException("角色编码和角色名称不能为空");
        }
        role.setTenantId(TenantContext.tenantId());
        role.setRoleCode(role.getRoleCode().trim().toLowerCase());
        if (protectedRole(role.getRoleCode())) throw new BusinessException("受保护系统角色由平台治理流程维护，不能直接创建");
        role.setRoleName(role.getRoleName().trim());
        role.setStatus(role.getStatus() == null || role.getStatus().isBlank() ? "ENABLED" : role.getStatus());
        role.setDataScope(normalizeScope(role.getDataScope()));
        roles.insert(role);
        return role;
    }

    // 更新角色。
    @Override
    @Transactional
    public SysRole update(Long roleId, SysRole request) {
        SysRole role = requireRole(roleId);
        if (protectedRole(role.getRoleCode()) && !TenantContext.hasAnyRole("superadmin")) throw new BusinessException("只有超级管理员可以维护受保护系统角色");
        if (request == null) throw new BusinessException("角色参数不能为空");
        if (request.getRoleName() != null && !request.getRoleName().isBlank()) role.setRoleName(request.getRoleName().trim());
        if (request.getStatus() != null && !request.getStatus().isBlank()) role.setStatus(request.getStatus().trim().toUpperCase());
        role.setDataScope(normalizeScope(request.getDataScope() == null ? role.getDataScope() : request.getDataScope()));
        roles.updateById(role);
        users.invalidateUsersByRole(roleId);
        return roles.selectById(roleId);
    }

    // 查询角色权限配置。
    @Override
    public RolePermissions permissions(Long roleId) {
        return view(requireRole(roleId));
    }

    // 替换角色权限。
    @Override
    @Transactional
    public RolePermissions replacePermissions(Long roleId, List<String> permissionCodes) {
        SysRole role = requireRole(roleId);
        if (protectedRole(role.getRoleCode()) && !TenantContext.hasAnyRole("superadmin")) throw new BusinessException("只有超级管理员可以维护受保护系统角色");
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

    private String normalizeScope(String value) {
        String scope = value == null || value.isBlank() ? "SELF" : value.trim().toUpperCase();
        if (!List.of("SELF", "PROJECT_MEMBER", "PROJECT_OWNER", "CUSTOM_DEPT", "DEPT", "DEPT_AND_CHILD", "ALL").contains(scope)) throw new BusinessException("数据范围不合法");
        return scope;
    }

    private boolean protectedRole(String code) {
        return List.of("superadmin", "system_admin", "hr_admin").contains(code);
    }
}
