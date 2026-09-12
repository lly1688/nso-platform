package com.nso.framework.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// API 文档配置属性。
@ConfigurationProperties(prefix = "nso.api-docs")
public class NsoApiDocsProperties {

    // 是否启用 API 文档
    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
