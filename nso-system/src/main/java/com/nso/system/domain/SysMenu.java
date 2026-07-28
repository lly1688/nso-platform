package com.nso.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

@Data
@TableName("sys_menu")
public class SysMenu {
    @TableId private Long id;
    private Long parentId;
    private String menuName;
    private String routePath;
    private String permissionCode;
    private String componentName;
    private Integer visible;
    private Integer sortNo;
    private String status;
    @Version private Integer version;
    @TableLogic private Integer deleted;
}
