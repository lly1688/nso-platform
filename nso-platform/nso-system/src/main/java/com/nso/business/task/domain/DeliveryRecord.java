package com.nso.business.task.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

// 交付记录实体。
@Data
@TableName("nso_delivery_record")
public class DeliveryRecord {

    // 交付记录编号
    @TableId
    private Long id;

    // 租户编号
    private Long tenantId;

    // 项目编号
    private Long projectId;

    // 交付数量
    private Integer quantity;

    // 物流单号
    private String logisticsNo;

    // 收货人
    private String receiver;

    // 客户反馈
    private String customerFeedback;

    // 交付状态
    private String status;

    // 发货时间
    private LocalDateTime shippedAt;
}
