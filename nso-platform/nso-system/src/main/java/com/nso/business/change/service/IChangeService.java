package com.nso.business.change.service;
import com.nso.business.core.NsoDtos.*;
import java.util.List;
public interface IChangeService {
 PageResult<ChangeOrderDto> list(Long projectId); ChangeOrderDto get(Long changeId); ChangeOrderDto create(ChangeRequest request);
 List<ChangeImpactDto> analyze(Long changeId); List<ChangeImpactDto> impacts(Long changeId); ChangeOrderDto approve(Long changeId); ChangeOrderDto approve(Long changeId, ChangeApprovalRequest request);
 ChangeImpactDto feedbackImpact(Long impactId, ChangeFeedbackRequest request); ChangeOrderDto feedbackChange(Long changeId, ChangeFeedbackRequest request); ChangeOrderDto close(Long changeId);
}
