package com.nso.business.change.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nso.business.change.domain.ChangeImpact;
import com.nso.business.change.domain.ChangeOrder;
import com.nso.business.change.mapper.ChangeImpactMapper;
import com.nso.business.change.mapper.ChangeOrderMapper;
import com.nso.business.change.service.ChangeImpactAnalysisCoordinator;
import com.nso.business.change.service.ChangeQueryCoordinator;
import com.nso.business.change.service.IChangeService;
import com.nso.business.core.BusinessLockPort;
import com.nso.business.core.NsoDtos.*;
import com.nso.business.core.TenantContext;
import com.nso.business.message.service.IMessageService;
import com.nso.business.project.domain.Project;
import com.nso.business.project.service.ProjectDataScope;
import com.nso.business.support.BusinessEventService;
import com.nso.business.support.BusinessNumberService;
import com.nso.business.support.OperationConfirmationService;
import com.nso.business.support.approval.IApprovalService;
import com.nso.shared.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

// 变更协同服务。
@Service

// 变更管理 服务层处理
public class ChangeServiceImpl implements IChangeService {
    // 变更Order数据映射
    private final ChangeOrderMapper changes;
    // 变更影响数据映射
    private final ChangeImpactMapper impacts;
    // 变更影响分析协调器
    private final ChangeImpactAnalysisCoordinator impactAnalysis;
    // 变更查询协调器
    private final ChangeQueryCoordinator queries;
    // 消息服务
    private final IMessageService messages;
    // 业务事件服务
    private final BusinessEventService events;
    // JDBC模板
    private final JdbcTemplate jdbc;
    // 业务锁端口
    private final BusinessLockPort locks;
    // 项目数据范围
    private final ProjectDataScope dataScope;
    // 业务Number服务
    private final BusinessNumberService numbers;
    // 操作确认服务
    private final OperationConfirmationService confirmations;
    // 审批服务
    private final IApprovalService approvals;

    public ChangeServiceImpl(
            ChangeOrderMapper changes,
            ChangeImpactMapper impacts,
            ChangeImpactAnalysisCoordinator impactAnalysis,
            ChangeQueryCoordinator queries,
            IMessageService messages,
            BusinessEventService events,
            JdbcTemplate jdbc,
            BusinessLockPort locks,
            ProjectDataScope dataScope,
            BusinessNumberService numbers,
            OperationConfirmationService confirmations,
            IApprovalService approvals) {
        this.changes = changes;
        this.impacts = impacts;
        this.impactAnalysis = impactAnalysis;
        this.queries = queries;
        this.messages = messages;
        this.events = events;
        this.jdbc = jdbc;
        this.locks = locks;
        this.dataScope = dataScope;
        this.numbers = numbers;
        this.confirmations = confirmations;
        this.approvals = approvals;
    }

    // 查询可见变更单。
    @Override
    public PageResult<ChangeOrderDto> list(Long projectId) {
        return queries.list(projectId);
    }

    // 按条件查询可见变更单。
    @Override
    public PageResult<ChangeOrderDto> list(Long projectId, String status, String changeType, PageQuery pageQuery) {
        return queries.list(projectId, status, changeType, pageQuery);
    }

    // 查询变更单详情。
    @Override
    public ChangeOrderDto get(Long changeId) {
        return queries.toDto(queries.requireChange(changeId));
    }

    // 创建待分析的变更单。
    @Override
    @Transactional
    public ChangeOrderDto create(ChangeRequest request) {
        if (request == null
                || request.projectId() == null
                || blank(request.changeType())
                || blank(request.afterContent())) {
            throw new BusinessException("项目、变更类型和变更后内容不能为空");
        }
        Project project = requireProject(request.projectId());
        dataScope.requireProjectRole(project.getId(), "PROJECT_MANAGER", "TECHNICAL", "PROCESS", "PURCHASER", "PRODUCTION");
        if ("CLOSED".equals(project.getStatus()) || "COMPLETED".equals(project.getStatus())) {
            throw BusinessException.ruleBlock("PROJECT_STATUS", "已关闭项目不能发起变更", project.getStatus(), "进行中", "新建补充项目或恢复后处理");
        }
        ChangeOrder row = new ChangeOrder();
        row.setTenantId(TenantContext.tenantId());

        row.setProjectId(project.getId());
        row.setApplicantUserId(TenantContext.userId());

        row.setChangeNo(numbers.next("CHANGE"));

        row.setChangeType(request.changeType());
        row.setUrgency(blank(request.urgency()) ? "NORMAL" : request.urgency());

        row.setBeforeContent(request.beforeContent());
        row.setAfterContent(request.afterContent());
        row.setReason(request.reason());

        row.setStatus("WAIT_IMPACT");
        row.setDelayDays(0);
        row.setReworkQty(0);
        changes.insert(row);

        events.record(project.getId(), "CHANGE", row.getId(), "CHANGE_CREATED", null, row.getStatus(), "已发起变更 " + row.getChangeNo());
        return toDto(row);
    }

    // 分析影响并准备审批节点。
    @Override
    @Transactional
    public List<ChangeImpactDto> analyze(Long changeId) {
        ChangeOrder change = requireChange(changeId);
        dataScope.requireProjectRole(change.getProjectId(), "TECHNICAL", "PROCESS");
        if (!Set.of("WAIT_IMPACT", "ANALYZED").contains(change.getStatus())) {
            throw BusinessException.ruleBlock("CHANGE_STATUS", "当前变更不能执行影响分析", change.getStatus(), "WAIT_IMPACT/ANALYZED", "刷新状态后重试");
        }
        if (impacts.selectCount(Wrappers.<ChangeImpact>lambdaQuery()
                .eq(ChangeImpact::getChangeId, changeId)) == 0) {
            generateImpacts(change);
        }
        if (impacts.selectCount(Wrappers.<ChangeImpact>lambdaQuery()
                .eq(ChangeImpact::getChangeId, changeId)) == 0) {
            throw BusinessException.ruleBlock("CHANGE_IMPACT", "未识别到可执行影响项", "0", ">0", "补充关联技术或任务后重新分析");
        }
        seedApprovals(change);
        String before = change.getStatus();
        change.setStatus("WAIT_APPROVAL");
        update(change);
        events.record(change.getProjectId(), "CHANGE", changeId, "CHANGE_ANALYZED", before, change.getStatus(), "已生成图纸、BOM、采购、生产、样品和检验影响项");
        return impacts(changeId);
    }

    // 读取变更影响项。
    @Override
    public List<ChangeImpactDto> impacts(Long changeId) {
        return queries.impacts(changeId);
    }

    // 分页查询变更影响项。
    @Override
    public PageResult<ChangeImpactDto> impactsPage(Long changeId, PageQuery pageQuery) {
        return queries.impactsPage(changeId, pageQuery);
    }

    // 使用默认结论审批通过变更单。
    @Override
    public ChangeOrderDto approve(Long changeId) {
        return approve(changeId, new ChangeApprovalRequest("APPROVED", null));
    }

    // 审批通过后进入执行阶段。
    @Override
    @Transactional
    public ChangeOrderDto approve(Long changeId, ChangeApprovalRequest request) {
        return locks.withLock("change:approve:" + changeId, () -> {
            ChangeOrder change = requireChange(changeId);
            if (!"WAIT_APPROVAL".equals(change.getStatus())) {
                throw BusinessException.ruleBlock("CHANGE_STATUS", "必须先完成影响分析才能审批", change.getStatus(), "WAIT_APPROVAL", "先执行影响分析");
            }
            String decision = request == null || blank(request.decision()) ? "APPROVED" : request.decision().toUpperCase();
            if (!Set.of("APPROVED", "REJECTED").contains(decision)) {
                throw new BusinessException("审批结论只能为 APPROVED 或 REJECTED");
            }
            if (isHighRisk(change) && change.getApplicantUserId() != null && change.getApplicantUserId().equals(TenantContext.userId())) {
                throw BusinessException.ruleBlock("CHANGE_DUTY_SEPARATION", "高风险变更申请人不能参与本变更审批", String.valueOf(TenantContext.userId()), "独立审批人", "由其他审批节点处理");
            }
            String node = resolveApprovalNode(changeId);
            dataScope.requireProjectRole(change.getProjectId(), node);
            if ("REJECTED".equals(decision)) {
                jdbc.update("UPDATE nso_change_approval SET decision='REJECTED', opinion=?, approver_id=?, approver_name=?, approved_at=NOW() WHERE tenant_id=? AND change_id=? AND node_code=? AND decision='PENDING'",
                        request == null ? null : request.opinion(), TenantContext.userId(), TenantContext.username(), TenantContext.tenantId(), changeId, node);
                approvals.syncChangeApprovals(changeId);
                change.setStatus("DRAFT");
                update(change);
                events.record(change.getProjectId(), "CHANGE", changeId, "CHANGE_REJECTED", "WAIT_APPROVAL", "DRAFT", "变更审批节点 " + node + " 驳回");
                return toDto(change);
            }
            if (TenantContext.hasAnyRole("admin")) {
                jdbc.update("UPDATE nso_change_approval SET decision='APPROVED', opinion=?, approver_id=?, approver_name=?, approved_at=NOW() WHERE tenant_id=? AND change_id=? AND decision='PENDING'",
                        request == null ? null : request.opinion(), TenantContext.userId(), TenantContext.username(), TenantContext.tenantId(), changeId);
            } else {
                jdbc.update("UPDATE nso_change_approval SET decision='APPROVED', opinion=?, approver_id=?, approver_name=?, approved_at=NOW() WHERE tenant_id=? AND change_id=? AND node_code=? AND decision='PENDING'",
                        request == null ? null : request.opinion(), TenantContext.userId(), TenantContext.username(), TenantContext.tenantId(), changeId, node);
            }
            approvals.syncChangeApprovals(changeId);
            Integer pending = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM nso_change_approval WHERE tenant_id=? AND change_id=? AND decision='PENDING'",
                    Integer.class,
                    TenantContext.tenantId(),
                    changeId);
            if (pending != null && pending > 0) {
                return toDto(change);
            }
            String before = change.getStatus();
            change.setStatus("EXECUTING");
            change.setApprovalNode("ALL_APPROVED");
            update(change);
            blockAffectedTasks(changeId);
            messages.notifyProject(change.getProjectId(), "CHANGE_APPROVED", "变更已批准",
                    change.getChangeNo() + " 已批准，请处理影响项", "CHANGE", changeId);
            events.record(change.getProjectId(), "CHANGE", changeId, "CHANGE_APPROVED", before, change.getStatus(), "变更审批完成并已通知责任人");
            return toDto(change);
        });
    }

    // 提交单项执行反馈。
    @Override
    @Transactional
    public ChangeImpactDto feedbackImpact(Long impactId, ChangeFeedbackRequest request) {
        ChangeImpact impact = impacts.selectById(impactId);
        if (impact == null) {
            throw new BusinessException("变更影响项不存在");
        }

        ChangeOrder change = requireChange(impact.getChangeId());
        dataScope.requireProjectRole(
                change.getProjectId(),
                projectRoleForImpact(impact.getDepartmentName()));

        if (!"EXECUTING".equals(change.getStatus())) {
            throw BusinessException.ruleBlock(
                    "CHANGE_STATUS",
                    "变更未批准，不能提交执行反馈",
                    change.getStatus(),
                    "EXECUTING",
                    "完成审批后再反馈");
        }
        if (request == null || blank(request.result())) {
            throw new BusinessException("执行反馈不能为空");
        }

        impact.setFeedbackResult(request.result());
        impact.setFeedbackPlan(request.plan());
        impact.setDelayDays(request.delayDays() == null ? 0 : request.delayDays());
        impact.setReworkQty(BigDecimal.valueOf(request.reworkQty() == null ? 0 : request.reworkQty()));
        impact.setResponsibleName(request.responsibleName());
        impact.setStatus("DONE");
        impact.setVerifiedFlag(1);

        if (request.version() != null && !request.version().equals(impact.getVersion())) {
            throw BusinessException.ruleBlock(
                    "OPTIMISTIC_LOCK",
                    "影响项已被其他用户更新",
                    String.valueOf(request.version()),
                    String.valueOf(impact.getVersion()),
                    "刷新影响项后重新填写反馈");
        }
        if (impacts.updateById(impact) != 1) {
            throw BusinessException.ruleBlock(
                    "OPTIMISTIC_LOCK",
                    "影响项已被其他用户更新",
                    "stale",
                    "latest",
                    "刷新影响项后重新填写反馈");
        }
        events.record(
                change.getProjectId(),
                "CHANGE_IMPACT",
                impactId,
                "CHANGE_IMPACT_FEEDBACK",
                "PENDING_FEEDBACK",
                "DONE",
                impact.getObjectType() + " 影响项已反馈");
        return toDto(impact);
    }

    // 汇总变更单的执行反馈。
    @Override
    @Transactional
    public ChangeOrderDto feedbackChange(Long changeId, ChangeFeedbackRequest request) {
        ChangeOrder change = requireChange(changeId);
        if (request == null || blank(request.result())) {
            throw new BusinessException("执行反馈不能为空");
        }
        List<ChangeImpact> pendingImpacts = impacts.selectList(
                Wrappers.<ChangeImpact>lambdaQuery()
                        .eq(ChangeImpact::getChangeId, changeId)
                        .eq(ChangeImpact::getStatus, "PENDING_FEEDBACK"));
        for (ChangeImpact impact : pendingImpacts) {
            feedbackImpact(
                    impact.getId(),
                    new ChangeFeedbackRequest(
                            impact.getId(),
                            request.result(),
                            request.plan(),
                            request.delayDays(),
                            request.reworkQty(),
                            request.responsibleName(),
                            impact.getVersion()));
        }
        return toDto(change);
    }

    // 完成反馈后关闭变更单。
    @Override
    @Transactional
    public ChangeOrderDto close(Long changeId) {
        ChangeOrder change = requireChange(changeId);
        dataScope.requireProjectRole(change.getProjectId(), "PROJECT_MANAGER");
        if (!Set.of("EXECUTING", "PENDING_VERIFICATION").contains(change.getStatus())) {
            throw BusinessException.ruleBlock("CHANGE_STATUS", "只有执行或待验证的变更可以关闭", change.getStatus(), "EXECUTING/PENDING_VERIFICATION", "先完成审批");
        }
        List<ChangeImpact> rows = impacts.selectList(
                Wrappers.<ChangeImpact>lambdaQuery()
                        .eq(ChangeImpact::getChangeId, changeId));
        long unfinished = rows.stream().filter(row -> !"DONE".equals(row.getStatus())).count();
        if (unfinished > 0) {
            throw BusinessException.ruleBlock("CHANGE_IMPACT_INCOMPLETE", "仍有未完成影响项", String.valueOf(unfinished), "0", "完成全部影响项反馈");
        }
        int delay = rows.stream().map(ChangeImpact::getDelayDays).filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).sum();
        int rework = rows.stream().map(ChangeImpact::getReworkQty).filter(java.util.Objects::nonNull).mapToInt(BigDecimal::intValue).sum();
        for (ChangeImpact item : rows) {
            jdbc.update(
                    "INSERT INTO nso_delay_rework (tenant_id,project_id,change_id,reason_type,delay_days,rework_qty,responsibility_stage) VALUES (?,?,?,?,?,?,?)",
                    TenantContext.tenantId(),
                    change.getProjectId(),
                    changeId,
                    change.getChangeType(),
                    item.getDelayDays() == null ? 0 : item.getDelayDays(),
                    item.getReworkQty() == null ? BigDecimal.ZERO : item.getReworkQty(),
                    item.getDepartmentName());
        }
        change.setDelayDays(delay);
        change.setReworkQty(rework);
        String before = change.getStatus();
        change.setStatus("CLOSED");
        update(change);
        events.record(change.getProjectId(), "CHANGE", changeId, "CHANGE_CLOSED", before, "CLOSED", "变更关闭，累计延期 " + delay + " 天、返工 " + rework + " 件");
        return toDto(change);
    }

    // 撤销已批准的变更。
    @Override
    @Transactional
    public ChangeOrderDto revoke(Long changeId, ChangeRevokeRequest request) {
        ChangeOrder change = requireChange(changeId);
        dataScope.requireProjectRole(change.getProjectId(), "PROJECT_MANAGER");
        if (request == null || blank(request.reason()) || request.confirmationId() == null) {
            throw new BusinessException("撤销已批准变更必须填写原因并完成二次确认");
        }
        if (request.version() != null && !request.version().equals(change.getVersion())) {
            throw BusinessException.ruleBlock("OPTIMISTIC_LOCK", "变更已被其他用户更新", String.valueOf(request.version()), String.valueOf(change.getVersion()), "刷新变更后重试");
        }
        if (!Set.of("EXECUTING", "PENDING_VERIFICATION").contains(change.getStatus())) {
            throw BusinessException.ruleBlock("CHANGE_STATUS", "仅已批准变更可撤销", change.getStatus(), "EXECUTING/PENDING_VERIFICATION", "刷新变更状态");
        }
        confirmations.consume(request.confirmationId(), "CHANGE_REVOKE", "CHANGE", changeId);
        String before = change.getStatus();
        change.setStatus("REVOKED");
        change.setRevokeReason(request.reason().trim());
        change.setRevokedAt(java.time.LocalDateTime.now());
        update(change);
        jdbc.update(
                "INSERT INTO nso_change_status_history (tenant_id,change_id,before_status,after_status,action_code,reason,operator_id) VALUES (?,?,?,?,?,?,?)",
                TenantContext.tenantId(),
                changeId,
                before,
                "REVOKED",
                "REVOKE",
                request.reason().trim(),
                TenantContext.userId());
        events.record(change.getProjectId(), "CHANGE", changeId, "CHANGE_REVOKED", before, "REVOKED", request.reason().trim());
        return toDto(change);
    }

    // 核验执行反馈后更新状态。
    @Override
    @Transactional
    public ChangeOrderDto verify(Long changeId, ChangeVerifyRequest request) {
        ChangeOrder change = requireChange(changeId);
        dataScope.requireProjectRole(change.getProjectId(), "PROJECT_MANAGER", "QUALITY");
        if (!"EXECUTING".equals(change.getStatus())) {
            throw BusinessException.ruleBlock("CHANGE_STATUS", "仅执行中的变更可以验证", change.getStatus(), "EXECUTING", "完成审批和执行反馈");
        }
        long unfinished = impacts.selectCount(Wrappers.<ChangeImpact>lambdaQuery()
                .eq(ChangeImpact::getChangeId, changeId)
                .ne(ChangeImpact::getStatus, "DONE"));
        if (unfinished > 0) {
            throw BusinessException.ruleBlock("CHANGE_IMPACT_INCOMPLETE", "仍有未完成影响项", String.valueOf(unfinished), "0", "完成影响项反馈");
        }
        change.setStatus("PENDING_VERIFICATION");
        change.setVerifiedAt(java.time.LocalDateTime.now());
        change.setVerifiedBy(TenantContext.userId());
        update(change);
        jdbc.update(
                "INSERT INTO nso_change_status_history (tenant_id,change_id,before_status,after_status,action_code,reason,operator_id) VALUES (?,?,?,?,?,?,?)",
                TenantContext.tenantId(),
                changeId,
                "EXECUTING",
                "PENDING_VERIFICATION",
                "VERIFY",
                request == null ? null : request.summary(),
                TenantContext.userId());
        events.record(change.getProjectId(), "CHANGE", changeId, "CHANGE_VERIFIED", "EXECUTING", "PENDING_VERIFICATION", request == null ? "变更执行验证完成" : request.summary());
        return toDto(change);
    }

    // 已关闭变更可派生补充单。
    @Override
    @Transactional
    public ChangeOrderDto supplement(Long changeId, ChangeRequest request) {
        ChangeOrder source = requireChange(changeId);
        if (!"CLOSED".equals(source.getStatus())) {
            throw BusinessException.ruleBlock("CHANGE_SUPPLEMENT", "仅已关闭变更可创建补充变更", source.getStatus(), "CLOSED", "先完成当前变更");
        }
        ChangeRequest effective = request == null
                ? new ChangeRequest(
                        source.getProjectId(),
                        source.getChangeType(),
                        source.getUrgency(),
                        source.getAfterContent(),
                        source.getAfterContent(),
                        "补充变更")
                : new ChangeRequest(
                        source.getProjectId(),
                        request.changeType(),
                        request.urgency(),
                        request.beforeContent(),
                        request.afterContent(),
                        request.reason());
        ChangeOrder created = new ChangeOrder();
        created.setTenantId(TenantContext.tenantId());
        created.setProjectId(source.getProjectId());
        created.setParentChangeId(source.getId());
        created.setApplicantUserId(TenantContext.userId());
        created.setChangeNo(numbers.next("CHANGE"));
        created.setChangeType(blank(effective.changeType()) ? source.getChangeType() : effective.changeType());
        created.setUrgency(blank(effective.urgency()) ? source.getUrgency() : effective.urgency());
        created.setBeforeContent(effective.beforeContent());
        created.setAfterContent(effective.afterContent());
        created.setReason(blank(effective.reason()) ? "补充变更" : effective.reason());
        created.setStatus("WAIT_IMPACT");
        created.setDelayDays(0);
        created.setReworkQty(0);
        changes.insert(created);
        events.record(source.getProjectId(), "CHANGE", created.getId(), "CHANGE_SUPPLEMENT_CREATED", null, "WAIT_IMPACT", "已从 " + source.getChangeNo() + " 创建补充变更");
        return toDto(created);
    }

    private void generateImpacts(ChangeOrder change) {
        impactAnalysis.generate(change);
    }

    private void seedApprovals(ChangeOrder change) {
        impactAnalysis.seedApprovals(change);
    }

    private String resolveApprovalNode(Long changeId) {
        if (TenantContext.hasAnyRole("admin")) {
            return "ADMIN";
        }
        String node = TenantContext.hasAnyRole("project_manager")
                ? "PROJECT_MANAGER"
                : TenantContext.hasAnyRole("technical", "designer")
                        ? "TECHNICAL"
                        : TenantContext.hasAnyRole("process")
                                ? "PROCESS"
                                : TenantContext.hasAnyRole("purchaser")
                                        ? "PURCHASER"
                                        : TenantContext.hasAnyRole("production")
                                                ? "PRODUCTION"
                                                : TenantContext.hasAnyRole("quality") ? "QUALITY" : null;
        if (node == null) {
            throw BusinessException.ruleBlock(
                    "CHANGE_APPROVAL_PERMISSION",
                    "当前角色无变更审批权限",
                    String.join(",", TenantContext.roles()),
                    "项目/技术/生产/质量审批角色",
                    "联系项目经理处理");
        }
        Integer pending = jdbc.queryForObject(
                "SELECT COUNT(*) FROM nso_change_approval WHERE tenant_id=? AND change_id=? AND node_code=? AND decision='PENDING'",
                Integer.class,
                TenantContext.tenantId(),
                changeId,
                node);
        if (pending == null || pending == 0) {
            throw BusinessException.ruleBlock("CHANGE_APPROVAL_NODE", "当前审批节点无需处理", node, "待办节点", "刷新审批列表");
        }
        return node;
    }

    private void blockAffectedTasks(Long changeId) {
        impactAnalysis.blockAffectedTasks(changeId);
    }

    private boolean isHighRisk(ChangeOrder change) {
        return Set.of("HIGH", "URGENT", "CRITICAL").contains(change.getUrgency());
    }

    private String projectRoleForImpact(String department) {
        return switch (department == null ? "" : department.toUpperCase()) {
            case "PROCUREMENT", "PURCHASE" -> "PURCHASER";
            case "PRODUCTION" -> "PRODUCTION";
            case "QUALITY", "INSPECTION" -> "QUALITY";
            case "PROCESS" -> "PROCESS";
            default -> "TECHNICAL";
        };
    }

    private ChangeOrder requireChange(Long id) {
        return queries.requireChange(id);
    }

    private Project requireProject(Long id) {
        return queries.requireProject(id);
    }

    private void update(ChangeOrder row) {
        if (changes.updateById(row) != 1) {
            throw BusinessException.ruleBlock("OPTIMISTIC_LOCK", "变更单已被其他用户修改", "stale", "latest", "刷新后重试");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private ChangeOrderDto toDto(ChangeOrder row) {
        return queries.toDto(row);
    }

    private ChangeImpactDto toDto(ChangeImpact row) {
        return queries.toDto(row);
    }
}
