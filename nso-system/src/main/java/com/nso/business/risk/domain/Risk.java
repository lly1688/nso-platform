package com.nso.business.risk.domain;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.time.LocalDateTime;
@Data @TableName("nso_risk") public class Risk { @TableId private Long id; private Long tenantId; private Long projectId; private String level; private Integer score; private String reasons; private String suggestion; private String status; private String ruleCode; private String ruleVersion; private Long ownerId; private LocalDateTime calculatedAt; @Version private Integer version; @TableLogic private Integer deleted; }
