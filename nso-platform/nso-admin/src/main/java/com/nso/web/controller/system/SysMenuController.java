package com.nso.web.controller.system;

import com.nso.shared.core.domain.AjaxResult;
import com.nso.system.domain.SysMenu;
import com.nso.system.service.ISysMenuService;
import com.nso.business.core.NsoDtos.PageQuery;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping({"/api/v1/admin/system/menus", "/api/v1/admin/system/menu"})

// 菜单管理接口 负责系统菜单的维护
public class SysMenuController {
    // 系统菜单服务
    private final ISysMenuService menus;
    public SysMenuController(ISysMenuService menus) {
        this.menus = menus;
    }
    // 查询菜单列表。
    @GetMapping
    @PreAuthorize("hasAuthority('sys:permission:manage')")
    public AjaxResult<?> list(@RequestParam(required = false) Integer pageNo,
                              @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(menus.list(new PageQuery(pageNo, pageSize)));
    }
    // 创建菜单。
    @PostMapping
    @PreAuthorize("hasAuthority('sys:permission:manage')")
    public AjaxResult<?> create(@RequestBody SysMenu menu) {
        return AjaxResult.success(menus.create(menu));
    }
}
