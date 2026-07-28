package com.nso.framework.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nso.security")
public class NsoSecurityProperties {
    private String issuer;
    private String secret;
    private long accessTokenMinutes = 30;
    private long refreshTokenDays = 7;
    private int loginMaxFailures = 5;
    private long loginFailureWindowMinutes = 15;
    private long loginLockMinutes = 15;
    private int authRateLimitPerMinute = 100;
    private int downloadRateLimitPerMinute = 60;

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public long getAccessTokenMinutes() { return accessTokenMinutes; }
    public void setAccessTokenMinutes(long accessTokenMinutes) { this.accessTokenMinutes = accessTokenMinutes; }
    public long getRefreshTokenDays() { return refreshTokenDays; }
    public void setRefreshTokenDays(long refreshTokenDays) { this.refreshTokenDays = refreshTokenDays; }
    public int getLoginMaxFailures() { return loginMaxFailures; }
    public void setLoginMaxFailures(int loginMaxFailures) { this.loginMaxFailures = loginMaxFailures; }
    public long getLoginFailureWindowMinutes() { return loginFailureWindowMinutes; }
    public void setLoginFailureWindowMinutes(long loginFailureWindowMinutes) { this.loginFailureWindowMinutes = loginFailureWindowMinutes; }
    public long getLoginLockMinutes() { return loginLockMinutes; }
    public void setLoginLockMinutes(long loginLockMinutes) { this.loginLockMinutes = loginLockMinutes; }
    public int getAuthRateLimitPerMinute() { return authRateLimitPerMinute; }
    public void setAuthRateLimitPerMinute(int authRateLimitPerMinute) { this.authRateLimitPerMinute = authRateLimitPerMinute; }
    public int getDownloadRateLimitPerMinute() { return downloadRateLimitPerMinute; }
    public void setDownloadRateLimitPerMinute(int downloadRateLimitPerMinute) { this.downloadRateLimitPerMinute = downloadRateLimitPerMinute; }
}
