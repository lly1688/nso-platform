package com.nso.system.service;

import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.system.domain.SysMenu;

import java.util.List;

/**
 * 菜单管理服务接口。
 */
public interface ISysMenuService {

    /**
     * 查询菜单列表。
     *
     * @return 菜单列表
     */
    List<SysMenu> list();

    /**
     * 分页查询菜单列表。
     *
     * @param pageQuery 分页参数
     * @return 菜单分页结果
     */
    PageResult<SysMenu> list(PageQuery pageQuery);

    /**
     * 创建菜单。
     *
     * @param menu 菜单信息
     * @return 新建的菜单
     */
    SysMenu create(SysMenu menu);
}
