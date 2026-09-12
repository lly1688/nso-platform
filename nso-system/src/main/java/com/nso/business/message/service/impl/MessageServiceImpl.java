package com.nso.business.message.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nso.business.core.NsoDtos.MessageDto;
import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.business.core.PageSupport;
import com.nso.business.core.TenantContext;
import com.nso.business.message.domain.Message;
import com.nso.business.message.domain.NotificationOutbox;
import com.nso.business.message.mapper.MessageMapper;
import com.nso.business.message.mapper.NotificationOutboxMapper;
import com.nso.business.message.service.IMessageService;
import com.nso.business.project.domain.ProjectMember;
import com.nso.business.project.mapper.ProjectMemberMapper;
import com.nso.shared.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Service

// 消息管理 服务层处理
public class MessageServiceImpl implements IMessageService {
    // 消息数据映射
    private final MessageMapper messages;
    // 通知发件箱数据映射
    private final NotificationOutboxMapper outbox;
    // 项目成员数据映射
    private final ProjectMemberMapper projectMembers;
    // JDBC模板
    private final JdbcTemplate jdbc;

    public MessageServiceImpl(MessageMapper messages, NotificationOutboxMapper outbox, ProjectMemberMapper projectMembers, JdbcTemplate jdbc) {
        this.messages = messages;
        this.outbox = outbox;
        this.projectMembers = projectMembers;
        this.jdbc = jdbc;
    }

    @Override public PageResult<MessageDto> list(String status) {
        List<MessageDto> rows = messages.selectList(Wrappers.<Message>lambdaQuery()
                .eq(status != null && !status.isBlank(), Message::getStatus, status)
                .eq(TenantContext.userId() != null, Message::getReceiverId, TenantContext.userId())
                .orderByDesc(Message::getCreatedAt)).stream().map(this::toDto).toList();
        return new PageResult<>(rows, rows.size());
    }

    // 分页查询当前用户消息。
    @Override
    public PageResult<MessageDto> list(String status, PageQuery pageQuery) {
        PageQuery page = PageSupport.normalize(pageQuery);
        Page<Message> entityPage = messages.selectPage(PageSupport.page(page), Wrappers.<Message>lambdaQuery()
                .eq(status != null && !status.isBlank(), Message::getStatus, status)
                .eq(TenantContext.userId() != null, Message::getReceiverId, TenantContext.userId())
                .orderByDesc(Message::getCreatedAt));
        return PageSupport.result(entityPage, page, this::toDto);
    }

    // 将消息标记为已读。
    @Override
    public MessageDto markRead(Long messageId) {
        Message row = messages.selectById(messageId);
        if (row == null || (TenantContext.userId() != null && row.getReceiverId() != null && !TenantContext.userId().equals(row.getReceiverId()))) throw new BusinessException("消息不存在或无权操作");
        row.setStatus("READ");
        row.setReadTime(LocalDateTime.now());
        if (messages.updateById(row) != 1) throw new BusinessException("消息已被其他用户修改");
        return toDto(row);
    }

    // 向当前用户发送消息。
    @Override
    public MessageDto notify(String title, String content, String type, String businessType, Long businessId) {
        return notifyTo(TenantContext.userId(), title, content, type, businessType, businessId);
    }

    // 向指定用户批量发送消息。
    @Override
    public int notifyUsers(List<Long> receiverIds, String title, String content, String type, String businessType, Long businessId) {
        if (receiverIds == null || receiverIds.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Long receiverId : receiverIds.stream().filter(java.util.Objects::nonNull).distinct().toList()) {
            notifyTo(receiverId, title, content, type, businessType, businessId);
            count++;
        }
        return count;
    }

    private MessageDto notifyTo(Long receiverId, String title, String content, String type, String businessType, Long businessId) {
        Message row = new Message();
        row.setTenantId(TenantContext.tenantId());
        row.setReceiverId(receiverId);
        row.setTitle(title);
        row.setContent(content);
        row.setType(type);
        row.setStatus("UNREAD");
        row.setBusinessType(businessType);
        row.setBusinessId(businessId);
        row.setChannel("IN_APP");
        row.setCreatedAt(LocalDateTime.now());
        messages.insert(row);

        NotificationOutbox item = new NotificationOutbox();
        item.setTenantId(TenantContext.tenantId());
        item.setMessageId(row.getId());
        item.setReceiverId(row.getReceiverId());
        item.setChannel("IN_APP");
        item.setPayloadJson("{\"messageId\":" + row.getId() + "}");
        item.setSendStatus("PENDING");
        item.setRetryCount(0);
        item.setNextRetryAt(LocalDateTime.now());
        item.setCreatedAt(LocalDateTime.now());
        outbox.insert(item);
        return toDto(row);
    }

    // 按业务维度幂等发送消息。
    @Override
    public MessageDto notifyOnce(String title, String content, String type, String businessType, Long businessId) {
        Message existing = messages.selectOne(Wrappers.<Message>lambdaQuery().eq(Message::getReceiverId, TenantContext.userId()).eq(Message::getType, type).eq(Message::getBusinessType, businessType).eq(Message::getBusinessId, businessId).last("LIMIT 1"));
        return existing == null ? notify(title, content, type, businessType, businessId) : toDto(existing);
    }

    // 向项目成员发送事件通知。
    @Override
    public int notifyProject(Long projectId, String eventType, String title, String content, String businessType, Long businessId) {
        List<String> roles = jdbc.queryForList("SELECT recipient_project_role FROM nso_message_recipient_rule WHERE tenant_id=? AND event_type=? AND enabled=1 AND recipient_project_role IS NOT NULL", String.class, TenantContext.tenantId(), eventType);
        // 收件人路由基于已持久化的项目职责，而非发起人上下文。
        // 仅在 V19 后表可用时，才通过不依赖 MyBatis 的 JDBC 读取矩阵；否则此调用为空操作。
        return notifyProjectMembers(projectId, eventType, title, content, businessType, businessId, roles);
    }

    private int notifyProjectMembers(Long projectId, String eventType, String title, String content, String businessType, Long businessId, List<String> configuredRoles) {
        List<ProjectMember> members = projectMembers.selectList(Wrappers.<ProjectMember>lambdaQuery().eq(ProjectMember::getProjectId, projectId).eq(ProjectMember::getStatus, "ACTIVE"));
        int count = 0;
        for (ProjectMember member : members) {
            if (member.getUserId() == null || (!configuredRoles.isEmpty() && !configuredRoles.contains(member.getProjectRole()))) continue;
            Message existing = messages.selectOne(Wrappers.<Message>lambdaQuery().eq(Message::getReceiverId, member.getUserId()).eq(Message::getType, eventType).eq(Message::getBusinessType, businessType).eq(Message::getBusinessId, businessId).last("LIMIT 1"));
            if (existing != null) continue;
            Message row = new Message();
            row.setTenantId(TenantContext.tenantId());
            row.setReceiverId(member.getUserId());
            row.setTitle(title);
            row.setContent(content);
            row.setType(eventType);
            row.setStatus("UNREAD");
            row.setBusinessType(businessType);
            row.setBusinessId(businessId);
            row.setChannel("IN_APP");
            row.setCreatedAt(LocalDateTime.now());
            messages.insert(row);

            NotificationOutbox item = new NotificationOutbox();
            item.setTenantId(TenantContext.tenantId());
            item.setMessageId(row.getId());
            item.setReceiverId(member.getUserId());
            item.setChannel("IN_APP");
            item.setPayloadJson("{\"messageId\":" + row.getId() + "}");
            item.setSendStatus("PENDING");
            item.setRetryCount(0);
            item.setNextRetryAt(LocalDateTime.now());
            item.setCreatedAt(LocalDateTime.now());
            outbox.insert(item);
            count++;
        }
        return count;
    }

    // 重试待发送通知。
    @Override
    public int retryPendingNotifications() {
        List<NotificationOutbox> rows = outbox.selectList(Wrappers.<NotificationOutbox>lambdaQuery().in(NotificationOutbox::getSendStatus, List.of("PENDING", "RETRY")).le(NotificationOutbox::getNextRetryAt, LocalDateTime.now()).orderByAsc(NotificationOutbox::getId));
        int delivered = 0;
        for (NotificationOutbox row : rows) {
            if ("IN_APP".equalsIgnoreCase(row.getChannel())) {
                row.setSendStatus("SENT");
                row.setSentAt(LocalDateTime.now());
                row.setFailReason(null);
                outbox.updateById(row);
                delivered++;
            } else {
                row.setSendStatus("RETRY");
                row.setRetryCount((row.getRetryCount() == null ? 0 : row.getRetryCount()) + 1);
                row.setNextRetryAt(LocalDateTime.now().plusMinutes(Math.min(60, 5 * row.getRetryCount())));
                row.setFailReason("出站渠道未配置；未伪造发送成功，等待可配置适配器重试");
                outbox.updateById(row);
            }
        }
        return delivered;
    }

    private MessageDto toDto(Message row) {
        return new MessageDto(row.getId(), row.getTitle(), row.getContent(), row.getType(), row.getStatus(), row.getBusinessType(), row.getBusinessId(), row.getCreatedAt());
    }
}
