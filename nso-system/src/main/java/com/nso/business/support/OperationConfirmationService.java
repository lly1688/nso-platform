package com.nso.business.support;

import com.nso.business.core.NsoDtos.OperationConfirmationDto;
import com.nso.business.core.NsoDtos.OperationConfirmationRequest;
import com.nso.business.core.NsoDtos.VerificationChallengeDto;
import com.nso.business.core.TenantContext;
import com.nso.shared.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Set;

// 操作确认服务，负责高风险操作的二次确认与验证码校验。
@Service

public class OperationConfirmationService {
    private static final Set<String> OPERATIONS = Set.of("DOCUMENT_PUBLISH", "DOCUMENT_VOID", "SPECIAL_RELEASE",
            "CHANGE_REVOKE", "PROJECT_TARGET_DATE", "PROJECT_ARCHIVE", "PROJECT_RESTORE", "EXPORT_BATCH", "FILE_DELETE");
    // JDBC模板
    private final JdbcTemplate jdbc;
    private final SecureRandom random = new SecureRandom();

    public OperationConfirmationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // 创建敏感操作确认挑战。
    @Transactional
    public OperationConfirmationDto request(OperationConfirmationRequest request) {
        if (request == null || request.businessId() == null || blank(request.reason()) || !OPERATIONS.contains(normalize(request.operationCode()))) {
            throw new BusinessException("高风险操作、业务对象和确认原因不能为空");
        }
        String channel = blank(request.channel()) ? "IN_APP" : request.channel().trim().toUpperCase();
        Long challengeId = null;
        if (!"IN_APP".equals(channel)) {
            String code = String.format("%06d", random.nextInt(1_000_000));
            LocalDateTime expires = LocalDateTime.now().plusMinutes(5);
            jdbc.update("INSERT INTO nso_verification_challenge (tenant_id,receiver_id,channel,purpose,code_hash,expires_at) VALUES (?,?,?,?,?,?)",
                    TenantContext.tenantId(),
                    TenantContext.userId(),
                    channel, "OPERATION_CONFIRM",
                    sha256(code), expires);
            challengeId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            jdbc.update("INSERT INTO nso_integration_log (tenant_id,direction,system_name,business_type,business_id,request_summary,response_code,final_status) VALUES (?,?,?,?,?,?,?,'NOT_CONFIGURED')",
                    TenantContext.tenantId(),
                    "OUTBOUND",
                    channel,
                    request.businessType(),
                    request.businessId(),
                    "verification challenge=" + challengeId,
                    "CHANNEL_NOT_CONFIGURED");
            // 实际适配器会在 nso-framework 中读取环境凭据。
            // 不得暴露验证码，也不得声称投递成功。
        }
        LocalDateTime expires = LocalDateTime.now().plusMinutes(10);
        jdbc.update("INSERT INTO nso_operation_confirmation (tenant_id,operation_code,business_type,business_id,requester_id,reason,challenge_id,status,expires_at) VALUES (?,?,?,?,?,?,?,'PENDING',?)",
                TenantContext.tenantId(),
                normalize(request.operationCode()),
                request.businessType(),
                request.businessId(),
                TenantContext.userId(),
                request.reason().trim(),
                challengeId, expires);
        Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return new OperationConfirmationDto(
                id,
                normalize(request.operationCode()),
                request.businessType(),
                request.businessId(),
                "PENDING",
                expires,
                challengeId);
    }

    // 校验操作确认验证码。
    @Transactional
    public VerificationChallengeDto verify(Long id, String code) {
        Challenge row = jdbc.query("SELECT id,channel,purpose,code_hash,status,expires_at,attempt_count,max_attempts FROM nso_verification_challenge WHERE id=? AND tenant_id=?",
                rs -> rs.next() ? new Challenge(rs.getLong(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getString(4),
                        rs.getString(5),
                        rs.getTimestamp(6).toLocalDateTime(),
                        rs.getInt(7),
                        rs.getInt(8)) : null,
                id, TenantContext.tenantId());

        if (row == null || !"PENDING".equals(row.status()) || row.expiresAt().isBefore(LocalDateTime.now())) throw new BusinessException("验证码已过期或不可用");

        boolean valid = !blank(code) && sha256(code).equals(row.hash());

        int attempts = row.attempts() + 1;

        jdbc.update("UPDATE nso_verification_challenge SET attempt_count=?,status=?,verified_at=? WHERE id=? AND tenant_id=?",
                attempts, valid ? "VERIFIED" : attempts >= row.maxAttempts() ? "LOCKED" : "PENDING", valid ? LocalDateTime.now() : null, id, TenantContext.tenantId());

        if (!valid) throw new BusinessException("验证码错误");

        return new VerificationChallengeDto(row.id(), row.channel(), row.purpose(), "VERIFIED", row.expiresAt());

    }

    // 确认敏感操作。
    @Transactional
    public OperationConfirmationDto confirm(Long id) {
        Confirmation row = find(id);
        if (!"PENDING".equals(row.status()) || row.expiresAt().isBefore(LocalDateTime.now())) throw new BusinessException("二次确认已过期或已处理");
        if (row.challengeId() != null) {
            Integer verified = jdbc.queryForObject("SELECT COUNT(*) FROM nso_verification_challenge WHERE id=? AND tenant_id=? AND status='VERIFIED' AND expires_at>NOW()",
                    Integer.class,
                    row.challengeId(),
                    TenantContext.tenantId());
            if (verified == null || verified == 0) throw new BusinessException("请先完成验证码核验");
        }
        jdbc.update("UPDATE nso_operation_confirmation SET status='CONFIRMED',confirmed_at=NOW() WHERE id=? AND tenant_id=?",
                id,
                TenantContext.tenantId());

        return new OperationConfirmationDto(
                row.id(),
                row.operationCode(),
                row.businessType(),
                row.businessId(),
                "CONFIRMED",
                row.expiresAt(),
                row.challengeId());
    }

    // 消费已确认的敏感操作凭证。
    @Transactional
    public void consume(
            Long id,
            String operationCode,
            String businessType,
            Long businessId) {
        Confirmation row = find(id);
        if (!"CONFIRMED".equals(row.status()) || row.expiresAt().isBefore(LocalDateTime.now()) || !normalize(operationCode).equals(row.operationCode())
                 || !businessType.equals(row.businessType()) || !businessId.equals(row.businessId())) {
            throw BusinessException.ruleBlock("OPERATION_CONFIRMATION", "高风险操作尚未完成二次确认", row.status(), "CONFIRMED", "填写原因并完成二次确认");
        }
        if (jdbc.update("UPDATE nso_operation_confirmation SET status='CONSUMED',consumed_at=NOW() WHERE id=? AND tenant_id=? AND status='CONFIRMED'",
                id,
                TenantContext.tenantId()) != 1) {
            throw new BusinessException("二次确认已被使用，请重新确认");
        }
    }

    private Confirmation find(Long id) {
        Confirmation row = jdbc.query("SELECT id,operation_code,business_type,business_id,status,expires_at,challenge_id FROM nso_operation_confirmation WHERE id=? AND tenant_id=? AND requester_id=?",
                rs -> rs.next() ? new Confirmation(rs.getLong(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getLong(4),
                        rs.getString(5),
                        rs.getTimestamp(6).
                                toLocalDateTime(),
                        (Long) rs.getObject(7)) : null,
                id,
                TenantContext.tenantId(),
                TenantContext.userId());
        if (row == null) throw new BusinessException("二次确认不存在或无权使用");
        return row;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        }

        catch (Exception ex) {
            throw new IllegalStateException("无法计算验证码摘要", ex);
        }
    }

    // 操作确认挑战数据。
    private record Challenge(
            Long id, String channel,
            String purpose,
            String hash, String status,
            LocalDateTime expiresAt,
            int attempts,
            int maxAttempts) {

    }
    // 已确认操作数据。
    private record Confirmation(
            Long id,
            String operationCode,
            String businessType,
            Long businessId,
            String status,
            LocalDateTime expiresAt,
            Long challengeId) {

    }
}
