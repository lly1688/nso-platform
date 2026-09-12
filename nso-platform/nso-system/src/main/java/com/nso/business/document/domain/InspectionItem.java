package com.nso.business.document.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

// 检验项目实体。
@Data
@TableName("nso_inspection_item")
public class InspectionItem {

    // 检验项目编号
    @TableId
    private Long id;

    // 租户编号
    private Long tenantId;

    // 检验规范编号
    private Long specId;

    // 检验项目名称
    private String itemName;

    // 标准值
    private String standardValue;

    // 抽样规则
    private String samplingRule;

    // 附件文件编号
    private Long attachmentFileId;
}
