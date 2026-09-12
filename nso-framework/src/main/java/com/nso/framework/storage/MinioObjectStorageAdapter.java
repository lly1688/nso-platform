package com.nso.framework.storage;

import com.nso.business.file.ObjectStoragePort;
import com.nso.framework.config.NsoMinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HexFormat;

// MinIO 对象存储适配器。
@Component
public class MinioObjectStorageAdapter implements ObjectStoragePort {

    // MinIO客户端
    private final MinioClient client;
    private final String bucket;

    public MinioObjectStorageAdapter(NsoMinioProperties properties,
                                     @Value("${nso.storage.bucket}") String bucket) {
        if (properties.getEndpoint() == null || properties.getEndpoint().isBlank()
                || properties.getAccessKey() == null || properties.getSecretKey() == null) {
            throw new IllegalStateException("MinIO endpoint and credentials must be configured");
        }
        this.client = MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
        this.bucket = bucket;
    }

    // 上传对象并计算内容摘要。
    @Override
    public StoredObject put(String name, String type, long size, InputStream input) throws IOException {
        try {
            ensureBucket();
            // 上传过程同步计算摘要，避免再次读取大文件。
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String key = name;
            DigestInputStream upload = new DigestInputStream(input, digest);
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(upload, size, -1)
                    .contentType(type == null ? "application/octet-stream" : type)
                    .build());
            return new StoredObject(key, HexFormat.of().formatHex(digest.digest()));
        } catch (Exception ex) {
            throw new IOException("MinIO upload failed", ex);
        }
    }

    // 读取指定对象。
    @Override
    public InputStream get(String key) throws IOException {
        try {
            return client.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .build());
        } catch (Exception ex) {
            throw new IOException("MinIO download failed", ex);
        }
    }

    // 删除指定对象。
    @Override
    public void delete(String key) throws IOException {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(key).build());
        } catch (Exception ex) {
            throw new IOException("MinIO delete failed", ex);
        }
    }

    private void ensureBucket() throws Exception {
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }
}
