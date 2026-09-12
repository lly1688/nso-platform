package com.nso.business.document.service;

import com.nso.business.core.NsoDtos.*;
import com.nso.business.file.service.FileDownloadPayload;

import java.util.Map;

/**
 * 技术文档服务接口。
 */
public interface IDocumentService {

    /**
     * 查询项目文档版本。
     *
     * @param projectId 项目编号
     * @return 文档版本分页结果
     */
    PageResult<DocumentVersionDto> documents(Long projectId);

    /**
     * 分页查询项目文档版本。
     *
     * @param projectId 项目编号
     * @param pageQuery 分页参数
     * @return 文档版本分页结果
     */
    PageResult<DocumentVersionDto> documents(Long projectId, PageQuery pageQuery);

    /**
     * 创建文档版本。
     *
     * @param projectId 项目编号
     * @param request 版本信息
     * @return 新建的文档版本
     */
    DocumentVersionDto createVersion(Long projectId, DocumentVersionRequest request);

    /**
     * 发布文档版本。
     *
     * @param versionId 版本编号
     * @return 发布后的文档版本
     */
    DocumentVersionDto publishVersion(Long versionId);

    /**
     * 查询文档版本详情。
     *
     * @param versionId 版本编号
     * @return 文档版本详情
     */
    DocumentVersionDto getVersion(Long versionId);

    /**
     * 下载文档版本文件。
     *
     * @param versionId 版本编号
     * @return 文件下载载荷
     */
    FileDownloadPayload downloadVersion(Long versionId);

    /**
     * 查询项目物料清单。
     *
     * @param projectId 项目编号
     * @return 物料清单分页结果
     */
    PageResult<BomDto> boms(Long projectId);

    /**
     * 分页查询项目物料清单。
     *
     * @param projectId 项目编号
     * @param pageQuery 分页参数
     * @return 物料清单分页结果
     */
    PageResult<BomDto> boms(Long projectId, PageQuery pageQuery);

    /**
     * 创建物料清单。
     *
     * @param request 物料清单信息
     * @return 新建的物料清单
     */
    BomDto createBom(BomRequest request);

    /**
     * 查询项目工艺路线。
     *
     * @param projectId 项目编号
     * @return 工艺路线分页结果
     */
    PageResult<ProcessRouteDto> processRoutes(Long projectId);

    /**
     * 分页查询项目工艺路线。
     *
     * @param projectId 项目编号
     * @param pageQuery 分页参数
     * @return 工艺路线分页结果
     */
    PageResult<ProcessRouteDto> processRoutes(Long projectId, PageQuery pageQuery);

    /**
     * 创建工艺路线。
     *
     * @param request 工艺路线信息
     * @return 新建的工艺路线
     */
    ProcessRouteDto createProcessRoute(ProcessRouteRequest request);

    /**
     * 查询项目检验规范。
     *
     * @param projectId 项目编号
     * @return 检验规范分页结果
     */
    PageResult<InspectionSpecDto> inspectionSpecs(Long projectId);

    /**
     * 分页查询项目检验规范。
     *
     * @param projectId 项目编号
     * @param pageQuery 分页参数
     * @return 检验规范分页结果
     */
    PageResult<InspectionSpecDto> inspectionSpecs(Long projectId, PageQuery pageQuery);

    /**
     * 创建检验规范。
     *
     * @param request 检验规范信息
     * @return 新建的检验规范
     */
    InspectionSpecDto createInspectionSpec(InspectionSpecRequest request);

    /**
     * 发布项目技术包。
     *
     * @param projectId 项目编号
     * @return 技术包发布结果
     */
    Map<String, Object> publishTechnicalPackage(Long projectId);
}
