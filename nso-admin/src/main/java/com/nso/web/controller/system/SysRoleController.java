package com.nso.web.controller.system;

import com.nso.shared.core.domain.AjaxResult;
import com.nso.system.domain.SysRole;
import com.nso.system.service.ISysRoleService;
import com.nso.business.core.NsoDtos.PageQuery;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/admin/system/roles", "/api/v1/admin/system/role"})

// 角色管理接口 负责角色的维护和权限分配
public class SysRoleController {
    // 系统角色服务
    private final ISysRoleService roles;
    public SysRoleController(ISysRoleService roles) {
        this.roles = roles;
    }
    // 查询角色列表。
    @GetMapping
    @PreAuthorize("hasAuthority('sys:role:read')")
    public AjaxResult<?> list(@RequestParam(required = false) Integer pageNo,
                              @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(roles.list(new PageQuery(pageNo, pageSize)));
    }
    // 创建角色。
    @PostMapping
    @PreAuthorize("hasAuthority('sys:role:manage')")
    public AjaxResult<?> create(@RequestBody SysRole role) {
        return AjaxResult.success(roles.create(role));
    }
    // 更新角色。
    @PutMapping("/{roleId}")
    @PreAuthorize("hasAuthority('sys:role:manage')")
    public AjaxResult<?> update(@PathVariable Long roleId, @RequestBody SysRole role) {

        return AjaxResult.success(roles.update(roleId, role));
    }
    // 查询角色权限配置。
    @GetMapping("/{roleId}/permissions")
    @PreAuthorize("hasAuthority('sys:role:read')")
    public AjaxResult<?> permissions(@PathVariable Long roleId) {
        return AjaxResult.success(roles.permissions(roleId));
    }
    // 替换角色权限。
    @PutMapping("/{roleId}/permissions")
    @PreAuthorize("hasAuthority('sys:role:grant')")
    public AjaxResult<?> replacePermissions(@PathVariable Long roleId, @RequestBody RolePermissionsRequest request) {
        return AjaxResult.success(roles.replacePermissions(roleId, request == null ? List.of() : request.permissionCodes()));

    }

    public record RolePermissionsRequest(
        // 权限标识列表
        List<String> permissionCodes
    ) {

    }
}
