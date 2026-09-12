package com.nso.business.change.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;

// 变更影响项实体。
@Data
@TableName("nso_change_impact")
public class ChangeImpact {

    // 影响项编号
    @TableId
    private Long id;

    // 租户编号
    private Long tenantId;

    // 变更单编号
    private Long changeId;

    // 影响对象类型
    private String objectType;

    // 影响对象编号
    private Long objectId;

    // 影响对象版本
    private String objectVersion;

    // 影响对象名称
    private String objectName;

    // 责任部门
    private String departmentName;

    // 建议措施
    private String suggestedAction;

    // 处理状态
    private String status;

    // 反馈结果
    private String feedbackResult;

    // 反馈计划
    private String feedbackPlan;

    // 责任人
    private String responsibleName;

    // 影响工期天数
    private Integer delayDays;

    // 返工数量
    private BigDecimal reworkQty;

    // 是否已验证
    private Integer verifiedFlag;

    // 数据版本
    @Version
    private Integer version;
}
