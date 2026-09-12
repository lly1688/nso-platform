package com.nso.business.message.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

// 站内消息实体。
@Data
@TableName("nso_message")
public class Message {

    // 消息编号
    @TableId
    private Long id;

    // 租户编号
    private Long tenantId;

    // 接收人编号
    private Long receiverId;

    // 消息标题
    private String title;

    // 消息内容
    private String content;

    // 消息类型
    private String type;

    // 阅读状态
    private String status;

    // 业务类型
    private String businessType;

    // 业务编号
    private Long businessId;

    // 发送渠道
    private String channel;

    // 阅读时间
    private LocalDateTime readTime;

    // 创建时间
    private LocalDateTime createdAt;

    // 数据版本
    @Version
    private Integer version;

    // 删除标志
    @TableLogic
    private Integer deleted;
}
