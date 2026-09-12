package com.nso.business.customer.service;

import com.nso.business.core.NsoDtos.*;

/**
 * 客户及外部联系人服务接口。
 */
public interface ICustomerService {

    /**
     * 查询客户列表。
     *
     * @param keyword 搜索关键字
     * @return 客户分页结果
     */
    PageResult<CustomerDto> list(String keyword);

    /**
     * 分页查询客户列表。
     *
     * @param keyword 搜索关键字
     * @param pageQuery 分页参数
     * @return 客户分页结果
     */
    PageResult<CustomerDto> list(String keyword, PageQuery pageQuery);

    /**
     * 创建客户。
     *
     * @param request 客户信息
     * @return 新建的客户
     */
    CustomerDto create(CustomerRequest request);

    /**
     * 查询客户联系人。
     *
     * @param customerId 客户编号
     * @return 联系人分页结果
     */
    PageResult<CustomerContactDto> contacts(Long customerId);

    /**
     * 分页查询客户联系人。
     *
     * @param customerId 客户编号
     * @param pageQuery 分页参数
     * @return 联系人分页结果
     */
    PageResult<CustomerContactDto> contacts(Long customerId, PageQuery pageQuery);

    /**
     * 新增或更新客户联系人。
     *
     * @param customerId 客户编号
     * @param contactId 联系人编号
     * @param request 联系人信息
     * @return 保存后的联系人
     */
    CustomerContactDto saveContact(Long customerId, Long contactId, CustomerContactRequest request);

    /**
     * 更新客户联系人状态。
     *
     * @param customerId 客户编号
     * @param contactId 联系人编号
     * @param status 联系人状态
     * @return 更新后的联系人
     */
    CustomerContactDto updateContactStatus(Long customerId, Long contactId, String status);

    /**
     * 授予联系人项目访问权限。
     *
     * @param projectId 项目编号
     * @param request 授权内容
     * @return 项目授权记录
     */
    ExternalProjectAccessDto authorizeProject(Long projectId, ExternalProjectAccessRequest request);

    /**
     * 查询项目外部联系人授权。
     *
     * @param projectId 项目编号
     * @return 授权分页结果
     */
    PageResult<ExternalProjectAccessDto> projectAuthorizations(Long projectId);

    /**
     * 分页查询项目外部联系人授权。
     *
     * @param projectId 项目编号
     * @param pageQuery 分页参数
     * @return 授权分页结果
     */
    PageResult<ExternalProjectAccessDto> projectAuthorizations(Long projectId, PageQuery pageQuery);

    /**
     * 撤销联系人项目访问权限。
     *
     * @param projectId 项目编号
     * @param authorizationId 授权编号
     * @param reason 撤销原因
     * @return 撤销后的授权记录
     */
    ExternalProjectAccessDto revokeProjectAuthorization(Long projectId, Long authorizationId, String reason);

    /**
     * 校验并返回有效项目授权。
     *
     * @param projectId 项目编号
     * @param contactId 联系人编号
     * @return 有效授权记录
     */
    ExternalProjectAccessDto requireActiveProjectAuthorization(Long projectId, Long contactId);
}
