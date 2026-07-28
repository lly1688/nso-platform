package com.nso.business.task.domain;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.time.LocalDateTime;
@Data @TableName("nso_delivery_record") public class DeliveryRecord { @TableId private Long id; private Long tenantId; private Long projectId; private Integer quantity; private String logisticsNo; private String receiver; private String customerFeedback; private String status; private LocalDateTime shippedAt; }
