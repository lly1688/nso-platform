package com.nso.business.document.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

// 物料清单明细实体。
@Data
@TableName("nso_bom_item")
public class BomItem {

    // 明细编号
    @TableId
    private Long id;

    // 租户编号
    private Long tenantId;

    // 物料清单编号
    private Long bomId;

    // 物料编码
    private String materialCode;

    // 物料名称
    private String materialName;

    // 规格型号
    private String specification;

    // 数量
    private BigDecimal quantity;

    // 单位
    private String unit;

    // 来源类型
    private String sourceType;

    // 替代料编码
    private String substituteCode;
}
