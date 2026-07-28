package com.nso.business.project.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

@Data
@TableName("nso_project_requirement")
public class ProjectRequirement {
    @TableId private Long id;
    private Long tenantId;
    private Long projectId;
    private String category;
    private String content;
    private String confirmStatus;
    private String lastReason;
    @Version private Integer version;
    @TableLogic private Integer deleted;
}
