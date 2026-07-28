package com.nso.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nso.common.exception.BusinessException;
import com.nso.system.domain.SysMenu;
import com.nso.system.mapper.SysMenuMapper;
import com.nso.system.service.ISysMenuService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SysMenuServiceImpl implements ISysMenuService {
    private final SysMenuMapper menus;

    public SysMenuServiceImpl(SysMenuMapper menus) { this.menus = menus; }

    @Override
    public List<SysMenu> list() {
        return menus.selectList(Wrappers.<SysMenu>lambdaQuery().orderByAsc(SysMenu::getSortNo).orderByAsc(SysMenu::getId));
    }

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
