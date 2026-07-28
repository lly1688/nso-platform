package com.nso.system.service;

import com.nso.system.domain.SysRole;
import com.nso.system.domain.SysMenu;
import java.util.List;

public interface ISysRoleService {
    List<SysRole> list();
    SysRole create(SysRole role);
    RolePermissions permissions(Long roleId);
    RolePermissions replacePermissions(Long roleId, List<String> permissionCodes);

    record RolePermissions(SysRole role, List<String> selectedPermissionCodes, List<SysMenu> catalog) { }
}
