package com.nso.business.task.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 项目任务实体。
@Data
@TableName("nso_task")
public class Task {

    // 任务编号
    @TableId
    private Long id;

    // 租户编号
    private Long tenantId;

    // 项目编号
    private Long projectId;

    // 纠正预防案例编号
    private Long capaCaseId;

    // 纠正预防任务幂等键
    private String capaIdempotencyKey;

    // 任务单号
    private String taskNo;

    // 任务类型
    private String taskType;

    // 任务标题
    private String title;

    // 引用版本号
    private String referencedVersion;

    // 引用文档版本编号
    private Long referencedDocVersionId;

    // 任务状态
    private String status;

    // 执行人编号
    private Long assigneeId;

    // 责任人姓名
    private String responsibleName;

    // 计划开始日期
    private LocalDate planStart;

    // 计划完成日期
    private LocalDate planFinish;

    // 实际开始时间
    private LocalDateTime actualStart;

    // 实际完成时间
    private LocalDateTime actualFinish;

    // 阻塞原因
    private String blockReason;

    // 数据版本
    @Version
    private Integer version;

    // 删除标志
    @TableLogic
    private Integer deleted;
}
