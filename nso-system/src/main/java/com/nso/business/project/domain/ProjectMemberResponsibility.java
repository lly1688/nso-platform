package com.nso.business.project.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

// 项目成员职责对象，对应表 {@code nso_project_member_responsibility}。
@Data
@TableName("nso_project_member_responsibility")

public class ProjectMemberResponsibility {
    @TableId
    // 主键 ID。
    private Long id;
    // 租户 ID。
    private Long tenantId;
    // 项目成员ID
    private Long projectMemberId;
    // 所属项目ID
    private Long projectId;
    // 用户 ID。
    private Long userId;
    // 职责编码。
    private String responsibilityCode;
    // 是否主要职责（0 否，1 是）。
    private Integer primaryFlag;
    // 唯一责任人槽位，例如 {@code TECH_OWNER}。
    private String ownerSlot;
    // 状态。
    private String status;
    @Version
    // 乐观锁版本号。
    private Integer version;
    @TableLogic
    // 删除标志（0 未删除，1 已删除）。
    private Integer deleted;
}
