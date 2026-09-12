package com.nso.business.project.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 非标订单项目实体。
@Data
@TableName("nso_project")
public class Project {

    // 项目编号
    @TableId
    private Long id;

    // 租户编号
    private Long tenantId;

    // 项目单号
    private String projectNo;

    // 客户编号
    private Long customerId;

    // 客户名称
    private String customerName;

    // 产品名称
    private String productName;

    // 项目负责人用户编号
    private Long ownerUserId;

    // 订单数量
    private Integer quantity;

    // 目标交付日期
    private LocalDate targetDate;

    // 计划开始日期
    private LocalDate planStartDate;

    // 项目负责人姓名
    private String ownerName;

    // 项目状态
    private String status;

    // 当前阶段
    private String stage;

    // 优先级
    private String priority;

    // 风险等级
    private String riskLevel;

    // 风险分值
    private Integer riskScore;

    // 样品状态
    private String sampleStatus;

    // 归档时间
    private LocalDateTime archivedAt;

    // 归档人编号
    private Long archivedBy;

    // 归档原因
    private String archiveReason;

    // 暂停原因
    private String suspendedReason;

    // 当前技术文档版本编号
    private Long currentDocVersionId;

    // 数据版本
    @Version
    private Integer version;

    // 删除标志
    @TableLogic
    private Integer deleted;
}
