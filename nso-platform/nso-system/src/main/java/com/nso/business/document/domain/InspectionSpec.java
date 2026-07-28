package com.nso.business.document.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

@Data @TableName("nso_inspection_spec")
public class InspectionSpec {
    @TableId private Long id; private Long tenantId; private Long projectId; private String specNo; private String versionNo;
    private Long boundDocVersionId; private String status; @Version private Integer version; @TableLogic private Integer deleted;
}
