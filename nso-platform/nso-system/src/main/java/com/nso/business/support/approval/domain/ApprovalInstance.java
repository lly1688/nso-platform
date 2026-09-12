package com.nso.business.support.approval.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Data;

// 审批实例对象，对应表 {@code nso_approval_instance}。
@Data
@TableName("nso_approval_instance")

public class ApprovalInstance {
    @TableId
    // 主键 ID。
    private Long id;
    // 租户 ID。
    private Long tenantId;
    // 业务类型。
    private String businessType;
    // 业务 ID。
    private Long businessId;
    // 所属项目编号。
    private Long projectId;
    // 审批模板编号。
    private Long templateId;
    // 审批模板编码。
    private String templateCode;
    // 审批模板版本号。
    private Integer templateVersion;
    // 审批模式。
    private String approvalMode;
    // 审批状态。
    private String status;
    // 当前审批节点顺序。
    private Integer currentNodeOrder;
    // 审批开始时间。
    private LocalDateTime startedAt;
    // 审批完成时间。
    private LocalDateTime finishedAt;
    // 升级处理时间。
    private LocalDateTime escalatedAt;
    // 创建时间。
    private LocalDateTime createdAt;
    // 更新时间。
    private LocalDateTime updatedAt;
    @Version
    // 乐观锁版本号。
    private Integer version;
    @TableLogic
    // 删除标志（0 未删除，1 已删除）。
    private Integer deleted;
}
