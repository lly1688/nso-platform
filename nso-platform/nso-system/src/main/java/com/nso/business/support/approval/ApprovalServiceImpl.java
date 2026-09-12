package com.nso.business.support.approval;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nso.business.core.NsoDtos.ApprovalDecisionRequest;
import com.nso.business.core.NsoDtos.ApprovalInstanceDto;
import com.nso.business.core.NsoDtos.ApprovalTemplateDto;
import com.nso.business.core.NsoDtos.ApprovalTemplateNodeDto;
import com.nso.business.core.NsoDtos.ApprovalTemplateNodeRequest;
import com.nso.business.core.NsoDtos.ApprovalTemplateRequest;
import com.nso.business.core.NsoDtos.ApprovalTodoDto;
import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.business.core.TenantContext;
import com.nso.business.message.service.IMessageService;
import com.nso.business.project.domain.ProjectMember;
import com.nso.business.project.service.ProjectDataScope;
import com.nso.business.support.BusinessEventService;
import com.nso.business.support.approval.domain.ApprovalInstance;
import com.nso.business.support.approval.domain.ApprovalTask;
import com.nso.business.support.approval.domain.ApprovalTemplate;
import com.nso.business.support.approval.domain.ApprovalTemplateNode;
import com.nso.business.support.approval.mapper.ApprovalInstanceMapper;
import com.nso.business.support.approval.mapper.ApprovalTaskMapper;
import com.nso.business.support.approval.mapper.ApprovalTemplateMapper;
import com.nso.business.support.approval.mapper.ApprovalTemplateNodeMapper;
import com.nso.shared.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 审批管理服务实现。 模板采用版本化配置，运行中的实例复制节点以避免后续模板修改影响审批。
@Service
public class ApprovalServiceImpl implements IApprovalService {
    private static final Set<String> SUPPORTED_BUSINESS_TYPES = Set.of("CHANGE", "SPECIAL_RELEASE", "EXCEPTION");
    // 会签沿用并行审批语义，同时兼容历史 PARALLEL 数据。
    private static final Set<String> SUPPORTED_MODES = Set.of("SERIAL", "COUNTERSIGN", "PARALLEL");

    // 审批模板数据映射
    private final ApprovalTemplateMapper templates;
    // 审批模板Node数据映射
    private final ApprovalTemplateNodeMapper templateNodes;
    // 审批Instance数据映射
    private final ApprovalInstanceMapper instances;
    // 审批任务数据映射
    private final ApprovalTaskMapper tasks;
    // 项目数据范围
    private final ProjectDataScope dataScope;
    // 业务事件服务
    private final BusinessEventService events;
    // 消息服务
    private final IMessageService messages;
    // JDBC模板
    private final JdbcTemplate jdbc;

    public ApprovalServiceImpl(
            ApprovalTemplateMapper templates,
            ApprovalTemplateNodeMapper templateNodes,
            ApprovalInstanceMapper instances,
            ApprovalTaskMapper tasks,
            ProjectDataScope dataScope,
            BusinessEventService events,
            IMessageService messages,
            JdbcTemplate jdbc) {
        this.templates = templates;
        this.templateNodes = templateNodes;
        this.instances = instances;
        this.tasks = tasks;
        this.dataScope = dataScope;
        this.events = events;
        this.messages = messages;
        this.jdbc = jdbc;
    }

    // 分页查询审批模板。
    @Override
    public PageResult<ApprovalTemplateDto> templates(String businessType, PageQuery pageQuery) {
        PageQuery page = normalize(pageQuery);
        List<ApprovalTemplateDto> all = templates.selectList(Wrappers.<ApprovalTemplate>lambdaQuery()
                        .eq(ApprovalTemplate::getTenantId, TenantContext.tenantId())
                        .eq(notBlank(businessType), ApprovalTemplate::getBusinessType, upper(businessType))
                        .orderByDesc(ApprovalTemplate::getBusinessType)
                        .orderByDesc(ApprovalTemplate::getTemplateVersion))
                .stream().map(this::toTemplateDto).toList();
        return page(all, page);
    }

    // 创建审批模板。
    @Override
    @Transactional
    public ApprovalTemplateDto createTemplate(ApprovalTemplateRequest request) {
        if (request == null || !notBlank(request.templateCode()) || !notBlank(request.templateName())
                || !notBlank(request.businessType()) || request.nodes() == null || request.nodes().isEmpty()) {
            throw new BusinessException("审批模板编码、名称、业务类型和至少一个节点不能为空");
        }
        String businessType = requireBusinessType(request.businessType());
        String mode = requireMode(request.approvalMode());
        String code = upper(request.templateCode());
        validateNodes(request.nodes());

        int nextVersion = templates.selectList(Wrappers.<ApprovalTemplate>lambdaQuery()
                        .eq(ApprovalTemplate::getTenantId, TenantContext.tenantId())
                        .eq(ApprovalTemplate::getTemplateCode, code))
                .stream().map(ApprovalTemplate::getTemplateVersion).filter(java.util.Objects::nonNull)
                .max(Integer::compareTo).orElse(0) + 1;
        ApprovalTemplate template = new ApprovalTemplate();
        template.setTenantId(TenantContext.tenantId());
        template.setTemplateCode(code);
        template.setBusinessType(businessType);
        template.setTemplateVersion(nextVersion);
        template.setTemplateName(request.templateName().trim());
        template.setApprovalMode(mode);
        template.setSlaMinutes(normalizeSla(request.slaMinutes()));
        template.setStatus("DRAFT");
        template.setCreatedBy(TenantContext.userId());
        template.setVersion(0);
        templates.insert(template);

        int order = 10;
        for (ApprovalTemplateNodeRequest node : request.nodes()) {
            ApprovalTemplateNode row = new ApprovalTemplateNode();
            row.setTenantId(TenantContext.tenantId());
            row.setTemplateId(template.getId());
            row.setNodeOrder(order);
            row.setNodeCode(upper(node.nodeCode()));
            row.setNodeName(node.nodeName().trim());
            row.setResponsibilityCode(upper(node.responsibilityCode()));
            row.setSlaMinutes(normalizeSla(node.slaMinutes() == null ? template.getSlaMinutes() : node.slaMinutes()));
            row.setEscalationRole(notBlank(node.escalationRole()) ? upper(node.escalationRole()) : "PROJECT_MANAGER");
            row.setVersion(0);
            templateNodes.insert(row);
            order += 10;
        }
        return toTemplateDto(template);
    }

    // 发布审批模板。
    @Override
    @Transactional
    public ApprovalTemplateDto publishTemplate(Long templateId) {
        ApprovalTemplate template = requireTemplate(templateId);
        if (!"DRAFT".equals(template.getStatus())) {
            throw BusinessException.ruleBlock("APPROVAL_TEMPLATE_STATUS", "仅草稿模板可以发布", template.getStatus(), "DRAFT", "新建下一版本模板");
        }
        List<ApprovalTemplateNode> nodes = nodes(template.getId());
        if (nodes.isEmpty()) {
            throw new BusinessException("审批模板至少需要一个节点");
        }
        templates.update(null, Wrappers.<ApprovalTemplate>lambdaUpdate()
                .eq(ApprovalTemplate::getTenantId, TenantContext.tenantId())
                .eq(ApprovalTemplate::getBusinessType, template.getBusinessType())
                .eq(ApprovalTemplate::getStatus, "PUBLISHED")
                .set(ApprovalTemplate::getStatus, "RETIRED"));
        template.setStatus("PUBLISHED");
        if (templates.updateById(template) != 1) {
            throw BusinessException.ruleBlock("OPTIMISTIC_LOCK", "审批模板已被其他用户更新", "stale", "latest", "刷新后重试");
        }
        return toTemplateDto(template);
    }

    // 创建审批实例。
    @Override
    @Transactional
    public ApprovalInstanceDto createInstance(String businessType, Long businessId, Long projectId, Collection<String> includedNodes) {
        if (businessId == null || projectId == null) {
            throw new BusinessException("审批业务对象和项目不能为空");
        }
        String type = requireBusinessType(businessType);
        ApprovalInstance existing = findInstance(type, businessId);
        if (existing != null) {
            return toInstanceDto(existing);
        }
        ApprovalTemplate template = activeTemplate(type);
        Set<String> include = includedNodes == null ? Set.of() : includedNodes.stream()
                .filter(this::notBlank).map(this::upper).collect(Collectors.toCollection(HashSet::new));
        List<ApprovalTemplateNode> templateNodes = nodes(template.getId()).stream()
                .sorted(Comparator.comparing(ApprovalTemplateNode::getNodeOrder))
                .toList();
        List<ApprovalTemplateNode> frozenNodes = templateNodes.stream()
                // 业务规则决定项目职责，节点标识仅作为模板内标签。
                .filter(node -> include.isEmpty() || include.contains(node.getResponsibilityCode()))
                .toList();
        if (frozenNodes.isEmpty() && !include.isEmpty()) {
            // 受限的历史路由不能使有效的角色模板失效。
            frozenNodes = templateNodes;
        }
        if (frozenNodes.isEmpty()) {
            throw BusinessException.ruleBlock("APPROVAL_TEMPLATE_NODE", "审批模板没有可用节点", type, "至少一个匹配节点", "检查模板或业务规则");
        }

        ApprovalInstance instance = new ApprovalInstance();
        instance.setTenantId(TenantContext.tenantId());
        instance.setBusinessType(type);
        instance.setBusinessId(businessId);
        instance.setProjectId(projectId);
        instance.setTemplateId(template.getId());
        instance.setTemplateCode(template.getTemplateCode());
        instance.setTemplateVersion(template.getTemplateVersion());
        instance.setApprovalMode(template.getApprovalMode());
        instance.setStatus("PENDING");
        instance.setCurrentNodeOrder(frozenNodes.get(0).getNodeOrder());
        instance.setStartedAt(LocalDateTime.now());
        instance.setVersion(0);
        instances.insert(instance);
        for (ApprovalTemplateNode node : frozenNodes) {
            ProjectMember assignee = dataScope.findActiveMemberForResponsibilities(projectId, node.getResponsibilityCode());
            ApprovalTask task = new ApprovalTask();
            task.setTenantId(TenantContext.tenantId());
            task.setInstanceId(instance.getId());
            task.setTemplateNodeId(node.getId());
            task.setNodeOrder(node.getNodeOrder());
            task.setNodeCode(node.getNodeCode());
            task.setNodeName(node.getNodeName());
            task.setResponsibilityCode(node.getResponsibilityCode());
            task.setAssigneeUserId(assignee == null ? null : assignee.getUserId());
            task.setAssigneeName(assignee == null ? null : assignee.getMemberName());
            task.setDecision("PENDING");
            task.setDueAt(LocalDateTime.now().plusMinutes(node.getSlaMinutes()));
            task.setVersion(0);
            tasks.insert(task);
        }
        events.record(projectId, "APPROVAL_INSTANCE", instance.getId(), "APPROVAL_INSTANCE_CREATED", null, "PENDING",
                type + " 审批实例已按模板 " + template.getTemplateCode() + " 冻结");
        return toInstanceDto(instance);
    }

    // 查询业务审批实例。
    @Override
    public ApprovalInstanceDto instance(String businessType, Long businessId) {
        ApprovalInstance instance = requireInstance(requireBusinessType(businessType), businessId);
        dataScope.requireAccess(instance.getProjectId());
        return toInstanceDto(instance);
    }

    // 分页查询当前用户待审批项。
    @Override
    public PageResult<ApprovalTodoDto> pending(String businessType, PageQuery pageQuery) {
        List<ApprovalTodoDto> rows = pendingForCurrentUser().stream()
                .filter(row -> !notBlank(businessType) || upper(businessType).equals(row.businessType()))
                .sorted(Comparator.comparing(ApprovalTodoDto::overdue).reversed()
                        .thenComparing(ApprovalTodoDto::dueAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        return page(rows, normalize(pageQuery));
    }

    // 查询当前用户待审批项。
    @Override
    public List<ApprovalTodoDto> pendingForCurrentUser() {
        List<Long> visible = dataScope.visibleProjectIds();
        if (visible != null && visible.isEmpty()) {
            return List.of();
        }
        return instances.selectList(Wrappers.<ApprovalInstance>lambdaQuery()
                        .eq(ApprovalInstance::getTenantId, TenantContext.tenantId())
                        .eq(ApprovalInstance::getStatus, "PENDING")
                        .in(visible != null, ApprovalInstance::getProjectId, visible == null ? List.of() : visible))
                .stream().flatMap(instance -> tasks(instance.getId()).stream()
                        .filter(task -> "PENDING".equals(task.getDecision()))
                        .filter(task -> isActiveNode(instance, task))
                        .filter(task -> canApprove(instance.getProjectId(), task.getResponsibilityCode()))
                        .map(task -> toTodoDto(instance, task)))
                .toList();
    }

    // 查询项目待审批项。
    @Override
    public List<ApprovalTodoDto> pendingForProject(Long projectId) {
        dataScope.requireAccess(projectId);
        return instances.selectList(Wrappers.<ApprovalInstance>lambdaQuery()
                        .eq(ApprovalInstance::getTenantId, TenantContext.tenantId())
                        .eq(ApprovalInstance::getProjectId, projectId)
                        .eq(ApprovalInstance::getStatus, "PENDING"))
                .stream().flatMap(instance -> tasks(instance.getId()).stream()
                        .filter(task -> "PENDING".equals(task.getDecision()))
                        .filter(task -> isActiveNode(instance, task))
                        .map(task -> toTodoDto(instance, task)))
                .toList();
    }

    // 查询待审批项详情。
    @Override
    public ApprovalTodoDto todo(Long todoId) {
        ApprovalTask task = requireTask(todoId);
        ApprovalInstance instance = requireInstance(task.getInstanceId());
        dataScope.requireAccess(instance.getProjectId());
        return toTodoDto(instance, task);
    }

    // 提交审批结论。
    @Override
    @Transactional
    public ApprovalTodoDto decide(Long todoId, ApprovalDecisionRequest request) {
        return decideInternal(todoId, request);
    }

    // 提交特殊放行审批结论。
    @Override
    @Transactional
    public ApprovalTodoDto decideSpecialRelease(Long todoId, ApprovalDecisionRequest request) {
        ApprovalTask task = requireTask(todoId);
        ApprovalInstance instance = requireInstance(task.getInstanceId());
        if (!"SPECIAL_RELEASE".equals(instance.getBusinessType())) {
            throw new BusinessException("当前审批待办不是特殊放行申请");
        }
        Map<String, Object> release = jdbc.query("SELECT project_id,applicant_user_id,status FROM nso_special_release WHERE tenant_id=? AND id=?",
                rs -> rs.next() ? Map.of("projectId", rs.getLong("project_id"), "applicant", rs.getLong("applicant_user_id"), "status", rs.getString("status")) : null,
                TenantContext.tenantId(), instance.getBusinessId());
        if (release == null || !"PENDING".equals(release.get("status"))) {
            throw BusinessException.ruleBlock("SPECIAL_RELEASE_STATUS", "当前放行申请不能审批", String.valueOf(release == null ? "MISSING" : release.get("status")), "PENDING", "刷新申请状态");
        }
        Long applicant = ((Number) release.get("applicant")).longValue();
        if (TenantContext.userId() != null && TenantContext.userId().equals(applicant)) {
            throw BusinessException.ruleBlock("SPECIAL_RELEASE_DUTY_SEPARATION", "放行申请人不能处理本申请", String.valueOf(applicant), "独立审批人", "由质量或授权批准人处理");
        }
        ApprovalTodoDto result = decideInternal(todoId, request);
        ApprovalInstance latest = requireInstance(instance.getId());
        if (!Set.of("APPROVED", "REJECTED").contains(latest.getStatus())) {
            return result;
        }
        int updated = jdbc.update("UPDATE nso_special_release SET approver_user_id=?, approved_at=NOW(), status=? WHERE tenant_id=? AND id=? AND status='PENDING'",
                TenantContext.userId(), latest.getStatus(), TenantContext.tenantId(), instance.getBusinessId());
        if (updated != 1) {
            throw BusinessException.ruleBlock("SPECIAL_RELEASE_STATUS", "特殊放行状态已被其他用户更新", "stale", "PENDING", "刷新后重试");
        }
        events.record(instance.getProjectId(), "SPECIAL_RELEASE", instance.getBusinessId(),
                "APPROVED".equals(latest.getStatus()) ? "SPECIAL_RELEASE_APPROVED" : "SPECIAL_RELEASE_REJECTED",
                "PENDING", latest.getStatus(), "特殊放行审批已处理");
        return result;
    }

    // 同步变更审批状态。
    @Override
    @Transactional
    public void syncChangeApprovals(Long changeId) {
        Map<String, Object> change = jdbc.query("SELECT project_id FROM nso_change_order WHERE tenant_id=? AND id=?",
                rs -> rs.next() ? Map.of("projectId", rs.getLong("project_id")) : null, TenantContext.tenantId(), changeId);
        if (change == null) {
            return;
        }
        List<LegacyDecision> decisions = jdbc.query("SELECT node_code,decision,opinion,approver_id,approver_name,approved_at FROM nso_change_approval WHERE tenant_id=? AND change_id=? ORDER BY id",
                (rs, row) -> new LegacyDecision(rs.getString("node_code"), rs.getString("decision"), rs.getString("opinion"),
                        rs.getObject("approver_id", Long.class), rs.getString("approver_name"), timestamp(rs.getObject("approved_at"))),
                TenantContext.tenantId(), changeId);
        if (decisions.isEmpty()) {
            return;
        }
        ApprovalInstance instance = findInstance("CHANGE", changeId);
        if (instance == null) {
            instance = instanceEntity(createInstance("CHANGE", changeId, ((Number) change.get("projectId")).longValue(),
                    decisions.stream().map(LegacyDecision::nodeCode).toList()).id());
        }
        for (LegacyDecision legacy : decisions) {
            ApprovalTask task = tasks.selectOne(Wrappers.<ApprovalTask>lambdaQuery()
                    .eq(ApprovalTask::getTenantId, TenantContext.tenantId())
                    .eq(ApprovalTask::getInstanceId, instance.getId())
                    .and(node -> node.eq(ApprovalTask::getNodeCode, legacy.nodeCode())
                            .or().eq(ApprovalTask::getResponsibilityCode, legacy.nodeCode()))
                    .last("LIMIT 1"));
            if (task == null || "PENDING".equals(legacy.decision())) {
                continue;
            }
            task.setDecision(legacy.decision());
            task.setOpinion(legacy.opinion());
            task.setDecidedBy(legacy.approverId());
            task.setDecidedByName(legacy.approverName());
            task.setDecidedAt(legacy.approvedAt());
            tasks.updateById(task);
        }
        refreshInstanceState(instance);
    }

    // 同步特殊放行审批状态。
    @Override
    @Transactional
    public void syncSpecialReleaseApproval(Long releaseId) {
        Map<String, Object> release = jdbc.query("SELECT project_id,status FROM nso_special_release WHERE tenant_id=? AND id=?",
                rs -> rs.next() ? Map.of("projectId", rs.getLong("project_id"), "status", rs.getString("status")) : null,
                TenantContext.tenantId(), releaseId);
        if (release == null) {
            return;
        }
        ApprovalInstance instance = findInstance("SPECIAL_RELEASE", releaseId);
        if (instance == null) {
            instance = instanceEntity(createInstance("SPECIAL_RELEASE", releaseId, ((Number) release.get("projectId")).longValue(), null).id());
        }
        String status = String.valueOf(release.get("status"));
        if ("PENDING".equals(status)) {
            return;
        }
        String decision = "APPROVED".equals(status) ? "APPROVED" : "REJECTED";
        for (ApprovalTask task : tasks(instance.getId())) {
            if ("PENDING".equals(task.getDecision())) {
                task.setDecision(decision);
                task.setDecidedBy(TenantContext.userId());
                task.setDecidedByName(TenantContext.username());
                task.setDecidedAt(LocalDateTime.now());
                tasks.updateById(task);
            }
        }
        refreshInstanceState(instance);
    }

    // 重新打开审批驳回的异常单。
    @Override
    @Transactional
    public void reopenRejectedExceptionApproval(Long exceptionCaseId) {
        ApprovalInstance instance = requireInstance("EXCEPTION", exceptionCaseId);
        if (!"REJECTED".equals(instance.getStatus())) {
            return;
        }
        for (ApprovalTask task : tasks(instance.getId())) {
            task.setDecision("PENDING");
            task.setOpinion(null);
            task.setDecidedBy(null);
            task.setDecidedByName(null);
            task.setDecidedAt(null);
            task.setDecisionIdempotencyKey(null);
            task.setDueAt(LocalDateTime.now().plusHours(24));
            task.setEscalatedAt(null);
            tasks.updateById(task);
        }
        instance.setStatus("PENDING");
        instance.setCurrentNodeOrder(tasks(instance.getId()).stream().map(ApprovalTask::getNodeOrder).min(Integer::compareTo).orElse(null));
        instance.setFinishedAt(null);
        instance.setEscalatedAt(null);
        instances.updateById(instance);
    }

    // 判断业务是否已审批通过。
    @Override
    public boolean isApproved(String businessType, Long businessId) {
        ApprovalInstance instance = findInstance(requireBusinessType(businessType), businessId);
        return instance != null && "APPROVED".equals(instance.getStatus());
    }

    // 升级逾期审批任务。
    @Override
    @Transactional
    public int escalateOverdue() {
        int escalated = 0;
        for (ApprovalInstance instance : instances.selectList(Wrappers.<ApprovalInstance>lambdaQuery()
                .eq(ApprovalInstance::getTenantId, TenantContext.tenantId()).eq(ApprovalInstance::getStatus, "PENDING"))) {
            for (ApprovalTask task : tasks(instance.getId())) {
                if (!"PENDING".equals(task.getDecision()) || task.getDueAt() == null || !task.getDueAt().isBefore(LocalDateTime.now())
                        || task.getEscalatedAt() != null || !isActiveNode(instance, task)) {
                    continue;
                }
                task.setEscalatedAt(LocalDateTime.now());
                tasks.updateById(task);
                messages.notifyProject(instance.getProjectId(), "APPROVAL_TIMEOUT", "审批待办超时",
                        task.getNodeName() + " 已超过 SLA，请项目经理协调处理", "APPROVAL", task.getId());
                events.record(instance.getProjectId(), "APPROVAL_TASK", task.getId(), "APPROVAL_ESCALATED", "PENDING", "PENDING", "审批节点已超时升级");
                escalated++;
            }
        }
        return escalated;
    }

    private ApprovalTodoDto decideInternal(Long todoId, ApprovalDecisionRequest request) {
        ApprovalTask task = requireTask(todoId);
        ApprovalInstance instance = requireInstance(task.getInstanceId());
        dataScope.requireProjectRole(instance.getProjectId(), task.getResponsibilityCode());
        if (!"PENDING".equals(instance.getStatus()) || !"PENDING".equals(task.getDecision()) || !isActiveNode(instance, task)) {
            if (request != null && notBlank(request.idempotencyKey()) && request.idempotencyKey().equals(task.getDecisionIdempotencyKey())) {
                return toTodoDto(instance, task);
            }
            throw BusinessException.ruleBlock("APPROVAL_TODO_STATUS", "当前审批待办不可处理", task.getDecision(), "PENDING", "刷新审批列表");
        }
        if (request != null && request.version() != null && !request.version().equals(task.getVersion())) {
            throw BusinessException.ruleBlock("OPTIMISTIC_LOCK", "审批待办已被其他用户更新", String.valueOf(request.version()), String.valueOf(task.getVersion()), "刷新后重试");
        }
        String decision = upper(request == null ? null : request.decision());
        if (!Set.of("APPROVED", "REJECTED").contains(decision)) {
            throw new BusinessException("审批结论只能是 APPROVED 或 REJECTED");
        }
        task.setDecision(decision);
        task.setOpinion(request == null ? null : trimToNull(request.opinion()));
        task.setDecidedBy(TenantContext.userId());
        task.setDecidedByName(TenantContext.username());
        task.setDecidedAt(LocalDateTime.now());
        task.setDecisionIdempotencyKey(request == null ? null : trimToNull(request.idempotencyKey()));
        if (tasks.updateById(task) != 1) {
            throw BusinessException.ruleBlock("OPTIMISTIC_LOCK", "审批待办已被其他用户更新", "stale", "latest", "刷新后重试");
        }
        if ("REJECTED".equals(decision)) {
            tasks.update(null, Wrappers.<ApprovalTask>lambdaUpdate()
                    .eq(ApprovalTask::getTenantId, TenantContext.tenantId())
                    .eq(ApprovalTask::getInstanceId, instance.getId())
                    .eq(ApprovalTask::getDecision, "PENDING")
                    .set(ApprovalTask::getDecision, "SKIPPED"));
        }
        refreshInstanceState(instance);
        events.record(instance.getProjectId(), "APPROVAL_TASK", task.getId(), "APPROVAL_DECIDED", "PENDING", decision,
                task.getNodeName() + "：" + ("APPROVED".equals(decision) ? "批准" : "驳回"));
        return toTodoDto(requireInstance(instance.getId()), requireTask(task.getId()));
    }

    private void refreshInstanceState(ApprovalInstance instance) {
        List<ApprovalTask> rows = tasks(instance.getId());
        boolean rejected = rows.stream().anyMatch(task -> "REJECTED".equals(task.getDecision()));
        boolean approved = !rows.isEmpty() && rows.stream().allMatch(task -> "APPROVED".equals(task.getDecision()));
        instance.setStatus(rejected ? "REJECTED" : approved ? "APPROVED" : "PENDING");
        instance.setCurrentNodeOrder(rows.stream().filter(task -> "PENDING".equals(task.getDecision()))
                .map(ApprovalTask::getNodeOrder).min(Integer::compareTo).orElse(null));
        instance.setFinishedAt(rejected || approved ? LocalDateTime.now() : null);
        if (instances.updateById(instance) != 1) {
            throw BusinessException.ruleBlock("OPTIMISTIC_LOCK", "审批实例已被其他用户更新", "stale", "latest", "刷新后重试");
        }
    }

    private boolean isActiveNode(ApprovalInstance instance, ApprovalTask task) {
        if (!"SERIAL".equals(instance.getApprovalMode())) {
            return true;
        }
        return tasks(instance.getId()).stream()
                .filter(previous -> previous.getNodeOrder() < task.getNodeOrder())
                .allMatch(previous -> "APPROVED".equals(previous.getDecision()));
    }

    private boolean canApprove(Long projectId, String responsibilityCode) {
        if (TenantContext.userId() == null) {
            return true;
        }
        try {
            dataScope.requireProjectRole(projectId, responsibilityCode);
            return true;
        } catch (BusinessException ignored) {
            return false;
        }
    }

    private ApprovalTemplate activeTemplate(String businessType) {
        ApprovalTemplate template = templates.selectOne(Wrappers.<ApprovalTemplate>lambdaQuery()
                .eq(ApprovalTemplate::getTenantId, TenantContext.tenantId())
                .eq(ApprovalTemplate::getBusinessType, businessType)
                .eq(ApprovalTemplate::getStatus, "PUBLISHED")
                .orderByDesc(ApprovalTemplate::getTemplateVersion).last("LIMIT 1"));
        if (template == null) {
            throw BusinessException.ruleBlock("APPROVAL_TEMPLATE_MISSING", "没有可用的审批模板", businessType, "已发布模板", "先发布对应业务的审批模板");
        }
        return template;
    }

    private ApprovalTemplate requireTemplate(Long id) {
        ApprovalTemplate template = templates.selectById(id);
        if (template == null || !Long.valueOf(TenantContext.tenantId()).equals(template.getTenantId())) {
            throw new BusinessException("审批模板不存在");
        }
        return template;
    }

    private ApprovalInstance requireInstance(String businessType, Long businessId) {
        ApprovalInstance instance = findInstance(businessType, businessId);
        if (instance == null) {
            throw new BusinessException("审批实例不存在");
        }
        return instance;
    }

    private ApprovalInstance requireInstance(Long id) {
        ApprovalInstance instance = instances.selectById(id);
        if (instance == null || !Long.valueOf(TenantContext.tenantId()).equals(instance.getTenantId())) {
            throw new BusinessException("审批实例不存在");
        }
        return instance;
    }

    private ApprovalInstance findInstance(String businessType, Long businessId) {
        return instances.selectOne(Wrappers.<ApprovalInstance>lambdaQuery()
                .eq(ApprovalInstance::getTenantId, TenantContext.tenantId())
                .eq(ApprovalInstance::getBusinessType, businessType)
                .eq(ApprovalInstance::getBusinessId, businessId).last("LIMIT 1"));
    }

    private ApprovalTask requireTask(Long id) {
        ApprovalTask task = tasks.selectById(id);
        if (task == null || !Long.valueOf(TenantContext.tenantId()).equals(task.getTenantId())) {
            throw new BusinessException("审批待办不存在");
        }
        return task;
    }

    private List<ApprovalTask> tasks(Long instanceId) {
        return tasks.selectList(Wrappers.<ApprovalTask>lambdaQuery()
                .eq(ApprovalTask::getTenantId, TenantContext.tenantId())
                .eq(ApprovalTask::getInstanceId, instanceId)
                .orderByAsc(ApprovalTask::getNodeOrder));
    }

    private List<ApprovalTemplateNode> nodes(Long templateId) {
        return templateNodes.selectList(Wrappers.<ApprovalTemplateNode>lambdaQuery()
                .eq(ApprovalTemplateNode::getTenantId, TenantContext.tenantId())
                .eq(ApprovalTemplateNode::getTemplateId, templateId)
                .orderByAsc(ApprovalTemplateNode::getNodeOrder));
    }

    private ApprovalTemplateDto toTemplateDto(ApprovalTemplate template) {
        return new ApprovalTemplateDto(template.getId(), template.getTemplateCode(), template.getBusinessType(), template.getTemplateVersion(),
                template.getTemplateName(), template.getApprovalMode(), template.getSlaMinutes(), template.getStatus(),
                nodes(template.getId()).stream().map(node -> new ApprovalTemplateNodeDto(node.getId(), node.getNodeOrder(), node.getNodeCode(),
                        node.getNodeName(), node.getResponsibilityCode(), node.getSlaMinutes(), node.getEscalationRole())).toList(), template.getVersion());
    }

    private ApprovalInstanceDto toInstanceDto(ApprovalInstance instance) {
        return new ApprovalInstanceDto(instance.getId(), instance.getBusinessType(), instance.getBusinessId(), instance.getProjectId(),
                instance.getTemplateCode(), instance.getTemplateVersion(), instance.getApprovalMode(), instance.getStatus(), instance.getCurrentNodeOrder(),
                tasks(instance.getId()).stream().map(task -> toTodoDto(instance, task)).toList(), instance.getStartedAt(), instance.getFinishedAt(), instance.getVersion());
    }

    private ApprovalTodoDto toTodoDto(ApprovalInstance instance, ApprovalTask task) {
        return new ApprovalTodoDto(task.getId(), instance.getId(), instance.getBusinessType(), instance.getBusinessId(), instance.getProjectId(),
                instance.getTemplateCode(), instance.getTemplateVersion(), instance.getApprovalMode(), task.getNodeCode(), task.getNodeName(),
                task.getResponsibilityCode(), task.getAssigneeUserId(), task.getAssigneeName(), task.getDecision(), task.getDueAt(),
                "PENDING".equals(task.getDecision()) && task.getDueAt() != null && task.getDueAt().isBefore(LocalDateTime.now()), task.getVersion());
    }

    private ApprovalInstance instanceEntity(Long id) {
        return requireInstance(id);
    }

    private PageQuery normalize(PageQuery pageQuery) {
        return pageQuery == null ? new PageQuery() : pageQuery;
    }

    private <T> PageResult<T> page(List<T> all, PageQuery page) {
        int start = Math.min((int) page.offset(), all.size());
        int end = Math.min(start + page.pageSizeValue(), all.size());
        return new PageResult<>(all.subList(start, end), all.size(), page.pageNoValue(), page.pageSizeValue());
    }

    private void validateNodes(List<ApprovalTemplateNodeRequest> nodes) {
        Set<String> codes = new HashSet<>();
        Set<String> responsibilities = new HashSet<>();
        for (ApprovalTemplateNodeRequest node : nodes) {
            if (node == null || !notBlank(node.nodeCode()) || !notBlank(node.nodeName()) || !notBlank(node.responsibilityCode())) {
                throw new BusinessException("审批节点编码、名称和项目职责不能为空");
            }
            if (!codes.add(upper(node.nodeCode()))) {
                throw new BusinessException("审批模板节点编码不能重复");
            }
            if (!responsibilities.add(upper(node.responsibilityCode()))) {
                throw new BusinessException("审批模板中同一项目职责只能配置一个节点");
            }
        }
    }

    private String requireBusinessType(String value) {
        String type = upper(value);
        if (!SUPPORTED_BUSINESS_TYPES.contains(type)) {
            throw new BusinessException("审批模板仅支持 CHANGE、SPECIAL_RELEASE、EXCEPTION 三种业务类型");
        }
        return type;
    }

    private String requireMode(String value) {
        String mode = notBlank(value) ? upper(value) : "SERIAL";
        if (!SUPPORTED_MODES.contains(mode)) {
            throw new BusinessException("审批模式只能是 SERIAL 或 COUNTERSIGN");
        }
        return mode;
    }

    private int normalizeSla(Integer value) {
        if (value == null) {
            return 1440;
        }
        if (value < 5 || value > 43200) {
            throw new BusinessException("审批 SLA 必须在 5 分钟至 30 天之间");
        }
        return value;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        return notBlank(value) ? value.trim() : null;
    }

    private LocalDateTime timestamp(Object value) {
        if (value instanceof java.sql.Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        if (value instanceof LocalDateTime time) {
            return time;
        }
        return null;
    }

    // 旧审批记录转换结果。
    private record LegacyDecision(String nodeCode, String decision, String opinion, Long approverId, String approverName,
                                  LocalDateTime approvedAt) {
    }
}
