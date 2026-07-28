package com.nso.business.sample.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nso.business.core.BusinessLockPort;
import com.nso.business.core.NsoDtos.*;
import com.nso.business.core.TenantContext;
import com.nso.business.project.domain.Project;
import com.nso.business.project.domain.ProjectMember;
import com.nso.business.project.mapper.ProjectMapper;
import com.nso.business.project.mapper.ProjectMemberMapper;
import com.nso.business.project.service.ProjectDataScope;
import com.nso.business.support.BusinessEventService;
import com.nso.business.task.domain.Task;
import com.nso.business.task.mapper.TaskMapper;
import com.nso.business.sample.domain.Sample;
import com.nso.business.sample.domain.SampleCheck;
import com.nso.business.sample.domain.SampleConfirm;
import com.nso.business.sample.mapper.SampleCheckMapper;
import com.nso.business.sample.mapper.SampleConfirmMapper;
import com.nso.business.sample.mapper.SampleMapper;
import com.nso.business.sample.service.ISampleService;
import com.nso.common.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class SampleServiceImpl implements ISampleService {
    private final SampleMapper sampleMapper;
    private final SampleCheckMapper sampleCheckMapper;
    private final SampleConfirmMapper sampleConfirmMapper;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final BusinessLockPort businessLock;
    private final ProjectDataScope dataScope;
    private final BusinessEventService events;
    private final TaskMapper taskMapper;
    private final JdbcTemplate jdbc;

    public SampleServiceImpl(SampleMapper sampleMapper, SampleCheckMapper sampleCheckMapper,
                              SampleConfirmMapper sampleConfirmMapper, ProjectMapper projectMapper, ProjectMemberMapper projectMemberMapper, BusinessLockPort businessLock, ProjectDataScope dataScope, BusinessEventService events, TaskMapper taskMapper, JdbcTemplate jdbc) {
        this.sampleMapper = sampleMapper; this.sampleCheckMapper = sampleCheckMapper;
        this.sampleConfirmMapper = sampleConfirmMapper; this.projectMapper = projectMapper; this.projectMemberMapper = projectMemberMapper;
        this.businessLock = businessLock;
        this.dataScope = dataScope; this.events = events; this.taskMapper = taskMapper; this.jdbc = jdbc;
    }

    @Override
    public PageResult<SampleDto> list(Long projectId) {
        List<Long> visibleIds = dataScope.visibleProjectIds();
        if (visibleIds != null && visibleIds.isEmpty()) return new PageResult<>(List.of(), 0);
        List<SampleDto> rows = sampleMapper.selectList(Wrappers.<Sample>lambdaQuery().eq(projectId != null, Sample::getProjectId, projectId)
                        .in(visibleIds != null, Sample::getProjectId, visibleIds == null ? List.of() : visibleIds).orderByDesc(Sample::getId))
                .stream().map(this::toDto).toList(); return new PageResult<>(rows, rows.size());
    }

    @Override
    public SampleDto get(Long sampleId) { return toDto(requireSample(sampleId)); }

    @Override
    @Transactional
    public SampleDto create(SampleRequest request) {
        if (request == null || request.projectId() == null || blank(request.purpose())) throw new BusinessException("项目和样品用途不能为空");
        Project project = requireProject(request.projectId());
        dataScope.requireProjectRole(project.getId(), "PROJECT_MANAGER");
        if (!"TECH_PUBLISHED".equals(project.getStatus())) throw BusinessException.ruleBlock("SAMPLE_TECHNICAL_PACKAGE", "技术包未发布，不能创建样品", project.getStatus(), "TECH_PUBLISHED", "先发布技术包");
        Sample sample = new Sample(); sample.setTenantId(TenantContext.tenantId()); sample.setProjectId(request.projectId()); sample.setSampleNo("SMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()); sample.setPurpose(request.purpose()); sample.setQuantity(request.quantity() == null || request.quantity() < 1 ? 1 : request.quantity()); sample.setPlanFinishDate(request.planFinishDate()); sample.setReferencedVersion(request.referencedVersion()); sample.setDocVersionId(project.getCurrentDocVersionId()); sample.setStatus("DRAFT"); sample.setConfirmConclusion("PENDING"); sample.setResponsibleName(request.responsibleName()); sample.setCreatedBy(TenantContext.userId()); sampleMapper.insert(sample);
        createSampleTasks(sample);
        project.setSampleStatus("DRAFT"); projectMapper.updateById(project); events.record(project.getId(), "SAMPLE", sample.getId(), "SAMPLE_CREATED", null, "DRAFT", "已创建样品单 " + sample.getSampleNo()); return toDto(sample);
    }

    @Override
    @Transactional
    public SampleDto submitConfirm(Long sampleId) {
        Sample sample = requireSample(sampleId);
        dataScope.requireProjectRole(sample.getProjectId(), "PROJECT_MANAGER");
        if (!"DRAFT".equals(sample.getStatus()) && !"CHECKED".equals(sample.getStatus())) throw BusinessException.ruleBlock("SAMPLE_STATUS", "当前样品不能提交客户确认", sample.getStatus(), "DRAFT/CHECKED", "完成样品制作和检验");
        sample.setStatus("WAIT_CUSTOMER_CONFIRM"); sample.setConfirmConclusion("PENDING"); update(sample);
        Project project = requireProject(sample.getProjectId()); project.setSampleStatus("WAIT_CUSTOMER_CONFIRM"); projectMapper.updateById(project); events.record(sample.getProjectId(), "SAMPLE", sampleId, "SAMPLE_SUBMITTED_CONFIRM", sample.getStatus(), "WAIT_CUSTOMER_CONFIRM", "样品已提交客户确认"); return toDto(sample);
    }

    @Override
    @Transactional
    public SampleDto confirm(Long sampleId, SampleConfirmRequest request) {
        return businessLock.withLock("sample:confirm:" + sampleId, () -> {
            Sample sample = requireSample(sampleId); dataScope.requireProjectRole(sample.getProjectId(), "PROJECT_MANAGER"); applyConfirmation(sample, request); return toDto(sample);
        });
    }

    @Override
    public PageResult<CustomerSampleDto> customerSamples(Long projectId) {
        List<Long> visibleIds = dataScope.visibleProjectIds();
        if (visibleIds != null && visibleIds.isEmpty()) return new PageResult<>(List.of(), 0);
        List<CustomerSampleDto> rows = sampleMapper.selectList(Wrappers.<Sample>lambdaQuery()
                        .eq(projectId != null, Sample::getProjectId, projectId)
                        .in(visibleIds != null, Sample::getProjectId, visibleIds == null ? List.of() : visibleIds)
                        .orderByDesc(Sample::getId))
                .stream().map(this::toCustomerDto).toList();
        return new PageResult<>(rows, rows.size());
    }

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

    @Override
    public PageResult<SampleCheckDto> checks(Long sampleId) {
        requireSample(sampleId); List<SampleCheckDto> rows = sampleCheckMapper.selectList(Wrappers.<SampleCheck>lambdaQuery().eq(SampleCheck::getSampleId, sampleId).orderByDesc(SampleCheck::getCheckedAt)).stream().map(this::toCheckDto).toList(); return new PageResult<>(rows, rows.size());
    }

    @Override
    @Transactional
    public SampleCheckDto addCheck(Long sampleId, SampleCheckRequest request) {
        Sample sample = requireSample(sampleId); if (request == null || blank(request.checkItem()) || blank(request.result())) throw new BusinessException("检验项目和结果不能为空");
        dataScope.requireProjectRole(sample.getProjectId(), "QUALITY");
        if (sample.getCreatedBy() != null && sample.getCreatedBy().equals(TenantContext.userId())) throw BusinessException.ruleBlock("SAMPLE_DUTY_SEPARATION", "样品制作人员不能填写质量检验结论", String.valueOf(TenantContext.userId()), "独立质量人员", "由质量人员完成检验");
        SampleCheck check = new SampleCheck(); check.setTenantId(TenantContext.tenantId()); check.setSampleId(sampleId); check.setCheckItem(request.checkItem()); check.setMeasuredValue(request.measuredValue()); check.setResult(request.result()); check.setIssueSummary(request.issueSummary()); check.setCorrectiveAction(request.correctiveAction()); check.setCheckerName(request.checkerName()); check.setCheckedAt(LocalDateTime.now()); sampleCheckMapper.insert(check);
        if ("PASS".equals(request.result()) && Set.of("DRAFT", "REWORKING").contains(sample.getStatus())) { String before = sample.getStatus(); sample.setStatus("CHECKED"); sample.setQualityConfirmedBy(TenantContext.userId()); update(sample); events.record(sample.getProjectId(), "SAMPLE", sampleId, "SAMPLE_CHECKED", before, "CHECKED", "样品检验通过"); }
        if ("FAIL".equals(request.result()) && Set.of("DRAFT", "CHECKED").contains(sample.getStatus())) { String before = sample.getStatus(); sample.setStatus("REWORKING"); sample.setIssueSummary(request.issueSummary()); update(sample); events.record(sample.getProjectId(), "SAMPLE", sampleId, "SAMPLE_CHECK_FAILED", before, "REWORKING", "样品检验不合格，进入整改"); }
        return toCheckDto(check);
    }

    @Override
    @Transactional
    public ConfirmationTokenDto createConfirmToken(Long sampleId, int validDays, int maxUseCount) {
        Sample sample = requireSample(sampleId); if (!"WAIT_CUSTOMER_CONFIRM".equals(sample.getStatus())) throw BusinessException.ruleBlock("SAMPLE_STATUS", "样品尚未进入客户确认状态", sample.getStatus(), "WAIT_CUSTOMER_CONFIRM", "先提交客户确认");
        dataScope.requireProjectRole(sample.getProjectId(), "PROJECT_MANAGER");
        SampleConfirm row = new SampleConfirm(); row.setTenantId(TenantContext.tenantId()); row.setSampleId(sampleId); row.setToken(UUID.randomUUID().toString().replace("-", "")); row.setExpireAt(LocalDateTime.now().plusDays(Math.max(1, validDays))); row.setUsedFlag(0); row.setMaxUseCount(Math.max(1, maxUseCount)); row.setUsedCount(0); sampleConfirmMapper.insert(row); return toTokenDto(row);
    }

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
        events.record(sample.getProjectId(), "SPECIAL_RELEASE", id, "SPECIAL_RELEASE_APPLIED", null, "PENDING", "已申请样品特殊放行");
        return releaseById(id);
    }

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
        events.record(((Number) release.get("projectId")).longValue(), "SPECIAL_RELEASE", releaseId, "SPECIAL_RELEASE_APPROVED", "PENDING", "APPROVED", "特殊放行已批准");
        return releaseById(releaseId);
    }

    @Override
    public PublicSampleConfirmationDto publicConfirmation(String token) {
        return TenantContext.withTenant(resolveTokenTenant(token), () -> {
            SampleConfirm confirmation = requireAvailableToken(token); return toPublicDto(requireSample(confirmation.getSampleId()));
        });
    }

    @Override
    @Transactional
    public PublicSampleConfirmationDto submitPublicConfirmation(String token, SampleConfirmRequest request) {
        long tokenTenant = resolveTokenTenant(token);
        return businessLock.withLock("sample:public-confirm:" + token, () -> TenantContext.withTenant(tokenTenant, () -> {
            SampleConfirm confirmation = requireAvailableToken(token);
            int changed = sampleConfirmMapper.update(null, Wrappers.<SampleConfirm>lambdaUpdate().eq(SampleConfirm::getId, confirmation.getId()).eq(SampleConfirm::getUsedCount, confirmation.getUsedCount()).setSql("used_count = used_count + 1").set(SampleConfirm::getUsedFlag, confirmation.getUsedCount() + 1 >= confirmation.getMaxUseCount() ? 1 : 0).set(SampleConfirm::getConclusion, request == null ? null : request.conclusion()).set(SampleConfirm::getOpinion, request == null ? null : request.opinion()).set(SampleConfirm::getConfirmer, request == null ? null : request.confirmer()).set(SampleConfirm::getSubmittedAt, LocalDateTime.now()));
            if (changed != 1) throw BusinessException.ruleBlock("CONFIRMATION_TOKEN", "确认链接已被使用", "used", "available", "刷新确认页面");
            Sample sample = requireSample(confirmation.getSampleId()); applyConfirmation(sample, request); return toPublicDto(sample);
        }));
    }

    private void applyConfirmation(Sample sample, SampleConfirmRequest request) {
        if (request == null || blank(request.conclusion())) throw new BusinessException("确认结论不能为空");
        String conclusion = request.conclusion().toUpperCase(); if (!Set.of("PASS", "CONDITIONAL_PASS", "REJECT", "WAIT_SUPPLEMENT").contains(conclusion)) throw new BusinessException("确认结论只能是 PASS、CONDITIONAL_PASS、REJECT 或 WAIT_SUPPLEMENT");
        if (blank(request.opinion())) throw new BusinessException("客户确认意见不能为空");
        if (!"WAIT_CUSTOMER_CONFIRM".equals(sample.getStatus())) throw BusinessException.ruleBlock("SAMPLE_STATUS", "样品当前不允许确认", sample.getStatus(), "WAIT_CUSTOMER_CONFIRM", "刷新样品状态");
        boolean proxy = TenantContext.userId() != null && !TenantContext.hasAnyRole("customer_confirm");
        if (proxy && request.evidenceFileId() == null) throw BusinessException.ruleBlock("SAMPLE_PROXY_EVIDENCE", "内部代录客户结论必须上传凭证", "missing", "evidenceFileId", "上传客户确认凭证后重试");
        String before = sample.getStatus(); sample.setConfirmConclusion(conclusion); sample.setIssueSummary(request.opinion()); sample.setStatus(switch (conclusion) { case "PASS" -> "CONFIRMED"; case "CONDITIONAL_PASS" -> "CONDITIONAL_PASS"; case "WAIT_SUPPLEMENT" -> "WAIT_SUPPLEMENT"; default -> "REJECTED"; });
        if (proxy) { sample.setProxyConfirmation(1); sample.setProxyConfirmedBy(TenantContext.userId()); }
        update(sample);
        Project project = requireProject(sample.getProjectId()); project.setSampleStatus(sample.getStatus()); if ("CONFIRMED".equals(sample.getStatus())) { project.setStage("EXECUTION"); } projectMapper.updateById(project);
        if ("REJECTED".equals(sample.getStatus())) {
            ProjectMember productionMember = requireTaskMember(sample.getProjectId(), "PRODUCTION", "FIELD_USER");
            createSampleTask(sample, "SAMPLE_REWORK", "样品驳回整改：", productionMember);
        }
        if (Set.of("CONFIRMED", "CONDITIONAL_PASS", "REJECTED").contains(sample.getStatus())) completeSampleConfirmTask(sample);
        if (request.evidenceFileId() != null) {
            Integer fileCount = jdbc.queryForObject("SELECT COUNT(*) FROM nso_file_object WHERE id=? AND tenant_id=? AND project_id=?", Integer.class, request.evidenceFileId(), TenantContext.tenantId(), sample.getProjectId());
            if (fileCount == null || fileCount == 0) throw new BusinessException("确认凭证不存在或无权使用");
            jdbc.update("INSERT IGNORE INTO nso_attachment_relation (tenant_id,project_id,business_type,business_id,file_id,evidence_type) VALUES (?,?,?,?,?,'SAMPLE_CONFIRM')", TenantContext.tenantId(), sample.getProjectId(), "SAMPLE", sample.getId(), request.evidenceFileId());
        }
        events.record(sample.getProjectId(), "SAMPLE", sample.getId(), "SAMPLE_CONFIRMED", before, sample.getStatus(), "客户确认结论：" + conclusion);
    }

    private SampleConfirm requireAvailableToken(String token) { SampleConfirm row = sampleConfirmMapper.selectOne(Wrappers.<SampleConfirm>lambdaQuery().eq(SampleConfirm::getToken, token)); if (row == null || row.getExpireAt().isBefore(LocalDateTime.now()) || row.getUsedCount() >= row.getMaxUseCount()) throw BusinessException.ruleBlock("CONFIRMATION_TOKEN", "确认链接无效、过期或已使用", "unavailable", "available", "联系项目负责人重新发起确认"); return row; }
    private void createSampleTasks(Sample sample) { createSampleTask(sample, "SAMPLE_PREPARE", "样品备料：", requireTaskMember(sample.getProjectId(), "PRODUCTION", "FIELD_USER")); createSampleTask(sample, "SAMPLE_MAKE", "样品制作：", requireTaskMember(sample.getProjectId(), "PRODUCTION", "FIELD_USER")); createSampleTask(sample, "INSPECTION", "样品检验：", requireTaskMember(sample.getProjectId(), "QUALITY")); createSampleTask(sample, "SAMPLE_CONFIRM", "客户确认：", requireTaskMember(sample.getProjectId(), "PROJECT_MANAGER")); }
    private ProjectMember requireTaskMember(Long projectId, String... roles) { ProjectMember member = projectMemberMapper.selectOne(Wrappers.<ProjectMember>lambdaQuery().eq(ProjectMember::getProjectId, projectId).eq(ProjectMember::getStatus, "ACTIVE").in(ProjectMember::getProjectRole, List.of(roles)).orderByAsc(ProjectMember::getId).last("LIMIT 1")); if (member == null || member.getUserId() == null) throw BusinessException.ruleBlock("SAMPLE_TASK_ASSIGNEE", "样品流程缺少有效项目责任人", String.join(",", roles), "有效项目成员", "补充生产、质量和项目经理成员后重试"); return member; }
    private void createSampleTask(Sample sample, String type, String titlePrefix, ProjectMember member) { Task task = new Task(); task.setTenantId(TenantContext.tenantId()); task.setProjectId(sample.getProjectId()); task.setTaskNo("TSK-SMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()); task.setTaskType(type); task.setTitle(titlePrefix + sample.getSampleNo()); task.setReferencedVersion(sample.getReferencedVersion()); task.setReferencedDocVersionId(sample.getDocVersionId()); task.setStatus("TODO"); task.setAssigneeId(member.getUserId()); task.setResponsibleName(member.getMemberName()); task.setPlanStart(LocalDateTime.now().toLocalDate()); task.setPlanFinish(sample.getPlanFinishDate()); task.setVersion(0); taskMapper.insert(task); events.record(sample.getProjectId(), "TASK", task.getId(), "SAMPLE_TASK_CREATED", null, "TODO", task.getTitle()); }
    private void completeSampleConfirmTask(Sample sample) { taskMapper.update(null, Wrappers.<Task>lambdaUpdate().eq(Task::getProjectId, sample.getProjectId()).eq(Task::getTaskType, "SAMPLE_CONFIRM").eq(Task::getTitle, "客户确认：" + sample.getSampleNo()).ne(Task::getStatus, "DONE").set(Task::getStatus, "DONE").set(Task::getActualFinish, LocalDateTime.now())); }
    private long resolveTokenTenant(String token) { Long tenantId = jdbc.query("SELECT tenant_id FROM nso_sample_confirm WHERE token=? ORDER BY id DESC LIMIT 1", rs -> rs.next() ? rs.getLong(1) : null, token); if (tenantId == null) throw BusinessException.ruleBlock("CONFIRMATION_TOKEN", "确认链接无效、过期或已使用", "unavailable", "available", "联系项目负责人重新发起确认"); return tenantId; }
    private Sample requireSample(Long id) { Sample row = sampleMapper.selectById(id); if (row == null) throw new BusinessException("样品不存在"); dataScope.requireAccess(row.getProjectId()); return row; }
    private Project requireProject(Long id) { Project row = projectMapper.selectById(id); if (row == null) throw new BusinessException("项目不存在"); dataScope.requireAccess(id); return row; }
    private void update(Sample sample) { if (sampleMapper.updateById(sample) != 1) throw BusinessException.ruleBlock("OPTIMISTIC_LOCK", "样品已被其他用户修改", "stale", "latest", "刷新后重试"); }
    private boolean blank(String value) { return value == null || value.isBlank(); }
    private String projectNo(Long projectId) { return requireProject(projectId).getProjectNo(); }
    private SampleDto toDto(Sample row) { return new SampleDto(row.getId(), row.getProjectId(), projectNo(row.getProjectId()), row.getSampleNo(), row.getPurpose(), row.getQuantity(), row.getPlanFinishDate(), row.getReferencedVersion(), row.getStatus(), row.getConfirmConclusion(), row.getResponsibleName(), row.getIssueSummary()); }
    private CustomerSampleDto toCustomerDto(Sample row) { return new CustomerSampleDto(row.getId(), row.getProjectId(), row.getSampleNo(), row.getPurpose(), row.getQuantity(), row.getPlanFinishDate(), row.getReferencedVersion(), row.getStatus(), row.getConfirmConclusion()); }
    private PublicSampleConfirmationDto toPublicDto(Sample row) { return new PublicSampleConfirmationDto(row.getId(), row.getSampleNo(), row.getPurpose(), row.getQuantity(), row.getPlanFinishDate(), row.getReferencedVersion(), row.getStatus(), row.getConfirmConclusion()); }
    private SampleCheckDto toCheckDto(SampleCheck row) { return new SampleCheckDto(row.getId(), row.getSampleId(), row.getCheckItem(), row.getMeasuredValue(), row.getResult(), row.getIssueSummary(), row.getCorrectiveAction(), row.getCheckerName(), row.getCheckedAt()); }
    private ConfirmationTokenDto toTokenDto(SampleConfirm row) { return new ConfirmationTokenDto(row.getToken(), row.getSampleId(), row.getExpireAt(), row.getMaxUseCount(), row.getUsedCount(), row.getUsedCount() >= row.getMaxUseCount() ? "USED" : "ACTIVE"); }
    private SpecialReleaseDto releaseById(Long id) { return jdbc.queryForObject("SELECT id,project_id,sample_id,applicant_user_id,approver_user_id,release_scope,valid_until,risk_statement,reason,status FROM nso_special_release WHERE id=? AND tenant_id=?", (rs, rowNum) -> new SpecialReleaseDto(rs.getLong("id"), rs.getLong("project_id"), rs.getObject("sample_id", Long.class), rs.getLong("applicant_user_id"), rs.getObject("approver_user_id", Long.class), rs.getString("release_scope"), rs.getTimestamp("valid_until").toLocalDateTime(), rs.getString("risk_statement"), rs.getString("reason"), rs.getString("status")), id, TenantContext.tenantId()); }
}
