package com.nso.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

// 系统角色实体。
@Data
@TableName("sys_role")
public class SysRole {

    // 角色编号
    @TableId
    private Long id;

    // 租户编号
    private Long tenantId;

    // 角色编码
    private String roleCode;

    // 角色名称
    private String roleName;

    // 角色状态
    private String status;

    // 数据权限范围
    private String dataScope;

    // 数据版本
    @Version
    private Integer version;

    // 删除标志
    @TableLogic
    private Integer deleted;
}
