package com.nso.business.core;

import java.util.List;
import java.util.function.Supplier;

/**
 * Request-scoped actor identity shared by the business module. The framework
 * populates it after token verification; scheduled jobs and migration tools
 * intentionally fall back to the default tenant only when no actor exists.
 */
public final class TenantContext {
    public static final long DEFAULT_TENANT_ID = 1L;
    private static final ThreadLocal<Actor> ACTOR = new ThreadLocal<>();

    private TenantContext() { }

    public static void set(Actor actor) { ACTOR.set(actor); }
    public static void clear() { ACTOR.remove(); }
    public static Actor current() { return ACTOR.get(); }
    public static long tenantId() { return current() == null || current().tenantId() == null ? DEFAULT_TENANT_ID : current().tenantId(); }
    public static Long userId() { return current() == null ? null : current().userId(); }
    public static String username() { return current() == null ? "SYSTEM" : current().username(); }
    public static List<String> roles() { return current() == null || current().roles() == null ? List.of() : current().roles(); }
    public static boolean hasAnyRole(String... expected) {
        for (String role : expected) if (roles().stream().anyMatch(item -> item.equalsIgnoreCase(role))) return true;
        return false;
    }

    public static <T> T withTenant(long tenantId, Supplier<T> action) {
        Actor previous = current();
        try { set(new Actor(tenantId, previous == null ? null : previous.userId(), previous == null ? "PUBLIC" : previous.username(), previous == null ? List.of() : previous.roles())); return action.get(); }
        finally { if (previous == null) clear(); else set(previous); }
    }

    public record Actor(Long tenantId, Long userId, String username, List<String> roles) { }
}
