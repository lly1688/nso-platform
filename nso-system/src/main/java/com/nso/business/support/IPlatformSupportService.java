package com.nso.business.support;
import com.nso.business.core.NsoDtos.*;
import org.springframework.core.io.Resource;
import java.util.Map;
public interface IPlatformSupportService {
 PageResult<SavedViewDto> listViews(String targetType); SavedViewDto saveView(SavedViewRequest request);
 PageResult<ExportTaskDto> listExports(); ExportTaskDto createExport(ExportRequest request); Resource downloadExport(Long exportId);
 Map<String,Object> importTemplate(String type); PageResult<ImportTaskDto> listImports(String importType); ImportTaskDto importRows(ImportRequest request);
 PageResult<RuleParamDto> listRules(String ruleCode); RuleParamDto saveRule(RuleParamRequest request); RuleParamDto publishRule(Long ruleId); PageResult<AuditLogDto> auditLogs(String businessType);
}
