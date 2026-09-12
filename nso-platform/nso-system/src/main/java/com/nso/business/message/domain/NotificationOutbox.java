package com.nso.business.message.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

// 通知发件箱实体。
@Data
@TableName("nso_notification_outbox")
public class NotificationOutbox {

    // 发件箱记录编号
    @TableId
    private Long id;

    // 租户编号
    private Long tenantId;

    // 消息编号
    private Long messageId;

    // 接收人编号
    private Long receiverId;

    // 发送渠道
    private String channel;

    // 消息载荷
    private String payloadJson;

    // 发送状态
    private String sendStatus;

    // 重试次数
    private Integer retryCount;

    // 下次重试时间
    private LocalDateTime nextRetryAt;

    // 发送时间
    private LocalDateTime sentAt;

    // 失败原因
    private String failReason;

    // 创建时间
    private LocalDateTime createdAt;
}
