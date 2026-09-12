package com.nso.business.risk.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

// 项目风险实体。
@Data
@TableName("nso_risk")
public class Risk {

    // 风险编号
    @TableId
    private Long id;

    // 租户编号
    private Long tenantId;

    // 项目编号
    private Long projectId;

    // 风险单号
    private String riskNo;

    // 风险等级
    private String level;

    // 风险分值
    private Integer score;

    // 风险原因
    private String reasons;

    // 处置建议
    private String suggestion;

    // 风险状态
    private String status;

    // 命中规则编码
    private String ruleCode;

    // 规则版本
    private String ruleVersion;

    // 计算批次键
    private String batchKey;

    // 计算输入快照
    private String inputSnapshot;

    // 人工覆盖原因
    private String overrideReason;

    // 人工覆盖等级
    private String overrideLevel;

    // 覆盖失效时间
    private LocalDateTime overrideExpiresAt;

    // 风险解除时间
    private LocalDateTime resolvedAt;

    // 风险负责人编号
    private Long ownerId;

    // 计算时间
    private LocalDateTime calculatedAt;

    // 数据版本
    @Version
    private Integer version;

    // 删除标志
    @TableLogic
    private Integer deleted;
}
