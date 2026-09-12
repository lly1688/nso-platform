package com.nso.business.project.service;

import com.nso.business.core.NsoDtos.*;

/**
 * 项目协同服务接口。
 */
public interface IProjectService {

    /**
     * 查询项目列表。
     *
     * @param keyword 搜索关键字
     * @return 项目分页结果
     */
    PageResult<ProjectDto> list(String keyword);

    /**
     * 按条件分页查询项目。
     *
     * @param keyword 搜索关键字
     * @param stage 项目阶段
     * @param status 项目状态
     * @param riskLevel 风险等级
     * @param dueState 交期状态
     * @param quickFilter 快捷筛选项
     * @param pageQuery 分页参数
     * @return 项目分页结果
     */
    PageResult<ProjectDto> list(String keyword, String stage, String status, String riskLevel,
                                String dueState, String quickFilter, PageQuery pageQuery);

    /**
     * 查询客户可见项目。
     *
     * @param keyword 搜索关键字
     * @return 客户项目分页结果
     */
    PageResult<CustomerProjectDto> customerProjects(String keyword);

    /**
     * 分页查询客户可见项目。
     *
     * @param keyword 搜索关键字
     * @param pageQuery 分页参数
     * @return 客户项目分页结果
     */
    PageResult<CustomerProjectDto> customerProjects(String keyword, PageQuery pageQuery);

    /**
     * 汇总项目列表统计数据。
     *
     * @param keyword 搜索关键字
     * @param stage 项目阶段
     * @param status 项目状态
     * @param riskLevel 风险等级
     * @param dueState 交期状态
     * @param quickFilter 快捷筛选项
     * @param attentionPage 关注项目分页参数
     * @return 项目统计结果
     */
    ProjectStatsDto stats(String keyword, String stage, String status, String riskLevel, String dueState,
                          String quickFilter, PageQuery attentionPage);

    /**
     * 创建项目。
     *
     * @param request 项目信息
     * @return 新建的项目
     */
    ProjectDto create(ProjectRequest request);

    /**
     * 提交项目立项评审。
     *
     * @param projectId 项目编号
     * @return 更新后的项目
     */
    ProjectDto submitReview(Long projectId);

    /**
     * 执行项目状态操作。
     *
     * @param projectId 项目编号
     * @param action 操作标识
     * @param request 操作内容
     * @return 更新后的项目
     */
    ProjectDto action(Long projectId, String action, ProjectActionRequest request);

    /**
     * 复制项目。
     *
     * @param projectId 来源项目编号
     * @param request 复制参数
     * @return 新建的项目
     */
    ProjectDto copy(Long projectId, ProjectCopyRequest request);

    /**
     * 查询项目状态历史。
     *
     * @param projectId 项目编号
     * @return 状态历史分页结果
     */
    PageResult<ProjectStatusHistoryDto> statusHistory(Long projectId);

    /**
     * 分页查询项目状态历史。
     *
     * @param projectId 项目编号
     * @param pageQuery 分页参数
     * @return 状态历史分页结果
     */
    PageResult<ProjectStatusHistoryDto> statusHistory(Long projectId, PageQuery pageQuery);

    /**
     * 查询项目需求。
     *
     * @param projectId 项目编号
     * @return 项目需求分页结果
     */
    PageResult<RequirementDto> requirements(Long projectId);

    /**
     * 分页查询项目需求。
     *
     * @param projectId 项目编号
     * @param pageQuery 分页参数
     * @return 项目需求分页结果
     */
    PageResult<RequirementDto> requirements(Long projectId, PageQuery pageQuery);

    /**
     * 保存项目需求。
     *
     * @param projectId 项目编号
     * @param request 需求内容
     * @return 保存后的需求
     */
    RequirementDto saveRequirement(Long projectId, RequirementRequest request);

    /**
     * 确认项目需求。
     *
     * @param requirementId 需求编号
     * @param request 确认内容
     * @return 确认后的需求
     */
    RequirementDto confirmRequirement(Long requirementId, RequirementRequest request);

    /**
     * 查询项目成员。
     *
     * @param projectId 项目编号
     * @return 项目成员分页结果
     */
    PageResult<ProjectMemberDto> members(Long projectId);

    /**
     * 分页查询项目成员。
     *
     * @param projectId 项目编号
     * @param pageQuery 分页参数
     * @return 项目成员分页结果
     */
    PageResult<ProjectMemberDto> members(Long projectId, PageQuery pageQuery);

    /**
     * 添加项目成员。
     *
     * @param projectId 项目编号
     * @param request 成员信息
     * @return 新增的项目成员
     */
    ProjectMemberDto addMember(Long projectId, ProjectMemberRequest request);

    /**
     * 更新项目成员。
     *
     * @param projectId 项目编号
     * @param memberId 成员编号
     * @param request 成员信息
     * @return 更新后的项目成员
     */
    ProjectMemberDto updateMember(Long projectId, Long memberId, ProjectMemberUpdateRequest request);

    /**
     * 移除项目成员。
     *
     * @param projectId 项目编号
     * @param memberId 成员编号
     * @return 移除后的项目成员
     */
    ProjectMemberDto removeMember(Long projectId, Long memberId);

    /**
     * 恢复项目成员。
     *
     * @param projectId 项目编号
     * @param memberId 成员编号
     * @return 恢复后的项目成员
     */
    ProjectMemberDto restoreMember(Long projectId, Long memberId);

    /**
     * 移交项目经理。
     *
     * @param projectId 项目编号
     * @param request 移交内容
     * @return 更新后的项目
     */
    ProjectDto transferManager(Long projectId, ProjectManagerTransferRequest request);

    /**
     * 查询项目经理候选人。
     *
     * @param keyword 搜索关键字
     * @return 候选人分页结果
     */
    PageResult<ProjectMemberCandidateDto> managerCandidates(String keyword);

    /**
     * 分页查询项目经理候选人。
     *
     * @param keyword 搜索关键字
     * @param pageQuery 分页参数
     * @return 候选人分页结果
     */
    PageResult<ProjectMemberCandidateDto> managerCandidates(String keyword, PageQuery pageQuery);

    /**
     * 查询项目成员候选人。
     *
     * @param projectId 项目编号
     * @param keyword 搜索关键字
     * @return 候选人分页结果
     */
    PageResult<ProjectMemberCandidateDto> memberCandidates(Long projectId, String keyword);

    /**
     * 分页查询项目成员候选人。
     *
     * @param projectId 项目编号
     * @param keyword 搜索关键字
     * @param pageQuery 分页参数
     * @return 候选人分页结果
     */
    PageResult<ProjectMemberCandidateDto> memberCandidates(Long projectId, String keyword, PageQuery pageQuery);

    /**
     * 查询项目详情。
     *
     * @param projectId 项目编号
     * @return 项目详情
     */
    ProjectDto get(Long projectId);
}
