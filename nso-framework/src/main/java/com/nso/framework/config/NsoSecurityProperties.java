package com.nso.framework.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// 认证与安全策略配置。
@ConfigurationProperties(prefix = "nso.security")
public class NsoSecurityProperties {

    // JWT 签发方
    private String issuer;

    // JWT 签名密钥
    private String secret;

    // 访问令牌有效分钟数
    private long accessTokenMinutes = 30;

    // 刷新令牌有效天数
    private long refreshTokenDays = 7;

    // 登录最大失败次数
    private int loginMaxFailures = 5;

    // 登录失败统计窗口分钟数
    private long loginFailureWindowMinutes = 15;

    // 登录锁定分钟数
    private long loginLockMinutes = 15;

    // 认证接口每分钟限流数
    private int authRateLimitPerMinute = 100;

    // 下载接口每分钟限流数
    private int downloadRateLimitPerMinute = 60;

    // 密码找回每分钟限流数
    private int passwordRecoveryRateLimitPerMinute = 10;

    // 访客工单每分钟限流数
    private int guestSupportRateLimitPerMinute = 30;

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTokenMinutes() {
        return accessTokenMinutes;
    }

    public void setAccessTokenMinutes(long accessTokenMinutes) {
        this.accessTokenMinutes = accessTokenMinutes;
    }

    public long getRefreshTokenDays() {
        return refreshTokenDays;
    }

    public void setRefreshTokenDays(long refreshTokenDays) {
        this.refreshTokenDays = refreshTokenDays;
    }

    public int getLoginMaxFailures() {
        return loginMaxFailures;
    }

    public void setLoginMaxFailures(int loginMaxFailures) {
        this.loginMaxFailures = loginMaxFailures;
    }

    public long getLoginFailureWindowMinutes() {
        return loginFailureWindowMinutes;
    }

    public void setLoginFailureWindowMinutes(long loginFailureWindowMinutes) {
        this.loginFailureWindowMinutes = loginFailureWindowMinutes;
    }

    public long getLoginLockMinutes() {
        return loginLockMinutes;
    }

    public void setLoginLockMinutes(long loginLockMinutes) {
        this.loginLockMinutes = loginLockMinutes;
    }

    public int getAuthRateLimitPerMinute() {
        return authRateLimitPerMinute;
    }

    public void setAuthRateLimitPerMinute(int authRateLimitPerMinute) {
        this.authRateLimitPerMinute = authRateLimitPerMinute;
    }

    public int getDownloadRateLimitPerMinute() {
        return downloadRateLimitPerMinute;
    }

    public void setDownloadRateLimitPerMinute(int downloadRateLimitPerMinute) {
        this.downloadRateLimitPerMinute = downloadRateLimitPerMinute;
    }

    public int getPasswordRecoveryRateLimitPerMinute() {
        return passwordRecoveryRateLimitPerMinute;
    }

    public void setPasswordRecoveryRateLimitPerMinute(int passwordRecoveryRateLimitPerMinute) {
        this.passwordRecoveryRateLimitPerMinute = passwordRecoveryRateLimitPerMinute;
    }

    public int getGuestSupportRateLimitPerMinute() {
        return guestSupportRateLimitPerMinute;
    }

    public void setGuestSupportRateLimitPerMinute(int guestSupportRateLimitPerMinute) {
        this.guestSupportRateLimitPerMinute = guestSupportRateLimitPerMinute;
    }
}
