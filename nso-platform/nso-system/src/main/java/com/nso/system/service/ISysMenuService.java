package com.nso.system.service;

import com.nso.system.domain.SysMenu;
import java.util.List;

public interface ISysMenuService {
    List<SysMenu> list();
    SysMenu create(SysMenu menu);
}
