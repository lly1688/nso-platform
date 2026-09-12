package com.nso.system.support.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

// 密码恢复申请对象，对应表 {@code nso_password_recovery_request}。
@Data
@TableName("nso_password_recovery_request")

public class PasswordRecoveryRequest {
    @TableId
    // 主键 ID。
    private Long id;
    // 租户 ID。
    private Long tenantId;
    // 用户 ID。
    private Long userId;
    // 用户名。
    private String username;
    // 联系人姓名。
    private String contactName;
    // 联系方式。
    private String contactValue;
    // 申请说明。
    private String requesterNote;
    // 处理状态。
    private String status;
    // 有效申请唯一键。
    private String activeKey;
    // 处理人 ID。
    private Long handlerId;
    // 处理说明。
    private String handlingNote;
    // 审核时间。
    private LocalDateTime reviewedAt;
    // 密码重置时间。
    private LocalDateTime resetAt;
    @Version
    // 乐观锁版本号。
    private Integer version;
    // 创建时间。
    private LocalDateTime createdAt;
    // 更新时间。
    private LocalDateTime updatedAt;
}
