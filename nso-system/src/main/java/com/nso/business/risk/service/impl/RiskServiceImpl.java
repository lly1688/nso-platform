package com.nso.business.risk.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.business.core.NsoDtos.RiskActionCloseRequest;
import com.nso.business.core.NsoDtos.RiskActionDto;
import com.nso.business.core.NsoDtos.RiskActionRequest;
import com.nso.business.core.NsoDtos.RiskDto;
import com.nso.business.core.TenantContext;
import com.nso.business.project.domain.Project;
import com.nso.business.project.domain.ProjectMember;
import com.nso.business.project.mapper.ProjectMapper;
import com.nso.business.project.mapper.ProjectMemberMapper;
import com.nso.business.project.service.ProjectDataScope;
import com.nso.business.risk.domain.Risk;
import com.nso.business.risk.domain.RiskAction;
import com.nso.business.risk.mapper.RiskActionMapper;
import com.nso.business.risk.mapper.RiskMapper;
import com.nso.business.risk.service.IRiskService;
import com.nso.business.sample.domain.Sample;
import com.nso.business.sample.mapper.SampleMapper;
import com.nso.business.support.BusinessEventService;
import com.nso.business.task.domain.Task;
import com.nso.business.task.mapper.TaskMapper;
import com.nso.common.exception.BusinessException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RiskServiceImpl implements IRiskService {
    private final RiskMapper riskMapper;
    private final RiskActionMapper riskActionMapper;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper memberMapper;
    private final SampleMapper sampleMapper;
    private final TaskMapper taskMapper;
    private final ProjectDataScope dataScope;
    private final BusinessEventService events;

    public RiskServiceImpl(RiskMapper riskMapper, RiskActionMapper riskActionMapper, ProjectMapper projectMapper,
                           ProjectMemberMapper memberMapper, SampleMapper sampleMapper, TaskMapper taskMapper,
                           ProjectDataScope dataScope, BusinessEventService events) {
        this.riskMapper = riskMapper;
        this.riskActionMapper = riskActionMapper;
        this.projectMapper = projectMapper;
        this.memberMapper = memberMapper;
        this.sampleMapper = sampleMapper;
        this.taskMapper = taskMapper;
        this.dataScope = dataScope;
        this.events = events;
    }

    @Override
    public PageResult<RiskDto> list(Long projectId) {
        if (projectId != null) dataScope.requireAccess(projectId);
        List<Long> visibleIds = dataScope.visibleProjectIds();
        if (visibleIds != null && visibleIds.isEmpty()) return new PageResult<>(List.of(), 0);
        List<RiskDto> rows = riskMapper.selectList(Wrappers.<Risk>lambdaQuery()
                .eq(projectId != null, Risk::getProjectId, projectId)
                .in(visibleIds != null, Risk::getProjectId, visibleIds == null ? List.of() : visibleIds)
                .orderByDesc(Risk::getCalculatedAt))
            .stream().map(this::toDto).toList();
        return new PageResult<>(rows, rows.size());
    }

    @Override
    @Transactional
    public RiskDto calculate(Long projectId) {
        Project project = requireProject(projectId);
        int score = 0;
        List<String> reasons = new ArrayList<>();
        if (project.getTargetDate() != null) {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), project.getTargetDate());
            if (days < 0) { score += 50; reasons.add("交期已逾期 " + (-days) + " 天"); }
            else if (days <= 2) { score += 35; reasons.add("距离交期仅 " + days + " 天"); }
            else if (days <= 7) { score += 15; reasons.add("交期临近 " + days + " 天"); }
        }
        long waiting = sampleMapper.selectCount(Wrappers.<Sample>lambdaQuery()
            .eq(Sample::getProjectId, projectId).eq(Sample::getStatus, "WAIT_CUSTOMER_CONFIRM"));
        if (waiting > 0) { score += 35; reasons.add("存在 " + waiting + " 个待客户确认样品"); }
        long blocked = taskMapper.selectCount(Wrappers.<Task>lambdaQuery()
            .eq(Task::getProjectId, projectId).in(Task::getStatus, List.of("BLOCKED", "PAUSED")));
        if (blocked > 0) { score += 25; reasons.add("存在 " + blocked + " 个受阻任务"); }
        long overdue = taskMapper.selectCount(Wrappers.<Task>lambdaQuery()
            .eq(Task::getProjectId, projectId).lt(Task::getPlanFinish, LocalDate.now())
            .notIn(Task::getStatus, List.of("DONE", "CANCELLED")));
        if (overdue > 0) { score += 20; reasons.add("存在 " + overdue + " 个逾期任务"); }
        String level = score >= 80 ? "SERIOUS" : score >= 50 ? "HIGH" : score >= 25 ? "MEDIUM" : "LOW";
        Risk risk = new Risk();
        risk.setTenantId(TenantContext.tenantId());
        risk.setProjectId(projectId);
        risk.setLevel(level);
        risk.setScore(score);
        risk.setReasons(jsonArray(reasons));
        risk.setSuggestion(score >= 50 ? "立即召开项目风险处置会并更新执行计划" : "按计划持续跟踪");
        risk.setStatus("OPEN");
        risk.setRuleCode("DELIVERY_RISK_V1");
        risk.setRuleVersion("V1");
        risk.setCalculatedAt(LocalDateTime.now());
        riskMapper.insert(risk);
        project.setRiskLevel(level);
        project.setRiskScore(score);
        projectMapper.updateById(project);
        events.record(projectId, "RISK", risk.getId(), "RISK_CALCULATED", null, level, "已更新交期风险评估");
        return toDto(risk);
    }

    @Override
    public PageResult<RiskActionDto> actions(Long riskId) {
        Risk risk = requireRisk(riskId);
        List<RiskActionDto> rows = riskActionMapper.selectList(Wrappers.<RiskAction>lambdaQuery()
                .eq(RiskAction::getRiskId, risk.getId())
                .eq(RiskAction::getTenantId, TenantContext.tenantId())
                .orderByDesc(RiskAction::getCreatedAt))
            .stream().map(this::toActionDto).toList();
        return new PageResult<>(rows, rows.size());
    }

    @Override
    @Transactional
    public RiskActionDto createAction(Long riskId, RiskActionRequest request) {
        Risk risk = requireRisk(riskId);
        dataScope.requireProjectRole(risk.getProjectId(), "PROJECT_MANAGER");
        if (request == null || blank(request.actionPlan()) || request.responsibleUserId() == null
                || request.planFinishTime() == null || blank(request.idempotencyKey())) {
            throw new BusinessException("处置措施、责任人、计划完成时间和幂等键不能为空");
        }
        RiskAction existing = riskActionMapper.selectOne(Wrappers.<RiskAction>lambdaQuery()
            .eq(RiskAction::getTenantId, TenantContext.tenantId())
            .eq(RiskAction::getRiskId, riskId)
            .eq(RiskAction::getIdempotencyKey, request.idempotencyKey()));
        if (existing != null) return toActionDto(existing);

        ProjectMember member = dataScope.requireActiveMember(risk.getProjectId(), request.responsibleUserId());
        RiskAction action = new RiskAction();
        action.setTenantId(TenantContext.tenantId());
        action.setRiskId(riskId);
        action.setActionPlan(request.actionPlan().trim());
        action.setResponsibleUserId(member.getUserId());
        action.setResponsibleName(member.getMemberName());
        action.setPlanFinishTime(request.planFinishTime());
        action.setStatus("OPEN");
        action.setIdempotencyKey(request.idempotencyKey());
        action.setVersion(0);
        riskActionMapper.insert(action);
        if (!"OPEN".equals(risk.getStatus())) {
            risk.setStatus("OPEN");
            riskMapper.updateById(risk);
        }
        events.record(risk.getProjectId(), "RISK_ACTION", action.getId(), "RISK_ACTION_CREATED", null, "OPEN", "已创建风险处置计划");
        return toActionDto(action);
    }

    @Override
    @Transactional
    public RiskActionDto closeAction(Long actionId, RiskActionCloseRequest request) {
        RiskAction action = riskActionMapper.selectById(actionId);
        if (action == null || !Long.valueOf(TenantContext.tenantId()).equals(action.getTenantId())) {
            throw new BusinessException("风险处置记录不存在");
        }
        Risk risk = requireRisk(action.getRiskId());
        if (request == null || blank(request.closeSummary())) {
            throw new BusinessException("关闭风险处置时必须填写关闭说明");
        }
        if (request.version() != null && !request.version().equals(action.getVersion())) {
            throw BusinessException.ruleBlock("OPTIMISTIC_LOCK", "风险处置已被其他用户更新", String.valueOf(request.version()), String.valueOf(action.getVersion()), "刷新后重试");
        }
        if (!canClose(risk.getProjectId(), action)) {
            throw BusinessException.accessDenied("RISK_ACTION_SCOPE", "仅项目经理或处置责任人可关闭该记录", String.valueOf(actionId), "项目经理或责任人", "由责任人处理或联系项目经理");
        }
        if ("CLOSED".equals(action.getStatus())) return toActionDto(action);
        action.setStatus("CLOSED");
        action.setCloseSummary(request.closeSummary().trim());
        action.setClosedBy(TenantContext.userId());
        action.setClosedAt(LocalDateTime.now());
        if (riskActionMapper.updateById(action) != 1) {
            throw BusinessException.ruleBlock("OPTIMISTIC_LOCK", "风险处置已被其他用户更新", "stale", "latest", "刷新后重试");
        }
        long openActions = riskActionMapper.selectCount(Wrappers.<RiskAction>lambdaQuery()
            .eq(RiskAction::getTenantId, TenantContext.tenantId())
            .eq(RiskAction::getRiskId, risk.getId())
            .ne(RiskAction::getStatus, "CLOSED"));
        if (openActions == 0 && !"CLOSED".equals(risk.getStatus())) {
            risk.setStatus("CLOSED");
            riskMapper.updateById(risk);
        }
        events.record(risk.getProjectId(), "RISK_ACTION", action.getId(), "RISK_ACTION_CLOSED", "OPEN", "CLOSED", "风险处置已关闭");
        return toActionDto(action);
    }

    private boolean canClose(Long projectId, RiskAction action) {
        if (TenantContext.hasAnyRole("admin")) return true;
        if (TenantContext.userId() != null && TenantContext.userId().equals(action.getResponsibleUserId())) return true;
        return memberMapper.selectCount(Wrappers.<ProjectMember>lambdaQuery()
            .eq(ProjectMember::getProjectId, projectId)
            .eq(ProjectMember::getUserId, TenantContext.userId())
            .eq(ProjectMember::getProjectRole, "PROJECT_MANAGER")
            .eq(ProjectMember::getStatus, "ACTIVE")) > 0;
    }

    private Project requireProject(Long id) {
        Project project = projectMapper.selectById(id);
        if (project == null) throw new BusinessException("项目不存在");
        dataScope.requireAccess(id);
        return project;
    }

    private Risk requireRisk(Long id) {
        Risk risk = riskMapper.selectById(id);
        if (risk == null || !Long.valueOf(TenantContext.tenantId()).equals(risk.getTenantId())) throw new BusinessException("风险不存在");
        dataScope.requireAccess(risk.getProjectId());
        return risk;
    }

    private String jsonArray(List<String> reasons) {
        return "[" + reasons.stream().map(item -> "\"" + item.replace("\"", "\\\"") + "\"")
            .collect(java.util.stream.Collectors.joining(",")) + "]";
    }

    private RiskDto toDto(Risk risk) {
        String raw = risk.getReasons();
        List<String> reasons = raw == null || raw.length() < 2 ? List.of()
            : List.of(raw.substring(1, raw.length() - 1).replace("\"", "").split(","));
        return new RiskDto(risk.getId(), risk.getProjectId(), requireProject(risk.getProjectId()).getProjectNo(),
            risk.getLevel(), risk.getScore(), reasons, risk.getSuggestion(), risk.getStatus());
    }

    private RiskActionDto toActionDto(RiskAction action) {
        return new RiskActionDto(action.getId(), action.getRiskId(), action.getActionPlan(), action.getResponsibleUserId(),
            action.getResponsibleName(), action.getPlanFinishTime(), action.getCloseSummary(), action.getStatus(),
            action.getVersion(), action.getCreatedAt(), action.getClosedAt());
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
