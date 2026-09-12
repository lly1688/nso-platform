package com.nso.business.support.capa.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import java.time.LocalDateTime;
import lombok.Data;

// 异常案例对象，对应表 {@code nso_exception_case}。
@Data
@TableName("nso_exception_case")

public class ExceptionCase {
    @TableId
    // 主键 ID。
    private Long id;
    // 租户 ID。
    private Long tenantId;
    // 异常单编号。
    private String caseNo;
    // 所属项目编号。
    private Long projectId;
    // 关联任务编号。
    private Long taskId;
    // 关联风险编号。
    private Long riskId;
    // 异常类型。
    private String exceptionType;
    // 异常摘要。
    private String summary;
    // 上报人名称。
    private String reporterName;
    // 负责人用户编号。
    private Long ownerUserId;
    // 负责人姓名。
    private String ownerName;
    // 处理截止时间。
    private LocalDateTime dueAt;
    // 状态。
    private String status;
    // 临时遏制方案。
    private String containmentPlan;
    // 根因分析。
    private String rootCause;
    // 纠正措施计划。
    private String correctivePlan;
    // 验证摘要。
    private String verificationSummary;
    // 关闭结论。
    private String closeConclusion;
    // 幂等键。
    private String idempotencyKey;
    // 创建人用户编号。
    private Long createdBy;
    // 创建时间。
    private LocalDateTime createdAt;
    // 更新时间。
    private LocalDateTime updatedAt;
    // 升级处理时间。
    private LocalDateTime escalatedAt;
    // 关闭时间。
    private LocalDateTime closedAt;
    @Version
    // 乐观锁版本号。
    private Integer version;
    @TableLogic
    // 删除标志（0 未删除，1 已删除）。
    private Integer deleted;
}
