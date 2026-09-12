package com.nso.business.sample.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

// 样品客户确认实体。
@Data
@TableName("nso_sample_confirm")
public class SampleConfirm {

    // 确认记录编号
    @TableId
    private Long id;

    // 租户编号
    private Long tenantId;

    // 样品编号
    private Long sampleId;

    // 确认令牌
    private String token;

    // 确认人
    private String confirmer;

    // 客户公司名称
    private String companyName;

    // 联系方式
    private String contact;

    // 确认结论
    private String conclusion;

    // 确认意见
    private String opinion;

    // 提交时间
    private LocalDateTime submittedAt;

    // 令牌过期时间
    private LocalDateTime expireAt;

    // 是否已使用
    private Integer usedFlag;

    // 最大使用次数
    private Integer maxUseCount;

    // 已使用次数
    private Integer usedCount;
}
