package com.nso.web.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class MvpDtos {

    private MvpDtos() {
    }

    public record PageResult<T>(List<T> records, long total) {
    }

    public record LoginRequest(String username, String password, String code) {
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
            List<DocumentVersionDto> documents,
            List<SampleDto> samples,
            List<ChangeOrderDto> changes,
            List<TaskDto> tasks,
            List<RiskDto> risks,
            List<TimelineItem> timeline) {
    }

    public record DocumentVersionDto(
            Long id,
            Long projectId,
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
            String changeSummary) {
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

    public record SampleRequest(
            Long projectId,
            String purpose,
            Integer quantity,
            LocalDate planFinishDate,
            String referencedVersion,
            String responsibleName) {
    }

    public record SampleConfirmRequest(String conclusion, String opinion, String confirmer) {
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
            String responsibleName) {
    }

    public record ChangeFeedbackRequest(
            Long impactId,
            String result,
            String plan,
            Integer delayDays,
            Integer reworkQty,
            String responsibleName) {
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
            String blockReason) {
    }

    public record TaskFeedbackRequest(String result, String notes) {
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
            List<Map<String, Object>> sampleEfficiency) {
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
