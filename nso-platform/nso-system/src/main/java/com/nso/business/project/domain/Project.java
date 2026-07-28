package com.nso.business.project.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("nso_project")
public class Project {
    @TableId private Long id;
    private Long tenantId;
    private String projectNo;
    private Long customerId;
    private String customerName;
    private String productName;
    private Long ownerUserId;
    private Integer quantity;
    private LocalDate targetDate;
    private LocalDate planStartDate;
    private String ownerName;
    private String status;
    private String stage;
    private String priority;
    private String riskLevel;
    private Integer riskScore;
    private String sampleStatus;
    private Long currentDocVersionId;
    @Version private Integer version;
    @TableLogic private Integer deleted;
}
