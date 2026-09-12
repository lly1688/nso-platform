package com.nso.business.sample.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDate;

// 样品单实体。
@Data
@TableName("nso_sample")
public class Sample {

    // 样品编号
    @TableId
    private Long id;

    // 租户编号
    private Long tenantId;

    // 项目编号
    private Long projectId;

    // 样品单号
    private String sampleNo;

    // 打样目的
    private String purpose;

    // 样品数量
    private Integer quantity;

    // 计划完成日期
    private LocalDate planFinishDate;

    // 引用版本号
    private String referencedVersion;

    // 文档版本编号
    private Long docVersionId;

    // 样品状态
    private String status;

    // 确认结论
    private String confirmConclusion;

    // 责任人姓名
    private String responsibleName;

    // 问题摘要
    private String issueSummary;

    // 创建人编号
    private Long createdBy;

    // 质量确认人编号
    private Long qualityConfirmedBy;

    // 代确认人编号
    private Long proxyConfirmedBy;

    // 是否代确认
    private Integer proxyConfirmation;

    // 数据版本
    @Version
    private Integer version;

    // 删除标志
    @TableLogic
    private Integer deleted;
}
