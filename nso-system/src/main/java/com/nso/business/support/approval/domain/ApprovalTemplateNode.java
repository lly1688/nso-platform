package com.nso.business.support.approval.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Data;

// 审批模板节点对象，对应表 {@code nso_approval_template_node}。
@Data
@TableName("nso_approval_template_node")

public class ApprovalTemplateNode {
    @TableId
    // 主键 ID。
    private Long id;
    // 租户 ID。
    private Long tenantId;
    // 模板ID
    private Long templateId;
    // 节点顺序
    private Integer nodeOrder;
    private String nodeCode;
    // 节点名称。
    private String nodeName;
    // 职责编码。
    private String responsibilityCode;
    private Integer slaMinutes;
    private String escalationRole;
    // 创建时间
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
