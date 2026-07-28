package com.nso.business.document.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data @TableName("nso_inspection_item")
public class InspectionItem {
    @TableId private Long id; private Long tenantId; private Long specId; private String itemName; private String standardValue;
    private String samplingRule; private Long attachmentFileId;
}
