package com.nso.framework.security;

public final class NsoClientBoundary {
    private NsoClientBoundary() {}
    public static boolean allows(String requestUri, String clientId) {
        String uri = requestUri == null ? "" : requestUri;
        int marker = uri.indexOf("/api/v1/");
        if (marker >= 0) uri = uri.substring(marker);
        if (uri.equals("/api/v1/admin") || uri.startsWith("/api/v1/admin/")) {
            return "admin".equals(clientId);
        }
        return true;
    }
}
