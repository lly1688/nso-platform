package com.nso.business.support.action.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Data;

// 待办行动项对象，对应表 {@code nso_action_item}。
@Data
@TableName("nso_action_item")

public class ActionItem {
    @TableId
    // 主键 ID。
    private Long id;
    // 租户 ID。
    private Long tenantId;
    // 来源类型。
    private String sourceType;
    // 来源业务编号。
    private Long sourceId;
    // 行动标识。
    private String actionCode;
    // 所属项目编号。
    private Long projectId;
    // 处理人用户编号。
    private Long assigneeUserId;
    // 处理人名称。
    private String assigneeName;
    // 行动标题。
    private String title;
    // 行动摘要。
    private String summary;
    // 优先级。
    private String priority;
    // 服务等级截止时间。
    private LocalDateTime slaDueAt;
    // 跳转路由。
    private String route;
    // 来源业务状态。
    private String sourceStatus;
    // 行动项状态。
    private String actionStatus;
    // 来源业务版本号。
    private Integer sourceVersion;
    // 创建时间。
    private LocalDateTime createdAt;
    // 更新时间。
    private LocalDateTime updatedAt;
    // 关闭时间。
    private LocalDateTime closedAt;
    @Version
    // 乐观锁版本号。
    private Integer version;
    @TableLogic
    // 删除标志（0 未删除，1 已删除）。
    private Integer deleted;
}
