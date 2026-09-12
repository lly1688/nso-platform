package com.nso.system.support.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.PageSupport;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nso.business.core.TenantContext;
import com.nso.business.file.ObjectStoragePort;
import com.nso.business.file.domain.FileObject;
import com.nso.business.file.mapper.FileObjectMapper;
import com.nso.business.file.service.FileDownloadPayload;
import com.nso.business.file.service.FileUploadPayload;
import com.nso.business.message.service.IMessageService;
import com.nso.business.support.BusinessNumberService;
import com.nso.shared.exception.BusinessException;
import com.nso.shared.util.FileContentSignature;
import com.nso.shared.exception.enums.ErrorCode;
import com.nso.system.domain.SysUser;
import com.nso.system.security.PasswordCredentialPort;
import com.nso.system.security.PublicRequestRateLimitPort;
import com.nso.system.service.ISysUserService;
import com.nso.system.support.domain.PasswordRecoveryRequest;
import com.nso.system.support.domain.SupportTicket;
import com.nso.system.support.domain.SupportTicketAttachment;
import com.nso.system.support.mapper.PasswordRecoveryRequestMapper;
import com.nso.system.support.mapper.SupportTicketAttachmentMapper;
import com.nso.system.support.mapper.SupportTicketMapper;
import com.nso.system.support.service.IAccountSupportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service

// 账号支持 服务层处理
public class AccountSupportServiceImpl implements IAccountSupportService {
    private static final Logger log = LoggerFactory.getLogger(AccountSupportServiceImpl.class);
    private static final Set<String> SUPPORT_CATEGORIES = Set.of("ACCOUNT", "PERMISSION", "FUNCTION", "DATA", "OTHER");
    private static final Set<String> SUPPORT_PRIORITIES = Set.of("LOW", "NORMAL", "HIGH", "URGENT");
    private static final Set<String> ADMIN_ROLES = Set.of("admin", "system_admin", "superadmin");
    private static final Map<String, String> IMAGE_CONTENT_TYPES = Map.of(
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "webp", "image/webp");
    private static final long MAX_SUPPORT_IMAGE_SIZE = 5L * 1024 * 1024;

    // 密码恢复请求数据映射
    private final PasswordRecoveryRequestMapper recoveryRequests;
    // 支持工单数据映射
    private final SupportTicketMapper supportTickets;
    // 支持工单附件数据映射
    private final SupportTicketAttachmentMapper attachments;
    // 文件对象数据映射
    private final FileObjectMapper fileObjects;
    // 系统用户服务
    private final ISysUserService users;
    // 密码凭据端口
    private final PasswordCredentialPort credentials;
    // 公开请求速率限流端口
    private final PublicRequestRateLimitPort rateLimits;
    // 业务Number服务
    private final BusinessNumberService numbers;
    // 消息服务
    private final IMessageService messages;
    // 对象存储端口
    private final ObjectStoragePort storage;

    public AccountSupportServiceImpl(PasswordRecoveryRequestMapper recoveryRequests,
                                     SupportTicketMapper supportTickets,
                                     SupportTicketAttachmentMapper attachments,
                                     FileObjectMapper fileObjects,
                                     ISysUserService users,
                                     PasswordCredentialPort credentials,
                                     PublicRequestRateLimitPort rateLimits,
                                     BusinessNumberService numbers,
                                     IMessageService messages,
                                     ObjectStoragePort storage) {
        this.recoveryRequests = recoveryRequests;
        this.supportTickets = supportTickets;
        this.attachments = attachments;
        this.fileObjects = fileObjects;
        this.users = users;
        this.credentials = credentials;
        this.rateLimits = rateLimits;
        this.numbers = numbers;
        this.messages = messages;
        this.storage = storage;
    }

    // 提交密码恢复申请。
    @Override
    @Transactional
    public void requestPasswordRecovery(PasswordRecoverySubmission request, String clientIp) {
        String username = requiredText(request == null ? null : request.username(), "账号", 64);
        String contactName = requiredText(request == null ? null : request.contactName(), "姓名", 64);
        String contactValue = requiredText(request == null ? null : request.contactValue(), "联系方式", 128);
        String requesterNote = optionalText(request == null ? null : request.requesterNote(), 1000);
        if (!rateLimits.allowPasswordRecovery(clientIp, username)) {
            throw new BusinessException(ErrorCode.RATE_LIMITED, "密码恢复申请过于频繁，请稍后再试");
        }

        // 无论账号是否存在，均返回相同结果以避免泄露账号状态。
        SysUser user = users.findByUsername(username).orElse(null);
        if (user == null) {
            return;
        }
        TenantContext.withTenant(user.getTenantId(), () -> {
            String activeKey = "PASSWORD_RECOVERY:" + user.getTenantId() + ":" + user.getId();
            if (recoveryRequests.selectCount(Wrappers.<PasswordRecoveryRequest>lambdaQuery()
                    .eq(PasswordRecoveryRequest::getActiveKey, activeKey)) > 0) {
                return null;
            }
            PasswordRecoveryRequest row = new PasswordRecoveryRequest();
            row.setTenantId(user.getTenantId());
            row.setUserId(user.getId());
            row.setUsername(user.getUsername());
            row.setContactName(contactName);
            row.setContactValue(contactValue);
            row.setRequesterNote(requesterNote);
            row.setStatus("PENDING");
            row.setActiveKey(activeKey);
            row.setVersion(0);
            try {
                recoveryRequests.insert(row);
            }
            catch (DuplicateKeyException ignored) {
                // 唯一活动键将并发重复申请收敛为一条待处理记录。
                return null;
            }
            notifyAdministrators("新的密码恢复申请", "账号 " + user.getUsername() + " 提交了密码恢复申请", "PASSWORD_RECOVERY_CREATED", "PASSWORD_RECOVERY", row.getId());
            return null;
        });
    }

    // 创建访客支持工单。
    @Override
    @Transactional
    public SupportTicketView createGuestSupportTicket(GuestSupportTicketSubmission request, String clientIp) {
        if (!rateLimits.allowGuestSupport(clientIp)) {
            throw new BusinessException(ErrorCode.RATE_LIMITED, "支持请求过于频繁，请稍后再试");
        }
        String username = optionalText(request == null ? null : request.username(), 64);
        String contactName = requiredText(request == null ? null : request.contactName(), "姓名", 64);
        String contactValue = requiredText(request == null ? null : request.contactValue(), "联系方式", 128);
        return createTicket(null, username, contactName, contactValue, "LOGIN",
                request == null ? null : request.category(), request == null ? null : request.priority(),
                request == null ? null : request.description(), null);
    }

    // 创建当前用户支持工单。
    @Override
    @Transactional
    public SupportTicketView createCurrentUserSupportTicket(CurrentUserSupportTicketSubmission request) {
        Long userId = requireCurrentUserId();
        SysUser user = users.findById(userId).orElseThrow(() -> new BusinessException("当前账号不存在"));
        if (TenantContext.tenantId() != user.getTenantId()) {
            throw new BusinessException("不能跨企业提交支持工单");
        }
        String contact = firstNonBlank(user.getPhone(), user.getEmail());
        return createTicket(user.getId(), user.getUsername(),
                firstNonBlank(user.getNickname(), user.getUsername()), contact, "IN_APP",
                request == null ? null : request.category(), request == null ? null : request.priority(),
                request == null ? null : request.description(), request == null ? null : request.pageContext());
    }

    // 查询当前用户支持工单。
    @Override
    public PageResult<SupportTicketView> listMySupportTickets() {
        Long userId = requireCurrentUserId();
        List<SupportTicketView> rows = supportTickets.selectList(Wrappers.<SupportTicket>lambdaQuery()
                        .eq(SupportTicket::getTenantId, TenantContext.tenantId())
                        .eq(SupportTicket::getRequesterUserId, userId)
                        .orderByDesc(SupportTicket::getCreatedAt))
                .stream().map(this::toTicketView).toList();
        return new PageResult<>(rows, rows.size());
    }

    // 分页查询当前用户支持工单。
    @Override
    public PageResult<SupportTicketView> listMySupportTickets(PageQuery pageQuery) {
        Long userId = requireCurrentUserId();
        PageQuery page = PageSupport.normalize(pageQuery);
        Page<SupportTicket> entityPage = supportTickets.selectPage(PageSupport.page(page), Wrappers.<SupportTicket>lambdaQuery()
                .eq(SupportTicket::getTenantId, TenantContext.tenantId())
                .eq(SupportTicket::getRequesterUserId, userId)
                .orderByDesc(SupportTicket::getCreatedAt));
        return PageSupport.result(entityPage, page, this::toTicketView);
    }

    // 上传支持工单图片。
    @Override
    @Transactional
    public SupportTicketAttachmentView uploadSupportTicketImage(Long ticketId, FileUploadPayload file) {
        SupportTicket ticket = requireTicket(ticketId);
        requireTicketAccess(ticket);
        if (!Set.of("OPEN", "PROCESSING").contains(ticket.getStatus())) {
            throw new BusinessException("已处理完成的工单不能继续上传图片");
        }
        if (file == null || file.inputStream() == null || file.size() <= 0) {
            throw new BusinessException("图片不能为空");
        }
        if (file.size() > MAX_SUPPORT_IMAGE_SIZE) {
            throw new BusinessException("工单图片不得超过 5MB");
        }
        String safeName = safeFileName(file.originalFilename());
        String extension = extensionOf(safeName);
        String expectedContentType = IMAGE_CONTENT_TYPES.get(extension);
        String contentType = normalizeContentType(file.contentType());
        if (expectedContentType == null || !expectedContentType.equals(contentType)) {
            throw new BusinessException("仅支持 PNG、JPG、JPEG 或 WEBP 图片");
        }
        Long userId = requireCurrentUserId();
        ObjectStoragePort.StoredObject stored = null;
        try (InputStream rawInput = file.inputStream(); InputStream input = FileContentSignature.verify(extension, rawInput)) {
            stored = storage.put(
                    TenantContext.tenantId() + "/support/" + ticket.getId() + "/" + UUID.randomUUID() + "/" + safeName,
                    contentType, file.size(), input);
            FileObject fileObject = new FileObject();
            fileObject.setTenantId(TenantContext.tenantId());
            fileObject.setCreatedBy(userId);
            fileObject.setFileName(safeName);
            fileObject.setContentType(contentType);
            fileObject.setFileSize(file.size());
            fileObject.setSha256(stored.sha256());
            fileObject.setStoragePath(stored.objectKey());
            fileObjects.insert(fileObject);

            SupportTicketAttachment attachment = new SupportTicketAttachment();
            attachment.setTenantId(TenantContext.tenantId());
            attachment.setTicketId(ticket.getId());
            attachment.setFileObjectId(fileObject.getId());
            attachment.setCreatedBy(userId);
            attachments.insert(attachment);
            return toAttachmentView(attachment, fileObject);
        }
        catch (Exception ex) {
            if (stored != null) {
                try {
                    storage.delete(stored.objectKey());
                } catch (Exception cleanupException) {
                    log.warn("Failed to delete orphaned support attachment object key={}", stored.objectKey(), cleanupException);
                }
            }
            if (ex instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException("工单图片上传失败：" + ex.getMessage());
        }
    }

    // 下载支持工单图片。
    @Override
    public AttachmentContent downloadSupportTicketImage(Long ticketId, Long attachmentId) {
        SupportTicket ticket = requireTicket(ticketId);
        requireTicketAccess(ticket);
        SupportTicketAttachment attachment = attachments.selectOne(Wrappers.<SupportTicketAttachment>lambdaQuery()
                .eq(SupportTicketAttachment::getTenantId, TenantContext.tenantId())
                .eq(SupportTicketAttachment::getTicketId, ticketId)
                .eq(SupportTicketAttachment::getId, attachmentId));
        if (attachment == null) {
            throw new BusinessException("工单图片不存在");
        }
        FileObject file = fileObjects.selectById(attachment.getFileObjectId());
        if (file == null || TenantContext.tenantId() != file.getTenantId()) {
            throw new BusinessException("工单图片不存在");
        }
        try {
            return new AttachmentContent(new FileDownloadPayload(file.getFileName(), file.getContentType(), file.getFileSize(),
                    file.getSha256(), new com.nso.business.file.service.IntegrityCheckingInputStream(
                    storage.get(file.getStoragePath()), file.getSha256())));
        }
        catch (Exception ex) {
            throw new BusinessException("工单图片读取失败：" + ex.getMessage());
        }
    }

    // 查询密码恢复申请。
    @Override
    public PageResult<PasswordRecoveryView> listPasswordRecoveryRequests(String status) {
        requireAdministrator();
        String normalizedStatus = optionalEnum(status, Set.of("PENDING", "IN_REVIEW", "RESET", "REJECTED"), "恢复申请状态");
        List<PasswordRecoveryView> rows = recoveryRequests.selectList(Wrappers.<PasswordRecoveryRequest>lambdaQuery()
                        .eq(PasswordRecoveryRequest::getTenantId, TenantContext.tenantId())
                        .eq(normalizedStatus != null, PasswordRecoveryRequest::getStatus, normalizedStatus)
                        .orderByDesc(PasswordRecoveryRequest::getCreatedAt))
                .stream().map(this::toRecoveryView).toList();
        return new PageResult<>(rows, rows.size());
    }

    // 分页查询密码恢复申请。
    @Override
    public PageResult<PasswordRecoveryView> listPasswordRecoveryRequests(String status, PageQuery pageQuery) {
        requireAdministrator();
        String normalizedStatus = optionalEnum(status, Set.of("PENDING", "IN_REVIEW", "RESET", "REJECTED"), "恢复申请状态");
        PageQuery page = PageSupport.normalize(pageQuery);
        Page<PasswordRecoveryRequest> entityPage = recoveryRequests.selectPage(PageSupport.page(page),
                Wrappers.<PasswordRecoveryRequest>lambdaQuery().eq(PasswordRecoveryRequest::getTenantId, TenantContext.tenantId())
                        .eq(normalizedStatus != null, PasswordRecoveryRequest::getStatus, normalizedStatus)
                        .orderByDesc(PasswordRecoveryRequest::getCreatedAt));
        return PageSupport.result(entityPage, page, this::toRecoveryView);
    }

    // 审核密码恢复申请。
    @Override
    @Transactional
    public PasswordRecoveryView reviewPasswordRecovery(Long requestId, RecoveryReviewSubmission request) {
        requireAdministrator();
        PasswordRecoveryRequest row = requireRecoveryRequest(requestId);
        requireExpectedVersion(row.getVersion(), request == null ? null : request.version());
        String target = requiredEnum(request == null ? null : request.status(), Set.of("IN_REVIEW", "REJECTED"), "恢复申请状态");
        String note = requiredText(request == null ? null : request.handlingNote(), "处理说明", 1000);
        if ("IN_REVIEW".equals(target) && !"PENDING".equals(row.getStatus())) {
            throw new BusinessException("只有待处理的恢复申请可以开始核验");
        }
        if ("REJECTED".equals(target) && !Set.of("PENDING", "IN_REVIEW").contains(row.getStatus())) {
            throw new BusinessException("当前恢复申请不能拒绝处理");
        }
        row.setStatus(target);
        row.setHandlerId(requireCurrentUserId());
        row.setHandlingNote(note);
        row.setReviewedAt(LocalDateTime.now());
        if ("REJECTED".equals(target)) {
            row.setActiveKey(null);
        }
        updateRecoveryOrThrow(row);
        return toRecoveryView(row);
    }

    // 为已核验申请重置密码。
    @Override
    @Transactional
    public PasswordResetResult resetRecoveredPassword(Long requestId, RecoveryResetSubmission request) {
        requireAdministrator();
        PasswordRecoveryRequest row = requireRecoveryRequest(requestId);
        requireExpectedVersion(row.getVersion(), request == null ? null : request.version());
        if (!"IN_REVIEW".equals(row.getStatus())) {
            throw new BusinessException("请先完成人工身份核验后再重置密码");
        }
        String note = requiredText(request == null ? null : request.handlingNote(), "核验说明", 1000);
        PasswordCredentialPort.TemporaryPassword temporaryPassword = credentials.issueTemporaryPassword();
        users.resetPassword(row.getUserId(), temporaryPassword.passwordHash());
        row.setStatus("RESET");
        row.setActiveKey(null);
        row.setHandlerId(requireCurrentUserId());
        row.setHandlingNote(note);
        row.setResetAt(LocalDateTime.now());
        updateRecoveryOrThrow(row);
        return new PasswordResetResult(toRecoveryView(row), temporaryPassword.plainText());
    }

    // 查询全部支持工单。
    @Override
    public PageResult<SupportTicketView> listSupportTickets(String status) {
        requireAdministrator();
        String normalizedStatus = optionalEnum(status, Set.of("OPEN", "PROCESSING", "RESOLVED", "CLOSED"), "工单状态");
        List<SupportTicketView> rows = supportTickets.selectList(Wrappers.<SupportTicket>lambdaQuery()
                        .eq(SupportTicket::getTenantId, TenantContext.tenantId())
                        .eq(normalizedStatus != null, SupportTicket::getStatus, normalizedStatus)
                        .orderByDesc(SupportTicket::getCreatedAt))
                .stream().map(this::toTicketView).toList();
        return new PageResult<>(rows, rows.size());
    }

    // 分页查询全部支持工单。
    @Override
    public PageResult<SupportTicketView> listSupportTickets(String status, PageQuery pageQuery) {
        requireAdministrator();
        String normalizedStatus = optionalEnum(status, Set.of("OPEN", "PROCESSING", "RESOLVED", "CLOSED"), "工单状态");
        PageQuery page = PageSupport.normalize(pageQuery);
        Page<SupportTicket> entityPage = supportTickets.selectPage(PageSupport.page(page),
                Wrappers.<SupportTicket>lambdaQuery().eq(SupportTicket::getTenantId, TenantContext.tenantId())
                        .eq(normalizedStatus != null, SupportTicket::getStatus, normalizedStatus)
                        .orderByDesc(SupportTicket::getCreatedAt));
        return PageSupport.result(entityPage, page, this::toTicketView);
    }

    // 更新支持工单处理状态。
    @Override
    @Transactional
    public SupportTicketView updateSupportTicket(Long ticketId, SupportTicketUpdateSubmission request) {
        requireAdministrator();
        SupportTicket ticket = requireTicket(ticketId);
        requireExpectedVersion(ticket.getVersion(), request == null ? null : request.version());
        String target = requiredEnum(request == null ? null : request.status(), Set.of("PROCESSING", "RESOLVED", "CLOSED"), "工单状态");
        String note = requiredText(request == null ? null : request.handlingNote(), "处理说明", 2000);
        if (!validTicketTransition(ticket.getStatus(), target)) {
            throw new BusinessException("工单状态不能从 " + ticket.getStatus() + " 变更为 " + target);
        }
        ticket.setStatus(target);
        ticket.setHandlerId(requireCurrentUserId());
        ticket.setHandlingNote(note);
        if (Set.of("RESOLVED", "CLOSED").contains(target)) {
            ticket.setResolvedAt(LocalDateTime.now());
        }
        if (supportTickets.updateById(ticket) != 1) {
            throw new BusinessException("工单已被其他管理员更新，请刷新后重试");
        }
        if (ticket.getRequesterUserId() != null) {
            messages.notifyUsers(List.of(ticket.getRequesterUserId()), "支持工单状态已更新",
                    "工单 " + ticket.getTicketNo() + " 已更新为 " + target, "SUPPORT_TICKET_UPDATED", "SUPPORT_TICKET", ticket.getId());
        }
        return toTicketView(ticket);
    }

    private SupportTicketView createTicket(Long requesterUserId, String requesterUsername, String contactName,
                                           String contactValue, String source, String category, String priority,
                                           String description, String pageContext) {
        String normalizedCategory = requiredEnum(category, SUPPORT_CATEGORIES, "问题分类");
        String normalizedPriority = requiredEnum(priority == null || priority.isBlank() ? "NORMAL" : priority,
                SUPPORT_PRIORITIES, "紧急程度");
        String normalizedDescription = requiredText(description, "问题描述", 2000);
        SupportTicket ticket = new SupportTicket();
        ticket.setTenantId(TenantContext.tenantId());
        ticket.setTicketNo(numbers.next("SUPPORT"));
        ticket.setRequesterUserId(requesterUserId);
        ticket.setRequesterUsername(optionalText(requesterUsername, 64));
        ticket.setContactName(requiredText(contactName, "联系人", 64));
        ticket.setContactValue(optionalText(contactValue, 128));
        ticket.setSource(source);
        ticket.setCategory(normalizedCategory);
        ticket.setPriority(normalizedPriority);
        ticket.setPageContext(optionalText(pageContext, 256));
        ticket.setDescription(normalizedDescription);
        ticket.setStatus("OPEN");
        ticket.setVersion(0);
        supportTickets.insert(ticket);
        notifyAdministrators("新的技术支持工单", "工单 " + ticket.getTicketNo() + " 等待处理", "SUPPORT_TICKET_CREATED", "SUPPORT_TICKET", ticket.getId());
        return toTicketView(ticket);
    }

    private PasswordRecoveryRequest requireRecoveryRequest(Long requestId) {
        if (requestId == null) {
            throw new BusinessException("恢复申请编号不能为空");
        }
        PasswordRecoveryRequest row = recoveryRequests.selectById(requestId);
        if (row == null || TenantContext.tenantId() != row.getTenantId()) {
            throw new BusinessException("恢复申请不存在或无权处理");
        }
        return row;
    }

    private SupportTicket requireTicket(Long ticketId) {
        if (ticketId == null) {
            throw new BusinessException("工单编号不能为空");
        }
        SupportTicket ticket = supportTickets.selectById(ticketId);
        if (ticket == null || TenantContext.tenantId() != ticket.getTenantId()) {
            throw new BusinessException("工单不存在或无权访问");
        }
        return ticket;
    }

    private void requireTicketAccess(SupportTicket ticket) {
        Long currentUserId = requireCurrentUserId();
        if (isAdministrator() || currentUserId.equals(ticket.getRequesterUserId())) {
            return;
        }
        throw new BusinessException("无权访问此工单");
    }

    private void requireAdministrator() {
        if (!isAdministrator()) {
            throw new BusinessException("仅系统管理员可处理账户支持请求");
        }
    }

    private boolean isAdministrator() {
        return TenantContext.roles().stream().map(value -> value.toLowerCase(Locale.ROOT)).anyMatch(ADMIN_ROLES::contains);
    }

    private Long requireCurrentUserId() {
        Long userId = TenantContext.userId();
        if (userId == null) {
            throw new BusinessException("请先登录后再执行此操作");
        }
        return userId;
    }

    private void updateRecoveryOrThrow(PasswordRecoveryRequest row) {
        if (recoveryRequests.updateById(row) != 1) {
            throw new BusinessException("恢复申请已被其他管理员更新，请刷新后重试");
        }
    }

    private void requireExpectedVersion(Integer currentVersion, Integer requestedVersion) {
        if (requestedVersion == null || currentVersion == null || !currentVersion.equals(requestedVersion)) {
            throw new BusinessException("数据已被其他用户更新，请刷新后重试");
        }
    }

    private boolean validTicketTransition(String current, String target) {
        return ("OPEN".equals(current) && "PROCESSING".equals(target))
                || ("PROCESSING".equals(current) && "RESOLVED".equals(target))
                || ("RESOLVED".equals(current) && "CLOSED".equals(target));
    }

    private void notifyAdministrators(String title, String content, String type, String businessType, Long businessId) {
        List<Long> recipients = users.listUsers().stream()
                .filter(user -> "INTERNAL".equals(user.getUserType()) && "ACTIVE".equals(user.getStatus()))
                .filter(user -> users.roleCodes(user.getId()).stream()
                        .map(value -> value.toLowerCase(Locale.ROOT)).anyMatch(ADMIN_ROLES::contains))
                .map(SysUser::getId)
                .toList();
        messages.notifyUsers(recipients, title, content, type, businessType, businessId);
    }

    private PasswordRecoveryView toRecoveryView(PasswordRecoveryRequest row) {
        return new PasswordRecoveryView(row.getId(), row.getUsername(), row.getContactName(), row.getContactValue(),
                row.getRequesterNote(), row.getStatus(), row.getHandlerId(), row.getHandlingNote(), row.getReviewedAt(),
                row.getResetAt(), row.getVersion(), row.getCreatedAt());
    }

    private SupportTicketView toTicketView(SupportTicket ticket) {
        List<SupportTicketAttachmentView> attachmentViews = attachments.selectList(Wrappers.<SupportTicketAttachment>lambdaQuery()
                        .eq(SupportTicketAttachment::getTenantId, ticket.getTenantId())
                        .eq(SupportTicketAttachment::getTicketId, ticket.getId())
                        .orderByAsc(SupportTicketAttachment::getId))
                .stream().map(attachment -> {
                    FileObject file = fileObjects.selectById(attachment.getFileObjectId());
                    return file == null ? null : toAttachmentView(attachment, file);
                }).filter(java.util.Objects::nonNull).toList();
        return new SupportTicketView(ticket.getId(), ticket.getTicketNo(), ticket.getRequesterUserId(),
                ticket.getRequesterUsername(), ticket.getContactName(), ticket.getContactValue(), ticket.getSource(),
                ticket.getCategory(), ticket.getPriority(), ticket.getPageContext(), ticket.getDescription(), ticket.getStatus(),
                ticket.getHandlerId(), ticket.getHandlingNote(), ticket.getResolvedAt(), ticket.getVersion(),
                ticket.getCreatedAt(), attachmentViews);
    }

    private SupportTicketAttachmentView toAttachmentView(SupportTicketAttachment attachment, FileObject file) {
        return new SupportTicketAttachmentView(attachment.getId(), file.getFileName(), file.getContentType(), file.getFileSize(),
                "/api/v1/admin/support-tickets/" + attachment.getTicketId() + "/attachments/" + attachment.getId());
    }

    private String requiredText(String value, String label, int maxLength) {
        String normalized = optionalText(value, maxLength);
        if (normalized == null) {
            throw new BusinessException(label + "不能为空");
        }
        return normalized;
    }

    private String optionalText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new BusinessException("内容长度不能超过 " + maxLength + " 个字符");
        }
        return normalized;
    }

    private String requiredEnum(String value, Set<String> allowed, String label) {
        String normalized = optionalEnum(value, allowed, label);
        if (normalized == null) {
            throw new BusinessException(label + "不能为空");
        }
        return normalized;
    }

    private String optionalEnum(String value, Set<String> allowed, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) {
            throw new BusinessException(label + "不合法");
        }
        return normalized;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String safeFileName(String originalFilename) {
        String value = originalFilename == null || originalFilename.isBlank() ? "support-image" : originalFilename;
        return value.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private String extensionOf(String filename) {
        int index = filename.lastIndexOf('.');
        return index < 0 ? "" : filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeContentType(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        int separator = value.indexOf(';');
        return (separator < 0 ? value : value.substring(0, separator)).trim().toLowerCase(Locale.ROOT);
    }
}
