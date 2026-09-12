package com.nso.business.core;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// 业务接口数据传输对象集合。
public final class NsoDtos {

    private NsoDtos() {
    }

    // 分页结果。
    public record PageResult<T>(List<T> list, long total, int pageNo, int pageSize) {
        public PageResult(List<T> list, long total) {
            this(list, total, 1, list == null ? 0 : list.size());
        }

        public static <T> PageResult<T> empty(PageQuery pageQuery) {
            PageQuery page = pageQuery == null ? new PageQuery() : pageQuery;
            return new PageResult<>(List.of(), 0, page.pageNoValue(), page.pageSizeValue());
        }

        @JsonIgnore
        public List<T> records() {
            return list;
        }
    }

    // 登录请求参数。
    public record LoginRequest(String username, String password, String code) {
    }

    // 刷新令牌请求参数。
    public record RefreshRequest(String refreshToken) {
    }

    // 认证会话。
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

    // 客户数据传输对象。
    public record CustomerDto(
            Long id,
            String customerCode,
            String name,
            String industry,
            String contactName,
            String phone,
            String status) {
    }

    // 客户请求参数。
    public record CustomerRequest(
            String name,
            String industry,
            String contactName,
            String phone,
            String status) {
    }

    // 项目数据传输对象。
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
            Long daysLeft,
            Integer version,
            Long ownerUserId,
            Integer blockerCount,
            Integer pendingDecisionCount,
            String recommendedActionLabel,
            String recommendedActionRoute) {
        public ProjectDto(Long id, String projectNo, Long customerId, String customerName, String productName,
                          Integer quantity, LocalDate targetDate, String ownerName, String status, String stage,
                          String priority, String riskLevel, String sampleStatus, Integer changeCount, Long daysLeft) {
            this(id, projectNo, customerId, customerName, productName, quantity, targetDate, ownerName, status,
                    stage, priority, riskLevel, sampleStatus, changeCount, daysLeft, null, null, 0, 0, null, null);
        }

        public ProjectDto(Long id, String projectNo, Long customerId, String customerName, String productName,
                          Integer quantity, LocalDate targetDate, String ownerName, String status, String stage,
                          String priority, String riskLevel, String sampleStatus, Integer changeCount, Long daysLeft,
                          Integer version, Long ownerUserId) {
            this(id, projectNo, customerId, customerName, productName, quantity, targetDate, ownerName, status,
                    stage, priority, riskLevel, sampleStatus, changeCount, daysLeft, version, ownerUserId, 0, 0, null, null);
        }
    }

    // 项目请求参数。
    public record ProjectRequest(
            Long customerId,
            String customerName,
            String productName,
            Integer quantity,
            LocalDate targetDate,
            String ownerName,
            String priority,
            List<String> requirements,
            Long managerUserId) {
        public ProjectRequest(Long customerId, String customerName, String productName, Integer quantity,
                              LocalDate targetDate, String ownerName, String priority, List<String> requirements) {
            this(customerId, customerName, productName, quantity, targetDate, ownerName, priority, requirements, null);
        }
    }

    // 项目时间线条目。
    public record TimelineItem(
            Long id,
            Long projectId,
            String eventType,
            String title,
            String summary,
            String operatorName,
            LocalDateTime occurredAt) {
    }

    // 项目综合详情。
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

    // 文档版本数据传输对象。
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

    // 文档版本请求参数。
    public record DocumentVersionRequest(
            String fileName,
            String fileType,
            String versionNo,
            LocalDate effectiveDate,
            String changeSummary,
            Long fileObjectId) {
    }

    // 样品数据传输对象。
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

    // 客户项目数据传输对象。
    public record CustomerProjectDto(
            Long id,
            String projectNo,
            String productName,
            LocalDate targetDate,
            String status,
            String stage,
            String sampleStatus) {
    }

    // 客户样品数据传输对象。
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

    // 公开样品确认数据。
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

    // 样品请求参数。
    public record SampleRequest(
            Long projectId,
            String purpose,
            Integer quantity,
            LocalDate planFinishDate,
            String referencedVersion,
            String responsibleName) {
    }

    // 样品确认请求参数。
    public record SampleConfirmRequest(String conclusion, String opinion, String confirmer, Long evidenceFileId) {
        public SampleConfirmRequest(String conclusion, String opinion, String confirmer) {
            this(conclusion, opinion, confirmer, null);
        }
    }

    // 项目需求数据传输对象。
    public record RequirementDto(
            Long id,
            Long projectId,
            String category,
            String content,
            String confirmStatus,
            String lastReason) {
    }

    // 项目需求请求参数。
    public record RequirementRequest(String category, String content, String confirmStatus, String reason) {
    }

    // 项目成员数据传输对象。
    public record ProjectMemberDto(
            Long id,
            Long projectId,
            Long userId,
            String memberName,
            String projectRole,
            String departmentName,
            String status,
            Long deptId,
            String userStatus,
            List<String> responsibilityCodes,
            String primaryResponsibilityCode) {
        public ProjectMemberDto(Long id, Long projectId, Long userId, String memberName, String projectRole,
                                String departmentName, String status) {
            this(id, projectId, userId, memberName, projectRole, departmentName, status, null, null,
                    projectRole == null ? List.of() : List.of(projectRole), projectRole);
        }
    }

    // 项目成员请求参数。
    public record ProjectMemberRequest(Long userId, String memberName, String projectRole, String departmentName,
                                       List<String> responsibilityCodes, String primaryResponsibilityCode) {
        public ProjectMemberRequest(Long userId, String memberName, String projectRole, String departmentName) {
            this(userId, memberName, projectRole, departmentName,
                    projectRole == null ? List.of() : List.of(projectRole), projectRole);
        }

        public ProjectMemberRequest(String memberName, String projectRole, String departmentName) {
            this(null, memberName, projectRole, departmentName,
                    projectRole == null ? List.of() : List.of(projectRole), projectRole);
        }
    }

    // 物料明细数据传输对象。
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

    // 物料明细请求参数。
    public record BomItemRequest(
            String materialCode,
            String materialName,
            String specification,
            Double quantity,
            String unit,
            String sourceType,
            String substituteCode) {
    }

    // 物料清单数据传输对象。
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

    // 物料清单请求参数。
    public record BomRequest(
            Long projectId,
            String bomNo,
            String versionNo,
            Long boundDocVersionId,
            List<BomItemRequest> items) {
    }

    // 工艺步骤数据传输对象。
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

    // 工艺步骤请求参数。
    public record ProcessStepRequest(
            Integer stepNo,
            String stepName,
            String workInstruction,
            String equipmentName,
            Double standardHours,
            Boolean outsourceFlag,
            Boolean inspectionPoint) {
    }

    // 工艺路线数据传输对象。
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

    // 工艺路线请求参数。
    public record ProcessRouteRequest(
            Long projectId,
            String routeNo,
            String versionNo,
            Long boundDocVersionId,
            List<ProcessStepRequest> steps) {
    }

    // 检验项数据传输对象。
    public record InspectionItemDto(
            Long id,
            String itemName,
            String standardValue,
            String samplingRule,
            Long attachmentFileId) {
    }

    // 检验项请求参数。
    public record InspectionItemRequest(
            String itemName,
            String standardValue,
            String samplingRule,
            Long attachmentFileId) {
    }

    // 检验规范数据传输对象。
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

    // 检验规范请求参数。
    public record InspectionSpecRequest(
            Long projectId,
            String specNo,
            String versionNo,
            Long boundDocVersionId,
            List<InspectionItemRequest> items) {
    }

    // 样品检验数据传输对象。
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

    // 样品检验请求参数。
    public record SampleCheckRequest(
            String checkItem,
            String measuredValue,
            String result,
            String issueSummary,
            String correctiveAction,
            String checkerName) {
    }

    // 确认令牌数据传输对象。
    public record ConfirmationTokenDto(
            String token,
            Long sampleId,
            LocalDateTime expireAt,
            Integer maxUseCount,
            Integer usedCount,
            String status) {
    }

    public record ConfirmationTokenRequest(
        // 已授权联系人编号
        Long contactId,
        // 链接有效天数
        Integer validDays,
        // 最大使用次数
        Integer maxUseCount
    ) {
    }

    // 特殊放行请求参数。
    public record SpecialReleaseRequest(String releaseScope, LocalDateTime validUntil, String riskStatement, String reason) {
    }

    // 特殊放行数据传输对象。
    public record SpecialReleaseDto(Long id, Long projectId, Long sampleId, Long applicantUserId, Long approverUserId,
                                    String releaseScope, LocalDateTime validUntil, String riskStatement,
                                    String reason, String status) {
    }

    // 变更单数据传输对象。
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

    // 变更请求参数。
    public record ChangeRequest(
            Long projectId,
            String changeType,
            String urgency,
            String beforeContent,
            String afterContent,
            String reason) {
    }

    // 变更影响项数据传输对象。
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

    // 变更反馈请求参数。
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

    // 变更审批请求参数。
    public record ChangeApprovalRequest(String decision, String opinion) {
    }

    // 任务数据传输对象。
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
            Integer version,
            Long assigneeId) {
    }

    // 任务反馈请求参数。
    public record TaskFeedbackRequest(String result, String notes, Integer version) {
        public TaskFeedbackRequest(String result, String notes) {
            this(result, notes, null);
        }
    }

    // 任务操作请求参数。
    public record TaskActionRequest(String notes, Integer version) {
        public TaskActionRequest(String notes) {
            this(notes, null);
        }
    }

    // 执行任务请求参数。
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

    // 异常上报请求参数。
    public record ExceptionReportRequest(
            Long taskId,
            Long projectId,
            String exceptionType,
            String summary,
            String imageUrl,
            String reporterName) {
    }

    // 交付记录数据传输对象。
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

    // 交付请求参数。
    public record DeliveryRequest(
            Long projectId,
            Integer quantity,
            String logisticsNo,
            String receiver,
            String feedback) {
    }

    // 风险数据传输对象。
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

    // 风险处置措施数据传输对象。
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

    // 风险处置措施请求参数。
    public record RiskActionRequest(
            String actionPlan,
            Long responsibleUserId,
            LocalDateTime planFinishTime,
            String idempotencyKey) {
    }

    // 风险处置关闭请求参数。
    public record RiskActionCloseRequest(String closeSummary, Integer version) {
    }

    // 项目里程碑数据传输对象。
    public record ProjectMilestoneDto(
            String code,
            String label,
            String state,
            LocalDateTime occurredAt) {
    }

    // 版本冲突数据传输对象。
    public record VersionConflictDto(
            Long taskId,
            String taskNo,
            String taskTitle,
            String referencedVersion,
            String currentVersion) {
    }

    // 流转码绑定数据。
    public record QrCodeBindingDto(
            String code,
            String businessType,
            Long businessId,
            String payload,
            String status) {
    }

    // 扫码详情数据。
    public record ScanDetailDto(
            String businessType,
            String title,
            ProjectDto project,
            List<DocumentVersionDto> documents,
            List<TaskDto> tasks,
            List<RiskDto> risks,
            List<ChangeOrderDto> changes) {
    }

    // 技术包同步结果。
    public record TechnicalPackageSyncResultDto(
            Long projectId,
            Long documentVersionId,
            String versionNo,
            int updatedTaskCount) {
    }

    // 项目工作区数据。
    public record ProjectWorkspaceDto(
            ProjectDetail detail,
            RiskDto currentRisk,
            List<RiskActionDto> riskActions,
            List<ProjectMilestoneDto> milestones,
            List<VersionConflictDto> versionConflicts,
            QrCodeBindingDto flowQrCode,
            List<ProjectOwnerGapDto> ownerGaps,
            ProjectProgressDto progress) {
        public ProjectWorkspaceDto(ProjectDetail detail, RiskDto currentRisk, List<RiskActionDto> riskActions,
                                   List<ProjectMilestoneDto> milestones, List<VersionConflictDto> versionConflicts,
                                   QrCodeBindingDto flowQrCode) {
            this(detail, currentRisk, riskActions, milestones, versionConflicts, flowQrCode, List.of());
        }

        public ProjectWorkspaceDto(ProjectDetail detail, RiskDto currentRisk, List<RiskActionDto> riskActions,
                                   List<ProjectMilestoneDto> milestones, List<VersionConflictDto> versionConflicts,
                                   QrCodeBindingDto flowQrCode, List<ProjectOwnerGapDto> ownerGaps) {
            this(detail, currentRisk, riskActions, milestones, versionConflicts, flowQrCode, ownerGaps, null);
        }
    }

    // 消息数据传输对象。
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

    // 报表概览。
    public record ReportOverview(
            Map<String, Number> metrics,
            List<Map<String, Object>> riskLevels,
            List<Map<String, Object>> changeTypes,
            List<Map<String, Object>> sampleEfficiency,
            List<Map<String, Object>> deliveryStats,
            List<Map<String, Object>> delayReasons,
            List<Map<String, Object>> departmentLoads) {
    }

    // 工作台数据。
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

    // 仪表盘周期数据。
    public record DashboardPeriodDto(
            String period,
            LocalDate startDate,
            LocalDate endDate) {
    }

    // 仪表盘图表条目。
    public record DashboardChartItemDto(
            String name,
            long value) {
    }

    // 仪表盘趋势点。
    public record DashboardTrendPointDto(
            LocalDate date,
            long dueTasks,
            long completedTasks) {
    }

    // 部门负载数据。
    public record DashboardDepartmentLoadDto(
            String name,
            long todoTasks,
            long overdueTasks,
            long impactedTasks) {
    }

    // 仪表盘数据。
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

    // 仪表盘首屏概览数据。列表与图表由独立接口按需获取。
    public record DashboardOverviewDto(
            DashboardPeriodDto period,
            Map<String, Number> metrics,
            List<Map<String, Object>> shortcuts) {
    }

    // 仪表盘分析数据。图表区域延迟加载，避免阻塞首屏可用。
    public record DashboardInsightsDto(
            DashboardPeriodDto period,
            List<DashboardTrendPointDto> deliveryTrend,
            List<DashboardChartItemDto> riskDistribution,
            List<DashboardChartItemDto> changeTypes,
            List<DashboardDepartmentLoadDto> departmentLoads) {
    }

    // 搜索结果条目。
    public record SearchResultItem(
            String type,
            Long id,
            String title,
            String summary,
            String route) {
    }

    // 保存视图数据。
    public record SavedViewDto(
            Long id,
            String viewName,
            String targetType,
            Map<String, Object> filters,
            LocalDateTime createdAt) {
    }

    // 保存视图请求参数。
    public record SavedViewRequest(String viewName, String targetType, Map<String, Object> filters) {
    }

    // 导出任务数据。
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

    // 导出请求参数。
    public record ExportRequest(String exportType, Map<String, Object> filters, List<String> fields) {
    }

    // 导入任务数据。
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

    // 导入请求参数。
    public record ImportRequest(String importType, String mode, List<Map<String, String>> rows) {
    }

    // 规则参数数据。
    public record RuleParamDto(
            Long id,
            String ruleCode,
            String ruleName,
            String ruleVersion,
            Map<String, Object> params,
            String status,
            LocalDateTime publishedAt) {
    }

    // 规则参数请求。
    public record RuleParamRequest(String ruleCode, String ruleName, Map<String, Object> params, String status) {
    }

    // 审计日志数据。
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

    // 文件上传结果。
    public record FileUploadResult(
            Long id,
            String fileName,
            String contentType,
            long size,
            String sha256,
            String downloadUrl) {
    }

    // 存储文件数据。
    public record StoredFile(
            Long id,
            String fileName,
            String contentType,
            long size,
            String sha256,
            String path) {
    }

    // 分页查询参数。
    public record PageQuery(Integer pageNo, Integer pageSize) {
        public PageQuery() {
            this(1, 20);
        }

        public int pageNoValue() {
            return pageNo == null || pageNo < 1 ? 1 : pageNo;
        }

        public int pageSizeValue() {
            return pageSize == null || pageSize < 1 ? 20 : Math.min(pageSize, 100);
        }

        public long offset() {
            return (long) (pageNoValue() - 1) * pageSizeValue();
        }
    }

    // 审批模板节点请求。
    public record ApprovalTemplateNodeRequest(String nodeCode, String nodeName, String responsibilityCode,
                                              Integer slaMinutes, String escalationRole) {
    }

    // 审批模板请求。
    public record ApprovalTemplateRequest(String templateCode, String templateName, String businessType,
                                          String approvalMode, Integer slaMinutes,
                                          List<ApprovalTemplateNodeRequest> nodes) {
    }

    // 审批处理请求。
    public record ApprovalDecisionRequest(String decision, String opinion, String idempotencyKey, Integer version) {
    }

    // 审批模板节点视图。
    public record ApprovalTemplateNodeDto(Long id, Integer nodeOrder, String nodeCode, String nodeName,
                                          String responsibilityCode, Integer slaMinutes, String escalationRole) {
    }

    // 审批模板视图。
    public record ApprovalTemplateDto(Long id, String templateCode, String businessType, Integer templateVersion,
                                      String templateName, String approvalMode, Integer slaMinutes, String status,
                                      List<ApprovalTemplateNodeDto> nodes, Integer version) {
    }

    // 审批实例视图。
    public record ApprovalInstanceDto(Long id, String businessType, Long businessId, Long projectId,
                                     String templateCode, Integer templateVersion, String approvalMode, String status,
                                     Integer currentNodeOrder, List<ApprovalTodoDto> todos,
                                     LocalDateTime startedAt, LocalDateTime finishedAt, Integer version) {
    }

    // 审批待办视图。
    public record ApprovalTodoDto(Long id, Long instanceId, String businessType, Long businessId, Long projectId,
                                  String templateCode, Integer templateVersion, String approvalMode,
                                  String nodeCode, String nodeName, String responsibilityCode,
                                  Long assigneeUserId, String assigneeName, String decision, LocalDateTime dueAt,
                                  boolean overdue, Integer version) {
    }

    // 二次确认请求。
    public record OperationConfirmationRequest(String operationCode, String businessType, Long businessId,
                                               String reason, String channel) {
    }

    // 二次确认凭证视图。
    public record OperationConfirmationDto(Long id, String operationCode, String businessType, Long businessId,
                                           String status, LocalDateTime expiresAt, Long challengeId) {
    }

    // 验证码挑战视图。
    public record VerificationChallengeDto(Long id, String channel, String purpose, String status,
                                           LocalDateTime expiresAt) {
    }

    public record VerificationChallengeRequest(
        // 验证码
        String code
    ) {
    }

    // 客户联系人视图。
    public record CustomerContactDto(Long id, Long customerId, String contactName, String phone, String email,
                                     String positionName, String preferredChannel, String status) {
    }

    // 客户联系人请求。
    public record CustomerContactRequest(String contactName, String phone, String email, String positionName,
                                         String preferredChannel, String status) {
    }

    // 外部项目授权视图。
    public record ExternalProjectAccessDto(Long id, Long identityId, Long customerId, Long contactId,
                                           String contactName, Long projectId, String projectNo, String status,
                                           LocalDateTime validUntil, LocalDateTime grantedAt,
                                           LocalDateTime revokedAt, String revokeReason) {
    }

    // 外部项目授权请求。
    public record ExternalProjectAccessRequest(Long contactId, LocalDateTime validUntil) {
    }

    public record RevokeProjectAccessRequest(
        // 撤销原因
        String reason
    ) {
    }

    // CAPA 创建请求。
    public record CapaCaseCreateRequest(Long projectId, Long taskId, String exceptionType, String summary,
                                        String reporterName, Long ownerUserId, LocalDateTime dueAt,
                                        String idempotencyKey) {
    }

    // CAPA 转换请求。
    public record CapaTransitionRequest(String toStatus, String comment, String rootCause, String correctivePlan,
                                        String verificationSummary, String closeConclusion, String evidenceRef,
                                        String evidenceSummary, String evidenceType, String idempotencyKey,
                                        Integer version) {
    }

    // CAPA 证据请求。
    public record CapaEvidenceRequest(String evidenceRef, String summary, String evidenceType) {
    }

    // CAPA 整改任务请求。
    public record CapaCorrectiveTaskRequest(String title, Long assigneeUserId, LocalDate planStart,
                                            LocalDate planFinish, String actionPlan, String idempotencyKey) {
    }

    // CAPA 案例视图。
    public record CapaCaseDto(Long id, String caseNo, Long projectId, Long taskId, Long riskId,
                              String exceptionType, String summary, String reporterName, Long ownerUserId,
                              String ownerName, LocalDateTime dueAt, String status, String containmentPlan,
                              String rootCause, String correctivePlan, String verificationSummary,
                              String closeConclusion, List<CapaEvidenceDto> evidences, Integer version,
                              LocalDateTime createdAt, LocalDateTime closedAt) {
    }

    // CAPA 证据视图。
    public record CapaEvidenceDto(Long id, String evidenceType, String evidenceRef, String summary,
                                  LocalDateTime createdAt) {
    }

    // CAPA 行动视图。
    public record CapaActionDto(String sourceType, Long sourceId, String title, String status,
                                Long assigneeUserId, String assigneeName, LocalDateTime dueAt,
                                String summary, Integer version) {
    }

    // 行动中心汇总。
    public record ActionCenterSummaryDto(Map<String, Number> metrics, List<ActionItemDto> priorityItems) {
    }

    // 行动中心条目。
    public record ActionItemDto(Long id, String sourceType, Long sourceId, String actionCode, Long projectId,
                                Long assigneeUserId, String assigneeName, String title, String summary,
                                String priority, LocalDateTime slaDueAt, String route, String sourceStatus,
                                String actionStatus, boolean overdue) {
    }

    // 项目动作请求。
    public record ProjectActionRequest(String reason, Integer version, Long confirmationId) {
    }

    // 项目复制请求。
    public record ProjectCopyRequest(String productName, LocalDate targetDate) {
    }

    // 项目成员职责更新请求。
    public record ProjectMemberUpdateRequest(List<String> responsibilityCodes, String primaryResponsibilityCode) {
    }

    // 项目经理交接请求。
    public record ProjectManagerTransferRequest(Long managerUserId, String reason) {
    }

    // 项目成员候选人。
    public record ProjectMemberCandidateDto(Long userId, String username, String displayName, Long deptId,
                                            String deptName, List<String> roleCodes,
                                            List<String> allowedResponsibilityCodes) {
    }

    // 项目统计视图。
    public record ProjectStatsDto(long total, long inProgress, long dueSoon, long highRisk,
                                  Map<String, Long> stageDistribution, PageResult<ProjectDto> attention) {
    }

    // 项目状态历史视图。
    public record ProjectStatusHistoryDto(String beforeStatus, String afterStatus, String actionCode,
                                          String reason, LocalDateTime occurredAt) {
    }

    // 变更撤销请求。
    public record ChangeRevokeRequest(String reason, Long confirmationId, Integer version) {
    }

    // 变更执行核验请求。
    public record ChangeVerifyRequest(String summary) {
    }

    // 风险明细视图。
    public record RiskDetailDto(String factorCode, String factorName, String rawValue, Integer scoreDelta,
                                Integer scoreCap, boolean matched) {
    }

    // 风险人工覆盖请求。
    public record RiskOverrideRequest(String level, String reason, LocalDateTime expiresAt) {
    }

    // 交付阻断项。
    public record DeliveryBlockerDto(String code, String message) {
    }

    // 项目交付就绪检查结果。
    public record DeliveryReadinessDto(Long projectId, boolean ready, List<DeliveryBlockerDto> blockers) {
    }

    // 项目推进主线语义。
    public record ProjectProgressDto(String stage, String stageLabel, String status, String completionCriteria,
                                     String dueState, String riskLevel, int blockerCount, int pendingDecisionCount,
                                     String nextActionCode, String nextActionLabel, String nextActionReason,
                                     String nextActionRoute) {
    }

    // 项目职责负责人缺口。
    public record ProjectOwnerGapDto(Long projectId, String responsibilityCode, String status, String detail) {
    }

    // 项目工作台概要。
    public record ProjectWorkspaceSummaryDto(ProjectDto project, RiskDto currentRisk,
                                             List<ProjectMilestoneDto> milestones, QrCodeBindingDto flowCode,
                                             long ownerGapCount) {
    }

    // 项目运行脉搏视图。
    public record ProjectPulseDto(Long projectId, String projectNo, String stage, String status,
                                  List<ActionItemDto> blockers, List<ApprovalTodoDto> pendingDecisions,
                                  List<VersionConflictDto> versionConflicts, List<CapaCaseDto> openExceptions,
                                  List<ProjectOwnerGapDto> ownerGaps, Map<String, Number> metrics) {
    }

    // 仪表盘摘要。
    public record DashboardSummaryDto(DashboardPeriodDto period, Map<String, Number> metrics,
                                      List<DashboardTrendPointDto> deliveryTrend,
                                      List<DashboardChartItemDto> riskDistribution,
                                      List<DashboardChartItemDto> changeTypes,
                                      List<DashboardDepartmentLoadDto> departmentLoads,
                                      List<Map<String, Object>> shortcuts) {
    }
}
