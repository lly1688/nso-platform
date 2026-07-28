package com.nso.web.controller.system;

import com.nso.common.core.domain.AjaxResult;
import com.nso.system.domain.SysRole;
import com.nso.system.service.ISysRoleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/admin/system/roles", "/api/v1/admin/system/role"})
@PreAuthorize("hasRole('ADMIN')")
public class SysRoleController {
    private final ISysRoleService roles;
    public SysRoleController(ISysRoleService roles) { this.roles = roles; }
    @GetMapping public AjaxResult<?> list() { return AjaxResult.success(roles.list()); }
    @PostMapping public AjaxResult<?> create(@RequestBody SysRole role) { return AjaxResult.success(roles.create(role)); }
    @GetMapping("/{roleId}/permissions") public AjaxResult<?> permissions(@PathVariable Long roleId) { return AjaxResult.success(roles.permissions(roleId)); }
    @PutMapping("/{roleId}/permissions") public AjaxResult<?> replacePermissions(@PathVariable Long roleId, @RequestBody RolePermissionsRequest request) {
        return AjaxResult.success(roles.replacePermissions(roleId, request == null ? List.of() : request.permissionCodes()));
    }

    public record RolePermissionsRequest(List<String> permissionCodes) { }
}
