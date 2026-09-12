package com.nso.system.support.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

// 工单附件对象，对应表 {@code nso_support_ticket_attachment}。
@Data
@TableName("nso_support_ticket_attachment")

public class SupportTicketAttachment {
    @TableId
    // 主键 ID。
    private Long id;
    // 租户 ID。
    private Long tenantId;
    // 工单 ID。
    private Long ticketId;
    // 文件对象 ID。
    private Long fileObjectId;
    // 创建人 ID。
    private Long createdBy;
    // 创建时间。
    private LocalDateTime createdAt;
}
