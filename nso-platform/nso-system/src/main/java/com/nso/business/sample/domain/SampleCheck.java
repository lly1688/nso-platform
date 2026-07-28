package com.nso.business.sample.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data @TableName("nso_sample_check")
public class SampleCheck {
    @TableId private Long id; private Long tenantId; private Long sampleId; private String checkItem; private String measuredValue;
    private String result; private String issueSummary; private String correctiveAction; private String checkerName; private LocalDateTime checkedAt;
}
