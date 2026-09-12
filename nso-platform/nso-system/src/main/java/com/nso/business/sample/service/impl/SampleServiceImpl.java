package com.nso.business.sample.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nso.business.core.BusinessLockPort;
import com.nso.business.core.NsoDtos.*;
import com.nso.business.core.PageSupport;
import com.nso.business.core.TenantContext;
import com.nso.business.customer.service.ICustomerService;
import com.nso.business.project.domain.Project;
import com.nso.business.project.domain.ProjectMember;
import com.nso.business.project.mapper.ProjectMapper;
import com.nso.business.project.service.ProjectDataScope;
import com.nso.business.support.BusinessEventService;
import com.nso.business.support.BusinessNumberService;
import com.nso.business.support.approval.IApprovalService;
import com.nso.business.task.domain.Task;
import com.nso.business.task.mapper.TaskMapper;
import com.nso.business.task.service.TaskResponsibilityCatalog;
import com.nso.business.sample.domain.Sample;
import com.nso.business.sample.domain.SampleCheck;
import com.nso.business.sample.domain.SampleConfirm;
import com.nso.business.sample.mapper.SampleCheckMapper;
import com.nso.business.sample.mapper.SampleConfirmMapper;
import com.nso.business.sample.mapper.SampleMapper;
import com.nso.business.sample.service.ISampleService;
import com.nso.shared.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

// 打样确认服务。
@Service

// 打样管理 服务层处理
public class SampleServiceImpl implements ISampleService {
    // 样品数据映射
    private final SampleMapper sampleMapper;
    // 样品检查数据映射
    private final SampleCheckMapper sampleCheckMapper;
    // 样品Confirm数据映射
    private final SampleConfirmMapper sampleConfirmMapper;
    // 项目数据映射
    private final ProjectMapper projectMapper;
    // 业务锁端口
    private final BusinessLockPort businessLock;
    // 项目数据范围
    private final ProjectDataScope dataScope;
    // 业务事件服务
    private final BusinessEventService events;
    // 任务数据映射
    private final TaskMapper taskMapper;
    // JDBC模板
    private final JdbcTemplate jdbc;
    // 业务Number服务
    private final BusinessNumberService numbers;
    // 客户服务
    private final ICustomerService customerService;
    // 审批服务
    private final IApprovalService approvals;

    public SampleServiceImpl(
            SampleMapper sampleMapper,
            SampleCheckMapper sampleCheckMapper,
            SampleConfirmMapper sampleConfirmMapper,
            ProjectMapper projectMapper,
            BusinessLockPort businessLock,
            ProjectDataScope dataScope,
            BusinessEventService events,
            TaskMapper taskMapper,
            JdbcTemplate jdbc,
            BusinessNumberService numbers,
            ICustomerService customerService,
            IApprovalService approvals) {
        this.sampleMapper = sampleMapper;
        this.sampleCheckMapper = sampleCheckMapper;
        this.sampleConfirmMapper = sampleConfirmMapper;
        this.projectMapper = projectMapper;
        this.businessLock = businessLock;
        this.dataScope = dataScope;
        this.events = events;
        this.taskMapper = taskMapper;
        this.jdbc = jdbc;
        this.numbers = numbers;
        this.customerService = customerService;
        this.approvals = approvals;
    }

    // 查询可见样品单。
    @Override
    public PageResult<SampleDto> list(Long projectId) {
        List<Long> visibleIds = dataScope.visibleProjectIds();
        if (visibleIds != null && visibleIds.isEmpty()) {
            return new PageResult<>(List.of(), 0);
        }
        List<SampleDto> rows = sampleMapper.selectList(Wrappers.<Sample>lambdaQuery().eq(projectId != null, Sample::getProjectId, projectId)
                        .in(visibleIds != null, Sample::getProjectId, visibleIds == null ? List.of() : visibleIds).orderByDesc(Sample::getId))
                .stream().map(this::toDto).toList();
        return new PageResult<>(rows, rows.size());
    }

    // 按状态分页查询项目样品。
    @Override
    public PageResult<SampleDto> list(Long projectId, String status, PageQuery pageQuery) {
        PageQuery page = PageSupport.normalize(pageQuery);
        List<Long> visibleIds = dataScope.visibleProjectIds();
        if (visibleIds != null && visibleIds.isEmpty()) {
            return PageResult.empty(page);
        }
        Page<Sample> entityPage = sampleMapper.selectPage(PageSupport.page(page), Wrappers.<Sample>lambdaQuery()
                .eq(projectId != null, Sample::getProjectId, projectId)
                .eq(status != null && !status.isBlank(), Sample::getStatus, status)
                .in(visibleIds != null, Sample::getProjectId, visibleIds == null ? List.of() : visibleIds)
                .orderByDesc(Sample::getId));
        return PageSupport.result(entityPage, page, this::toDto);
    }

    // 查询样品详情。
    @Override
    public SampleDto get(Long sampleId) {
        return toDto(requireSample(sampleId));
    }

    // 创建样品单时同步生成打样任务。
    @Override
    @Transactional
    public SampleDto create(SampleRequest request) {
        if (request == null || request.projectId() == null || blank(request.purpose())) throw new BusinessException("项目和样品用途不能为空");
        Project project = requireProject(request.projectId());
        dataScope.requireProjectRole(project.getId(), "PROJECT_MANAGER");
        if (!"TECH_PUBLISHED".equals(project.getStatus())) throw BusinessException.ruleBlock("SAMPLE_TECHNICAL_PACKAGE", "技术包未发布，不能创建样品", project.getStatus(), "TECH_PUBLISHED", "先发布技术包");
        Sample sample = new Sample();
        sample.setTenantId(TenantContext.tenantId());
        sample.setProjectId(request.projectId());
        sample.setSampleNo(numbers.next("SAMPLE"));
        sample.setPurpose(request.purpose());
        sample.setQuantity(request.quantity() == null || request.quantity() < 1 ? 1 : request.quantity());
        sample.setPlanFinishDate(request.planFinishDate());
        sample.setReferencedVersion(request.referencedVersion());
        sample.setDocVersionId(project.getCurrentDocVersionId());
        sample.setStatus("DRAFT");
        sample.setConfirmConclusion("PENDING");
        sample.setResponsibleName(request.responsibleName());
        sample.setCreatedBy(TenantContext.userId());
        sampleMapper.insert(sample);
        createSampleTasks(sample);
        project.setSampleStatus("DRAFT");
        moveProject(project, "SAMPLE_CREATED", "SAMPLING", "SAMPLE", "已创建样品单 " + sample.getSampleNo());
        events.record(project.getId(), "SAMPLE", sample.getId(), "SAMPLE_CREATED", null, "DRAFT", "已创建样品单 " + sample.getSampleNo());
        return toDto(sample);
    }

    // 提交样品确认。
    @Override
    @Transactional
    public SampleDto submitConfirm(Long sampleId) {
        Sample sample = requireSample(sampleId);
        dataScope.requireProjectRole(sample.getProjectId(), "PROJECT_MANAGER");
        if (!"CHECKED".equals(sample.getStatus()) || sample.getQualityConfirmedBy() == null) {
            throw BusinessException.ruleBlock("SAMPLE_QUALITY_CHECK", "样品必须由独立质量人员完成合格检验后才能提交客户确认", sample.getStatus(), "CHECKED with quality confirmation", "先完成质量检验并录入合格结论");
        }
        assertSamplePreparationCompleted(sample);
        String before = sample.getStatus();
        sample.setStatus("WAIT_CUSTOMER_CONFIRM");
        sample.setConfirmConclusion("PENDING");
        update(sample);
        Project project = requireProject(sample.getProjectId());
        project.setSampleStatus("WAIT_CUSTOMER_CONFIRM");
        moveProject(project, "SAMPLE_SUBMITTED_CONFIRM", "CUSTOMER_CONFIRMING", "SAMPLE", "样品已提交客户确认");
        events.record(sample.getProjectId(), "SAMPLE", sampleId, "SAMPLE_SUBMITTED_CONFIRM", before, "WAIT_CUSTOMER_CONFIRM", "样品已提交客户确认");
        return toDto(sample);
    }

    // 提交样品确认结论。
    @Override
    @Transactional
    public SampleDto confirm(Long sampleId, SampleConfirmRequest request) {
        return businessLock.withLock("sample:confirm:" + sampleId, () -> {
            Sample sample = requireSample(sampleId);
            dataScope.requireProjectRole(sample.getProjectId(), "PROJECT_MANAGER");
            applyConfirmation(sample, request);
            return toDto(sample);
        });
    }

    // 查询客户可见样品。
    @Override
    public PageResult<CustomerSampleDto> customerSamples(Long projectId) {
        List<Long> visibleIds = dataScope.visibleProjectIds();
        if (visibleIds != null && visibleIds.isEmpty())
            return new PageResult<>(List.of(), 0);
        List<CustomerSampleDto> rows = sampleMapper.selectList(Wrappers.<Sample>lambdaQuery()
                        .eq(projectId != null, Sample::getProjectId, projectId)
                        .in(visibleIds != null, Sample::getProjectId, visibleIds == null ? List.of() : visibleIds)
                        .orderByDesc(Sample::getId))
                .stream().map(this::toCustomerDto).toList();
        return new PageResult<>(rows, rows.size());
    }

    // 分页查询客户可见样品。
    @Override
    public PageResult<CustomerSampleDto> customerSamples(Long projectId, PageQuery pageQuery) {
        PageQuery page = PageSupport.normalize(pageQuery);
        List<Long> visibleIds = dataScope.visibleProjectIds();
        if (visibleIds != null && visibleIds.isEmpty()) {
            return PageResult.empty(page);
        }
        Page<Sample> entityPage = sampleMapper.selectPage(PageSupport.page(page), Wrappers.<Sample>lambdaQuery()
                .eq(projectId != null, Sample::getProjectId, projectId)
                .in(visibleIds != null, Sample::getProjectId, visibleIds == null ? List.of() : visibleIds)
                .orderByDesc(Sample::getId));
        return PageSupport.result(entityPage, page, this::toCustomerDto);
    }

    // 提交客户样品确认结论。
    @Override
    @Transactional
    public CustomerSampleDto confirmCustomer(Long sampleId, SampleConfirmRequest request) {
        return businessLock.withLock("sample:confirm:" + sampleId, () -> {
            Sample sample = requireSample(sampleId);
            dataScope.requireProjectRole(sample.getProjectId(), "CUSTOMER_CONFIRM");
            applyConfirmation(sample, request);
            return toCustomerDto(sample);
        });
    }

    // 查询样品检验记录。
    @Override
    public PageResult<SampleCheckDto> checks(Long sampleId) {
        requireSample(sampleId);
        List<SampleCheckDto> rows = sampleCheckMapper.selectList(Wrappers.<SampleCheck>lambdaQuery().eq(SampleCheck::getSampleId, sampleId).orderByDesc(SampleCheck::getCheckedAt)).stream().map(this::toCheckDto).toList();
        return new PageResult<>(rows, rows.size());
    }

    // 分页查询样品检验记录。
    @Override
    public PageResult<SampleCheckDto> checks(Long sampleId, PageQuery pageQuery) {
        requireSample(sampleId);
        PageQuery page = PageSupport.normalize(pageQuery);
        Page<SampleCheck> entityPage = sampleCheckMapper.selectPage(PageSupport.page(page),
                Wrappers.<SampleCheck>lambdaQuery().eq(SampleCheck::getSampleId, sampleId)
                        .orderByDesc(SampleCheck::getCheckedAt));
        return PageSupport.result(entityPage, page, this::toCheckDto);
    }

    // 新增样品检验记录。
    @Override
    @Transactional
    public SampleCheckDto addCheck(Long sampleId, SampleCheckRequest request) {
        Sample sample = requireSample(sampleId);
        if (request == null || blank(request.checkItem()) || blank(request.result())) throw new BusinessException("检验项目和结果不能为空");
        dataScope.requireProjectRole(sample.getProjectId(), "QUALITY");
        if (sample.getCreatedBy() != null && sample.getCreatedBy().equals(TenantContext.userId())) throw BusinessException.ruleBlock("SAMPLE_DUTY_SEPARATION", "样品制作人员不能填写质量检验结论", String.valueOf(TenantContext.userId()), "独立质量人员", "由质量人员完成检验");
        SampleCheck check = new SampleCheck();
        check.setTenantId(TenantContext.tenantId());
        check.setSampleId(sampleId);
        check.setCheckItem(request.checkItem());
        check.setMeasuredValue(request.measuredValue());
        check.setResult(request.result());
        check.setIssueSummary(request.issueSummary());
        check.setCorrectiveAction(request.correctiveAction());
        check.setCheckerName(request.checkerName());
        check.setCheckedAt(LocalDateTime.now());
        sampleCheckMapper.insert(check);
        if ("PASS".equals(request.result()) && Set.of("DRAFT", "REWORKING").contains(sample.getStatus())) {
            String before = sample.getStatus();
            sample.setStatus("CHECKED");
            sample.setQualityConfirmedBy(TenantContext.userId());
            update(sample);
            events.record(sample.getProjectId(), "SAMPLE", sampleId, "SAMPLE_CHECKED", before, "CHECKED", "样品检验通过");
        }
        if ("FAIL".equals(request.result()) && Set.of("DRAFT", "CHECKED").contains(sample.getStatus())) {
            String before = sample.getStatus();
            sample.setStatus("REWORKING");
            sample.setIssueSummary(request.issueSummary());
            update(sample);
            events.record(sample.getProjectId(), "SAMPLE", sampleId, "SAMPLE_CHECK_FAILED", before, "REWORKING", "样品检验不合格，进入整改");
        }
        return toCheckDto(check);
    }

    // 创建客户确认令牌。
    @Override
    @Transactional
    public ConfirmationTokenDto createConfirmToken(Long sampleId, Long contactId, int validDays, int maxUseCount) {
        Sample sample = requireSample(sampleId);
        if (!"WAIT_CUSTOMER_CONFIRM".equals(sample.getStatus())) throw BusinessException.ruleBlock("SAMPLE_STATUS", "样品尚未进入客户确认状态", sample.getStatus(), "WAIT_CUSTOMER_CONFIRM", "先提交客户确认");
        dataScope.requireProjectRole(sample.getProjectId(), "PROJECT_MANAGER");
        ExternalProjectAccessDto access = customerService.requireActiveProjectAuthorization(sample.getProjectId(), contactId);
        SampleConfirm row = new SampleConfirm();
        row.setTenantId(TenantContext.tenantId());
        row.setSampleId(sampleId);
        row.setToken(UUID.randomUUID().toString().replace("-", ""));
        row.setExpireAt(LocalDateTime.now().plusDays(Math.max(1, validDays)));
        row.setUsedFlag(0);
        row.setMaxUseCount(Math.max(1, maxUseCount));
        row.setUsedCount(0);
        sampleConfirmMapper.insert(row);
        jdbc.update("INSERT INTO nso_external_token (tenant_id,identity_id,project_id,sample_id,legacy_confirmation_id,token_hash,expire_at,max_uses,used_count,status,created_by) VALUES (?,?,?,?,?,?,?,?,0,'ACTIVE',?)",
                TenantContext.tenantId(), access.identityId(), sample.getProjectId(), sampleId, row.getId(), sha256(row.getToken()), row.getExpireAt(), row.getMaxUseCount(), TenantContext.userId());
        events.record(sample.getProjectId(), "SAMPLE", sampleId, "CUSTOMER_CONFIRM_TOKEN_CREATED", null, "ACTIVE", "已向客户联系人 " + access.contactName() + " 生成确认链接");
        return toTokenDto(row);
    }

    // 撤销样品的全部确认令牌。
    @Override
    @Transactional
    public void revokeConfirmTokens(Long sampleId) {
        Sample sample = requireSample(sampleId);
        dataScope.requireProjectRole(sample.getProjectId(), "PROJECT_MANAGER");
        sampleConfirmMapper.update(null, Wrappers.<SampleConfirm>lambdaUpdate().eq(SampleConfirm::getTenantId, TenantContext.tenantId()).eq(SampleConfirm::getSampleId, sampleId).set(SampleConfirm::getUsedFlag, 1).set(SampleConfirm::getUsedCount, 999999));
        jdbc.update("UPDATE nso_external_token SET status='REVOKED',revoked_at=NOW(),revocation_reason='项目经理撤销确认链接' WHERE tenant_id=? AND sample_id=? AND status='ACTIVE'", TenantContext.tenantId(), sampleId);
        events.record(sample.getProjectId(), "SAMPLE", sampleId, "CUSTOMER_CONFIRM_TOKEN_REVOKED", null, "REVOKED", "项目经理撤销客户确认授权");
    }

    // 申请样品特殊放行。
    @Override
    @Transactional
    public SpecialReleaseDto applySpecialRelease(Long sampleId, SpecialReleaseRequest request) {
        Sample sample = requireSample(sampleId);
        dataScope.requireProjectRole(sample.getProjectId(), "PRODUCTION");
        if (request == null || blank(request.releaseScope()) || blank(request.riskStatement()) || blank(request.reason()) || request.validUntil() == null || !request.validUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException("特殊放行必须填写范围、有效期、风险承担说明和原因");
        }
        jdbc.update("INSERT INTO nso_special_release (tenant_id,project_id,sample_id,applicant_user_id,release_scope,valid_until,risk_statement,reason,status) VALUES (?,?,?,?,?,?,?,?,'PENDING')",
                TenantContext.tenantId(), sample.getProjectId(), sampleId, TenantContext.userId(), request.releaseScope().trim(), request.validUntil(), request.riskStatement().trim(), request.reason().trim());
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        approvals.createInstance("SPECIAL_RELEASE", id, sample.getProjectId(), null);
        events.record(sample.getProjectId(), "SPECIAL_RELEASE", id, "SPECIAL_RELEASE_APPLIED", null, "PENDING", "已申请样品特殊放行");
        return releaseById(id);
    }

    // 审批样品特殊放行。
    @Override
    @Transactional
    public SpecialReleaseDto approveSpecialRelease(Long releaseId) {
        Map<String, Object> release = jdbc.query("SELECT id,project_id,applicant_user_id,status FROM nso_special_release WHERE id=? AND tenant_id=?",
                rs -> rs.next() ? Map.of("id", rs.getLong("id"), "projectId", rs.getLong("project_id"), "applicant", rs.getLong("applicant_user_id"), "status", rs.getString("status")) : null,
                releaseId, TenantContext.tenantId());
        if (release == null) throw new BusinessException("特殊放行不存在");
        requireProject(((Number) release.get("projectId")).longValue());
        dataScope.requireProjectRole(((Number) release.get("projectId")).longValue(), "QUALITY");
        if (!"PENDING".equals(release.get("status"))) throw BusinessException.ruleBlock("SPECIAL_RELEASE_STATUS", "当前放行申请不能审批", String.valueOf(release.get("status")), "PENDING", "刷新申请状态");
        if (((Number) release.get("applicant")).longValue() == TenantContext.userId()) throw BusinessException.ruleBlock("SPECIAL_RELEASE_DUTY_SEPARATION", "放行申请人不能自行批准", String.valueOf(TenantContext.userId()), "独立批准人", "由质量或授权批准人处理");
        jdbc.update("UPDATE nso_special_release SET approver_user_id=?, approved_at=NOW(), status='APPROVED' WHERE id=? AND tenant_id=? AND status='PENDING'",
                TenantContext.userId(), releaseId, TenantContext.tenantId());
        approvals.syncSpecialReleaseApproval(releaseId);
        events.record(((Number) release.get("projectId")).longValue(), "SPECIAL_RELEASE", releaseId, "SPECIAL_RELEASE_APPROVED", "PENDING", "APPROVED", "特殊放行已批准");
        return releaseById(releaseId);
    }

    // 查询公开样品确认信息。
    @Override
    public PublicSampleConfirmationDto publicConfirmation(String token) {
        ExternalTokenContext context = resolveExternalToken(token);
        return TenantContext.withTenant(context.tenantId(), () -> {
            SampleConfirm confirmation = requireAvailableToken(token);
            ensureConfirmationMatches(context, confirmation);
            return toPublicDto(requireSample(confirmation.getSampleId()));
        });
    }

    // 提交公开样品确认结论。
    @Override
    @Transactional
    public PublicSampleConfirmationDto submitPublicConfirmation(String token, SampleConfirmRequest request) {
        ExternalTokenContext context = resolveExternalToken(token);
        return businessLock.withLock("sample:public-confirm:" + token, () -> TenantContext.withTenant(context.tenantId(), () -> {
            SampleConfirm confirmation = requireAvailableToken(token);
            ensureConfirmationMatches(context, confirmation);
            SampleConfirmRequest confirmedRequest = new SampleConfirmRequest(request == null ? null : request.conclusion(), request == null ? null : request.opinion(), context.confirmer(), null);
            int changed = sampleConfirmMapper.update(null, Wrappers.<SampleConfirm>lambdaUpdate().eq(SampleConfirm::getId, confirmation.getId()).eq(SampleConfirm::getUsedCount, confirmation.getUsedCount()).setSql("used_count = used_count + 1").set(SampleConfirm::getUsedFlag, confirmation.getUsedCount() + 1 >= confirmation.getMaxUseCount() ? 1 : 0).set(SampleConfirm::getConclusion, confirmedRequest.conclusion()).set(SampleConfirm::getOpinion, confirmedRequest.opinion()).set(SampleConfirm::getConfirmer, confirmedRequest.confirmer()).set(SampleConfirm::getSubmittedAt, LocalDateTime.now()));
            if (changed != 1) throw BusinessException.ruleBlock("CONFIRMATION_TOKEN", "确认链接已被使用", "used", "available", "刷新确认页面");
            if (!context.legacyFallback()) {
                jdbc.update("UPDATE nso_external_token SET used_count=used_count+1,status=CASE WHEN used_count+1>=max_uses THEN 'USED' ELSE 'ACTIVE' END WHERE tenant_id=? AND token_hash=? AND status='ACTIVE' AND used_count<max_uses",
                        context.tenantId(), sha256(token));
            }
            Sample sample = requireSample(confirmation.getSampleId());
            applyConfirmation(sample, confirmedRequest);
            return toPublicDto(sample);
        }));
    }

    private void applyConfirmation(Sample sample, SampleConfirmRequest request) {
        if (request == null || blank(request.conclusion())) throw new BusinessException("确认结论不能为空");
        String conclusion = request.conclusion().toUpperCase();
        if (!Set.of("PASS", "CONDITIONAL_PASS", "REJECT", "WAIT_SUPPLEMENT").contains(conclusion)) throw new BusinessException("确认结论只能为 PASS、CONDITIONAL_PASS、REJECT 或 WAIT_SUPPLEMENT");
        if (blank(request.opinion())) throw new BusinessException("客户确认意见不能为空");
        if (!"WAIT_CUSTOMER_CONFIRM".equals(sample.getStatus())) throw BusinessException.ruleBlock("SAMPLE_STATUS", "样品当前不允许确认", sample.getStatus(), "WAIT_CUSTOMER_CONFIRM", "刷新样品状态");
        boolean proxy = TenantContext.userId() != null && !TenantContext.hasAnyRole("customer_confirm");
        if (proxy && request.evidenceFileId() == null) throw BusinessException.ruleBlock("SAMPLE_PROXY_EVIDENCE", "内部代录客户结论必须上传凭证", "missing", "evidenceFileId", "上传客户确认凭证后重试");
        String before = sample.getStatus();
        sample.setConfirmConclusion(conclusion);
        sample.setIssueSummary(request.opinion());
        sample.setStatus(switch (conclusion) {
            case "PASS" -> "CONFIRMED";
            case "CONDITIONAL_PASS" -> "CONDITIONAL_PASS";
            case "WAIT_SUPPLEMENT" -> "WAIT_SUPPLEMENT";
            default -> "REJECTED";
        });
        if (proxy) {
            sample.setProxyConfirmation(1);
            sample.setProxyConfirmedBy(TenantContext.userId());
        }
        update(sample);
        Project project = requireProject(sample.getProjectId());
        project.setSampleStatus(sample.getStatus());
        if ("CONFIRMED".equals(sample.getStatus())) {
            moveProject(project, "SAMPLE_CONFIRMED", "CUSTOMER_CONFIRMED", "SAMPLE", "客户确认通过");
        } else if ("REJECTED".equals(sample.getStatus())) {
            moveProject(project, "SAMPLE_REJECTED", "SAMPLING", "SAMPLE", "客户确认驳回，进入整改");
        } else {
            projectMapper.updateById(project);
        }
        if ("REJECTED".equals(sample.getStatus())) {
            ProjectMember productionMember = requireTaskMember(sample.getProjectId(), TaskResponsibilityCatalog.forTaskType("SAMPLE_REWORK"));
            createSampleTask(sample, "SAMPLE_REWORK", "样品驳回整改", productionMember);
        }
        if (Set.of("CONFIRMED", "CONDITIONAL_PASS", "REJECTED").contains(sample.getStatus())) completeSampleConfirmTask(sample);
        if (request.evidenceFileId() != null) {
            Integer fileCount = jdbc.queryForObject("SELECT COUNT(*) FROM nso_file_object WHERE id=? AND tenant_id=? AND project_id=?", Integer.class, request.evidenceFileId(), TenantContext.tenantId(), sample.getProjectId());
            if (fileCount == null || fileCount == 0) throw new BusinessException("确认凭证不存在或无权使用");
            jdbc.update("INSERT IGNORE INTO nso_attachment_relation (tenant_id,project_id,business_type,business_id,file_id,evidence_type) VALUES (?,?,?,?,?,'SAMPLE_CONFIRM')", TenantContext.tenantId(), sample.getProjectId(), "SAMPLE", sample.getId(), request.evidenceFileId());
        }
        events.record(sample.getProjectId(), "SAMPLE", sample.getId(), "SAMPLE_CONFIRMED", before, sample.getStatus(), "客户确认结论：" + conclusion);
    }

    private SampleConfirm requireAvailableToken(String token) {
        SampleConfirm row = sampleConfirmMapper.selectOne(Wrappers.<SampleConfirm>lambdaQuery().eq(SampleConfirm::getToken, token));
        if (row == null || row.getExpireAt().isBefore(LocalDateTime.now()) || row.getUsedCount() >= row.getMaxUseCount()) throw BusinessException.ruleBlock("CONFIRMATION_TOKEN", "确认链接无效、过期或已使用", "unavailable", "available", "联系项目负责人重新发起确认");
        return row;
    }
    private void assertSamplePreparationCompleted(Sample sample) {
        for (String taskType : List.of("SAMPLE_PREPARE", "SAMPLE_MAKE")) {
            Task task = taskMapper.selectOne(Wrappers.<Task>lambdaQuery()
                    .eq(Task::getProjectId, sample.getProjectId())
                    .eq(Task::getTaskType, taskType)
                    .eq(Task::getTitle, ("SAMPLE_PREPARE".equals(taskType) ? "样品备料" : "样品制作") + sample.getSampleNo())
                    .last("LIMIT 1"));
            if (task == null || !"DONE".equals(task.getStatus())) {
                throw BusinessException.ruleBlock("SAMPLE_TASKS_PENDING", "样品备料和制作任务必须全部完成后才能提交客户确认", taskType, "DONE", "由生产人员完成样品备料和制作任务");
            }
        }
    }
    private void createSampleTasks(Sample sample) {
        createSampleTask(sample, "SAMPLE_PREPARE", "样品备料", requireTaskMember(sample.getProjectId(), TaskResponsibilityCatalog.forTaskType("SAMPLE_PREPARE")));

        createSampleTask(sample, "SAMPLE_MAKE", "样品制作", requireTaskMember(sample.getProjectId(), TaskResponsibilityCatalog.forTaskType("SAMPLE_MAKE")));

        createSampleTask(sample, "INSPECTION", "样品检验：", requireTaskMember(sample.getProjectId(), TaskResponsibilityCatalog.forTaskType("INSPECTION")));

        createSampleTask(sample, "SAMPLE_CONFIRM", "客户确认", requireTaskMember(sample.getProjectId(), TaskResponsibilityCatalog.forTaskType("SAMPLE_CONFIRM")));
    }
    private ProjectMember requireTaskMember(Long projectId, List<String> roles) {
        ProjectMember member = dataScope.findActiveMemberForResponsibilities(projectId, roles.toArray(String[]::new));

        if (member == null) throw BusinessException.ruleBlock("SAMPLE_TASK_ASSIGNEE", "样品流程缺少有效项目责任人", String.join(",", roles), "有效项目成员", "补充生产、质量和项目经理成员后重试");

        return member;
    }
    private void createSampleTask(Sample sample, String type, String titlePrefix, ProjectMember member) {
        Task task = new Task();
        task.setTenantId(TenantContext.tenantId());
        task.setProjectId(sample.getProjectId());
        task.setTaskNo(numbers.next("TASK"));
        task.setTaskType(type);
        task.setTitle(titlePrefix + sample.getSampleNo());
        task.setReferencedVersion(sample.getReferencedVersion());
        task.setReferencedDocVersionId(sample.getDocVersionId());
        task.setStatus("TODO");
        task.setAssigneeId(member.getUserId());
        task.setResponsibleName(member.getMemberName());
        task.setPlanStart(LocalDateTime.now().toLocalDate());
        task.setPlanFinish(sample.getPlanFinishDate());
        task.setVersion(0);
        taskMapper.insert(task);
        events.record(sample.getProjectId(), "TASK", task.getId(), "SAMPLE_TASK_CREATED", null, "TODO", task.getTitle());
    }
    private void completeSampleConfirmTask(Sample sample) {
        taskMapper.update(null, Wrappers.<Task>lambdaUpdate().eq(Task::getProjectId, sample.getProjectId()).eq(Task::getTaskType, "SAMPLE_CONFIRM").eq(Task::getTitle, "客户确认" + sample.getSampleNo()).ne(Task::getStatus, "DONE").set(Task::getStatus, "DONE").set(Task::getActualFinish, LocalDateTime.now())); }
    private void moveProject(Project project, String action, String targetStatus, String targetStage, String reason) {
        String before = project.getStatus();
        project.setStatus(targetStatus);
        project.setStage(targetStage);
        if (projectMapper.updateById(project) != 1) throw BusinessException.ruleBlock("OPTIMISTIC_LOCK", "项目已被其他用户修改", "stale", "latest", "刷新后重试");
        jdbc.update("INSERT INTO nso_project_status_history (tenant_id,project_id,before_status,after_status,action_code,reason,operator_id) VALUES (?,?,?,?,?,?,?)",
                TenantContext.tenantId(), project.getId(), before, targetStatus, action, reason, TenantContext.userId());
    }
    private ExternalTokenContext resolveExternalToken(String token) {
        String hash = sha256(token);
        ExternalTokenContext mapped = jdbc.query("SELECT et.tenant_id,et.legacy_confirmation_id,COALESCE(ei.name,cc.contact_name) confirmer FROM nso_external_token et JOIN nso_external_identity ei ON ei.id=et.identity_id AND ei.tenant_id=et.tenant_id JOIN nso_external_project_access epa ON epa.identity_id=ei.id AND epa.project_id=et.project_id AND epa.tenant_id=et.tenant_id JOIN crm_contact cc ON cc.id=ei.contact_id AND cc.tenant_id=ei.tenant_id AND cc.deleted=0 WHERE et.token_hash=? AND et.status='ACTIVE' AND et.expire_at>NOW() AND et.used_count<et.max_uses AND ei.status='ACTIVE' AND cc.status='ENABLED' AND epa.status='ACTIVE' AND (epa.valid_until IS NULL OR epa.valid_until>NOW()) LIMIT 1",
                rs -> rs.next() ? new ExternalTokenContext(rs.getLong("tenant_id"), rs.getLong("legacy_confirmation_id"), rs.getString("confirmer"), false) : null, hash);
        if (mapped != null) return mapped;
        Integer mappedTokenCount = jdbc.queryForObject("SELECT COUNT(*) FROM nso_external_token WHERE token_hash=?", Integer.class, hash);
        if (mappedTokenCount != null && mappedTokenCount > 0) {
            throw BusinessException.ruleBlock("CONFIRMATION_TOKEN", "确认链接无效、过期或已使用", "unavailable", "available", "联系项目负责人重新发起确认");
        }
        ExternalTokenContext legacy = jdbc.query("SELECT tenant_id,id,COALESCE(confirmer,contact,'客户联系人') FROM nso_sample_confirm WHERE token=? AND expire_at>NOW() AND used_count<max_use_count AND used_flag=0 AND deleted=0 ORDER BY id DESC LIMIT 1",
                rs -> rs.next() ? new ExternalTokenContext(rs.getLong(1), rs.getLong(2), rs.getString(3), true) : null, token);
        if (legacy != null) return legacy;
        throw BusinessException.ruleBlock("CONFIRMATION_TOKEN", "确认链接无效、过期或已使用", "unavailable", "available", "联系项目负责人重新发起确认");
    }
    private void ensureConfirmationMatches(ExternalTokenContext context, SampleConfirm confirmation) {
        if (context.confirmationId() != confirmation.getId()) throw BusinessException.ruleBlock("CONFIRMATION_TOKEN", "确认链接校验失败", "mismatched", "linked confirmation", "联系项目负责人重新发起确认");
    }
    // 外部确认令牌上下文。
    private record ExternalTokenContext(long tenantId, long confirmationId, String confirmer, boolean legacyFallback) {

    }
    private String sha256(String token) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("cannot hash confirmation token", exception);
        }
    }
    private Sample requireSample(Long id) { Sample row = sampleMapper.selectById(id);
        if (row == null) throw new BusinessException("样品不存在");
        dataScope.requireAccess(row.getProjectId());
        return row;
    }
    private Project requireProject(Long id) {
        Project row = projectMapper.selectById(id);
        if (row == null) throw new BusinessException("项目不存在");
        dataScope.requireAccess(id);
        return row;
    }

    private void update(Sample sample) {
        if (sampleMapper.updateById(sample) != 1) throw BusinessException.ruleBlock("OPTIMISTIC_LOCK", "样品已被其他用户修改", "stale", "latest", "刷新后重试");
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
    private String projectNo(Long projectId) {
        return requireProject(projectId).getProjectNo();
    }

    private SampleDto toDto(Sample row) {
        return new SampleDto(row.getId(),
                row.getProjectId(),
                projectNo(row.getProjectId()),
                row.getSampleNo(),
                row.getPurpose(),
                row.getQuantity(),
                row.getPlanFinishDate(),
                row.getReferencedVersion(),
                row.getStatus(),
                row.getConfirmConclusion(),
                row.getResponsibleName(),
                row.getIssueSummary());
    }
    private CustomerSampleDto toCustomerDto(Sample row) {
        return new CustomerSampleDto(
                row.getId(),
                row.getProjectId(),
                row.getSampleNo(),
                row.getPurpose(),
                row.getQuantity(),
                row.getPlanFinishDate(),
                row.getReferencedVersion(),
                row.getStatus(),
                row.getConfirmConclusion());
    }
    private PublicSampleConfirmationDto toPublicDto(Sample row) {
        return new PublicSampleConfirmationDto(
                row.getId(),
                row.getSampleNo(),
                row.getPurpose(),
                row.getQuantity(),
                row.getPlanFinishDate(),
                row.getReferencedVersion(),
                row.getStatus(),
                row.getConfirmConclusion());
    }
    private SampleCheckDto toCheckDto(SampleCheck row) {
        return new SampleCheckDto(
                row.getId(),
                row.getSampleId(),
                row.getCheckItem(),
                row.getMeasuredValue(),
                row.getResult(),
                row.getIssueSummary(),
                row.getCorrectiveAction(),
                row.getCheckerName(),
                row.getCheckedAt());
    }

    private ConfirmationTokenDto toTokenDto(SampleConfirm row) {
        return new ConfirmationTokenDto(
                row.getToken(),
                row.getSampleId(),
                row.getExpireAt(),
                row.getMaxUseCount(),
                row.getUsedCount(),
                row.getUsedCount() >= row.getMaxUseCount() ? "USED" : "ACTIVE");
    }

    private SpecialReleaseDto releaseById(Long id) {
        return jdbc.queryForObject("SELECT id,project_id,sample_id,applicant_user_id,approver_user_id,release_scope,valid_until,risk_statement,reason,status FROM nso_special_release WHERE id=? AND tenant_id=?",
                (rs, rowNum) -> new SpecialReleaseDto(rs.getLong("id"),
                        rs.getLong("project_id"),
                        rs.getObject("sample_id", Long.class),
                        rs.getLong("applicant_user_id"),
                        rs.getObject("approver_user_id", Long.class),
                        rs.getString("release_scope"),
                        rs.getTimestamp("valid_until").toLocalDateTime(),
                        rs.getString("risk_statement"),
                        rs.getString("reason"),
                        rs.getString("status")), id, TenantContext.tenantId());
    }
}
