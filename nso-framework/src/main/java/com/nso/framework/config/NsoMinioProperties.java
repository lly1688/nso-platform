package com.nso.framework.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// MinIO 对象存储配置。
@ConfigurationProperties(prefix = "nso.minio")
public class NsoMinioProperties {

    // 服务地址
    private String endpoint;

    // 访问密钥
    private String accessKey;

    // 密钥口令
    private String secretKey;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
}
