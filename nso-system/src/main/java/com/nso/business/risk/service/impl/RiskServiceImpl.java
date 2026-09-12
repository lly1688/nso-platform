package com.nso.business.risk.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.business.core.PageSupport;
import com.nso.business.core.NsoDtos.RiskActionCloseRequest;
import com.nso.business.core.NsoDtos.RiskActionDto;
import com.nso.business.core.NsoDtos.RiskActionRequest;
import com.nso.business.core.NsoDtos.RiskDto;
import com.nso.business.core.NsoDtos.RiskDetailDto;
import com.nso.business.core.NsoDtos.RiskOverrideRequest;
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
import com.nso.business.support.BusinessNumberService;
import com.nso.business.task.domain.Task;
import com.nso.business.task.mapper.TaskMapper;
import com.nso.shared.exception.BusinessException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

@Service

// 风险管理 服务层处理
public class RiskServiceImpl implements IRiskService {
    // 风险数据映射
    private final RiskMapper riskMapper;
    // 风险操作数据映射
    private final RiskActionMapper riskActionMapper;
    // 项目数据映射
    private final ProjectMapper projectMapper;
    // 项目成员数据映射
    private final ProjectMemberMapper memberMapper;
    // 样品数据映射
    private final SampleMapper sampleMapper;
    // 任务数据映射
    private final TaskMapper taskMapper;
    // 项目数据范围
    private final ProjectDataScope dataScope;
    // 业务事件服务
    private final BusinessEventService events;
    // 业务Number服务
    private final BusinessNumberService numbers;
    // JDBC模板
    private final JdbcTemplate jdbc;

    public RiskServiceImpl(RiskMapper riskMapper,
                            RiskActionMapper riskActionMapper,
                            ProjectMapper projectMapper,
                            ProjectMemberMapper memberMapper,
                            SampleMapper sampleMapper,
                            TaskMapper taskMapper,
                            ProjectDataScope dataScope,
                            BusinessEventService events,
                            BusinessNumberService numbers,
                            JdbcTemplate jdbc) {
        this.riskMapper = riskMapper;
        this.riskActionMapper = riskActionMapper;
        this.projectMapper = projectMapper;
        this.memberMapper = memberMapper;
        this.sampleMapper = sampleMapper;
        this.taskMapper = taskMapper;
        this.dataScope = dataScope;
        this.events = events;
        this.numbers = numbers;
        this.jdbc = jdbc;
    }

    // 查询项目风险。
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

    // 按条件分页查询项目风险。
    @Override
    public PageResult<RiskDto> list(Long projectId, String level, String status, PageQuery pageQuery) {
        if (projectId != null) dataScope.requireAccess(projectId);
        PageQuery page = PageSupport.normalize(pageQuery);
        List<Long> visibleIds = dataScope.visibleProjectIds();
        if (visibleIds != null && visibleIds.isEmpty()) return PageResult.empty(page);
        Page<Risk> entityPage = riskMapper.selectPage(PageSupport.page(page), Wrappers.<Risk>lambdaQuery()
                .eq(projectId != null, Risk::getProjectId, projectId)
                .eq(level != null && !level.isBlank(), Risk::getLevel, level)
                .eq(status != null && !status.isBlank(), Risk::getStatus, status)
                .in(visibleIds != null, Risk::getProjectId, visibleIds == null ? List.of() : visibleIds)
                .orderByDesc(Risk::getCalculatedAt));
        return PageSupport.result(entityPage, page, this::toDto);
    }

    // 重新计算项目风险。
    @Override
    @Transactional
    public RiskDto calculate(Long projectId) {
        Project project = requireProject(projectId);
        String batchKey = LocalDate.now().toString();
        Risk existing = riskMapper.selectOne(Wrappers.<Risk>lambdaQuery().eq(Risk::getProjectId, projectId)
                .eq(Risk::getRuleCode, "DELIVERY_RISK_V1").eq(Risk::getBatchKey, batchKey).last("LIMIT 1"));
        if (existing != null) return toDto(existing);
        int score = 0;
        List<String> reasons = new ArrayList<>();
        List<RiskFactor> factors = new ArrayList<>();
        boolean forceSerious = false;
        if (project.getTargetDate() != null) {
            long days = ChronoUnit.DAYS.between(LocalDate.now(), project.getTargetDate());
            if (days < 0) { score += 40;
                forceSerious = true;
                reasons.add("项目已延期 " + (-days) + " 天");
                factors.add(new RiskFactor("DELIVERY_OVERDUE", "项目已延期", String.valueOf(days), 40, 40));
            }
            else if (days <= 2) {
                score += 20;
                reasons.add("距离交期仅 " + days + " 天");
                factors.add(new RiskFactor("REMAINING_WORK", "剩余工作不足", String.valueOf(days), 20, 20));
            }
            else if (days <= 7) {
                score += 15;
                reasons.add("交期临近 " + days + " 天");
            }
        }
        long waiting = sampleMapper.selectCount(Wrappers.<Sample>lambdaQuery().eq(Sample::getProjectId, projectId).eq(Sample::getStatus, "WAIT_CUSTOMER_CONFIRM"));

        if (waiting > 0 && project.getTargetDate() != null && ChronoUnit.DAYS.between(LocalDate.now(), project.getTargetDate()) <= 3) {
            score += 30;
            reasons.add("临近投产仍有待客户确认样品");
            factors.add(new RiskFactor("SAMPLE_CONFIRM", "样品未确认", String.valueOf(waiting), 30, 30));
        }
        long severeChanges = count("SELECT COUNT(*) FROM nso_change_order WHERE tenant_id=? AND project_id=? AND deleted=0 AND status IN ('WAIT_APPROVAL','EXECUTING','PENDING_VERIFICATION') AND urgency IN ('HIGH','URGENT','CRITICAL')", projectId);

        if (severeChanges > 0) {
            score += 25;
            reasons.add("存在未关闭高风险变更");
            factors.add(new RiskFactor("HIGH_CHANGE", "高风险变更", String.valueOf(severeChanges), 25, 25));
        }
        long materialLate = count("SELECT COUNT(*) FROM nso_purchase_task WHERE tenant_id=? AND project_id=? AND plan_arrival_date > ?", projectId, project.getPlanStartDate());

        if (materialLate > 0) {
            score += 25;
            reasons.add("关键物料预计晚于开工");
            factors.add(new RiskFactor("MATERIAL_LATE", "关键物料延期", String.valueOf(materialLate), 25, 25));
        }
        if (project.getCurrentDocVersionId() == null && project.getPlanStartDate() != null && ChronoUnit.DAYS.between(LocalDate.now(), project.getPlanStartDate()) <= 2) {
            score += 20;
            reasons.add("临近开工尚无生效技术包");
            factors.add(new RiskFactor("TECH_PACKAGE", "技术包未发布", "missing", 20, 20));
        }

        long blocked = taskMapper.selectCount(Wrappers.<Task>lambdaQuery()
            .eq(Task::getProjectId, projectId).in(Task::getStatus, List.of("BLOCKED", "PAUSED")));

        if (blocked > 0) {
            int delta = Math.min(30, (int) blocked * 10);
            score += delta;
            reasons.add("存在 " + blocked + " 个受阻任务");
            factors.add(new RiskFactor("BLOCKED_TASK", "受阻任务", String.valueOf(blocked), delta, 30));
        }
        long overdue = taskMapper.selectCount(Wrappers.<Task>lambdaQuery()
            .eq(Task::getProjectId, projectId).lt(Task::getPlanFinish, LocalDate.now())
            .notIn(Task::getStatus, List.of("DONE", "CANCELLED")));

        if (overdue > 0) {
            int delta = Math.min(30, (int) overdue * 10);
            score += delta;
            reasons.add("存在 " + overdue + " 个逾期任务");
            factors.add(new RiskFactor("OVERDUE_TASK", "关键任务超期", String.valueOf(overdue), delta, 30));
        }

        if (factors.isEmpty()) {
            score -= 10;
            reasons.add("当前未命中交期风险因素");
            factors.add(new RiskFactor("ALL_CLEAR", "全部条件满足", "true", -10, -10));
        }

        score = Math.max(0, Math.min(100, score));

        String level = forceSerious || score >= 70 ? "SERIOUS" : score >= 40 ? "HIGH" : score >= 20 ? "MEDIUM" : "LOW";
        Risk risk = new Risk();

        risk.setTenantId(TenantContext.tenantId());

        risk.setProjectId(projectId);

        risk.setRiskNo(numbers.next("RISK"));

        risk.setLevel(level);

        risk.setScore(score);

        risk.setReasons(jsonArray(reasons));

        risk.setSuggestion(score >= 50 ? "立即召开项目风险处置会并更新执行计划" : "按计划持续跟进");

        risk.setStatus("OPEN");

        risk.setRuleCode("DELIVERY_RISK_V1");

        risk.setRuleVersion("V1");

        risk.setBatchKey(batchKey);

        risk.setInputSnapshot("{\"targetDate\":\"" + project.getTargetDate() + "\",\"waitingSamples\":" + waiting + ",\"overdueTasks\":" + overdue + "}");

        risk.setCalculatedAt(LocalDateTime.now());

        riskMapper.insert(risk);

        for (RiskFactor factor : factors) jdbc.update("INSERT INTO nso_risk_detail (tenant_id,risk_id,factor_code,factor_name,raw_value,score_delta,score_cap,matched_flag) VALUES (?,?,?,?,?,?,?,1)",
                TenantContext.tenantId(),
                risk.getId(),
                factor.code(),
                factor.name(),
                factor.rawValue(),
                factor.delta(),
                factor.cap());

        jdbc.update("INSERT INTO nso_rule_execution (tenant_id,rule_code,rule_version,business_type,business_id,batch_key,input_snapshot,decision_summary,status,duration_ms) VALUES (?,?,?,?,?,?,?,?, 'SUCCESS',0)",
                TenantContext.tenantId(), "DELIVERY_RISK_V1", "V1", "PROJECT", projectId, batchKey, risk.getInputSnapshot(), level + ":" + score);
        project.setRiskLevel(level);
        project.setRiskScore(score);
        projectMapper.updateById(project);
        events.record(projectId, "RISK", risk.getId(), "RISK_CALCULATED", null, level, "已更新交期风险评估");
        return toDto(risk);
    }

    // 查询风险明细。
    @Override
    public PageResult<RiskDetailDto> details(Long riskId) {
        requireRisk(riskId);
        List<RiskDetailDto> rows = jdbc.query("SELECT factor_code,factor_name,raw_value,score_delta,score_cap,matched_flag FROM nso_risk_detail WHERE tenant_id=? AND risk_id=? ORDER BY id",
                (rs, row) -> new RiskDetailDto(rs.getString(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getInt(4),
                        (Integer) rs.getObject(5),
                        rs.getBoolean(6)),
                TenantContext.tenantId(), riskId);
        return new PageResult<>(rows, rows.size());
    }

    // 分页查询风险明细。
    @Override
    public PageResult<RiskDetailDto> details(Long riskId, PageQuery pageQuery) {
        requireRisk(riskId);
        PageQuery page = PageSupport.normalize(pageQuery);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM nso_risk_detail WHERE tenant_id=? AND risk_id=?",
                Long.class, TenantContext.tenantId(), riskId);
        List<RiskDetailDto> rows = jdbc.query("SELECT factor_code,factor_name,raw_value,score_delta,score_cap,matched_flag FROM nso_risk_detail WHERE tenant_id=? AND risk_id=? ORDER BY id LIMIT ? OFFSET ?",
                (rs, row) -> new RiskDetailDto(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4),
                        (Integer) rs.getObject(5), rs.getBoolean(6)),
                TenantContext.tenantId(), riskId, page.pageSizeValue(), page.offset());
        return new PageResult<>(rows, total == null ? 0 : total, page.pageNoValue(), page.pageSizeValue());
    }

    // 人工调整风险等级。
    @Override
    @Transactional
    public RiskDto override(Long riskId, RiskOverrideRequest request) {
        Risk risk = requireRisk(riskId);
        dataScope.requireProjectRole(risk.getProjectId(), "PROJECT_MANAGER");
        if (request == null || blank(request.level()) || blank(request.reason()) || request.expiresAt() == null || !request.expiresAt().isAfter(LocalDateTime.now())) throw new BusinessException("风险覆盖必须填写等级、原因和未来有效期");
        if (!List.of("LOW", "MEDIUM", "HIGH", "SERIOUS").contains(request.level().toUpperCase())) throw new BusinessException("风险覆盖等级不合法");
        risk.setOverrideLevel(request.level().toUpperCase());
        risk.setOverrideReason(request.reason().trim());
        risk.setOverrideExpiresAt(request.expiresAt());
        risk.setLevel(risk.getOverrideLevel());
        riskMapper.updateById(risk);
        events.record(risk.getProjectId(), "RISK", riskId, "RISK_MANUAL_OVERRIDE", null, risk.getLevel(), request.reason().trim());
        return toDto(risk);
    }

    // 恢复已到期的人工调整。
    @Override
    @Transactional
    public int restoreExpiredOverrides() {
        return jdbc.update("UPDATE nso_risk SET level=CASE WHEN score>=70 THEN 'SERIOUS' WHEN score>=40 THEN 'HIGH' WHEN score>=20 THEN 'MEDIUM' ELSE 'LOW' END, override_level=NULL, override_reason=NULL, override_expires_at=NULL WHERE tenant_id=? AND override_expires_at IS NOT NULL AND override_expires_at<=NOW()",
                TenantContext.tenantId());
    }

    // 查询风险处置措施。
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

    // 分页查询风险处置措施。
    @Override
    public PageResult<RiskActionDto> actions(Long riskId, PageQuery pageQuery) {
        Risk risk = requireRisk(riskId);
        PageQuery page = PageSupport.normalize(pageQuery);
        Page<RiskAction> entityPage = riskActionMapper.selectPage(PageSupport.page(page),
                Wrappers.<RiskAction>lambdaQuery().eq(RiskAction::getRiskId, risk.getId())
                        .eq(RiskAction::getTenantId, TenantContext.tenantId())
                        .orderByDesc(RiskAction::getCreatedAt));
        return PageSupport.result(entityPage, page, this::toActionDto);
    }

    // 创建风险处置措施。
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

    // 关闭风险处置措施。
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
        if ("CLOSED".equals(action.getStatus()))
            return toActionDto(action);
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

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
    private long count(String sql, Object... values) {
        Object[] params = new Object[values.length + 1];
        params[0] = TenantContext.tenantId();
        System.arraycopy(values, 0, params, 1, values.length);
        Integer row = jdbc.queryForObject(sql, Integer.class, params);
        return row == null ? 0 : row;
    }

    // 风险计算因子。
    private record RiskFactor(String code, String name, String rawValue, int delta, int cap) {

    }
}
