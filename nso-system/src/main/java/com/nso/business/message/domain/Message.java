package com.nso.business.message.domain;
import com.baomidou.mybatisplus.annotation.*; import lombok.Data; import java.time.LocalDateTime;
@Data @TableName("nso_message") public class Message { @TableId private Long id; private Long tenantId; private Long receiverId; private String title; private String content; private String type; private String status; private String businessType; private Long businessId; private String channel; private LocalDateTime readTime; private LocalDateTime createdAt; @Version private Integer version; @TableLogic private Integer deleted; }
