package com.nso.business.core;

import java.util.List;
import java.util.function.Supplier;

// 请求级租户与操作人上下文。
public final class TenantContext {

    // 无登录身份时使用的默认租户编号
    public static final long DEFAULT_TENANT_ID = 1L;

    // 当前线程操作人
    private static final ThreadLocal<Actor> ACTOR = new ThreadLocal<>();

    private TenantContext() {
    }

    // 设置当前操作人。
    public static void set(Actor actor) {
        ACTOR.set(actor);
    }

    // 清除当前操作人。
    public static void clear() {
        ACTOR.remove();
    }

    // 获取当前操作人。
    public static Actor current() {
        return ACTOR.get();
    }

    // 获取当前租户编号。
    public static long tenantId() {
        return current() == null || current().tenantId() == null
                ? DEFAULT_TENANT_ID : current().tenantId();
    }

    // 获取当前用户编号。
    public static Long userId() {
        return current() == null ? null : current().userId();
    }

    // 获取当前用户名。
    public static String username() {
        return current() == null ? "SYSTEM" : current().username();
    }

    // 获取当前角色列表。
    public static List<String> roles() {
        return current() == null || current().roles() == null ? List.of() : current().roles();
    }

    // 判断当前操作人是否拥有任一指定角色。
    public static boolean hasAnyRole(String... expected) {
        for (String role : expected)
            if (roles().stream().anyMatch(item -> item.equalsIgnoreCase(role)))
                return true;
        return false;
    }

    // 在临时租户上下文中执行业务操作并恢复原上下文。
    public static <T> T withTenant(long tenantId, Supplier<T> action) {
        Actor previous = current();
        try {
            set(new Actor(
                    tenantId,
                    previous == null ? null : previous.userId(),
                    previous == null ? "PUBLIC" : previous.username(),
                    previous == null ? List.of() : previous.roles()));
            return action.get();
        } finally {
            if (previous == null)
                clear();
            else
                set(previous);
        }
    }

    public record Actor(
        // 租户编号
        Long tenantId,
        // 用户编号
        Long userId,
        // 用户名
        String username,
        // 角色编码列表
        List<String> roles
    ) {
    }
}
