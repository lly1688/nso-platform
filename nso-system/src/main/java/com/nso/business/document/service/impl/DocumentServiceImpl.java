package com.nso.business.document.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nso.business.core.BusinessLockPort;
import com.nso.business.core.NsoDtos.*;
import com.nso.business.core.PageSupport;
import com.nso.business.core.TenantContext;
import com.nso.business.document.domain.*;
import com.nso.business.document.config.DocumentWorkflowProperties;
import com.nso.business.document.mapper.*;
import com.nso.business.file.service.IFileService;
import com.nso.business.file.domain.FileObject;
import com.nso.business.file.mapper.FileObjectMapper;
import com.nso.business.document.service.IDocumentService;
import com.nso.business.project.domain.Project;
import com.nso.business.project.mapper.ProjectMapper;
import com.nso.business.project.service.ProjectDataScope;
import com.nso.business.support.BusinessEventService;
import com.nso.shared.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 技术资料服务。
@Service

// 文档管理 服务层处理
public class DocumentServiceImpl implements IDocumentService {
    // 文档版本数据映射
    private final DocumentVersionMapper documentVersionMapper;
    // BOM数据映射
    private final BomMapper bomMapper;
    // BOMItem数据映射
    private final BomItemMapper bomItemMapper;
    // 工艺路线数据映射
    private final ProcessRouteMapper routeMapper;
    // 工艺Step数据映射
    private final ProcessStepMapper stepMapper;
    // 检验Spec数据映射
    private final InspectionSpecMapper inspectionSpecMapper;
    // 检验Item数据映射
    private final InspectionItemMapper inspectionItemMapper;
    // 文件对象数据映射
    private final FileObjectMapper fileObjectMapper;
    // 文件服务
    private final IFileService fileService;
    // 项目数据映射
    private final ProjectMapper projectMapper;
    // 业务锁端口
    private final BusinessLockPort businessLock;
    // 项目数据范围
    private final ProjectDataScope dataScope;
    // 业务事件服务
    private final BusinessEventService events;
    // 文档工作流配置
    private final DocumentWorkflowProperties workflowProperties;
    // JDBC模板
    private final JdbcTemplate jdbc;

    public DocumentServiceImpl(
            DocumentVersionMapper documentVersionMapper,
            BomMapper bomMapper,
            BomItemMapper bomItemMapper,
            ProcessRouteMapper routeMapper,
            ProcessStepMapper stepMapper,
            InspectionSpecMapper inspectionSpecMapper,
            InspectionItemMapper inspectionItemMapper,
            FileObjectMapper fileObjectMapper,
            IFileService fileService,
            ProjectMapper projectMapper,
            BusinessLockPort businessLock,
            ProjectDataScope dataScope,
            BusinessEventService events,
            DocumentWorkflowProperties workflowProperties,
            JdbcTemplate jdbc) {
        this.documentVersionMapper = documentVersionMapper;
        this.bomMapper = bomMapper;
        this.bomItemMapper = bomItemMapper;
        this.routeMapper = routeMapper;
        this.stepMapper = stepMapper;
        this.inspectionSpecMapper = inspectionSpecMapper;
        this.inspectionItemMapper = inspectionItemMapper;
        this.fileObjectMapper = fileObjectMapper;
        this.fileService = fileService;
        this.projectMapper = projectMapper;
        this.businessLock = businessLock;
        this.dataScope = dataScope;
        this.events = events;
        this.workflowProperties = workflowProperties;
        this.jdbc = jdbc;
    }

    // 查询项目技术版本。
    @Override
    public PageResult<DocumentVersionDto> documents(Long projectId) {
        List<Long> visibleIds = dataScope.visibleProjectIds();
        if (visibleIds != null && visibleIds.isEmpty()) {
            return new PageResult<>(List.of(), 0);
        }
        List<DocumentVersionDto> rows = documentVersionMapper.selectList(Wrappers.<DocumentVersion>lambdaQuery()
                        .eq(projectId != null, DocumentVersion::getProjectId, projectId)
                        .in(visibleIds != null, DocumentVersion::getProjectId, visibleIds == null ? List.of() : visibleIds).orderByDesc(DocumentVersion::getId))
                .stream().map(this::toDocumentDto).toList();
        return new PageResult<>(rows, rows.size());
    }

    // 分页查询项目文档版本。
    @Override
    public PageResult<DocumentVersionDto> documents(Long projectId, PageQuery pageQuery) {
        PageQuery page = PageSupport.normalize(pageQuery);
        List<Long> visibleIds = dataScope.visibleProjectIds();
        if (visibleIds != null && visibleIds.isEmpty()) return PageResult.empty(page);
        Page<DocumentVersion> entityPage = documentVersionMapper.selectPage(PageSupport.page(page),
                Wrappers.<DocumentVersion>lambdaQuery().eq(projectId != null, DocumentVersion::getProjectId, projectId)
                        .in(visibleIds != null, DocumentVersion::getProjectId,
                                visibleIds == null ? List.of() : visibleIds).orderByDesc(DocumentVersion::getId));
        return PageSupport.result(entityPage, page, this::toDocumentDto);
    }

    // 创建待发布的技术版本。
    @Override
    @Transactional
    public DocumentVersionDto createVersion(Long projectId, DocumentVersionRequest request) {
        requireProject(projectId);
        dataScope.requireProjectRole(projectId, "TECHNICAL");
        if (request == null || blank(request.fileName()) || blank(request.fileType()) || blank(request.versionNo())) {
            throw new BusinessException("文件名称、类型和版本号不能为空");
        }
        FileObject file = requireProjectFile(projectId, request.fileObjectId());
        if (documentVersionMapper.selectCount(Wrappers.<DocumentVersion>lambdaQuery().eq(DocumentVersion::getProjectId, projectId)
                .eq(DocumentVersion::getFileType, request.fileType()).eq(DocumentVersion::getVersionNo, request.versionNo())) > 0) {
            throw new BusinessException("同一项目内文件类型和版本号不能重复");
        }
        DocumentVersion version = new DocumentVersion();
        version.setTenantId(TenantContext.tenantId());
        version.setProjectId(projectId);
        version.setFileObjectId(file.getId());
        version.setFileName(request.fileName().trim());
        version.setFileType(request.fileType().trim());
        version.setVersionNo(request.versionNo().trim());
        version.setStatus("DRAFT");
        version.setEffectiveDate(request.effectiveDate());
        version.setChangeSummary(request.changeSummary());
        version.setCurrentVersion(0);
        version.setSha256(file.getSha256());
        version.setStoragePath(file.getStoragePath());
        version.setUploadedBy(TenantContext.userId());
        documentVersionMapper.insert(version);
        events.record(projectId, "DOCUMENT_VERSION", version.getId(), "DOCUMENT_VERSION_CREATED", null, "DRAFT", "已创建技术文件版本 " + version.getVersionNo());
        return toDocumentDto(version);
    }

    // 发布文档版本。
    @Override
    @Transactional
    public DocumentVersionDto publishVersion(Long versionId) {
        return businessLock.withLock("document:publish:" + versionId, () -> publishVersionUnderLock(versionId));
    }

    private DocumentVersionDto publishVersionUnderLock(Long versionId) {
        DocumentVersion version = requireDocument(versionId);
        dataScope.requireProjectRole(version.getProjectId(), "TECHNICAL");
        if (!"DRAFT".equals(version.getStatus()) && !"PUBLISHED".equals(version.getStatus())) {
            throw BusinessException.ruleBlock("DOCUMENT_STATUS", "当前文件版本不能发布", version.getStatus(), "DRAFT", "重新创建待发布版本");
        }
        if (!workflowProperties.isAllowSelfPublish() && version.getUploadedBy() != null && version.getUploadedBy().equals(TenantContext.userId())) {
            throw BusinessException.ruleBlock("DOCUMENT_DUTY_SEPARATION", "文件上传人与发布审批人不能为同一人", String.valueOf(TenantContext.userId()), "独立发布审批人", "由另一名技术负责人发布或开启流程参与");
        }
        documentVersionMapper.update(null, Wrappers.<DocumentVersion>lambdaUpdate().eq(DocumentVersion::getProjectId, version.getProjectId())
                .eq(DocumentVersion::getFileType, version.getFileType()).eq(DocumentVersion::getCurrentVersion, 1).set(DocumentVersion::getCurrentVersion, 0));
        version.setCurrentVersion(1);
        version.setStatus("PUBLISHED");
        version.setPublishedBy(TenantContext.userId());
        version.setPublishBy(TenantContext.username());
        if (documentVersionMapper.updateById(version) != 1) {
            throw BusinessException.ruleBlock("OPTIMISTIC_LOCK", "文件版本已被其他用户修改", "stale", "latest", "刷新后重试");
        }
        Project project = requireProject(version.getProjectId());
        project.setCurrentDocVersionId(version.getId());
        projectMapper.updateById(project);
        return toDocumentDto(version);
    }

    // 查询文档版本详情。
    @Override
    public DocumentVersionDto getVersion(Long versionId) {
        return toDocumentDto(requireDocument(versionId));
    }

    // 下载文档版本文件。
    @Override
    public com.nso.business.file.service.FileDownloadPayload downloadVersion(Long versionId) {
        DocumentVersion version = requireDocument(versionId);
        if (version.getFileObjectId() == null) throw new BusinessException("技术版本未绑定受控文件");
        return fileService.download(version.getFileObjectId());
    }

    // 查询项目物料清单。
    @Override
    public PageResult<BomDto> boms(Long projectId) {
        List<Long> visibleIds = dataScope.visibleProjectIds();
        if (visibleIds != null && visibleIds.isEmpty())
            return new PageResult<>(List.of(), 0);
        List<BomDto> rows = bomMapper.selectList(Wrappers.<Bom>lambdaQuery().eq(projectId != null, Bom::getProjectId, projectId)
                        .in(visibleIds != null, Bom::getProjectId, visibleIds == null ? List.of() : visibleIds).orderByDesc(Bom::getId))
                .stream().map(this::toBomDto).toList();
        return new PageResult<>(rows, rows.size());
    }

    // 分页查询项目物料清单。
    @Override
    public PageResult<BomDto> boms(Long projectId, PageQuery pageQuery) {
        PageQuery page = PageSupport.normalize(pageQuery);
        List<Long> visibleIds = dataScope.visibleProjectIds();
        if (visibleIds != null && visibleIds.isEmpty()) return PageResult.empty(page);
        Page<Bom> entityPage = bomMapper.selectPage(PageSupport.page(page),
                Wrappers.<Bom>lambdaQuery().eq(projectId != null, Bom::getProjectId, projectId)
                        .in(visibleIds != null, Bom::getProjectId, visibleIds == null ? List.of() : visibleIds)
                        .orderByDesc(Bom::getId));
        return PageSupport.result(entityPage, page, this::toBomDto);
    }

    // 创建物料清单。
    @Override
    @Transactional
    public BomDto createBom(BomRequest request) {
        if (request == null || request.projectId() == null || blank(request.bomNo()) || blank(request.versionNo())) throw new BusinessException("项目、BOM 编号和版本号不能为空");
        requireProject(request.projectId());
        dataScope.requireProjectRole(request.projectId(), "TECHNICAL");
        requireDocumentIfPresent(request.boundDocVersionId(), request.projectId());
        Bom bom = new Bom();
        bom.setTenantId(TenantContext.tenantId());
        bom.setProjectId(request.projectId());
        bom.setBomNo(request.bomNo());
        bom.setVersionNo(request.versionNo());
        bom.setBoundDocVersionId(request.boundDocVersionId());
        bom.setStatus("DRAFT");
        bomMapper.insert(bom);
        if (request.items() != null) for (BomItemRequest item : request.items()) {
            if (blank(item.materialCode()) || blank(item.materialName()) || item.quantity() == null || item.quantity() <= 0 || blank(item.unit())) throw new BusinessException("BOM 明细物料、数量和单位不能为空");
            BomItem row = new BomItem();
            row.setTenantId(TenantContext.tenantId());
            row.setBomId(bom.getId());
            row.setMaterialCode(item.materialCode());
            row.setMaterialName(item.materialName());
            row.setSpecification(item.specification());
            row.setQuantity(BigDecimal.valueOf(item.quantity()));
            row.setUnit(item.unit());
            row.setSourceType(item.sourceType() == null ? "PURCHASE" : item.sourceType());
            row.setSubstituteCode(item.substituteCode());
            bomItemMapper.insert(row);
        }
        return toBomDto(bom);
    }

    // 查询项目工艺路线。
    @Override
    public PageResult<ProcessRouteDto> processRoutes(Long projectId) {
        List<Long> visibleIds = dataScope.visibleProjectIds();
        if (visibleIds != null && visibleIds.isEmpty())
            return new PageResult<>(List.of(), 0);
        List<ProcessRouteDto> rows = routeMapper.selectList(Wrappers.<ProcessRoute>lambdaQuery().eq(projectId != null, ProcessRoute::getProjectId, projectId)
                        .in(visibleIds != null, ProcessRoute::getProjectId, visibleIds == null ? List.of() : visibleIds).orderByDesc(ProcessRoute::getId))
                .stream().map(this::toRouteDto).toList();
                return new PageResult<>(rows, rows.size());
    }

    // 分页查询项目工艺路线。
    @Override
    public PageResult<ProcessRouteDto> processRoutes(Long projectId, PageQuery pageQuery) {
        PageQuery page = PageSupport.normalize(pageQuery);
        List<Long> visibleIds = dataScope.visibleProjectIds();
        if (visibleIds != null && visibleIds.isEmpty()) return PageResult.empty(page);
        Page<ProcessRoute> entityPage = routeMapper.selectPage(PageSupport.page(page),
                Wrappers.<ProcessRoute>lambdaQuery().eq(projectId != null, ProcessRoute::getProjectId, projectId)
                        .in(visibleIds != null, ProcessRoute::getProjectId,
                                visibleIds == null ? List.of() : visibleIds).orderByDesc(ProcessRoute::getId));
        return PageSupport.result(entityPage, page, this::toRouteDto);
    }

    // 创建工艺路线。
    @Override
    @Transactional
    public ProcessRouteDto createProcessRoute(ProcessRouteRequest request) {
        if (request == null || request.projectId() == null || blank(request.routeNo()) || blank(request.versionNo())) throw new BusinessException("项目、工艺路线编号和版本号不能为空");
        requireProject(request.projectId());
        dataScope.requireProjectRole(request.projectId(), "PROCESS");
        requireDocumentIfPresent(request.boundDocVersionId(), request.projectId());
        ProcessRoute route = new ProcessRoute();
        route.setTenantId(TenantContext.tenantId());
        route.setProjectId(request.projectId());
        route.setRouteNo(request.routeNo());
        route.setVersionNo(request.versionNo());
        route.setBoundDocVersionId(request.boundDocVersionId());
        route.setStatus("DRAFT");
        routeMapper.insert(route);
        if (request.steps() != null) for (ProcessStepRequest item : request.steps()) {
            if (item.stepNo() == null || blank(item.stepName())) throw new BusinessException("工艺步骤编号和名称不能为空");
            ProcessStep row = new ProcessStep();
            row.setTenantId(TenantContext.tenantId());
            row.setRouteId(route.getId());
            row.setStepNo(item.stepNo());
            row.setStepName(item.stepName());
            row.setWorkInstruction(item.workInstruction());
            row.setEquipmentName(item.equipmentName());
            row.setStandardHours(item.standardHours() == null ? null : BigDecimal.valueOf(item.standardHours()));
            row.setOutsourceFlag(Boolean.TRUE.equals(item.outsourceFlag()) ? 1 : 0);
            row.setInspectionPoint(Boolean.TRUE.equals(item.inspectionPoint()) ? 1 : 0);
            stepMapper.insert(row);
        }
        return toRouteDto(route);
    }

    // 查询项目检验规范。
    @Override
    public PageResult<InspectionSpecDto> inspectionSpecs(Long projectId) {
        List<Long> visibleIds = dataScope.visibleProjectIds();
        if (visibleIds != null && visibleIds.isEmpty()) return new PageResult<>(List.of(), 0);
        List<InspectionSpecDto> rows = inspectionSpecMapper.selectList(Wrappers.<InspectionSpec>lambdaQuery().eq(projectId != null, InspectionSpec::getProjectId, projectId)
                        .in(visibleIds != null, InspectionSpec::getProjectId, visibleIds == null ? List.of() : visibleIds).orderByDesc(InspectionSpec::getId))
                .stream().map(this::toInspectionDto).toList();
                return new PageResult<>(rows, rows.size());
    }

    // 分页查询项目检验规范。
    @Override
    public PageResult<InspectionSpecDto> inspectionSpecs(Long projectId, PageQuery pageQuery) {
        PageQuery page = PageSupport.normalize(pageQuery);
        List<Long> visibleIds = dataScope.visibleProjectIds();
        if (visibleIds != null && visibleIds.isEmpty()) return PageResult.empty(page);
        Page<InspectionSpec> entityPage = inspectionSpecMapper.selectPage(PageSupport.page(page),
                Wrappers.<InspectionSpec>lambdaQuery().eq(projectId != null, InspectionSpec::getProjectId, projectId)
                        .in(visibleIds != null, InspectionSpec::getProjectId,
                                visibleIds == null ? List.of() : visibleIds).orderByDesc(InspectionSpec::getId));
        return PageSupport.result(entityPage, page, this::toInspectionDto);
    }

    // 创建检验规范。
    @Override
    @Transactional
    public InspectionSpecDto createInspectionSpec(InspectionSpecRequest request) {
        if (request == null || request.projectId() == null || blank(request.specNo()) || blank(request.versionNo())) throw new BusinessException("项目、检验规范编号和版本号不能为空");
        requireProject(request.projectId());
        dataScope.requireProjectRole(request.projectId(), "PROCESS");
        requireDocumentIfPresent(request.boundDocVersionId(), request.projectId());
        InspectionSpec spec = new InspectionSpec();
        spec.setTenantId(TenantContext.tenantId());
        spec.setProjectId(request.projectId());
        spec.setSpecNo(request.specNo());
        spec.setVersionNo(request.versionNo());
        spec.setBoundDocVersionId(request.boundDocVersionId());
        spec.setStatus("DRAFT");
        inspectionSpecMapper.insert(spec);
        if (request.items() != null) for (InspectionItemRequest item : request.items()) {
            if (blank(item.itemName())) throw new BusinessException("检验项目名称不能为空");
            InspectionItem row = new InspectionItem();
            row.setTenantId(TenantContext.tenantId());
            row.setSpecId(spec.getId());
            row.setItemName(item.itemName());
            row.setStandardValue(item.standardValue());
            row.setSamplingRule(item.samplingRule());
            row.setAttachmentFileId(item.attachmentFileId());
            inspectionItemMapper.insert(row);
        }
        return toInspectionDto(spec);
    }

    // 发布当前技术包。
    @Override
    @Transactional
    public Map<String, Object> publishTechnicalPackage(Long projectId) {
        return businessLock.withLock("project:technical-package:" + projectId, () -> publishTechnicalPackageUnderLock(projectId));
    }

    private Map<String, Object> publishTechnicalPackageUnderLock(Long projectId) {
        Project project = requireProject(projectId);
        dataScope.requireProjectRole(projectId, "TECHNICAL");
        DocumentVersion current = documentVersionMapper.selectOne(Wrappers.<DocumentVersion>lambdaQuery().eq(DocumentVersion::getProjectId, projectId).eq(DocumentVersion::getCurrentVersion, 1).last("LIMIT 1"));
        if (current == null) throw BusinessException.ruleBlock("TECHNICAL_VERSION", "必须先发布当前技术文件", "none", "published", "发布图纸或技术文件");
        Bom bom = bomMapper.selectOne(Wrappers.<Bom>lambdaQuery().eq(Bom::getProjectId, projectId).eq(Bom::getBoundDocVersionId, current.getId()).last("LIMIT 1"));
        ProcessRoute route = routeMapper.selectOne(Wrappers.<ProcessRoute>lambdaQuery().eq(ProcessRoute::getProjectId, projectId).eq(ProcessRoute::getBoundDocVersionId, current.getId()).last("LIMIT 1"));
        InspectionSpec spec = inspectionSpecMapper.selectOne(Wrappers.<InspectionSpec>lambdaQuery().eq(InspectionSpec::getProjectId, projectId).eq(InspectionSpec::getBoundDocVersionId, current.getId()).last("LIMIT 1"));
        if (bom == null || route == null || spec == null) throw BusinessException.ruleBlock("TECHNICAL_PACKAGE", "BOM、工艺和检验规范必须绑定当前技术版本", "incomplete", "complete", "补齐技术资料");
        bom.setStatus("PUBLISHED");
        route.setStatus("PUBLISHED");
        spec.setStatus("PUBLISHED");
        bomMapper.updateById(bom);
        routeMapper.updateById(route);
        inspectionSpecMapper.updateById(spec);
        String beforeStatus = project.getStatus();
        project.setCurrentDocVersionId(current.getId());
        project.setStatus("TECH_PUBLISHED");
        project.setStage("SAMPLE");
        if (projectMapper.updateById(project) != 1) throw BusinessException.ruleBlock("OPTIMISTIC_LOCK", "项目已被其他用户修改", "stale", "latest", "刷新项目后重试");
        jdbc.update("INSERT INTO nso_project_status_history (tenant_id,project_id,before_status,after_status,action_code,reason,operator_id) VALUES (?,?,?,?,?,?,?)",
                TenantContext.tenantId(), projectId, beforeStatus, "TECH_PUBLISHED", "TECHNICAL_PACKAGE_PUBLISHED", "技术包发布", TenantContext.userId());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("projectId", projectId);
        result.put("documentVersionId", current.getId());
        result.put("status", project.getStatus());
        return result;
    }

    private Project requireProject(Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null) throw new BusinessException("项目不存在");
        dataScope.requireAccess(id);
        return project;
    }
    private DocumentVersion requireDocument(Long id) {
        DocumentVersion version = documentVersionMapper.selectById(id);
        if (version == null) throw new BusinessException("技术文件版本不存在");
        dataScope.requireAccess(version.getProjectId());
        return version; }
    private FileObject requireProjectFile(Long projectId, Long fileObjectId) {
        if (fileObjectId == null) throw new BusinessException("技术版本必须绑定已上传文件");
        FileObject file = fileObjectMapper.selectById(fileObjectId);
        if (file == null || file.getTenantId() == null || file.getTenantId() != TenantContext.tenantId() || !projectId.equals(file.getProjectId())) throw BusinessException.accessDenied("FILE_PROJECT_SCOPE", "文件不属于当前项目", String.valueOf(fileObjectId), "当前项目文件", "重新上传项目文件");
        return file;
    }
    private void requireDocumentIfPresent(Long id, Long projectId) {
        if (id != null && !projectId.equals(requireDocument(id).getProjectId())) throw new BusinessException("技术版本不属于当前项目");
    }
    private boolean blank(String value) {

        return value == null || value.isBlank();
    }
    private String projectNo(Long id) {

        return requireProject(id).getProjectNo();
    }
    private DocumentVersionDto toDocumentDto(DocumentVersion x) {
        return new DocumentVersionDto(x.getId(), x.getProjectId(), x.getFileObjectId(), projectNo(x.getProjectId()), x.getFileName(), x.getFileType(), x.getVersionNo(), x.getStatus(), x.getEffectiveDate(), x.getChangeSummary(), x.getCurrentVersion() != null && x.getCurrentVersion() == 1, x.getSha256(), x.getFileObjectId() == null ? null : "/api/v1/admin/document-versions/" + x.getId() + "/download");
    }
    private BomDto toBomDto(Bom x) {
        List<BomItemDto> items = bomItemMapper.selectList(Wrappers.<BomItem>lambdaQuery().eq(BomItem::getBomId, x.getId())).stream().map(item -> new BomItemDto(item.getId(), item.getMaterialCode(), item.getMaterialName(), item.getSpecification(), item.getQuantity().doubleValue(), item.getUnit(), item.getSourceType(), item.getSubstituteCode())).toList();
        return new BomDto(x.getId(), x.getProjectId(), projectNo(x.getProjectId()), x.getBomNo(), x.getVersionNo(), x.getBoundDocVersionId(), x.getStatus(), items);
    }
    private ProcessRouteDto toRouteDto(ProcessRoute x) {
        List<ProcessStepDto> steps = stepMapper.selectList(Wrappers.<ProcessStep>lambdaQuery().eq(ProcessStep::getRouteId, x.getId()).orderByAsc(ProcessStep::getStepNo)).stream().map(item -> new ProcessStepDto(item.getId(), item.getStepNo(), item.getStepName(), item.getWorkInstruction(), item.getEquipmentName(), item.getStandardHours() == null ? null : item.getStandardHours().doubleValue(), item.getOutsourceFlag() != null && item.getOutsourceFlag() == 1, item.getInspectionPoint() != null && item.getInspectionPoint() == 1)).toList();
        return new ProcessRouteDto(x.getId(), x.getProjectId(), projectNo(x.getProjectId()), x.getRouteNo(), x.getVersionNo(), x.getBoundDocVersionId(), x.getStatus(), steps); }
    private InspectionSpecDto toInspectionDto(InspectionSpec x) {
        List<InspectionItemDto> items = inspectionItemMapper.selectList(Wrappers.<InspectionItem>lambdaQuery().eq(InspectionItem::getSpecId, x.getId())).stream().map(item -> new InspectionItemDto(item.getId(), item.getItemName(), item.getStandardValue(), item.getSamplingRule(), item.getAttachmentFileId())).toList();
        return new InspectionSpecDto(x.getId(), x.getProjectId(), projectNo(x.getProjectId()), x.getSpecNo(), x.getVersionNo(), x.getBoundDocVersionId(), x.getStatus(), items);
    }
}
