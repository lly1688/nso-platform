package com.nso.web.controller.business;

import com.nso.shared.core.domain.AjaxResult;
import com.nso.business.core.NsoDtos.*;
import com.nso.business.document.service.IDocumentService;
import com.nso.business.support.IFlowCodeService;
import com.nso.business.task.service.ITaskService;
import com.nso.web.controller.common.WebFilePayloads;
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

// 文档管理接口 负责技术文档和版本管理
public class DocumentController {

    // 文档服务
    private final IDocumentService documentService;
    // 任务服务
    private final ITaskService taskService;
    // 流程编码服务
    private final IFlowCodeService flowCodes;

    public DocumentController(IDocumentService documentService, ITaskService taskService, IFlowCodeService flowCodes) {
        this.documentService = documentService;
        this.taskService = taskService;
        this.flowCodes = flowCodes;
    }

    // 查询项目文档版本。
    @GetMapping("/documents")
    @PreAuthorize("hasAnyAuthority('nso:document:view', 'document:view')")
    public AjaxResult<?> documents(@RequestParam(required = false) Long projectId,
                                  @RequestParam(required = false) Integer pageNo,
                                  @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(documentService.documents(projectId, new PageQuery(pageNo, pageSize)));
    }

    // 创建文档版本。
    @PostMapping("/documents/{projectId}/versions")
    @PreAuthorize("hasAnyAuthority('nso:document:upload', 'document:upload')")
    public AjaxResult<?> createDocumentVersion(@PathVariable Long projectId, @RequestBody DocumentVersionRequest request) {
        return AjaxResult.success(documentService.createVersion(projectId, request));
    }

    // 发布文档版本。
    @PostMapping("/document-versions/{id}/publish")
    @PreAuthorize("hasAnyAuthority('nso:document:publish', 'document:publish')")
    public AjaxResult<?> publishDocumentVersion(@PathVariable Long id) {
        return AjaxResult.success(documentService.publishVersion(id));
    }

    // 查询文档版本详情。
    @GetMapping("/document-versions/{id}")
    @PreAuthorize("hasAnyAuthority('nso:document:view', 'document:view')")
    public AjaxResult<?> documentVersion(@PathVariable Long id) {
        return AjaxResult.success(documentService.getVersion(id));
    }

    // 下载文档版本文件。
    @GetMapping("/document-versions/{id}/download")
    @PreAuthorize("hasAnyAuthority('nso:document:view', 'document:view')")
    public ResponseEntity<Resource> downloadDocumentVersion(@PathVariable Long id) {
        return WebFilePayloads.attachment(documentService.downloadVersion(id));
    }

    @GetMapping("/document-versions/{id}/inline")
    @PreAuthorize("hasAnyAuthority('nso:document:view', 'document:view')")
    public ResponseEntity<Resource> inlineDocumentVersion(@PathVariable Long id) {
        return WebFilePayloads.inline(documentService.downloadVersion(id));
    }

    // 查询项目物料清单。
    @GetMapping("/boms")
    @PreAuthorize("hasAnyAuthority('nso:document:view', 'document:view')")
    public AjaxResult<?> boms(@RequestParam(required = false) Long projectId,
                              @RequestParam(required = false) Integer pageNo,
                              @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(documentService.boms(projectId, new PageQuery(pageNo, pageSize)));
    }

    // 创建物料清单。
    @PostMapping("/boms")
    @PreAuthorize("hasAnyAuthority('nso:bom:manage', 'bom:manage')")
    public AjaxResult<?> createBom(@RequestBody BomRequest request) {
        return AjaxResult.success(documentService.createBom(request));
    }

    // 查询项目工艺路线。
    @GetMapping("/process-routes")
    @PreAuthorize("hasAnyAuthority('nso:document:view', 'document:view')")
    public AjaxResult<?> processRoutes(@RequestParam(required = false) Long projectId,
                                       @RequestParam(required = false) Integer pageNo,
                                       @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(documentService.processRoutes(projectId, new PageQuery(pageNo, pageSize)));
    }

    // 创建工艺路线。
    @PostMapping("/process-routes")
    @PreAuthorize("hasAnyAuthority('nso:process:manage', 'process:manage')")
    public AjaxResult<?> createProcessRoute(@RequestBody ProcessRouteRequest request) {
        return AjaxResult.success(documentService.createProcessRoute(request));
    }

    // 查询项目检验规范。
    @GetMapping("/inspection-specs")
    @PreAuthorize("hasAnyAuthority('nso:document:view', 'document:view')")
    public AjaxResult<?> inspectionSpecs(@RequestParam(required = false) Long projectId,
                                         @RequestParam(required = false) Integer pageNo,
                                         @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(documentService.inspectionSpecs(projectId, new PageQuery(pageNo, pageSize)));
    }

    // 创建检验规范。
    @PostMapping("/inspection-specs")
    @PreAuthorize("hasAnyAuthority('nso:inspection:manage', 'inspection:manage')")
    public AjaxResult<?> createInspectionSpec(@RequestBody InspectionSpecRequest request) {
        return AjaxResult.success(documentService.createInspectionSpec(request));
    }

    // 发布项目技术包。
    @PostMapping("/projects/{id}/technical-package/publish")
    @PreAuthorize("hasAnyAuthority('nso:document:publish', 'document:publish')")
    public AjaxResult<?> publishTechnicalPackage(@PathVariable Long id) {
        return AjaxResult.success(documentService.publishTechnicalPackage(id));
    }

    // 同步技术包执行任务。
    @PostMapping("/projects/{id}/technical-package/sync-tasks")
    @PreAuthorize("hasAnyAuthority('nso:document:sync', 'document:sync')")
    public AjaxResult<?> syncTechnicalPackageTasks(@PathVariable Long id) {
        return AjaxResult.success(taskService.syncTechnicalPackageTasks(id));
    }

    // 生成文档版本流转码。
    @PostMapping("/document-versions/{id}/qr")
    @PreAuthorize("hasAnyAuthority('nso:document:view', 'document:view')")
    public AjaxResult<?> documentQr(@PathVariable Long id) {
        return AjaxResult.success(flowCodes.ensureDocumentVersionCode(id));
    }
}
