package com.nso.web.controller.system;

import com.nso.common.core.domain.AjaxResult;
import com.nso.system.domain.SysMenu;
import com.nso.system.service.ISysMenuService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/admin/system/menus", "/api/v1/admin/system/menu"})
@PreAuthorize("hasRole('ADMIN')")
public class SysMenuController {
    private final ISysMenuService menus;
    public SysMenuController(ISysMenuService menus) { this.menus = menus; }
    @GetMapping public AjaxResult<?> list() { return AjaxResult.success(menus.list()); }
    @PostMapping public AjaxResult<?> create(@RequestBody SysMenu menu) { return AjaxResult.success(menus.create(menu)); }
}
