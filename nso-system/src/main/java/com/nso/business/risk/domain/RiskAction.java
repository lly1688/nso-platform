package com.nso.business.risk.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("nso_risk_action")
public class RiskAction {
    @TableId
    private Long id;
    private Long tenantId;
    private Long riskId;
    private String actionPlan;
    private Long responsibleUserId;
    private String responsibleName;
    private LocalDateTime planFinishTime;
    private String closeSummary;
    private String status;
    private String idempotencyKey;
    private Long closedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;
    @Version
    private Integer version;
    @TableLogic
    private Integer deleted;
}
