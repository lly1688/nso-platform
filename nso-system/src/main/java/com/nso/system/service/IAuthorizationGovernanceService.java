package com.nso.system.service;

import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;

import java.util.List;
import java.util.Map;

/**
 * 受保护角色授权治理服务接口。
 */
public interface IAuthorizationGovernanceService {

    /**
     * 申请受保护角色。
     *
     * @param userId 目标用户编号
     * @param roleCode 角色标识
     * @param reason 申请原因
     * @return 授权申请
     */
    Map<String, Object> requestProtectedRole(Long userId, String roleCode, String reason);

    /**
     * 审批受保护角色申请。
     *
     * @param requestId 申请编号
     * @return 审批结果
     */
    Map<String, Object> approveProtectedRole(Long requestId);

    /**
     * 查询授权申请。
     *
     * @return 授权申请列表
     */
    List<Map<String, Object>> requests();

    /**
     * 分页查询授权申请。
     *
     * @param pageQuery 分页参数
     * @return 授权申请分页结果
     */
    PageResult<Map<String, Object>> requests(PageQuery pageQuery);
}
