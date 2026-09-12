package com.nso.business.support.approval;

import com.nso.business.core.NsoDtos.ApprovalDecisionRequest;
import com.nso.business.core.NsoDtos.ApprovalInstanceDto;
import com.nso.business.core.NsoDtos.ApprovalTemplateDto;
import com.nso.business.core.NsoDtos.ApprovalTemplateRequest;
import com.nso.business.core.NsoDtos.ApprovalTodoDto;
import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;

import java.util.Collection;
import java.util.List;

/**
 * 通用审批服务接口。
 */
public interface IApprovalService {

    /**
     * 分页查询审批模板。
     *
     * @param businessType 业务类型
     * @param pageQuery 分页参数
     * @return 审批模板分页结果
     */
    PageResult<ApprovalTemplateDto> templates(String businessType, PageQuery pageQuery);

    /**
     * 创建审批模板。
     *
     * @param request 模板内容
     * @return 新建的审批模板
     */
    ApprovalTemplateDto createTemplate(ApprovalTemplateRequest request);

    /**
     * 发布审批模板。
     *
     * @param templateId 模板编号
     * @return 发布后的审批模板
     */
    ApprovalTemplateDto publishTemplate(Long templateId);

    /**
     * 创建审批实例。
     *
     * @param businessType 业务类型
     * @param businessId 业务编号
     * @param projectId 项目编号
     * @param includedNodes 参与审批的节点
     * @return 新建的审批实例
     */
    ApprovalInstanceDto createInstance(String businessType, Long businessId, Long projectId, Collection<String> includedNodes);

    /**
     * 查询业务审批实例。
     *
     * @param businessType 业务类型
     * @param businessId 业务编号
     * @return 审批实例
     */
    ApprovalInstanceDto instance(String businessType, Long businessId);

    /**
     * 分页查询当前用户待审批项。
     *
     * @param businessType 业务类型
     * @param pageQuery 分页参数
     * @return 待审批项分页结果
     */
    PageResult<ApprovalTodoDto> pending(String businessType, PageQuery pageQuery);

    /**
     * 查询当前用户待审批项。
     *
     * @return 待审批项列表
     */
    List<ApprovalTodoDto> pendingForCurrentUser();

    /**
     * 查询项目待审批项。
     *
     * @param projectId 项目编号
     * @return 待审批项列表
     */
    List<ApprovalTodoDto> pendingForProject(Long projectId);

    /**
     * 查询待审批项详情。
     *
     * @param todoId 待审批项编号
     * @return 待审批项详情
     */
    ApprovalTodoDto todo(Long todoId);

    /**
     * 提交审批结论。
     *
     * @param todoId 待审批项编号
     * @param request 审批内容
     * @return 更新后的待审批项
     */
    ApprovalTodoDto decide(Long todoId, ApprovalDecisionRequest request);

    /**
     * 提交特殊放行审批结论。
     *
     * @param todoId 待审批项编号
     * @param request 审批内容
     * @return 更新后的待审批项
     */
    ApprovalTodoDto decideSpecialRelease(Long todoId, ApprovalDecisionRequest request);

    /**
     * 同步变更审批状态。
     *
     * @param changeId 变更单编号
     */
    void syncChangeApprovals(Long changeId);

    /**
     * 同步特殊放行审批状态。
     *
     * @param releaseId 放行记录编号
     */
    void syncSpecialReleaseApproval(Long releaseId);

    /**
     * 重新打开审批驳回的异常单。
     *
     * @param exceptionCaseId 异常单编号
     */
    void reopenRejectedExceptionApproval(Long exceptionCaseId);

    /**
     * 判断业务是否已审批通过。
     *
     * @param businessType 业务类型
     * @param businessId 业务编号
     * @return 是否已审批通过
     */
    boolean isApproved(String businessType, Long businessId);

    /**
     * 升级逾期审批任务。
     *
     * @return 升级数量
     */
    int escalateOverdue();
}
