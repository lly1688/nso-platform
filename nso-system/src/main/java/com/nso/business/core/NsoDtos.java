package com.nso.business.core;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class NsoDtos {

    private NsoDtos() {
    }

    public record PageResult<T>(List<T> list, long total, int pageNo, int pageSize) {
        public PageResult(List<T> list, long total) {
            this(list, total, 1, list == null ? 0 : list.size());
        }

        @JsonIgnore
        public List<T> records() {
            return list;
        }
    }

    public record LoginRequest(String username, String password, String code) {
    }

    public record RefreshRequest(String refreshToken) {
    }

    public record AuthSession(
            Long userId,
            String username,
            String nickname,
            String clientId,
            List<String> roles,
            List<String> permissions,
            List<String> menus,
            String accessToken,
            String refreshToken) {
    }

    public record CustomerDto(
            Long id,
            String customerCode,
            String name,
            String industry,
            String contactName,
            String phone,
            String status) {
    }

    public record CustomerRequest(
            String name,
            String industry,
            String contactName,
            String phone,
            String status) {
    }

    public record ProjectDto(
            Long id,
            String projectNo,
            Long customerId,
            String customerName,
            String productName,
            Integer quantity,
            LocalDate targetDate,
            String ownerName,
            String status,
            String stage,
            String priority,
            String riskLevel,
            String sampleStatus,
            Integer changeCount,
            Long daysLeft) {
    }

    public record ProjectRequest(
            Long customerId,
            String customerName,
            String productName,
            Integer quantity,
            LocalDate targetDate,
            String ownerName,
            String priority,
            List<String> requirements) {
    }

    public record TimelineItem(
            Long id,
            Long projectId,
            String eventType,
            String title,
            String summary,
            String operatorName,
            LocalDateTime occurredAt) {
    }

    public record ProjectDetail(
            ProjectDto project,
            List<RequirementDto> requirements,
            List<ProjectMemberDto> members,
            List<DocumentVersionDto> documents,
            List<BomDto> boms,
            List<ProcessRouteDto> processRoutes,
            List<InspectionSpecDto> inspectionSpecs,
            List<SampleDto> samples,
            List<ChangeOrderDto> changes,
            List<TaskDto> tasks,
            List<RiskDto> risks,
            List<TimelineItem> timeline) {
    }

    public record DocumentVersionDto(
            Long id,
            Long projectId,
            Long fileObjectId,
            String projectNo,
            String fileName,
            String fileType,
            String versionNo,
            String status,
            LocalDate effectiveDate,
            String changeSummary,
            Boolean currentVersion,
            String sha256,
            String downloadUrl) {
    }

    public record DocumentVersionRequest(
            String fileName,
            String fileType,
            String versionNo,
            LocalDate effectiveDate,
            String changeSummary,
            Long fileObjectId) {
    }

    public record SampleDto(
            Long id,
            Long projectId,
            String projectNo,
            String sampleNo,
            String purpose,
            Integer quantity,
            LocalDate planFinishDate,
            String referencedVersion,
            String status,
            String confirmConclusion,
            String responsibleName,
            String issueSummary) {
    }

    public record CustomerProjectDto(
            Long id,
            String projectNo,
            String productName,
            LocalDate targetDate,
            String status,
            String stage,
            String sampleStatus) {
    }

    public record CustomerSampleDto(
            Long id,
            Long projectId,
            String sampleNo,
            String purpose,
            Integer quantity,
            LocalDate planFinishDate,
            String referencedVersion,
            String status,
            String confirmConclusion) {
    }

    public record PublicSampleConfirmationDto(
            Long sampleId,
            String sampleNo,
            String purpose,
            Integer quantity,
            LocalDate planFinishDate,
            String referencedVersion,
            String status,
            String confirmConclusion) {
    }

    public record SampleRequest(
            Long projectId,
            String purpose,
            Integer quantity,
            LocalDate planFinishDate,
            String referencedVersion,
            String responsibleName) {
    }

    public record SampleConfirmRequest(String conclusion, String opinion, String confirmer, Long evidenceFileId) {
        public SampleConfirmRequest(String conclusion, String opinion, String confirmer) {
            this(conclusion, opinion, confirmer, null);
        }
    }

    public record RequirementDto(
            Long id,
            Long projectId,
            String category,
            String content,
            String confirmStatus,
            String lastReason) {
    }

    public record RequirementRequest(String category, String content, String confirmStatus, String reason) {
    }

    public record ProjectMemberDto(
            Long id,
            Long projectId,
            Long userId,
            String memberName,
            String projectRole,
            String departmentName,
            String status) {
    }

    public record ProjectMemberRequest(Long userId, String memberName, String projectRole, String departmentName) {
        public ProjectMemberRequest(String memberName, String projectRole, String departmentName) {
            this(null, memberName, projectRole, departmentName);
        }
    }

    public record BomItemDto(
            Long id,
            String materialCode,
            String materialName,
            String specification,
            Double quantity,
            String unit,
            String sourceType,
            String substituteCode) {
    }

    public record BomItemRequest(
            String materialCode,
            String materialName,
            String specification,
            Double quantity,
            String unit,
            String sourceType,
            String substituteCode) {
    }

    public record BomDto(
            Long id,
            Long projectId,
            String projectNo,
            String bomNo,
            String versionNo,
            Long boundDocVersionId,
            String status,
            List<BomItemDto> items) {
    }

    public record BomRequest(
            Long projectId,
            String bomNo,
            String versionNo,
            Long boundDocVersionId,
            List<BomItemRequest> items) {
    }

    public record ProcessStepDto(
            Long id,
            Integer stepNo,
            String stepName,
            String workInstruction,
            String equipmentName,
            Double standardHours,
            Boolean outsourceFlag,
            Boolean inspectionPoint) {
    }

    public record ProcessStepRequest(
            Integer stepNo,
            String stepName,
            String workInstruction,
            String equipmentName,
            Double standardHours,
            Boolean outsourceFlag,
            Boolean inspectionPoint) {
    }

    public record ProcessRouteDto(
            Long id,
            Long projectId,
            String projectNo,
            String routeNo,
            String versionNo,
            Long boundDocVersionId,
            String status,
            List<ProcessStepDto> steps) {
    }

    public record ProcessRouteRequest(
            Long projectId,
            String routeNo,
            String versionNo,
            Long boundDocVersionId,
            List<ProcessStepRequest> steps) {
    }

    public record InspectionItemDto(
            Long id,
            String itemName,
            String standardValue,
            String samplingRule,
            Long attachmentFileId) {
    }

    public record InspectionItemRequest(
            String itemName,
            String standardValue,
            String samplingRule,
            Long attachmentFileId) {
    }

    public record InspectionSpecDto(
            Long id,
            Long projectId,
            String projectNo,
            String specNo,
            String versionNo,
            Long boundDocVersionId,
            String status,
            List<InspectionItemDto> items) {
    }

    public record InspectionSpecRequest(
            Long projectId,
            String specNo,
            String versionNo,
            Long boundDocVersionId,
            List<InspectionItemRequest> items) {
    }

    public record SampleCheckDto(
            Long id,
            Long sampleId,
            String checkItem,
            String measuredValue,
            String result,
            String issueSummary,
            String correctiveAction,
            String checkerName,
            LocalDateTime checkedAt) {
    }

    public record SampleCheckRequest(
            String checkItem,
            String measuredValue,
            String result,
            String issueSummary,
            String correctiveAction,
            String checkerName) {
    }

    public record ConfirmationTokenDto(
            String token,
            Long sampleId,
            LocalDateTime expireAt,
            Integer maxUseCount,
            Integer usedCount,
            String status) {
    }

    public record SpecialReleaseRequest(String releaseScope, LocalDateTime validUntil, String riskStatement, String reason) {
    }

    public record SpecialReleaseDto(Long id, Long projectId, Long sampleId, Long applicantUserId, Long approverUserId,
                                    String releaseScope, LocalDateTime validUntil, String riskStatement,
                                    String reason, String status) {
    }

    public record ChangeOrderDto(
            Long id,
            Long projectId,
            String projectNo,
            String changeNo,
            String changeType,
            String urgency,
            String beforeContent,
            String afterContent,
            String reason,
            String status,
            Integer delayDays,
            Integer reworkQty,
            Integer impactCount,
            Integer feedbackCount) {
    }

    public record ChangeRequest(
            Long projectId,
            String changeType,
            String urgency,
            String beforeContent,
            String afterContent,
            String reason) {
    }

    public record ChangeImpactDto(
            Long id,
            Long changeId,
            String objectType,
            String objectName,
            String departmentName,
            String suggestedAction,
            String status,
            String feedbackResult,
            String responsibleName,
            Integer version) {
    }

    public record ChangeFeedbackRequest(
            Long impactId,
            String result,
            String plan,
            Integer delayDays,
            Integer reworkQty,
            String responsibleName,
            Integer version) {
        public ChangeFeedbackRequest(Long impactId, String result, String plan, Integer delayDays, Integer reworkQty, String responsibleName) {
            this(impactId, result, plan, delayDays, reworkQty, responsibleName, null);
        }
    }

    public record ChangeApprovalRequest(String decision, String opinion) {
    }

    public record TaskDto(
            Long id,
            Long projectId,
            String projectNo,
            String taskNo,
            String taskType,
            String title,
            String referencedVersion,
            String status,
            String responsibleName,
            LocalDate planStart,
            LocalDate planFinish,
            String blockReason,
            Integer version) {
    }

    public record TaskFeedbackRequest(String result, String notes, Integer version) {
        public TaskFeedbackRequest(String result, String notes) {
            this(result, notes, null);
        }
    }

    public record TaskActionRequest(String notes, Integer version) {
        public TaskActionRequest(String notes) {
            this(notes, null);
        }
    }

    public record ExecutionTaskRequest(
            Long projectId,
            String taskType,
            String title,
            String referencedVersion,
            String responsibleName,
            Long assigneeId,
            LocalDate planStart,
            LocalDate planFinish,
            String supplierName) {
    }

    public record ExceptionReportRequest(
            Long taskId,
            Long projectId,
            String exceptionType,
            String summary,
            String imageUrl,
            String reporterName) {
    }

    public record DeliveryRecordDto(
            Long id,
            Long projectId,
            String projectNo,
            Integer quantity,
            String logisticsNo,
            String receiver,
            String feedback,
            String status,
            LocalDateTime shippedAt) {
    }

    public record DeliveryRequest(
            Long projectId,
            Integer quantity,
            String logisticsNo,
            String receiver,
            String feedback) {
    }

    public record RiskDto(
            Long id,
            Long projectId,
            String projectNo,
            String level,
            Integer score,
            List<String> reasons,
            String suggestion,
            String status) {
    }

    public record RiskActionDto(
            Long id,
            Long riskId,
            String actionPlan,
            Long responsibleUserId,
            String responsibleName,
            LocalDateTime planFinishTime,
            String closeSummary,
            String status,
            Integer version,
            LocalDateTime createdAt,
            LocalDateTime closedAt) {
    }

    public record RiskActionRequest(
            String actionPlan,
            Long responsibleUserId,
            LocalDateTime planFinishTime,
            String idempotencyKey) {
    }

    public record RiskActionCloseRequest(String closeSummary, Integer version) {
    }

    public record ProjectMilestoneDto(
            String code,
            String label,
            String state,
            LocalDateTime occurredAt) {
    }

    public record VersionConflictDto(
            Long taskId,
            String taskNo,
            String taskTitle,
            String referencedVersion,
            String currentVersion) {
    }

    public record QrCodeBindingDto(
            String code,
            String businessType,
            Long businessId,
            String payload,
            String status) {
    }

    public record ScanDetailDto(
            String businessType,
            String title,
            ProjectDto project,
            List<DocumentVersionDto> documents,
            List<TaskDto> tasks,
            List<RiskDto> risks,
            List<ChangeOrderDto> changes) {
    }

    public record TechnicalPackageSyncResultDto(
            Long projectId,
            Long documentVersionId,
            String versionNo,
            int updatedTaskCount) {
    }

    public record ProjectWorkspaceDto(
            ProjectDetail detail,
            RiskDto currentRisk,
            List<RiskActionDto> riskActions,
            List<ProjectMilestoneDto> milestones,
            List<VersionConflictDto> versionConflicts,
            QrCodeBindingDto flowQrCode) {
    }

    public record MessageDto(
            Long id,
            String title,
            String content,
            String type,
            String status,
            String businessType,
            Long businessId,
            LocalDateTime createdAt) {
    }

    public record ReportOverview(
            Map<String, Number> metrics,
            List<Map<String, Object>> riskLevels,
            List<Map<String, Object>> changeTypes,
            List<Map<String, Object>> sampleEfficiency,
            List<Map<String, Object>> deliveryStats,
            List<Map<String, Object>> delayReasons,
            List<Map<String, Object>> departmentLoads) {
    }

    public record WorkbenchDto(
            Map<String, Number> metrics,
            List<TaskDto> todos,
            List<TaskDto> dueTasks,
            List<MessageDto> unreadNotifications,
            List<ProjectDto> watchedProjects,
            List<TimelineItem> recentOperations,
            List<RiskDto> riskCards,
            List<Map<String, Object>> shortcuts) {
    }

    public record DashboardPeriodDto(
            String period,
            LocalDate startDate,
            LocalDate endDate) {
    }

    public record DashboardChartItemDto(
            String name,
            long value) {
    }

    public record DashboardTrendPointDto(
            LocalDate date,
            long dueTasks,
            long completedTasks) {
    }

    public record DashboardDepartmentLoadDto(
            String name,
            long todoTasks,
            long overdueTasks,
            long impactedTasks) {
    }

    public record DashboardDto(
            DashboardPeriodDto period,
            Map<String, Number> metrics,
            List<DashboardTrendPointDto> deliveryTrend,
            List<DashboardChartItemDto> riskDistribution,
            List<DashboardChartItemDto> changeTypes,
            List<DashboardDepartmentLoadDto> departmentLoads,
            List<ProjectDto> projects,
            List<RiskDto> criticalRisks,
            List<TaskDto> todos,
            List<TaskDto> dueTasks,
            List<MessageDto> unreadNotifications,
            List<Map<String, Object>> shortcuts) {
    }

    public record SearchResultItem(
            String type,
            Long id,
            String title,
            String summary,
            String route) {
    }

    public record SavedViewDto(
            Long id,
            String viewName,
            String targetType,
            Map<String, Object> filters,
            LocalDateTime createdAt) {
    }

    public record SavedViewRequest(String viewName, String targetType, Map<String, Object> filters) {
    }

    public record ExportTaskDto(
            Long id,
            String exportType,
            String status,
            String fileName,
            String querySummary,
            Integer downloadCount,
            LocalDateTime createdAt,
            LocalDateTime finishedAt) {
    }

    public record ExportRequest(String exportType, Map<String, Object> filters, List<String> fields) {
    }

    public record ImportTaskDto(
            Long id,
            String importType,
            String mode,
            String status,
            Integer successCount,
            Integer failCount,
            List<String> errors,
            LocalDateTime createdAt) {
    }

    public record ImportRequest(String importType, String mode, List<Map<String, String>> rows) {
    }

    public record RuleParamDto(
            Long id,
            String ruleCode,
            String ruleName,
            String ruleVersion,
            Map<String, Object> params,
            String status,
            LocalDateTime publishedAt) {
    }

    public record RuleParamRequest(String ruleCode, String ruleName, Map<String, Object> params, String status) {
    }

    public record AuditLogDto(
            Long id,
            String userName,
            String moduleName,
            String operationType,
            String businessType,
            Long businessId,
            String result,
            String summary,
            LocalDateTime operatedAt) {
    }

    public record FileUploadResult(
            Long id,
            String fileName,
            String contentType,
            long size,
            String sha256,
            String downloadUrl) {
    }

    public record StoredFile(
            Long id,
            String fileName,
            String contentType,
            long size,
            String sha256,
            String path) {
    }
}
