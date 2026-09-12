package com.nso.business.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// 技术文档工作流配置。
@ConfigurationProperties(prefix = "nso.workflow.document")
public class DocumentWorkflowProperties {

    // 是否允许上传人自行发布
    private boolean allowSelfPublish;

    public boolean isAllowSelfPublish() {
        return allowSelfPublish;
    }

    public void setAllowSelfPublish(boolean allowSelfPublish) {
        this.allowSelfPublish = allowSelfPublish;
    }
}
