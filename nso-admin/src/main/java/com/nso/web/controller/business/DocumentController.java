package com.nso.web.controller.business;

import com.nso.common.core.domain.AjaxResult;
import com.nso.business.core.NsoDtos.*;
import com.nso.business.document.service.IDocumentService;
import com.nso.business.support.IFlowCodeService;
import com.nso.business.task.service.ITaskService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class DocumentController {

    private final IDocumentService documentService;
    private final ITaskService taskService;
    private final IFlowCodeService flowCodes;

    public DocumentController(IDocumentService documentService, ITaskService taskService, IFlowCodeService flowCodes) {
        this.documentService = documentService;
        this.taskService = taskService;
        this.flowCodes = flowCodes;
    }

    @GetMapping("/documents")
    @PreAuthorize("hasAuthority('document:view')")
    public AjaxResult<?> documents(@RequestParam(required = false) Long projectId) {
        return AjaxResult.success(documentService.documents(projectId));
    }

    @PostMapping("/documents/{projectId}/versions")
    @PreAuthorize("hasAuthority('document:upload')")
    public AjaxResult<?> createDocumentVersion(@PathVariable Long projectId, @RequestBody DocumentVersionRequest request) {
        return AjaxResult.success(documentService.createVersion(projectId, request));
    }

    @PostMapping("/document-versions/{id}/publish")
    @PreAuthorize("hasAuthority('document:publish')")
    public AjaxResult<?> publishDocumentVersion(@PathVariable Long id) {
        return AjaxResult.success(documentService.publishVersion(id));
    }

    @GetMapping("/document-versions/{id}")
    @PreAuthorize("hasAuthority('document:view')")
    public AjaxResult<?> documentVersion(@PathVariable Long id) {
        return AjaxResult.success(documentService.getVersion(id));
    }

    @GetMapping("/document-versions/{id}/download")
    @PreAuthorize("hasAuthority('document:view')")
    public ResponseEntity<Resource> downloadDocumentVersion(@PathVariable Long id) {
        DocumentVersionDto version = documentService.getVersion(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + version.fileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(documentService.downloadVersion(id));
    }

    @GetMapping("/boms")
    @PreAuthorize("hasAuthority('document:view')")
    public AjaxResult<?> boms(@RequestParam(required = false) Long projectId) {
        return AjaxResult.success(documentService.boms(projectId));
    }

    @PostMapping("/boms")
    @PreAuthorize("hasAuthority('bom:manage')")
    public AjaxResult<?> createBom(@RequestBody BomRequest request) {
        return AjaxResult.success(documentService.createBom(request));
    }

    @GetMapping("/process-routes")
    @PreAuthorize("hasAuthority('document:view')")
    public AjaxResult<?> processRoutes(@RequestParam(required = false) Long projectId) {
        return AjaxResult.success(documentService.processRoutes(projectId));
    }

    @PostMapping("/process-routes")
    @PreAuthorize("hasAuthority('process:manage')")
    public AjaxResult<?> createProcessRoute(@RequestBody ProcessRouteRequest request) {
        return AjaxResult.success(documentService.createProcessRoute(request));
    }

    @GetMapping("/inspection-specs")
    @PreAuthorize("hasAuthority('document:view')")
    public AjaxResult<?> inspectionSpecs(@RequestParam(required = false) Long projectId) {
        return AjaxResult.success(documentService.inspectionSpecs(projectId));
    }

    @PostMapping("/inspection-specs")
    @PreAuthorize("hasAuthority('inspection:manage')")
    public AjaxResult<?> createInspectionSpec(@RequestBody InspectionSpecRequest request) {
        return AjaxResult.success(documentService.createInspectionSpec(request));
    }

    @PostMapping("/projects/{id}/technical-package/publish")
    @PreAuthorize("hasAuthority('document:publish')")
    public AjaxResult<?> publishTechnicalPackage(@PathVariable Long id) {
        return AjaxResult.success(documentService.publishTechnicalPackage(id));
    }

    @PostMapping("/projects/{id}/technical-package/sync-tasks")
    @PreAuthorize("hasAuthority('document:sync')")
    public AjaxResult<?> syncTechnicalPackageTasks(@PathVariable Long id) {
        return AjaxResult.success(taskService.syncTechnicalPackageTasks(id));
    }

    @PostMapping("/document-versions/{id}/qr")
    @PreAuthorize("hasAuthority('document:view')")
    public AjaxResult<?> documentQr(@PathVariable Long id) {
        return AjaxResult.success(flowCodes.ensureDocumentVersionCode(id));
    }
}
