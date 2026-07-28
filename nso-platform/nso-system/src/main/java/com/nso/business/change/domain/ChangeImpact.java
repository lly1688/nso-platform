package com.nso.business.change.domain;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data;
@Data @TableName("nso_change_impact") public class ChangeImpact {
 @TableId private Long id; private Long tenantId; private Long changeId; private String objectType; private Long objectId; private String objectVersion; private String objectName; private String departmentName; private String suggestedAction; private String status; private String feedbackResult; private String feedbackPlan; private String responsibleName; private Integer delayDays; private java.math.BigDecimal reworkQty; private Integer verifiedFlag; @Version private Integer version;
}
