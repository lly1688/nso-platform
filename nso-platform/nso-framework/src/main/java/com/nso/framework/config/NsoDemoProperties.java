package com.nso.framework.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// 演示环境初始化配置。
@ConfigurationProperties(prefix = "nso.demo")
public class NsoDemoProperties {

    // 是否启用演示数据初始化
    private boolean enabled;

    // 演示账号默认密码
    private String defaultPassword;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultPassword() {
        return defaultPassword;
    }

    public void setDefaultPassword(String defaultPassword) {
        this.defaultPassword = defaultPassword;
    }
}
