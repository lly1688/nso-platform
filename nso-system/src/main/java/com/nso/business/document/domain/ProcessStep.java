package com.nso.business.document.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;

@Data @TableName("nso_process_step")
public class ProcessStep {
    @TableId private Long id; private Long tenantId; private Long routeId; private Integer stepNo; private String stepName;
    private String workInstruction; private String equipmentName; private BigDecimal standardHours; private Integer outsourceFlag; private Integer inspectionPoint;
}
