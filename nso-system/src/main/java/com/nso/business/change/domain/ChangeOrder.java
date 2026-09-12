package com.nso.business.change.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

// 变更单实体。
@Data
@TableName("nso_change_order")
public class ChangeOrder {

    // 变更单编号
    @TableId
    private Long id;

    // 租户编号
    private Long tenantId;

    // 项目编号
    private Long projectId;

    // 父变更单编号
    private Long parentChangeId;

    // 申请人编号
    private Long applicantUserId;

    // 变更单号
    private String changeNo;

    // 变更类型
    private String changeType;

    // 紧急程度
    private String urgency;

    // 变更前内容
    private String beforeContent;

    // 变更后内容
    private String afterContent;

    // 变更原因
    private String reason;

    // 变更状态
    private String status;

    // 来源阶段
    private String sourceStage;

    // 当前审批节点
    private String approvalNode;

    // 撤回时间
    private LocalDateTime revokedAt;

    // 撤回原因
    private String revokeReason;

    // 验证时间
    private LocalDateTime verifiedAt;

    // 验证人编号
    private Long verifiedBy;

    // 影响工期天数
    private Integer delayDays;

    // 返工数量
    private Integer reworkQty;

    // 数据版本
    @Version
    private Integer version;

    // 删除标志
    @TableLogic
    private Integer deleted;
}
