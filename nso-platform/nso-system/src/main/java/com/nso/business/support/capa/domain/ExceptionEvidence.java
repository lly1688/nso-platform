package com.nso.business.support.capa.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

// 异常证据对象，对应表 {@code nso_exception_evidence}。
@Data
@TableName("nso_exception_evidence")

public class ExceptionEvidence {
    @TableId
    // 主键 ID。
    private Long id;
    // 租户 ID。
    private Long tenantId;
    // 异常单编号。
    private Long exceptionCaseId;
    // 证据类型。
    private String evidenceType;
    // 证据引用。
    private String evidenceRef;
    // 证据摘要。
    private String summary;
    // 提交人用户编号。
    private Long submittedBy;
    // 创建时间。
    private LocalDateTime createdAt;
    @TableLogic
    // 删除标志（0 未删除，1 已删除）。
    private Integer deleted;
}
