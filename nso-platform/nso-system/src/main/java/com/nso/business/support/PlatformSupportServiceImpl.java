package com.nso.business.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nso.business.core.NsoDtos.*;
import com.nso.business.core.TenantContext;
import com.nso.business.customer.service.ICustomerService;
import com.nso.business.file.ObjectStoragePort;
import com.nso.business.file.domain.FileObject;
import com.nso.business.file.mapper.FileObjectMapper;
import com.nso.business.project.service.IProjectService;
import com.nso.common.exception.BusinessException;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PlatformSupportServiceImpl implements IPlatformSupportService {
    private final JdbcTemplate jdbc; private final ObjectMapper json; private final ICustomerService customers;
    private final IProjectService projects; private final ObjectStoragePort storage; private final FileObjectMapper files;

    public PlatformSupportServiceImpl(JdbcTemplate jdbc, ObjectMapper json, ICustomerService customers, IProjectService projects, ObjectStoragePort storage, FileObjectMapper files) {
        this.jdbc = jdbc; this.json = json; this.customers = customers; this.projects = projects; this.storage = storage; this.files = files;
    }

    @Override public PageResult<SavedViewDto> listViews(String targetType) {
        List<SavedViewDto> rows = jdbc.query("SELECT id,view_name,target_type,filter_json,created_at FROM nso_saved_view WHERE tenant_id=? AND user_id=? AND (? IS NULL OR target_type=?) ORDER BY id DESC", (rs, n) -> new SavedViewDto(rs.getLong("id"), rs.getString("view_name"), rs.getString("target_type"), toMap(rs.getString("filter_json")), rs.getTimestamp("created_at").toLocalDateTime()), tenant(), user(), targetType, targetType);
        return new PageResult<>(rows, rows.size());
    }
    @Override @Transactional public SavedViewDto saveView(SavedViewRequest request) {
        if (request == null || blank(request.viewName()) || blank(request.targetType())) throw new BusinessException("视图名称和目标类型不能为空");
        String filters = toJson(request.filters()); jdbc.update("INSERT INTO nso_saved_view (tenant_id,user_id,view_name,target_type,filter_json) VALUES (?,?,?,?,CAST(? AS JSON)) ON DUPLICATE KEY UPDATE filter_json=VALUES(filter_json),updated_at=CURRENT_TIMESTAMP", tenant(), user(), request.viewName(), request.targetType(), filters);
        return jdbc.queryForObject("SELECT id,view_name,target_type,filter_json,created_at FROM nso_saved_view WHERE tenant_id=? AND user_id=? AND target_type=? AND view_name=?", (rs, n) -> new SavedViewDto(rs.getLong("id"), rs.getString("view_name"), rs.getString("target_type"), toMap(rs.getString("filter_json")), rs.getTimestamp("created_at").toLocalDateTime()), tenant(), user(), request.targetType(), request.viewName());
    }

    @Override public PageResult<ExportTaskDto> listExports() {
        List<ExportTaskDto> rows = jdbc.query("SELECT id,export_type,status,file_name,query_json,download_count,created_at,finished_at FROM nso_export_task WHERE tenant_id=? AND user_id=? ORDER BY id DESC", this::exportRow, tenant(), user()); return new PageResult<>(rows, rows.size());
    }
    @Override @Transactional public ExportTaskDto createExport(ExportRequest request) {
        if (request == null || blank(request.exportType())) throw new BusinessException("导出类型不能为空");
        jdbc.update("INSERT INTO nso_export_task (tenant_id,user_id,export_type,query_json,field_json,status) VALUES (?,?,?,CAST(? AS JSON),CAST(? AS JSON),'PENDING')", tenant(), user(), request.exportType(), toJson(request.filters()), toJson(request.fields()));
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class); String content = exportContent(request.exportType()); String name = request.exportType().toLowerCase() + "-" + id + ".csv";
        try {
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8); ObjectStoragePort.StoredObject object = storage.put(tenant() + "/exports/" + name, "text/csv", bytes.length, new ByteArrayInputStream(bytes));
            FileObject file = new FileObject(); file.setTenantId(tenant()); file.setCreatedBy(TenantContext.userId()); file.setFileName(name); file.setContentType("text/csv"); file.setFileSize((long) bytes.length); file.setSha256(object.sha256()); file.setStoragePath(object.objectKey()); files.insert(file);
            jdbc.update("UPDATE nso_export_task SET status='DONE',file_id=?,file_name=?,finished_at=CURRENT_TIMESTAMP WHERE id=? AND tenant_id=? AND user_id=?", file.getId(), name, id, tenant(), user());
        } catch (Exception ex) { jdbc.update("UPDATE nso_export_task SET status='FAILED' WHERE id=? AND tenant_id=? AND user_id=?", id, tenant(), user()); throw new BusinessException("导出文件生成失败: " + ex.getMessage()); }
        return exportById(id);
    }
    @Override public Resource downloadExport(Long exportId) {
        Long fileId = jdbc.queryForObject("SELECT file_id FROM nso_export_task WHERE id=? AND tenant_id=? AND user_id=? AND status='DONE'", Long.class, exportId, tenant(), user());
        if (fileId == null) throw new BusinessException("导出文件尚未就绪或无访问权限"); FileObject file = files.selectById(fileId); if (file == null) throw new BusinessException("导出文件不存在");
        try { jdbc.update("UPDATE nso_export_task SET download_count=download_count+1 WHERE id=? AND tenant_id=? AND user_id=?", exportId, tenant(), user()); return new InputStreamResource(storage.get(file.getStoragePath())); }
        catch (Exception ex) { throw new BusinessException("读取导出文件失败: " + ex.getMessage()); }
    }

    @Override public Map<String, Object> importTemplate(String type) { return Map.of("type", type, "fields", "CUSTOMER".equalsIgnoreCase(type) ? List.of("name", "industry", "contactName", "phone") : List.of("customerName", "productName", "quantity", "targetDate", "ownerName"), "mode", "INSERT_ONLY"); }
    @Override public PageResult<ImportTaskDto> listImports(String importType) { List<ImportTaskDto> rows = jdbc.query("SELECT id,import_type,mode,status,success_count,fail_count,created_at FROM nso_import_task WHERE tenant_id=? AND (? IS NULL OR import_type=?) ORDER BY id DESC", (rs, n) -> new ImportTaskDto(rs.getLong("id"), rs.getString("import_type"), rs.getString("mode"), rs.getString("status"), rs.getInt("success_count"), rs.getInt("fail_count"), List.of(), rs.getTimestamp("created_at").toLocalDateTime()), tenant(), importType, importType); return new PageResult<>(rows, rows.size()); }
    @Override @Transactional public ImportTaskDto importRows(ImportRequest request) {
        if (request == null || blank(request.importType()) || request.rows() == null) throw new BusinessException("导入类型和数据行不能为空");
        jdbc.update("INSERT INTO nso_import_task (tenant_id,import_type,mode,status) VALUES (?,?,?,'PROCESSING')", tenant(), request.importType(), blank(request.mode()) ? "INSERT_ONLY" : request.mode()); Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        int success = 0, failed = 0, line = 0; List<String> errors = new ArrayList<>();
        for (Map<String, String> row : request.rows()) { line++; try {
            if ("CUSTOMER".equalsIgnoreCase(request.importType())) customers.create(new CustomerRequest(row.get("name"), row.get("industry"), row.get("contactName"), row.get("phone"), "ENABLED"));
            else if ("PROJECT".equalsIgnoreCase(request.importType())) projects.create(new ProjectRequest(null, row.get("customerName"), row.get("productName"), parseInt(row.get("quantity"), 1), parseDate(row.get("targetDate")), row.get("ownerName"), row.get("priority"), List.of()));
            else throw new BusinessException("当前仅支持 CUSTOMER 和 PROJECT 导入");
            success++; jdbc.update("INSERT INTO nso_import_task_detail (tenant_id,import_task_id,row_no,row_json,status) VALUES (?,?,?,CAST(? AS JSON),'SUCCESS')", tenant(), id, line, toJson(row));
        } catch (Exception ex) { failed++; String message = ex.getMessage(); errors.add("第" + line + "行: " + message); jdbc.update("INSERT INTO nso_import_task_detail (tenant_id,import_task_id,row_no,row_json,status,error_message) VALUES (?,?,?,CAST(? AS JSON),'FAILED',?)", tenant(), id, line, toJson(row), message); }}
        String status = failed == 0 ? "DONE" : success == 0 ? "FAILED" : "PARTIAL"; jdbc.update("UPDATE nso_import_task SET status=?,success_count=?,fail_count=? WHERE id=? AND tenant_id=?", status, success, failed, id, tenant()); return new ImportTaskDto(id, request.importType(), request.mode(), status, success, failed, errors, LocalDateTime.now());
    }

    @Override public PageResult<RuleParamDto> listRules(String ruleCode) { List<RuleParamDto> rows = jdbc.query("SELECT id,rule_code,rule_name,rule_version,param_json,status,published_at FROM nso_rule_param WHERE tenant_id=? AND (? IS NULL OR rule_code=?) ORDER BY id DESC", (rs, n) -> new RuleParamDto(rs.getLong("id"), rs.getString("rule_code"), rs.getString("rule_name"), rs.getString("rule_version"), toMap(rs.getString("param_json")), rs.getString("status"), rs.getTimestamp("published_at") == null ? null : rs.getTimestamp("published_at").toLocalDateTime()), tenant(), ruleCode, ruleCode); return new PageResult<>(rows, rows.size()); }
    @Override @Transactional public RuleParamDto saveRule(RuleParamRequest request) { if (request == null || blank(request.ruleCode()) || blank(request.ruleName())) throw new BusinessException("规则编码和名称不能为空"); String version = "V" + System.currentTimeMillis(); jdbc.update("INSERT INTO nso_rule_param (tenant_id,rule_code,rule_name,rule_version,param_json,status) VALUES (?,?,?,?,CAST(? AS JSON),?)", tenant(), request.ruleCode(), request.ruleName(), version, toJson(request.params()), blank(request.status()) ? "DRAFT" : request.status()); return ruleById(jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class)); }
    @Override @Transactional public RuleParamDto publishRule(Long ruleId) { jdbc.update("UPDATE nso_rule_param SET status='PUBLISHED',published_at=CURRENT_TIMESTAMP WHERE id=? AND tenant_id=?", ruleId, tenant()); return ruleById(ruleId); }
    @Override public PageResult<AuditLogDto> auditLogs(String businessType) { List<AuditLogDto> rows = jdbc.query("SELECT id,user_name,module_name,operation_type,business_type,business_id,result,after_summary,operated_at FROM nso_audit_log WHERE tenant_id=? AND (? IS NULL OR business_type=?) ORDER BY id DESC", (rs, n) -> new AuditLogDto(rs.getLong("id"), rs.getString("user_name"), rs.getString("module_name"), rs.getString("operation_type"), rs.getString("business_type"), rs.getObject("business_id", Long.class), rs.getString("result"), rs.getString("after_summary"), rs.getTimestamp("operated_at").toLocalDateTime()), tenant(), businessType, businessType); return new PageResult<>(rows, rows.size()); }

    private ExportTaskDto exportById(Long id) { return jdbc.queryForObject("SELECT id,export_type,status,file_name,query_json,download_count,created_at,finished_at FROM nso_export_task WHERE id=? AND tenant_id=? AND user_id=?", this::exportRow, id, tenant(), user()); }
    private ExportTaskDto exportRow(java.sql.ResultSet rs, int n) throws java.sql.SQLException { return new ExportTaskDto(rs.getLong("id"), rs.getString("export_type"), rs.getString("status"), rs.getString("file_name"), rs.getString("query_json"), rs.getInt("download_count"), rs.getTimestamp("created_at").toLocalDateTime(), rs.getTimestamp("finished_at") == null ? null : rs.getTimestamp("finished_at").toLocalDateTime()); }
    private String exportContent(String type) { if ("PROJECT".equalsIgnoreCase(type)) { StringBuilder csv = new StringBuilder("项目编号,客户,产品,状态,目标日期\n"); for (ProjectDto project : projects.list(null).list()) csv.append(csv(project.projectNo())).append(',').append(csv(project.customerName())).append(',').append(csv(project.productName())).append(',').append(csv(project.status())).append(',').append(project.targetDate() == null ? "" : project.targetDate()).append('\n'); return csv.toString(); } if ("CUSTOMER".equalsIgnoreCase(type)) { StringBuilder csv = new StringBuilder("客户编码,客户名称,行业,联系人,电话\n"); for (CustomerDto customer : customers.list(null).list()) csv.append(csv(customer.customerCode())).append(',').append(csv(customer.name())).append(',').append(csv(customer.industry())).append(',').append(csv(customer.contactName())).append(',').append(csv(customer.phone())).append('\n'); return csv.toString(); } throw new BusinessException("当前仅支持 PROJECT 和 CUSTOMER 导出"); }
    private long tenant() { return TenantContext.tenantId(); } private long user() { return TenantContext.userId() == null ? 1L : TenantContext.userId(); }
    private String csv(String value) { return value == null ? "" : "\"" + value.replace("\"", "\"\"") + "\""; } private boolean blank(String value) { return value == null || value.isBlank(); } private int parseInt(String value, int fallback) { try { return value == null ? fallback : Integer.parseInt(value); } catch (Exception ex) { return fallback; } } private LocalDate parseDate(String value) { try { return blank(value) ? null : LocalDate.parse(value); } catch (Exception ex) { throw new BusinessException("日期必须为 yyyy-MM-dd"); } }
    private String toJson(Object value) { try { return json.writeValueAsString(value == null ? Map.of() : value); } catch (Exception ex) { throw new BusinessException("JSON 序列化失败"); } } private Map<String, Object> toMap(String value) { try { return value == null || value.isBlank() ? Map.of() : json.readValue(value, new TypeReference<Map<String, Object>>() {}); } catch (Exception ex) { return Map.of(); } } private RuleParamDto ruleById(Long id) { return jdbc.queryForObject("SELECT id,rule_code,rule_name,rule_version,param_json,status,published_at FROM nso_rule_param WHERE id=? AND tenant_id=?", (rs, n) -> new RuleParamDto(rs.getLong("id"), rs.getString("rule_code"), rs.getString("rule_name"), rs.getString("rule_version"), toMap(rs.getString("param_json")), rs.getString("status"), rs.getTimestamp("published_at") == null ? null : rs.getTimestamp("published_at").toLocalDateTime()), id, tenant()); }
}
