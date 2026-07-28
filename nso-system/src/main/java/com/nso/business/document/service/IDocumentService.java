package com.nso.business.document.service;

import com.nso.business.core.NsoDtos.*;
import java.util.Map;
import org.springframework.core.io.Resource;

public interface IDocumentService {
    PageResult<DocumentVersionDto> documents(Long projectId);
    DocumentVersionDto createVersion(Long projectId, DocumentVersionRequest request);
    DocumentVersionDto publishVersion(Long versionId);
    DocumentVersionDto getVersion(Long versionId);
    Resource downloadVersion(Long versionId);
    PageResult<BomDto> boms(Long projectId);
    BomDto createBom(BomRequest request);
    PageResult<ProcessRouteDto> processRoutes(Long projectId);
    ProcessRouteDto createProcessRoute(ProcessRouteRequest request);
    PageResult<InspectionSpecDto> inspectionSpecs(Long projectId);
    InspectionSpecDto createInspectionSpec(InspectionSpecRequest request);
    Map<String, Object> publishTechnicalPackage(Long projectId);
}
