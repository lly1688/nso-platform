package com.nso.business.change.service;

import com.nso.business.core.NsoDtos.*;

import java.util.List;

/**
 * 变更协同服务接口。
 */
public interface IChangeService {

    /**
     * 查询项目变更单。
     *
     * @param projectId 项目编号
     * @return 变更单分页结果
     */
    PageResult<ChangeOrderDto> list(Long projectId);

    /**
     * 按条件查询项目变更单。
     *
     * @param projectId 项目编号
     * @param status 变更状态
     * @param changeType 变更类型
     * @param pageQuery 分页参数
     * @return 变更单分页结果
     */
    PageResult<ChangeOrderDto> list(Long projectId, String status, String changeType, PageQuery pageQuery);

    /**
     * 查询变更单详情。
     *
     * @param changeId 变更单编号
     * @return 变更单详情
     */
    ChangeOrderDto get(Long changeId);

    /**
     * 创建变更单。
     *
     * @param request 变更申请
     * @return 新建的变更单
     */
    ChangeOrderDto create(ChangeRequest request);

    /**
     * 分析变更影响并准备审批。
     *
     * @param changeId 变更单编号
     * @return 变更影响列表
     */
    List<ChangeImpactDto> analyze(Long changeId);

    /**
     * 查询变更影响项。
     *
     * @param changeId 变更单编号
     * @return 变更影响列表
     */
    List<ChangeImpactDto> impacts(Long changeId);

    /**
     * 分页查询变更影响项。
     *
     * @param changeId 变更单编号
     * @param pageQuery 分页参数
     * @return 变更影响分页结果
     */
    PageResult<ChangeImpactDto> impactsPage(Long changeId, PageQuery pageQuery);

    /**
     * 审批通过变更单。
     *
     * @param changeId 变更单编号
     * @return 审批后的变更单
     */
    ChangeOrderDto approve(Long changeId);

    /**
     * 提交变更审批结论。
     *
     * @param changeId 变更单编号
     * @param request 审批内容
     * @return 审批后的变更单
     */
    ChangeOrderDto approve(Long changeId, ChangeApprovalRequest request);

    /**
     * 提交单项变更执行反馈。
     *
     * @param impactId 影响项编号
     * @param request 执行反馈
     * @return 更新后的影响项
     */
    ChangeImpactDto feedbackImpact(Long impactId, ChangeFeedbackRequest request);

    /**
     * 提交变更整体执行反馈。
     *
     * @param changeId 变更单编号
     * @param request 执行反馈
     * @return 更新后的变更单
     */
    ChangeOrderDto feedbackChange(Long changeId, ChangeFeedbackRequest request);

    /**
     * 关闭已完成的变更单。
     *
     * @param changeId 变更单编号
     * @return 关闭后的变更单
     */
    ChangeOrderDto close(Long changeId);

    /**
     * 撤销变更单。
     *
     * @param changeId 变更单编号
     * @param request 撤销内容
     * @return 撤销后的变更单
     */
    ChangeOrderDto revoke(Long changeId, ChangeRevokeRequest request);

    /**
     * 验证变更执行结果。
     *
     * @param changeId 变更单编号
     * @param request 验证内容
     * @return 验证后的变更单
     */
    ChangeOrderDto verify(Long changeId, ChangeVerifyRequest request);

    /**
     * 补充变更申请内容。
     *
     * @param changeId 变更单编号
     * @param request 变更申请
     * @return 更新后的变更单
     */
    ChangeOrderDto supplement(Long changeId, ChangeRequest request);
}
