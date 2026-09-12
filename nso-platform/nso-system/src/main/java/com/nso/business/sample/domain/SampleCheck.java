package com.nso.business.sample.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

// 样品检验记录实体。
@Data
@TableName("nso_sample_check")
public class SampleCheck {

    // 检验记录编号
    @TableId
    private Long id;

    // 租户编号
    private Long tenantId;

    // 样品编号
    private Long sampleId;

    // 检验项目
    private String checkItem;

    // 实测值
    private String measuredValue;

    // 检验结果
    private String result;

    // 问题摘要
    private String issueSummary;

    // 纠正措施
    private String correctiveAction;

    // 检验人
    private String checkerName;

    // 检验时间
    private LocalDateTime checkedAt;
}
