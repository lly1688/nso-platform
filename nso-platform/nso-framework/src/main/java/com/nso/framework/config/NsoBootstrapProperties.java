package com.nso.framework.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// 系统引导账号配置。
@ConfigurationProperties(prefix = "nso.bootstrap")
public class NsoBootstrapProperties {

    // 是否启用引导账号
    private boolean enabled;

    // 管理员用户名
    private String adminUsername;

    // 管理员初始密码
    private String adminPassword;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }
}
