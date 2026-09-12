package com.nso.business.sample.service;

import com.nso.business.core.NsoDtos.*;

/**
 * 样品确认服务接口。
 */
public interface ISampleService {

    /**
     * 查询项目样品。
     *
     * @param projectId 项目编号
     * @return 样品分页结果
     */
    PageResult<SampleDto> list(Long projectId);

    /**
     * 按状态分页查询项目样品。
     *
     * @param projectId 项目编号
     * @param status 样品状态
     * @param pageQuery 分页参数
     * @return 样品分页结果
     */
    PageResult<SampleDto> list(Long projectId, String status, PageQuery pageQuery);

    /**
     * 查询样品详情。
     *
     * @param sampleId 样品编号
     * @return 样品详情
     */
    SampleDto get(Long sampleId);

    /**
     * 创建样品。
     *
     * @param request 样品信息
     * @return 新建的样品
     */
    SampleDto create(SampleRequest request);

    /**
     * 提交样品确认。
     *
     * @param sampleId 样品编号
     * @return 更新后的样品
     */
    SampleDto submitConfirm(Long sampleId);

    /**
     * 提交内部样品确认结论。
     *
     * @param sampleId 样品编号
     * @param request 确认内容
     * @return 更新后的样品
     */
    SampleDto confirm(Long sampleId, SampleConfirmRequest request);

    /**
     * 查询客户可见样品。
     *
     * @param projectId 项目编号
     * @return 客户样品分页结果
     */
    PageResult<CustomerSampleDto> customerSamples(Long projectId);

    /**
     * 分页查询客户可见样品。
     *
     * @param projectId 项目编号
     * @param pageQuery 分页参数
     * @return 客户样品分页结果
     */
    PageResult<CustomerSampleDto> customerSamples(Long projectId, PageQuery pageQuery);

    /**
     * 提交客户样品确认结论。
     *
     * @param sampleId 样品编号
     * @param request 确认内容
     * @return 客户样品确认结果
     */
    CustomerSampleDto confirmCustomer(Long sampleId, SampleConfirmRequest request);

    /**
     * 查询样品检验记录。
     *
     * @param sampleId 样品编号
     * @return 检验记录分页结果
     */
    PageResult<SampleCheckDto> checks(Long sampleId);

    /**
     * 分页查询样品检验记录。
     *
     * @param sampleId 样品编号
     * @param pageQuery 分页参数
     * @return 检验记录分页结果
     */
    PageResult<SampleCheckDto> checks(Long sampleId, PageQuery pageQuery);

    /**
     * 新增样品检验记录。
     *
     * @param sampleId 样品编号
     * @param request 检验内容
     * @return 新建的检验记录
     */
    SampleCheckDto addCheck(Long sampleId, SampleCheckRequest request);

    /**
     * 创建客户确认令牌。
     *
     * @param sampleId 样品编号
     * @param contactId 联系人编号
     * @param validDays 有效天数
     * @param maxUseCount 最大使用次数
     * @return 确认令牌
     */
    ConfirmationTokenDto createConfirmToken(Long sampleId, Long contactId, int validDays, int maxUseCount);

    /**
     * 撤销样品的全部确认令牌。
     *
     * @param sampleId 样品编号
     */
    void revokeConfirmTokens(Long sampleId);

    /**
     * 申请样品特殊放行。
     *
     * @param sampleId 样品编号
     * @param request 放行申请
     * @return 特殊放行记录
     */
    SpecialReleaseDto applySpecialRelease(Long sampleId, SpecialReleaseRequest request);

    /**
     * 审批样品特殊放行。
     *
     * @param releaseId 放行记录编号
     * @return 审批后的放行记录
     */
    SpecialReleaseDto approveSpecialRelease(Long releaseId);

    /**
     * 查询公开样品确认信息。
     *
     * @param token 确认令牌
     * @return 公开确认信息
     */
    PublicSampleConfirmationDto publicConfirmation(String token);

    /**
     * 提交公开样品确认结论。
     *
     * @param token 确认令牌
     * @param request 确认内容
     * @return 公开确认结果
     */
    PublicSampleConfirmationDto submitPublicConfirmation(String token, SampleConfirmRequest request);
}
