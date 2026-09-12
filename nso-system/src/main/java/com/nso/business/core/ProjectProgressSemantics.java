package com.nso.business.core;

import java.time.LocalDate;
import java.util.Map;

/**
 * 项目推进主线使用的统一业务语义。
 *
 * <p>该类只做确定性映射，数据库查询和权限范围由调用方负责。</p>
 */
public final class ProjectProgressSemantics {

    private static final Map<String, String> STAGE_LABELS = Map.ofEntries(
            Map.entry("REQUIREMENT", "需求评审"),
            Map.entry("TECHNICAL", "技术包"),
            Map.entry("SAMPLE", "打样检验"),
            Map.entry("CONFIRM", "客户确认"),
            Map.entry("EXECUTION", "生产执行"),
            Map.entry("DELIVERY", "交付"),
            Map.entry("ARCHIVE", "归档"));

    private static final Map<String, String> COMPLETION_CRITERIA = Map.ofEntries(
            Map.entry("REQUIREMENT", "需求范围、交期和责任人已确认"),
            Map.entry("TECHNICAL", "当前技术包已发布且未存在版本冲突"),
            Map.entry("SAMPLE", "样品检验完成并形成质量结论"),
            Map.entry("CONFIRM", "完成样品确认并记录客户结论"),
            Map.entry("EXECUTION", "生产任务完成且异常已关闭"),
            Map.entry("DELIVERY", "交付预检通过并完成签收反馈"),
            Map.entry("ARCHIVE", "交付记录和项目资料已归档"));

    private ProjectProgressSemantics() {
    }

    public static Progress describe(String stage, String status, LocalDate targetDate,
                                    int blockerCount, int pendingDecisionCount) {
        String normalizedStage = normalize(stage);
        String normalizedStatus = normalize(status);
        String dueState = terminal(normalizedStatus) ? "ON_TRACK" : dueState(targetDate);
        String riskLevel = blockerCount > 0 || "OVERDUE".equals(dueState) ? "HIGH"
                : "DUE_SOON".equals(dueState) || pendingDecisionCount > 0 ? "MEDIUM" : "LOW";
        Recommendation recommendation = recommendation(normalizedStage, normalizedStatus, dueState,
                blockerCount, pendingDecisionCount);
        return new Progress(normalizedStage, stageLabel(normalizedStage), normalizedStatus,
                COMPLETION_CRITERIA.getOrDefault(normalizedStage, "完成当前阶段的必需事项"),
                dueState, riskLevel, blockerCount, pendingDecisionCount, recommendation);
    }

    public static String stageLabel(String stage) {
        String normalized = normalize(stage);
        return STAGE_LABELS.getOrDefault(normalized, normalized.isBlank() ? "未设置阶段" : normalized);
    }

    public static String dueState(LocalDate targetDate) {
        if (targetDate == null) {
            return "NO_DATE";
        }
        if (targetDate.isBefore(LocalDate.now())) {
            return "OVERDUE";
        }
        if (!targetDate.isAfter(LocalDate.now().plusDays(3))) {
            return "DUE_SOON";
        }
        return "ON_TRACK";
    }

    private static Recommendation recommendation(String stage, String status, String dueState,
                                                 int blockerCount, int pendingDecisionCount) {
        if (terminal(status)) {
            return new Recommendation("VIEW_HISTORY", "查看项目记录", "项目已结束，查看交付与归档记录", "/projects");
        }
        if ("OVERDUE".equals(dueState)) {
            return new Recommendation("HANDLE_OVERDUE", "处理逾期阻塞", "优先处理已超过目标交期的事项", "/actions?dueState=OVERDUE");
        }
        if (blockerCount > 0) {
            return new Recommendation("CLEAR_BLOCKER", "处理阻塞", "先清除影响阶段推进的阻塞项", "/actions?priority=CRITICAL");
        }
        if (pendingDecisionCount > 0) {
            return new Recommendation("DECIDE", "处理待决策", "完成待决策事项后继续推进项目", "/actions?tab=approvals");
        }
        if ("DRAFT".equals(status)) {
            return new Recommendation("SUBMIT_REVIEW", "提交需求评审", "补齐项目范围和责任人后提交评审", "/projects");
        }
        if ("CUSTOMER_CONFIRMING".equals(status) || "CONFIRM".equals(stage)) {
            return new Recommendation("REQUEST_CONFIRMATION", "优先催办客户确认", "获取客户结论并回填确认记录", "/samples");
        }
        if ("PENDING_DELIVERY".equals(status) || "DELIVERY".equals(stage)) {
            return new Recommendation("RUN_DELIVERY_PRECHECK", "开始交付预检", "完成预检后提交交付记录", "/tasks/deliveries");
        }
        return new Recommendation("ADVANCE_STAGE", "推进当前阶段", "完成当前阶段完成条件", "/projects");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private static boolean terminal(String status) {
        return switch (status) {
            case "COMPLETED", "CLOSED", "ARCHIVED", "CANCELLED" -> true;
            default -> false;
        };
    }

    public record Progress(String stage, String stageLabel, String status, String completionCriteria,
                           String dueState, String riskLevel, int blockerCount, int pendingDecisionCount,
                           Recommendation recommendedAction) {
    }

    public record Recommendation(String code, String label, String reason, String route) {
    }
}
