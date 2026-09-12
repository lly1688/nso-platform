package com.nso.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.business.core.PageSupport;
import com.nso.shared.exception.BusinessException;
import com.nso.system.domain.SysMenu;
import com.nso.system.mapper.SysMenuMapper;
import com.nso.system.service.ISysMenuService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service

// 菜单管理 服务层处理
public class SysMenuServiceImpl implements ISysMenuService {
    // 系统菜单数据映射
    private final SysMenuMapper menus;

    public SysMenuServiceImpl(SysMenuMapper menus) {
        this.menus = menus;
    }

    // 查询菜单列表。
    @Override
    public List<SysMenu> list() {
        return menus.selectList(Wrappers.<SysMenu>lambdaQuery().orderByAsc(SysMenu::getSortNo).orderByAsc(SysMenu::getId));
    }

    // 分页查询菜单列表。
    @Override
    public PageResult<SysMenu> list(PageQuery pageQuery) {
        PageQuery page = PageSupport.normalize(pageQuery);
        Page<SysMenu> entityPage = menus.selectPage(PageSupport.page(page),
                Wrappers.<SysMenu>lambdaQuery().orderByAsc(SysMenu::getSortNo).orderByAsc(SysMenu::getId));
        return new PageResult<>(entityPage.getRecords(), entityPage.getTotal(), page.pageNoValue(), page.pageSizeValue());
    }

    // 创建菜单。
    @Override
    @Transactional
    public SysMenu create(SysMenu menu) {
        if (menu == null || menu.getMenuName() == null || menu.getMenuName().isBlank()
                 || menu.getPermissionCode() == null || menu.getPermissionCode().isBlank()) {
            throw new BusinessException("菜单名称和权限编码不能为空");
        }
        menu.setMenuName(menu.getMenuName().trim());
        menu.setPermissionCode(menu.getPermissionCode().trim());
        menu.setVisible(menu.getVisible() == null ? 1 : menu.getVisible());
        menu.setSortNo(menu.getSortNo() == null ? 0 : menu.getSortNo());
        menu.setStatus(menu.getStatus() == null || menu.getStatus().isBlank() ? "ENABLED" : menu.getStatus());
        menus.insert(menu);
        return menu;
    }
}
