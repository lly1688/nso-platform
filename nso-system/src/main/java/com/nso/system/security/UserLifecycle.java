package com.nso.system.security;

import com.nso.shared.exception.BusinessException;

import java.util.Set;

// 内部账号生命周期工具，与部门和角色的启用状态保持分离。
public final class UserLifecycle {
    // 待激活状态。
    public static final String PENDING = "PENDING";
    // 已启用状态。
    public static final String ACTIVE = "ACTIVE";
    // 已锁定状态。
    public static final String LOCKED = "LOCKED";
    // 已暂停状态。
    public static final String SUSPENDED = "SUSPENDED";
    // 已禁用状态。
    public static final String DISABLED = "DISABLED";
    // 已离职状态。
    public static final String LEFT = "LEFT";
    // 支持的生命周期状态。
    private static final Set<String> VALUES = Set.of(PENDING, ACTIVE, LOCKED, SUSPENDED, DISABLED, LEFT);

    private UserLifecycle() {
    }

    // 判断账号是否已启用。
    public static boolean isActive(String status) {
        return ACTIVE.equals(status);
    }

    // 规范化并校验账号状态。
    public static String normalize(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase();
        if (!VALUES.contains(normalized)) {
            throw new BusinessException("账号状态不合法");
        }
        return normalized;
    }
}
