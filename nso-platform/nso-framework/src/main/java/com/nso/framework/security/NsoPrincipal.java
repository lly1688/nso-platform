package com.nso.framework.security;

import java.util.List;

public record NsoPrincipal(
    // 租户编号
    Long tenantId,
    // 用户编号
    Long userId,
    // 用户名
    String username,
    // 客户端编号
    String clientId,
    // 角色编码列表
    List<String> roles,
    // 权限编码列表
    List<String> permissions,
    // 授权版本
    int authVersion
) {
}
