package com.nso.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

// 系统菜单实体。
@Data
@TableName("sys_menu")
public class SysMenu {

    // 菜单编号
    @TableId
    private Long id;

    // 上级菜单编号
    private Long parentId;

    // 菜单名称
    private String menuName;

    // 路由路径
    private String routePath;

    // 权限编码
    private String permissionCode;

    // 前端组件名称
    private String componentName;

    // 是否显示
    private Integer visible;

    // 显示顺序
    private Integer sortNo;

    // 菜单状态
    private String status;

    // 数据版本
    @Version
    private Integer version;

    // 删除标志
    @TableLogic
    private Integer deleted;
}
