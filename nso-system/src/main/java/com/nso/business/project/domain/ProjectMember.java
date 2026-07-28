package com.nso.business.project.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

@Data
@TableName("nso_project_member")
public class ProjectMember {
    @TableId private Long id;
    private Long tenantId;
    private Long projectId;
    private Long userId;
    private String memberName;
    private String projectRole;
    private Long deptId;
    private String departmentName;
    private String status;
    @Version private Integer version;
    @TableLogic private Integer deleted;
}
