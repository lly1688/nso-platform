package com.nso.business.file.service;

import java.io.InputStream;

public record FileDownloadPayload(
    // 文件名
    String fileName,
    // 内容类型
    String contentType,
    // 文件大小
    long size,
    // 上传时生成并落库的内容摘要，用于校验下载输入流。
    String sha256,
    // 文件输入流
    InputStream inputStream
) {
}
