package com.nso.business.risk.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Data;

// 风险处置行动实体。
@Data
@TableName("nso_risk_action")
public class RiskAction {

    // 行动编号
    @TableId
    private Long id;

    // 租户编号
    private Long tenantId;

    // 风险编号
    private Long riskId;

    // 处置计划
    private String actionPlan;

    // 责任人用户编号
    private Long responsibleUserId;

    // 责任人姓名
    private String responsibleName;

    // 计划完成时间
    private LocalDateTime planFinishTime;

    // 关闭说明
    private String closeSummary;

    // 行动状态
    private String status;

    // 幂等键
    private String idempotencyKey;

    // 关闭人编号
    private Long closedBy;

    // 创建时间
    private LocalDateTime createdAt;

    // 更新时间
    private LocalDateTime updatedAt;

    // 关闭时间
    private LocalDateTime closedAt;

    // 数据版本
    @Version
    private Integer version;

    // 删除标志
    @TableLogic
    private Integer deleted;
}
