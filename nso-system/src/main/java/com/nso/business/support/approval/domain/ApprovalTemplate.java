package com.nso.business.support.approval.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Data;

// 审批模板对象，对应表 {@code nso_approval_template}。
@Data
@TableName("nso_approval_template")

public class ApprovalTemplate {
    @TableId
    // 主键 ID。
    private Long id;
    // 租户 ID。
    private Long tenantId;
    // 模板编码
    private String templateCode;
    // 业务类型。
    private String businessType;
    private Integer templateVersion;
    // 模板名称
    private String templateName;
    private String approvalMode;
    private Integer slaMinutes;
    // 模板状态。
    private String status;
    // 创建人ID
    private Long createdBy;
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
