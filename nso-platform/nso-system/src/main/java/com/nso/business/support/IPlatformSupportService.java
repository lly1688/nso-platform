package com.nso.business.support;

import com.nso.business.core.NsoDtos.*;
import org.springframework.core.io.Resource;

import java.util.Map;

/**
 * 平台视图、导入导出及规则服务接口。
 */
public interface IPlatformSupportService {

    /**
     * 查询当前用户保存的视图。
     *
     * @param targetType 视图目标类型
     * @return 保存视图分页结果
     */
    PageResult<SavedViewDto> listViews(String targetType);

    /**
     * 分页查询当前用户保存的视图。
     *
     * @param targetType 视图目标类型
     * @param pageQuery 分页参数
     * @return 保存视图分页结果
     */
    PageResult<SavedViewDto> listViews(String targetType, PageQuery pageQuery);

    /**
     * 保存用户视图。
     *
     * @param request 视图内容
     * @return 保存后的视图
     */
    SavedViewDto saveView(SavedViewRequest request);

    /**
     * 查询导出任务。
     *
     * @return 导出任务分页结果
     */
    PageResult<ExportTaskDto> listExports();

    /**
     * 分页查询导出任务。
     *
     * @param pageQuery 分页参数
     * @return 导出任务分页结果
     */
    PageResult<ExportTaskDto> listExports(PageQuery pageQuery);

    /**
     * 创建导出任务。
     *
     * @param request 导出内容
     * @return 新建的导出任务
     */
    ExportTaskDto createExport(ExportRequest request);

    /**
     * 下载导出文件。
     *
     * @param exportId 导出任务编号
     * @return 导出文件资源
     */
    Resource downloadExport(Long exportId);

    /**
     * 获取导入模板。
     *
     * @param type 导入类型
     * @return 导入模板内容
     */
    Map<String, Object> importTemplate(String type);

    /**
     * 查询导入任务。
     *
     * @param importType 导入类型
     * @return 导入任务分页结果
     */
    PageResult<ImportTaskDto> listImports(String importType);

    /**
     * 分页查询导入任务。
     *
     * @param importType 导入类型
     * @param pageQuery 分页参数
     * @return 导入任务分页结果
     */
    PageResult<ImportTaskDto> listImports(String importType, PageQuery pageQuery);

    /**
     * 导入结构化数据行。
     *
     * @param request 导入内容
     * @return 导入任务结果
     */
    ImportTaskDto importRows(ImportRequest request);

    /**
     * 查询规则参数。
     *
     * @param ruleCode 规则标识
     * @return 规则参数分页结果
     */
    PageResult<RuleParamDto> listRules(String ruleCode);

    /**
     * 分页查询规则参数。
     *
     * @param ruleCode 规则标识
     * @param pageQuery 分页参数
     * @return 规则参数分页结果
     */
    PageResult<RuleParamDto> listRules(String ruleCode, PageQuery pageQuery);

    /**
     * 保存规则参数。
     *
     * @param request 规则内容
     * @return 保存后的规则参数
     */
    RuleParamDto saveRule(RuleParamRequest request);

    /**
     * 发布规则参数。
     *
     * @param ruleId 规则编号
     * @return 发布后的规则参数
     */
    RuleParamDto publishRule(Long ruleId);

    /**
     * 查询业务审计日志。
     *
     * @param businessType 业务类型
     * @return 审计日志分页结果
     */
    PageResult<AuditLogDto> auditLogs(String businessType);

    /**
     * 分页查询业务审计日志。
     *
     * @param businessType 业务类型
     * @param pageQuery 分页参数
     * @return 审计日志分页结果
     */
    PageResult<AuditLogDto> auditLogs(String businessType, PageQuery pageQuery);
}
