package com.nso.business.support.approval.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Data;

// 审批任务对象，对应表 {@code nso_approval_task}。
@Data
@TableName("nso_approval_task")

public class ApprovalTask {
    @TableId
    // 主键 ID。
    private Long id;
    // 租户 ID。
    private Long tenantId;
    // 审批实例编号。
    private Long instanceId;
    // 模板节点编号。
    private Long templateNodeId;
    // 节点顺序。
    private Integer nodeOrder;
    // 节点标识。
    private String nodeCode;
    // 节点名称。
    private String nodeName;
    // 职责编码。
    private String responsibilityCode;
    // 审批人用户编号。
    private Long assigneeUserId;
    // 审批人名称。
    private String assigneeName;
    // 审批结论。
    private String decision;
    // 审批意见。
    private String opinion;
    // 实际审批人编号。
    private Long decidedBy;
    // 实际审批人名称。
    private String decidedByName;
    // 审批时间。
    private LocalDateTime decidedAt;
    // 审批截止时间。
    private LocalDateTime dueAt;
    // 升级处理时间。
    private LocalDateTime escalatedAt;
    // 审批幂等键。
    private String decisionIdempotencyKey;
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
