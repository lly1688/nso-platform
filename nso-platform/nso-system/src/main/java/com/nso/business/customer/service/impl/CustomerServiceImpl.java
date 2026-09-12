package com.nso.business.customer.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nso.business.core.NsoDtos.CustomerDto;
import com.nso.business.core.NsoDtos.CustomerRequest;
import com.nso.business.core.NsoDtos.CustomerContactDto;
import com.nso.business.core.NsoDtos.CustomerContactRequest;
import com.nso.business.core.NsoDtos.ExternalProjectAccessDto;
import com.nso.business.core.NsoDtos.ExternalProjectAccessRequest;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.PageSupport;
import com.nso.business.core.TenantContext;
import com.nso.business.customer.domain.Customer;
import com.nso.business.customer.mapper.CustomerMapper;
import com.nso.business.customer.service.ICustomerService;
import com.nso.business.project.domain.Project;
import com.nso.business.project.mapper.ProjectMapper;
import com.nso.business.project.service.ProjectDataScope;
import com.nso.business.support.BusinessNumberService;
import com.nso.business.support.SensitiveDataPolicyService;
import com.nso.shared.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service

// 客户管理 服务层处理
public class CustomerServiceImpl implements ICustomerService {
    // 客户数据映射
    private final CustomerMapper customerMapper;
    // 业务Number服务
    private final BusinessNumberService numbers;
    // 敏感数据策略服务
    private final SensitiveDataPolicyService sensitiveData;
    // 项目数据映射
    private final ProjectMapper projectMapper;
    // 项目数据范围
    private final ProjectDataScope dataScope;
    // JDBC模板
    private final JdbcTemplate jdbc;

    public CustomerServiceImpl(CustomerMapper customerMapper,
                                BusinessNumberService numbers,
                                SensitiveDataPolicyService sensitiveData,
                                ProjectMapper projectMapper,
                                ProjectDataScope dataScope,
                                JdbcTemplate jdbc) {
        this.customerMapper = customerMapper;
        this.numbers = numbers;
        this.sensitiveData = sensitiveData;
        this.projectMapper = projectMapper;
        this.dataScope = dataScope;
        this.jdbc = jdbc;
    }

    // 查询客户列表。
    @Override
    public PageResult<CustomerDto> list(String keyword) {
        boolean canViewContacts = sensitiveData.canReadFull("CUSTOMER_CONTACT");
        List<CustomerDto> rows = customerMapper.selectList(Wrappers.<Customer>lambdaQuery()
                        .and(keyword != null && !keyword.isBlank(), query -> query.like(Customer::getName, keyword)
                                .or().like(Customer::getCustomerCode, keyword)
                                .or(canViewContacts, nested -> nested.like(Customer::getContactName, keyword)))
                        .orderByDesc(Customer::getId))
                .stream().map(this::toDto).toList();
        return new PageResult<>(rows, rows.size());
    }

    // 分页查询客户列表。
    @Override
    public PageResult<CustomerDto> list(String keyword, PageQuery pageQuery) {
        PageQuery page = PageSupport.normalize(pageQuery);
        boolean canViewContacts = sensitiveData.canReadFull("CUSTOMER_CONTACT");
        Page<Customer> entityPage = customerMapper.selectPage(PageSupport.page(page), Wrappers.<Customer>lambdaQuery()
                .and(keyword != null && !keyword.isBlank(), query -> query.like(Customer::getName, keyword)
                        .or().like(Customer::getCustomerCode, keyword)
                        .or(canViewContacts, nested -> nested.like(Customer::getContactName, keyword)))
                .orderByDesc(Customer::getId));
        return PageSupport.result(entityPage, page, this::toDto);
    }

    // 创建客户。
    @Override
    @Transactional
    public CustomerDto create(CustomerRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new BusinessException("客户名称不能为空");
        }
        Customer customer = new Customer();
        customer.setTenantId(TenantContext.tenantId());
        if (request.phone() != null && !request.phone().isBlank() && !sensitiveData.canWrite("CUSTOMER_CONTACT")) {
            throw BusinessException.accessDenied("SENSITIVE_FIELD_WRITE", "当前角色不能编辑客户联系方式", "CUSTOMER_CONTACT", "FULL", "由项目经理或管理员维护");
        }
        customer.setCustomerCode(numbers.next("CUSTOMER"));
        customer.setName(request.name().trim());
        customer.setIndustry(request.industry());
        customer.setContactName(request.contactName());
        customer.setPhone(request.phone());
        customer.setStatus(request.status() == null || request.status().isBlank() ? "ENABLED" : request.status());
        customerMapper.insert(customer);
        return toDto(customer);
    }

    // 查询客户联系人。
    @Override
    public PageResult<CustomerContactDto> contacts(Long customerId) {
        requireCustomer(customerId);
        boolean full = sensitiveData.canReadFull("CUSTOMER_CONTACT");
        List<CustomerContactDto> rows = jdbc.query("SELECT id,customer_id,contact_name,phone,email,position_name,preferred_channel,status FROM crm_contact WHERE tenant_id=? AND customer_id=? AND deleted=0 ORDER BY id",
                (rs, rowNum) -> new CustomerContactDto(rs.getLong("id"), rs.getLong("customer_id"),
                        sensitiveData.project("CUSTOMER_CONTACT", rs.getString("contact_name")),
                        full ? rs.getString("phone") : sensitiveData.project("CUSTOMER_CONTACT", rs.getString("phone")),
                        full ? rs.getString("email") : sensitiveData.project("CUSTOMER_CONTACT", rs.getString("email")),
                        rs.getString("position_name"), rs.getString("preferred_channel"), rs.getString("status")),
                TenantContext.tenantId(), customerId);
        return new PageResult<>(rows, rows.size());
    }

    // 分页查询客户联系人。
    @Override
    public PageResult<CustomerContactDto> contacts(Long customerId, PageQuery pageQuery) {
        requireCustomer(customerId);
        PageQuery page = PageSupport.normalize(pageQuery);
        boolean full = sensitiveData.canReadFull("CUSTOMER_CONTACT");
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM crm_contact WHERE tenant_id=? AND customer_id=? AND deleted=0",
                Long.class, TenantContext.tenantId(), customerId);
        List<CustomerContactDto> rows = jdbc.query("SELECT id,customer_id,contact_name,phone,email,position_name,preferred_channel,status FROM crm_contact WHERE tenant_id=? AND customer_id=? AND deleted=0 ORDER BY id LIMIT ? OFFSET ?",
                (rs, rowNum) -> new CustomerContactDto(rs.getLong("id"), rs.getLong("customer_id"),
                        sensitiveData.project("CUSTOMER_CONTACT", rs.getString("contact_name")),
                        full ? rs.getString("phone") : sensitiveData.project("CUSTOMER_CONTACT", rs.getString("phone")),
                        full ? rs.getString("email") : sensitiveData.project("CUSTOMER_CONTACT", rs.getString("email")),
                        rs.getString("position_name"), rs.getString("preferred_channel"), rs.getString("status")),
                TenantContext.tenantId(), customerId, page.pageSizeValue(), page.offset());
        return new PageResult<>(rows, total == null ? 0 : total, page.pageNoValue(), page.pageSizeValue());
    }

    // 新增或更新客户联系人。
    @Override
    @Transactional
    public CustomerContactDto saveContact(Long customerId, Long contactId, CustomerContactRequest request) {
        requireCustomer(customerId);
        if (request == null || blank(request.contactName())) throw new BusinessException("客户联系人姓名不能为空");
        if (!sensitiveData.canWrite("CUSTOMER_CONTACT")) {
            throw BusinessException.accessDenied("SENSITIVE_FIELD_WRITE", "当前角色不能维护客户联系人", "CUSTOMER_CONTACT", "FULL", "由项目经理或管理人员维护");
        }
        if (contactId == null) {
            jdbc.update("INSERT INTO crm_contact (tenant_id,customer_id,contact_name,phone,email,position_name,preferred_channel,status,deleted) VALUES (?,?,?,?,?,?,?, ?,0)",
                    TenantContext.tenantId(), customerId, request.contactName().trim(), trim(request.phone()), trim(request.email()),
                    trim(request.positionName()), blank(request.preferredChannel()) ? "LINK" : request.preferredChannel().trim(),
                    blank(request.status()) ? "ENABLED" : request.status().trim().toUpperCase());
            contactId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        } else {
            int changed = jdbc.update("UPDATE crm_contact SET contact_name=?,phone=?,email=?,position_name=?,preferred_channel=?,status=?,version=version+1 WHERE id=? AND tenant_id=? AND customer_id=? AND deleted=0",
                    request.contactName().trim(), trim(request.phone()), trim(request.email()), trim(request.positionName()),
                    blank(request.preferredChannel()) ? "LINK" : request.preferredChannel().trim(), blank(request.status()) ? "ENABLED" : request.status().trim().toUpperCase(),
                    contactId, TenantContext.tenantId(), customerId);
            if (changed != 1) throw new BusinessException("客户联系人不存在或不属于当前客户");
        }
        return contactById(contactId);
    }

    // 更新客户联系人状态。
    @Override
    @Transactional
    public CustomerContactDto updateContactStatus(Long customerId, Long contactId, String status) {
        requireCustomer(customerId);
        if (!"ENABLED".equalsIgnoreCase(status) && !"DISABLED".equalsIgnoreCase(status)) throw new BusinessException("联系人状态只能为 ENABLED 或 DISABLED");
        int changed = jdbc.update("UPDATE crm_contact SET status=?,version=version+1 WHERE id=? AND tenant_id=? AND customer_id=? AND deleted=0",
                status.toUpperCase(), contactId, TenantContext.tenantId(), customerId);
        if (changed != 1) throw new BusinessException("客户联系人不存在或不属于当前客户");
        if ("DISABLED".equalsIgnoreCase(status)) {
            jdbc.update("UPDATE nso_external_identity SET status='DISABLED' WHERE tenant_id=? AND contact_id=? AND status='ACTIVE'", TenantContext.tenantId(), contactId);
            jdbc.update("UPDATE nso_external_project_access epa JOIN nso_external_identity ei ON ei.id=epa.identity_id AND ei.tenant_id=epa.tenant_id SET epa.status='REVOKED',epa.revoked_at=NOW(),epa.revoke_reason='客户联系人已停用' WHERE epa.tenant_id=? AND ei.contact_id=? AND epa.status='ACTIVE'", TenantContext.tenantId(), contactId);
            jdbc.update("UPDATE nso_external_token et JOIN nso_external_identity ei ON ei.id=et.identity_id AND ei.tenant_id=et.tenant_id SET et.status='REVOKED',et.revoked_at=NOW(),et.revocation_reason='客户联系人已停用' WHERE et.tenant_id=? AND ei.contact_id=? AND et.status='ACTIVE'", TenantContext.tenantId(), contactId);
        }
        return contactById(contactId);
    }

    // 授予联系人项目访问权限。
    @Override
    @Transactional
    public ExternalProjectAccessDto authorizeProject(Long projectId, ExternalProjectAccessRequest request) {
        if (request == null || request.contactId() == null) throw new BusinessException("请选择客户联系人后再授权项目");
        Project project = requireProjectForAuthorization(projectId);
        Map<String, Object> contact = jdbc.query("SELECT id,customer_id,contact_name,phone,email,status FROM crm_contact WHERE id=? AND tenant_id=? AND deleted=0",
                rs -> rs.next() ? Map.of("id", rs.getLong("id"), "customerId", rs.getLong("customer_id"), "name", rs.getString("contact_name"),
                        "phone", value(rs.getString("phone")), "email", value(rs.getString("email")), "status", rs.getString("status")) : null,
                request.contactId(), TenantContext.tenantId());
        if (contact == null || !project.getCustomerId().equals(((Number) contact.get("customerId")).longValue())) throw new BusinessException("客户联系人必须属于当前项目客户");
        if (!"ENABLED".equals(contact.get("status"))) throw new BusinessException("客户联系人已停用，不能授权");
        Long identityId = jdbc.query("SELECT id FROM nso_external_identity WHERE tenant_id=? AND contact_id=? LIMIT 1",
                rs -> rs.next() ? rs.getLong(1) : null, TenantContext.tenantId(), request.contactId());
        if (identityId == null) {
            jdbc.update("INSERT INTO nso_external_identity (tenant_id,customer_id,contact_id,name,mobile,email,status) VALUES (?,?,?,?,?,?,'ACTIVE')",
                    TenantContext.tenantId(), project.getCustomerId(), request.contactId(), contact.get("name"), contact.get("phone"), contact.get("email"));
            identityId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        } else {
            jdbc.update("UPDATE nso_external_identity SET customer_id=?,name=?,mobile=?,email=?,status='ACTIVE' WHERE id=? AND tenant_id=?",
                    project.getCustomerId(), contact.get("name"), contact.get("phone"), contact.get("email"), identityId, TenantContext.tenantId());
        }
        jdbc.update("INSERT INTO nso_external_project_access (tenant_id,identity_id,project_id,status,valid_until,granted_by,granted_at) VALUES (?,?,?,'ACTIVE',?,?,NOW()) ON DUPLICATE KEY UPDATE status='ACTIVE',valid_until=VALUES(valid_until),granted_by=VALUES(granted_by),granted_at=NOW(),revoked_by=NULL,revoked_at=NULL,revoke_reason=NULL",
                TenantContext.tenantId(), identityId, projectId, request.validUntil(), TenantContext.userId());
        Long accessId = jdbc.queryForObject("SELECT id FROM nso_external_project_access WHERE tenant_id=? AND identity_id=? AND project_id=?", Long.class, TenantContext.tenantId(), identityId, projectId);
        audit("EXTERNAL_PROJECT_ACCESS_GRANTED", accessId, "客户联系人项目授权");
        return authorizationById(accessId);
    }

    // 查询项目外部联系人授权。
    @Override
    public PageResult<ExternalProjectAccessDto> projectAuthorizations(Long projectId) {
        requireProjectForRead(projectId);
        List<ExternalProjectAccessDto> rows = jdbc.query("SELECT epa.id,epa.identity_id,ei.customer_id,ei.contact_id,cc.contact_name,epa.project_id,p.project_no,epa.status,epa.valid_until,epa.granted_at,epa.revoked_at,epa.revoke_reason FROM nso_external_project_access epa JOIN nso_external_identity ei ON ei.id=epa.identity_id AND ei.tenant_id=epa.tenant_id LEFT JOIN crm_contact cc ON cc.id=ei.contact_id AND cc.tenant_id=ei.tenant_id JOIN nso_project p ON p.id=epa.project_id AND p.tenant_id=epa.tenant_id WHERE epa.tenant_id=? AND epa.project_id=? ORDER BY epa.id DESC",
                (rs, rowNum) -> accessDto(rs), TenantContext.tenantId(), projectId);
        return new PageResult<>(rows, rows.size());
    }

    // 分页查询项目外部联系人授权。
    @Override
    public PageResult<ExternalProjectAccessDto> projectAuthorizations(Long projectId, PageQuery pageQuery) {
        requireProjectForRead(projectId);
        PageQuery page = PageSupport.normalize(pageQuery);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM nso_external_project_access WHERE tenant_id=? AND project_id=?",
                Long.class, TenantContext.tenantId(), projectId);
        List<ExternalProjectAccessDto> rows = jdbc.query("SELECT epa.id,epa.identity_id,ei.customer_id,ei.contact_id,cc.contact_name,epa.project_id,p.project_no,epa.status,epa.valid_until,epa.granted_at,epa.revoked_at,epa.revoke_reason FROM nso_external_project_access epa JOIN nso_external_identity ei ON ei.id=epa.identity_id AND ei.tenant_id=epa.tenant_id LEFT JOIN crm_contact cc ON cc.id=ei.contact_id AND cc.tenant_id=ei.tenant_id JOIN nso_project p ON p.id=epa.project_id AND p.tenant_id=epa.tenant_id WHERE epa.tenant_id=? AND epa.project_id=? ORDER BY epa.id DESC LIMIT ? OFFSET ?",
                (rs, rowNum) -> accessDto(rs), TenantContext.tenantId(), projectId, page.pageSizeValue(), page.offset());
        return new PageResult<>(rows, total == null ? 0 : total, page.pageNoValue(), page.pageSizeValue());
    }

    // 撤销联系人项目访问权限。
    @Override
    @Transactional
    public ExternalProjectAccessDto revokeProjectAuthorization(Long projectId, Long authorizationId, String reason) {
        requireProjectForAuthorization(projectId);
        if (blank(reason)) throw new BusinessException("撤销客户项目授权必须填写原因");
        int changed = jdbc.update("UPDATE nso_external_project_access SET status='REVOKED',revoked_by=?,revoked_at=NOW(),revoke_reason=? WHERE id=? AND tenant_id=? AND project_id=? AND status='ACTIVE'",
                TenantContext.userId(), reason.trim(), authorizationId, TenantContext.tenantId(), projectId);
        if (changed != 1) throw new BusinessException("客户项目授权不存在、已撤销或不属于当前项目");
        jdbc.update("UPDATE nso_external_token SET status='REVOKED',revoked_at=NOW(),revocation_reason=? WHERE tenant_id=? AND project_id=? AND identity_id=(SELECT identity_id FROM nso_external_project_access WHERE id=?) AND status='ACTIVE'",
                reason.trim(), TenantContext.tenantId(), projectId, authorizationId);
        audit("EXTERNAL_PROJECT_ACCESS_REVOKED", authorizationId, reason.trim());
        return authorizationById(authorizationId);
    }

    // 校验并返回有效项目授权。
    @Override
    public ExternalProjectAccessDto requireActiveProjectAuthorization(Long projectId, Long contactId) {
        if (contactId == null) throw new BusinessException("必须指定已授权的客户联系人");
        ExternalProjectAccessDto row = jdbc.query("SELECT epa.id,epa.identity_id,ei.customer_id,ei.contact_id,cc.contact_name,epa.project_id,p.project_no,epa.status,epa.valid_until,epa.granted_at,epa.revoked_at,epa.revoke_reason FROM nso_external_project_access epa JOIN nso_external_identity ei ON ei.id=epa.identity_id AND ei.tenant_id=epa.tenant_id JOIN crm_contact cc ON cc.id=ei.contact_id AND cc.tenant_id=ei.tenant_id JOIN nso_project p ON p.id=epa.project_id AND p.tenant_id=epa.tenant_id WHERE epa.tenant_id=? AND epa.project_id=? AND ei.contact_id=? AND epa.status='ACTIVE' AND ei.status='ACTIVE' AND cc.status='ENABLED' AND (epa.valid_until IS NULL OR epa.valid_until>NOW()) LIMIT 1",
                rs -> rs.next() ? accessDto(rs) : null, TenantContext.tenantId(), projectId, contactId);
        if (row == null) throw BusinessException.ruleBlock("EXTERNAL_PROJECT_ACCESS", "客户联系人尚未获得当前项目授权或授权已失效", String.valueOf(contactId), "ACTIVE project authorization", "先在项目客户授权区域完成授权");
        return row;
    }

    private Project requireProjectForAuthorization(Long projectId) {
        Project project = requireProjectForRead(projectId);
        dataScope.requireProjectRole(projectId, "SALES", "PROJECT_MANAGER");
        return project;
    }

    private Project requireProjectForRead(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null || TenantContext.tenantId() != project.getTenantId()) throw new BusinessException("项目不存在或不属于当前企业");
        dataScope.requireAccess(projectId);
        return project;
    }

    private Customer requireCustomer(Long customerId) {
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null || TenantContext.tenantId() != customer.getTenantId()) throw new BusinessException("客户不存在或不属于当前企业");
        return customer;
    }

    private CustomerContactDto contactById(Long contactId) {
        CustomerContactDto contact = jdbc.query("SELECT id,customer_id,contact_name,phone,email,position_name,preferred_channel,status FROM crm_contact WHERE id=? AND tenant_id=? AND deleted=0",
                rs -> rs.next() ? new CustomerContactDto(rs.getLong("id"), rs.getLong("customer_id"), rs.getString("contact_name"), rs.getString("phone"), rs.getString("email"), rs.getString("position_name"), rs.getString("preferred_channel"), rs.getString("status")) : null,
                contactId, TenantContext.tenantId());
        if (contact == null) throw new BusinessException("客户联系人不存在");
        return contact;
    }

    private ExternalProjectAccessDto authorizationById(Long accessId) {
        ExternalProjectAccessDto row = jdbc.query("SELECT epa.id,epa.identity_id,ei.customer_id,ei.contact_id,cc.contact_name,epa.project_id,p.project_no,epa.status,epa.valid_until,epa.granted_at,epa.revoked_at,epa.revoke_reason FROM nso_external_project_access epa JOIN nso_external_identity ei ON ei.id=epa.identity_id AND ei.tenant_id=epa.tenant_id LEFT JOIN crm_contact cc ON cc.id=ei.contact_id AND cc.tenant_id=ei.tenant_id JOIN nso_project p ON p.id=epa.project_id AND p.tenant_id=epa.tenant_id WHERE epa.id=? AND epa.tenant_id=?",
                rs -> rs.next() ? accessDto(rs) : null, accessId, TenantContext.tenantId());
        if (row == null) throw new BusinessException("客户项目授权不存在");
        return row;
    }

    private ExternalProjectAccessDto accessDto(java.sql.ResultSet rs) throws java.sql.SQLException {
        return new ExternalProjectAccessDto(rs.getLong("id"), rs.getLong("identity_id"), rs.getLong("customer_id"), rs.getLong("contact_id"),
                rs.getString("contact_name"), rs.getLong("project_id"), rs.getString("project_no"), rs.getString("status"),
                rs.getTimestamp("valid_until") == null ? null : rs.getTimestamp("valid_until").toLocalDateTime(),
                rs.getTimestamp("granted_at") == null ? null : rs.getTimestamp("granted_at").toLocalDateTime(),
                rs.getTimestamp("revoked_at") == null ? null : rs.getTimestamp("revoked_at").toLocalDateTime(), rs.getString("revoke_reason"));
    }

    private void audit(String changeType, Long subjectId, String reason) {
        jdbc.update("INSERT INTO nso_authorization_audit (tenant_id,subject_type,subject_id,change_type,reason,operator_id) VALUES (?,?,?,?,?,?)",
                TenantContext.tenantId(), "EXTERNAL_PROJECT_ACCESS", subjectId, changeType, reason, TenantContext.userId());
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
    private static String value(String value) {
        return value == null ? "" : value;
    }
    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private CustomerDto toDto(Customer source) {
        return new CustomerDto(source.getId(), source.getCustomerCode(), source.getName(), source.getIndustry(),
                sensitiveData.project("CUSTOMER_CONTACT", source.getContactName()), sensitiveData.project("CUSTOMER_CONTACT", source.getPhone()), source.getStatus());
    }
}
