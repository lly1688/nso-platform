package com.nso.system.service;

import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.system.domain.SysMenu;
import com.nso.system.domain.SysRole;

import java.util.List;

/**
 * 角色与权限服务接口。
 */
public interface ISysRoleService {

    /**
     * 查询角色列表。
     *
     * @return 角色列表
     */
    List<SysRole> list();

    /**
     * 分页查询角色列表。
     *
     * @param pageQuery 分页参数
     * @return 角色分页结果
     */
    PageResult<SysRole> list(PageQuery pageQuery);

    /**
     * 创建角色。
     *
     * @param role 角色信息
     * @return 新建的角色
     */
    SysRole create(SysRole role);

    /**
     * 更新角色。
     *
     * @param roleId 角色编号
     * @param request 角色信息
     * @return 更新后的角色
     */
    SysRole update(Long roleId, SysRole request);

    /**
     * 查询角色权限配置。
     *
     * @param roleId 角色编号
     * @return 角色权限配置
     */
    RolePermissions permissions(Long roleId);

    /**
     * 替换角色权限。
     *
     * @param roleId 角色编号
     * @param permissionCodes 权限标识列表
     * @return 更新后的角色权限配置
     */
    RolePermissions replacePermissions(Long roleId, List<String> permissionCodes);

    /**
     * 角色权限配置。
     *
     * @param role 角色信息
     * @param selectedPermissionCodes 已选权限标识
     * @param catalog 菜单权限目录
     */
    record RolePermissions(SysRole role, List<String> selectedPermissionCodes, List<SysMenu> catalog) {
    }
}
