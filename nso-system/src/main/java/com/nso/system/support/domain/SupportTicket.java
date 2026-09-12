package com.nso.system.support.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

// 支持工单对象，对应表 {@code nso_support_ticket}。
@Data
@TableName("nso_support_ticket")

public class SupportTicket {
    @TableId
    // 主键 ID。
    private Long id;
    // 租户 ID。
    private Long tenantId;
    // 工单编号。
    private String ticketNo;
    private Long requesterUserId;
    private String requesterUsername;
    // 联系人姓名。
    private String contactName;
    private String contactValue;
    private String source;
    // 需求分类。
    private String category;
    // 优先级。
    private String priority;
    private String pageContext;
    private String description;
    // 工单状态。
    private String status;
    // 处理人 ID。
    private Long handlerId;
    private String handlingNote;
    // 解决时间。
    private LocalDateTime resolvedAt;
    @Version
    // 乐观锁版本号。
    private Integer version;
    // 创建时间
    private LocalDateTime createdAt;
    // 更新时间。
    private LocalDateTime updatedAt;
}
