package com.nso.business.document.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nso.workflow.document")
public class DocumentWorkflowProperties {
    private boolean allowSelfPublish;

    public boolean isAllowSelfPublish() { return allowSelfPublish; }
    public void setAllowSelfPublish(boolean allowSelfPublish) { this.allowSelfPublish = allowSelfPublish; }
}
