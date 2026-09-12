package com.nso.business.risk.service;

import com.nso.business.core.NsoDtos.*;

/**
 * 项目风险服务接口。
 */
public interface IRiskService {

    /**
     * 查询项目风险。
     *
     * @param projectId 项目编号
     * @return 风险分页结果
     */
    PageResult<RiskDto> list(Long projectId);

    /**
     * 按条件分页查询项目风险。
     *
     * @param projectId 项目编号
     * @param level 风险等级
     * @param status 风险状态
     * @param pageQuery 分页参数
     * @return 风险分页结果
     */
    PageResult<RiskDto> list(Long projectId, String level, String status, PageQuery pageQuery);

    /**
     * 重新计算项目风险。
     *
     * @param projectId 项目编号
     * @return 计算后的项目风险
     */
    RiskDto calculate(Long projectId);

    /**
     * 查询风险明细。
     *
     * @param riskId 风险编号
     * @return 风险明细分页结果
     */
    PageResult<RiskDetailDto> details(Long riskId);

    /**
     * 分页查询风险明细。
     *
     * @param riskId 风险编号
     * @param pageQuery 分页参数
     * @return 风险明细分页结果
     */
    PageResult<RiskDetailDto> details(Long riskId, PageQuery pageQuery);

    /**
     * 人工调整风险等级。
     *
     * @param riskId 风险编号
     * @param request 调整内容
     * @return 更新后的风险
     */
    RiskDto override(Long riskId, RiskOverrideRequest request);

    /**
     * 恢复已到期的人工调整。
     *
     * @return 恢复数量
     */
    int restoreExpiredOverrides();

    /**
     * 查询风险处置措施。
     *
     * @param riskId 风险编号
     * @return 处置措施分页结果
     */
    PageResult<RiskActionDto> actions(Long riskId);

    /**
     * 分页查询风险处置措施。
     *
     * @param riskId 风险编号
     * @param pageQuery 分页参数
     * @return 处置措施分页结果
     */
    PageResult<RiskActionDto> actions(Long riskId, PageQuery pageQuery);

    /**
     * 创建风险处置措施。
     *
     * @param riskId 风险编号
     * @param request 处置措施内容
     * @return 新建的处置措施
     */
    RiskActionDto createAction(Long riskId, RiskActionRequest request);

    /**
     * 关闭风险处置措施。
     *
     * @param actionId 措施编号
     * @param request 关闭内容
     * @return 关闭后的处置措施
     */
    RiskActionDto closeAction(Long actionId, RiskActionCloseRequest request);
}
