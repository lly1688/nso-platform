package com.nso.system.service;

import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;

import java.util.List;
import java.util.Map;

/**
 * 数据保留治理服务接口。
 */
public interface IRetentionGovernanceService {

    /**
     * 查询数据保留策略。
     *
     * @return 保留策略列表
     */
    List<Map<String, Object>> policies();

    /**
     * 分页查询数据保留策略。
     *
     * @param pageQuery 分页参数
     * @return 保留策略分页结果
     */
    PageResult<Map<String, Object>> policies(PageQuery pageQuery);

    /**
     * 更新数据保留策略。
     *
     * @param policyCode 策略标识
     * @param retentionDays 保留天数
     * @param archiveAfterDays 归档等待天数
     * @param enabled 启用状态
     * @return 更新后的策略
     */
    Map<String, Object> updatePolicy(String policyCode, Integer retentionDays, Integer archiveAfterDays, Boolean enabled);

    /**
     * 执行指定数据保留策略。
     *
     * @param policyCode 策略标识
     * @param confirmationRef 操作确认凭证
     * @return 策略执行结果
     */
    Map<String, Object> runPolicy(String policyCode, String confirmationRef);

    /**
     * 执行全部租户的临时数据策略。
     *
     * @return 执行数量
     */
    int runTemporaryPoliciesForAllTenants();
}
