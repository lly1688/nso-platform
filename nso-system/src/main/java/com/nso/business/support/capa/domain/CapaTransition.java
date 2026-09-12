package com.nso.business.support.capa.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

// CAPA 状态流转记录对象，对应表 {@code nso_capa_transition}。
@Data
@TableName("nso_capa_transition")

public class CapaTransition {
    @TableId
    // 主键 ID。
    private Long id;
    // 租户 ID。
    private Long tenantId;
    private Long exceptionCaseId;
    // 源状态。
    private String fromStatus;
    // 目标状态。
    private String toStatus;
    // 幂等键。
    private String idempotencyKey;
    private String comment;
    // 操作人 ID。
    private Long operatorId;
    // 创建时间
    private LocalDateTime createdAt;
}
