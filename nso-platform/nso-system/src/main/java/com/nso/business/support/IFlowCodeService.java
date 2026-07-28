package com.nso.business.support;

import com.nso.business.core.NsoDtos.QrCodeBindingDto;
import com.nso.business.core.NsoDtos.ScanDetailDto;
import java.util.List;

public interface IFlowCodeService {
    QrCodeBindingDto ensureProjectFlowCode(Long projectId);
    QrCodeBindingDto ensureDocumentVersionCode(Long documentVersionId);
    QrCodeBindingDto findProjectFlowCode(Long projectId);
    ScanDetailDto resolveScan(String rawCode, List<String> permissions);
}
