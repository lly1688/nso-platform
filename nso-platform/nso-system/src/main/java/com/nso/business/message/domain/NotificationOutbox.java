package com.nso.business.message.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("nso_notification_outbox")
public class NotificationOutbox {
    @TableId private Long id;
    private Long tenantId;
    private Long messageId;
    private Long receiverId;
    private String channel;
    private String payloadJson;
    private String sendStatus;
    private Integer retryCount;
    private LocalDateTime nextRetryAt;
    private LocalDateTime sentAt;
    private String failReason;
    private LocalDateTime createdAt;
}
