package com.nso.business.document.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;

@Data @TableName("nso_bom_item")
public class BomItem {
    @TableId private Long id; private Long tenantId; private Long bomId; private String materialCode; private String materialName;
    private String specification; private BigDecimal quantity; private String unit; private String sourceType; private String substituteCode;
}
