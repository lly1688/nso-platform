package com.nso.business.risk.service;

import com.nso.business.core.NsoDtos.PageResult;
import com.nso.business.core.NsoDtos.RiskActionCloseRequest;
import com.nso.business.core.NsoDtos.RiskActionDto;
import com.nso.business.core.NsoDtos.RiskActionRequest;
import com.nso.business.core.NsoDtos.RiskDto;

public interface IRiskService {
    PageResult<RiskDto> list(Long projectId);
    RiskDto calculate(Long projectId);
    PageResult<RiskActionDto> actions(Long riskId);
    RiskActionDto createAction(Long riskId, RiskActionRequest request);
    RiskActionDto closeAction(Long actionId, RiskActionCloseRequest request);
}
