package com.nso.business.task.domain;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.time.*;
@Data @TableName("nso_task") public class Task {
 @TableId private Long id; private Long tenantId; private Long projectId; private String taskNo; private String taskType; private String title; private String referencedVersion; private Long referencedDocVersionId; private String status; private Long assigneeId; private String responsibleName; private LocalDate planStart; private LocalDate planFinish; private LocalDateTime actualStart; private LocalDateTime actualFinish; private String blockReason; @Version private Integer version; @TableLogic private Integer deleted;
}
