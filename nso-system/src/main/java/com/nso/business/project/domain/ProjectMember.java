package com.nso.business.project.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

// 项目成员实体。
@Data
@TableName("nso_project_member")
public class ProjectMember {

    // 成员记录编号
    @TableId
    private Long id;

    // 租户编号
    private Long tenantId;

    // 项目编号
    private Long projectId;

    // 用户编号
    private Long userId;

    // 成员姓名
    private String memberName;

    // 项目角色
    private String projectRole;

    // 部门编号
    private Long deptId;

    // 部门名称
    private String departmentName;

    // 成员状态
    private String status;

    // 数据版本
    @Version
    private Integer version;

    // 删除标志
    @TableLogic
    private Integer deleted;
}
