package com.nso.business.document.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

// 工艺步骤实体。
@Data
@TableName("nso_process_step")
public class ProcessStep {

    // 工艺步骤编号
    @TableId
    private Long id;

    // 租户编号
    private Long tenantId;

    // 工艺路线编号
    private Long routeId;

    // 步骤序号
    private Integer stepNo;

    // 步骤名称
    private String stepName;

    // 作业指导
    private String workInstruction;

    // 设备名称
    private String equipmentName;

    // 标准工时
    private BigDecimal standardHours;

    // 是否外协
    private Integer outsourceFlag;

    // 是否为检验点
    private Integer inspectionPoint;
}
