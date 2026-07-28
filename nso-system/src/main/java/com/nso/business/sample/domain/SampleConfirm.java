package com.nso.business.sample.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("nso_sample_confirm")
public class SampleConfirm {
    @TableId private Long id; private Long tenantId; private Long sampleId; private String token; private String confirmer;
    private String companyName; private String contact; private String conclusion; private String opinion; private LocalDateTime submittedAt;
    private LocalDateTime expireAt; private Integer usedFlag; private Integer maxUseCount; private Integer usedCount;
}
