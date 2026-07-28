package com.nso.business.change.domain;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data;
@Data @TableName("nso_change_order") public class ChangeOrder {
 @TableId private Long id; private Long tenantId; private Long projectId; private Long applicantUserId; private String changeNo; private String changeType; private String urgency; private String beforeContent; private String afterContent; private String reason; private String status; private String sourceStage; private String approvalNode; private Integer delayDays; private Integer reworkQty; @Version private Integer version; @TableLogic private Integer deleted;
}
