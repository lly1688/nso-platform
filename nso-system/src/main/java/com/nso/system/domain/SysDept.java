package com.nso.system.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

// 系统部门实体。
@Data
@TableName("sys_dept")
public class SysDept {

    // 部门编号
    @TableId
    private Long id;

    // 租户编号
    private Long tenantId;

    // 上级部门编号
    private Long parentId;

    // 祖级路径
    private String ancestors;

    // 部门名称
    private String deptName;

    // 负责人姓名
    private String leaderName;

    // 负责人用户编号
    private Long leaderUserId;

    // 联系电话
    private String phone;

    // 显示顺序
    private Integer sortNo;

    // 部门状态
    private String status;

    // 数据版本
    @Version
    private Integer version;

    // 删除标志
    @TableLogic
    private Integer deleted;
}
