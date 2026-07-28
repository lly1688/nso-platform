package com.nso.business.message.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nso.business.core.NsoDtos.MessageDto;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.business.core.TenantContext;
import com.nso.business.message.domain.Message;
import com.nso.business.message.domain.NotificationOutbox;
import com.nso.business.message.mapper.MessageMapper;
import com.nso.business.message.mapper.NotificationOutboxMapper;
import com.nso.business.message.service.IMessageService;
import com.nso.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageServiceImpl implements IMessageService {
    private final MessageMapper messages;
    private final NotificationOutboxMapper outbox;

    public MessageServiceImpl(MessageMapper messages, NotificationOutboxMapper outbox) { this.messages = messages; this.outbox = outbox; }

    @Override public PageResult<MessageDto> list(String status) {
        List<MessageDto> rows = messages.selectList(Wrappers.<Message>lambdaQuery()
                .eq(status != null && !status.isBlank(), Message::getStatus, status)
                .eq(TenantContext.userId() != null, Message::getReceiverId, TenantContext.userId())
                .orderByDesc(Message::getCreatedAt)).stream().map(this::toDto).toList();
        return new PageResult<>(rows, rows.size());
    }

    @Override public MessageDto markRead(Long messageId) {
        Message row = messages.selectById(messageId);
        if (row == null || (TenantContext.userId() != null && row.getReceiverId() != null && !TenantContext.userId().equals(row.getReceiverId()))) throw new BusinessException("消息不存在或无权操作");
        row.setStatus("READ"); row.setReadTime(LocalDateTime.now()); if (messages.updateById(row) != 1) throw new BusinessException("消息已被其他用户修改"); return toDto(row);
    }

    @Override public MessageDto notify(String title, String content, String type, String businessType, Long businessId) {
        Message row = new Message(); row.setTenantId(TenantContext.tenantId()); row.setReceiverId(TenantContext.userId()); row.setTitle(title); row.setContent(content); row.setType(type); row.setStatus("UNREAD"); row.setBusinessType(businessType); row.setBusinessId(businessId); row.setChannel("IN_APP"); row.setCreatedAt(LocalDateTime.now()); messages.insert(row);
        NotificationOutbox item = new NotificationOutbox(); item.setTenantId(TenantContext.tenantId()); item.setMessageId(row.getId()); item.setReceiverId(row.getReceiverId()); item.setChannel("IN_APP"); item.setPayloadJson("{\"messageId\":" + row.getId() + "}"); item.setSendStatus("PENDING"); item.setRetryCount(0); item.setNextRetryAt(LocalDateTime.now()); item.setCreatedAt(LocalDateTime.now()); outbox.insert(item);
        return toDto(row);
    }

    @Override public MessageDto notifyOnce(String title, String content, String type, String businessType, Long businessId) {
        Message existing = messages.selectOne(Wrappers.<Message>lambdaQuery().eq(Message::getReceiverId, TenantContext.userId()).eq(Message::getType, type).eq(Message::getBusinessType, businessType).eq(Message::getBusinessId, businessId).last("LIMIT 1"));
        return existing == null ? notify(title, content, type, businessType, businessId) : toDto(existing);
    }

    @Override public int retryPendingNotifications() {
        List<NotificationOutbox> rows = outbox.selectList(Wrappers.<NotificationOutbox>lambdaQuery().in(NotificationOutbox::getSendStatus, List.of("PENDING", "RETRY")).le(NotificationOutbox::getNextRetryAt, LocalDateTime.now()).orderByAsc(NotificationOutbox::getId));
        for (NotificationOutbox row : rows) { row.setSendStatus("SENT"); row.setSentAt(LocalDateTime.now()); row.setFailReason(null); outbox.updateById(row); }
        return rows.size();
    }

    private MessageDto toDto(Message row) { return new MessageDto(row.getId(), row.getTitle(), row.getContent(), row.getType(), row.getStatus(), row.getBusinessType(), row.getBusinessId(), row.getCreatedAt()); }
}
