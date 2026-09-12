package com.nso.business.project.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

// 项目需求实体。
@Data
@TableName("nso_project_requirement")
public class ProjectRequirement {

    // 需求编号
    @TableId
    private Long id;

    // 租户编号
    private Long tenantId;

    // 项目编号
    private Long projectId;

    // 需求类别
    private String category;

    // 需求内容
    private String content;

    // 确认状态
    private String confirmStatus;

    // 最近变更原因
    private String lastReason;

    // 数据版本
    @Version
    private Integer version;

    // 删除标志
    @TableLogic
    private Integer deleted;
}
