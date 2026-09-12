package com.nso.business.file;

import java.io.IOException;
import java.io.InputStream;

/**
 * 对象存储端口接口。
 */
public interface ObjectStoragePort {

    /**
     * 写入对象。
     *
     * @param objectName 对象名称
     * @param contentType 内容类型
     * @param size 对象大小
     * @param inputStream 对象内容
     * @return 已存储对象信息
     * @throws IOException 对象写入失败
     */
    StoredObject put(String objectName, String contentType, long size, InputStream inputStream) throws IOException;

    /**
     * 读取对象。
     *
     * @param objectKey 对象标识
     * @return 对象输入流
     * @throws IOException 对象读取失败
     */
    InputStream get(String objectKey) throws IOException;

    /**
     * 删除对象。
     *
     * @param objectKey 对象标识
     * @throws IOException 对象删除失败
     */
    void delete(String objectKey) throws IOException;

    /**
     * 已存储对象信息。
     *
     * @param objectKey 对象标识
     * @param sha256 内容摘要
     */
    record StoredObject(String objectKey,
                        // 上传时基于实际写入对象存储的字节计算；文件服务将其持久化为下载校验基准。
                        String sha256) {
    }
}
