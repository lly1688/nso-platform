package com.nso.business.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nso.business.change.service.IChangeService;
import com.nso.business.core.NsoDtos.ChangeOrderDto;
import com.nso.business.core.NsoDtos.DocumentVersionDto;
import com.nso.business.core.NsoDtos.ProjectDto;
import com.nso.business.core.NsoDtos.QrCodeBindingDto;
import com.nso.business.core.NsoDtos.RiskDto;
import com.nso.business.core.NsoDtos.ScanDetailDto;
import com.nso.business.core.NsoDtos.TaskDto;
import com.nso.business.core.TenantContext;
import com.nso.business.document.domain.DocumentVersion;
import com.nso.business.document.mapper.DocumentVersionMapper;
import com.nso.business.project.domain.Project;
import com.nso.business.project.mapper.ProjectMapper;
import com.nso.business.project.service.ProjectDataScope;
import com.nso.business.risk.domain.Risk;
import com.nso.business.risk.mapper.RiskMapper;
import com.nso.business.task.domain.Task;
import com.nso.business.task.mapper.TaskMapper;
import com.nso.common.exception.BusinessException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FlowCodeServiceImpl implements IFlowCodeService {
    private static final String PROJECT_FLOW = "PROJECT_FLOW";
    private static final String DOCUMENT_VERSION = "DOCUMENT_VERSION";

    private final JdbcTemplate jdbc;
    private final ProjectMapper projects;
    private final DocumentVersionMapper documents;
    private final TaskMapper tasks;
    private final RiskMapper risks;
    private final IChangeService changes;
    private final ProjectDataScope dataScope;

    public FlowCodeServiceImpl(JdbcTemplate jdbc, ProjectMapper projects, DocumentVersionMapper documents,
                               TaskMapper tasks, RiskMapper risks, IChangeService changes, ProjectDataScope dataScope) {
        this.jdbc = jdbc;
        this.projects = projects;
        this.documents = documents;
        this.tasks = tasks;
        this.risks = risks;
        this.changes = changes;
        this.dataScope = dataScope;
    }

    @Override
    @Transactional
    public QrCodeBindingDto ensureProjectFlowCode(Long projectId) {
        requireProject(projectId);
        return ensure(PROJECT_FLOW, projectId);
    }

    @Override
    @Transactional
    public QrCodeBindingDto ensureDocumentVersionCode(Long documentVersionId) {
        DocumentVersion document = documents.selectById(documentVersionId);
        if (document == null || !Long.valueOf(TenantContext.tenantId()).equals(document.getTenantId())) {
            throw new BusinessException("技术文件版本不存在");
        }
        requireProject(document.getProjectId());
        return ensure(DOCUMENT_VERSION, documentVersionId);
    }

    @Override
    public QrCodeBindingDto findProjectFlowCode(Long projectId) {
        requireProject(projectId);
        return find(PROJECT_FLOW, projectId);
    }

    @Override
    public ScanDetailDto resolveScan(String rawCode, List<String> permissions) {
        String code = normalize(rawCode);
        Binding binding = jdbc.query("SELECT qr_code,business_type,business_id,status FROM nso_qrcode_binding WHERE tenant_id=? AND qr_code=?",
            (rs, row) -> new Binding(rs.getString("qr_code"), rs.getString("business_type"), rs.getLong("business_id"), rs.getString("status")),
            TenantContext.tenantId(), code).stream().findFirst().orElseThrow(() -> new BusinessException("未找到对应的现场流转二维码"));
        if (!"ACTIVE".equals(binding.status())) throw new BusinessException("该现场流转二维码已失效");

        Long projectId = PROJECT_FLOW.equals(binding.businessType()) ? binding.businessId() : documentProjectId(binding.businessId());
        Project project = requireProject(projectId);
        List<DocumentVersionDto> documentRows;
        if (DOCUMENT_VERSION.equals(binding.businessType()) && hasPermission(permissions, "document:view")) {
            documentRows = List.of(toDocumentDto(requireDocument(binding.businessId()), project.getProjectNo()));
        } else if (hasPermission(permissions, "document:view")) {
            documentRows = documents.selectList(Wrappers.<DocumentVersion>lambdaQuery()
                    .eq(DocumentVersion::getProjectId, projectId)
                    .eq(DocumentVersion::getCurrentVersion, 1)
                    .orderByDesc(DocumentVersion::getId))
                .stream().map(row -> toDocumentDto(row, project.getProjectNo())).toList();
        } else {
            documentRows = List.of();
        }
        List<TaskDto> taskRows = hasPermission(permissions, "task:view") ? tasks.selectList(Wrappers.<Task>lambdaQuery()
                .eq(Task::getProjectId, projectId)
                .notIn(Task::getStatus, List.of("DONE", "CANCELLED"))
                .orderByAsc(Task::getPlanFinish))
            .stream().map(row -> toTaskDto(row, project.getProjectNo())).toList() : List.of();
        List<RiskDto> riskRows = hasPermission(permissions, "risk:view") ? risks.selectList(Wrappers.<Risk>lambdaQuery()
                .eq(Risk::getProjectId, projectId)
                .ne(Risk::getStatus, "CLOSED")
                .orderByDesc(Risk::getCalculatedAt))
            .stream().map(row -> toRiskDto(row, project.getProjectNo())).toList() : List.of();
        List<ChangeOrderDto> changeRows = hasPermission(permissions, "change:view") ? changes.list(projectId).list() : List.of();
        if (DOCUMENT_VERSION.equals(binding.businessType()) && documentRows.isEmpty()) {
            return new ScanDetailDto(binding.businessType(), project.getProjectNo(), toProjectDto(project), documentRows, taskRows, riskRows, changeRows);
        }
        String title = PROJECT_FLOW.equals(binding.businessType()) ? project.getProjectNo() + " 现场流转" : documentRows.get(0).fileName();
        return new ScanDetailDto(binding.businessType(), title, toProjectDto(project), documentRows, taskRows, riskRows, changeRows);
    }

    private QrCodeBindingDto ensure(String businessType, Long businessId) {
        QrCodeBindingDto existing = find(businessType, businessId);
        if (existing != null && "ACTIVE".equals(existing.status())) return existing;
        String code = "NSO-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase();
        if (existing != null) {
            jdbc.update("UPDATE nso_qrcode_binding SET qr_code=?,target_url=?,status='ACTIVE',updated_at=CURRENT_TIMESTAMP WHERE tenant_id=? AND business_type=? AND business_id=?",
                code, payload(code), TenantContext.tenantId(), businessType, businessId);
        } else {
            jdbc.update("INSERT INTO nso_qrcode_binding (tenant_id,business_type,business_id,qr_code,target_url,status) VALUES (?,?,?,?,?,'ACTIVE')",
                TenantContext.tenantId(), businessType, businessId, code, payload(code));
        }
        return new QrCodeBindingDto(code, businessType, businessId, payload(code), "ACTIVE");
    }

    private QrCodeBindingDto find(String businessType, Long businessId) {
        return jdbc.query("SELECT qr_code,business_type,business_id,status FROM nso_qrcode_binding WHERE tenant_id=? AND business_type=? AND business_id=?",
            (rs, row) -> new QrCodeBindingDto(rs.getString("qr_code"), rs.getString("business_type"), rs.getLong("business_id"),
                payload(rs.getString("qr_code")), rs.getString("status")),
            TenantContext.tenantId(), businessType, businessId).stream().findFirst().orElse(null);
    }

    private Project requireProject(Long projectId) {
        Project project = projects.selectById(projectId);
        if (project == null || !Long.valueOf(TenantContext.tenantId()).equals(project.getTenantId())) throw new BusinessException("项目不存在");
        dataScope.requireAccess(projectId);
        return project;
    }

    private DocumentVersion requireDocument(Long id) {
        DocumentVersion document = documents.selectById(id);
        if (document == null || !Long.valueOf(TenantContext.tenantId()).equals(document.getTenantId())) throw new BusinessException("技术文件版本不存在");
        return document;
    }

    private Long documentProjectId(Long documentVersionId) { return requireDocument(documentVersionId).getProjectId(); }
    private String normalize(String value) {
        if (value == null || value.isBlank()) throw new BusinessException("二维码内容不能为空");
        String trimmed = value.trim();
        if (trimmed.startsWith("nso://scan/")) return trimmed.substring("nso://scan/".length());
        if (trimmed.startsWith("NSO:")) return trimmed.substring(4);
        return trimmed;
    }
    private String payload(String code) { return "nso://scan/" + code; }

    private boolean hasPermission(List<String> permissions, String authority) {
        return permissions != null && permissions.contains(authority);
    }

    private ProjectDto toProjectDto(Project row) {
        long daysLeft = row.getTargetDate() == null ? 0 : ChronoUnit.DAYS.between(LocalDate.now(), row.getTargetDate());
        return new ProjectDto(row.getId(), row.getProjectNo(), row.getCustomerId(), row.getCustomerName(), row.getProductName(), row.getQuantity(),
            row.getTargetDate(), row.getOwnerName(), row.getStatus(), row.getStage(), row.getPriority(), row.getRiskLevel(), row.getSampleStatus(), 0, daysLeft);
    }

    private DocumentVersionDto toDocumentDto(DocumentVersion row, String projectNo) {
        return new DocumentVersionDto(row.getId(), row.getProjectId(), row.getFileObjectId(), projectNo, row.getFileName(), row.getFileType(),
            row.getVersionNo(), row.getStatus(), row.getEffectiveDate(), row.getChangeSummary(), row.getCurrentVersion() != null && row.getCurrentVersion() == 1,
            row.getSha256(), "/api/v1/mp/documents/" + row.getId());
    }

    private TaskDto toTaskDto(Task row, String projectNo) {
        return new TaskDto(row.getId(), row.getProjectId(), projectNo, row.getTaskNo(), row.getTaskType(), row.getTitle(), row.getReferencedVersion(),
            row.getStatus(), row.getResponsibleName(), row.getPlanStart(), row.getPlanFinish(), row.getBlockReason(), row.getVersion());
    }

    private RiskDto toRiskDto(Risk row, String projectNo) {
        String raw = row.getReasons();
        List<String> reasons = raw == null || raw.length() < 2 ? List.of() : List.of(raw.substring(1, raw.length() - 1).replace("\"", "").split(","));
        return new RiskDto(row.getId(), row.getProjectId(), projectNo, row.getLevel(), row.getScore(), reasons, row.getSuggestion(), row.getStatus());
    }

    private record Binding(String code, String businessType, Long businessId, String status) { }
}
