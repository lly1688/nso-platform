package com.nso.framework.security;

import java.util.List;

public record NsoPrincipal(Long tenantId, Long userId, String username, String clientId,
                           List<String> roles, List<String> permissions, int authVersion) {
}
