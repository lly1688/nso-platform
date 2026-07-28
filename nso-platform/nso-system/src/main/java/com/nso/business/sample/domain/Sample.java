package com.nso.business.sample.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import java.time.LocalDate;

@Data @TableName("nso_sample")
public class Sample {
    @TableId private Long id; private Long tenantId; private Long projectId; private String sampleNo; private String purpose;
    private Integer quantity; private LocalDate planFinishDate; private String referencedVersion; private Long docVersionId;
    private String status; private String confirmConclusion; private String responsibleName; private String issueSummary;
    private Long createdBy; private Long qualityConfirmedBy; private Long proxyConfirmedBy; private Integer proxyConfirmation;
    @Version private Integer version; @TableLogic private Integer deleted;
}
