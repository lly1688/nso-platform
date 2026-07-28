package com.nso.framework.storage;
import com.nso.business.file.ObjectStoragePort;
import com.nso.framework.config.NsoMinioProperties;
import io.minio.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.*;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component public class MinioObjectStorageAdapter implements ObjectStoragePort {
 private final MinioClient client; private final String bucket;
 public MinioObjectStorageAdapter(NsoMinioProperties p,@Value("${nso.storage.bucket}") String bucket){if(p.getEndpoint()==null||p.getEndpoint().isBlank()||p.getAccessKey()==null||p.getSecretKey()==null)throw new IllegalStateException("MinIO endpoint and credentials must be configured");this.client=MinioClient.builder().endpoint(p.getEndpoint()).credentials(p.getAccessKey(),p.getSecretKey()).build();this.bucket=bucket;}
 @Override public StoredObject put(String name,String type,long size,InputStream input) throws IOException {try{ensureBucket();ByteArrayOutputStream copy=new ByteArrayOutputStream();MessageDigest digest=MessageDigest.getInstance("SHA-256");byte[] buffer=new byte[8192];int read;while((read=input.read(buffer))!=-1){copy.write(buffer,0,read);digest.update(buffer,0,read);}String key=name;try(InputStream upload=new ByteArrayInputStream(copy.toByteArray())){client.putObject(PutObjectArgs.builder().bucket(bucket).object(key).stream(upload,size,-1).contentType(type==null?"application/octet-stream":type).build());}return new StoredObject(key,HexFormat.of().formatHex(digest.digest()));}catch(Exception ex){throw new IOException("MinIO upload failed",ex);}}
 @Override public InputStream get(String key) throws IOException {try{return client.getObject(GetObjectArgs.builder().bucket(bucket).object(key).build());}catch(Exception ex){throw new IOException("MinIO download failed",ex);}}
 private void ensureBucket() throws Exception {if(!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build()))client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());}
}
