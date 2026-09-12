package com.nso.business.file.service;

import java.io.InputStream;

public record FileUploadPayload(
    // 原始文件名
    String originalFilename,
    // 内容类型
    String contentType,
    // 文件大小
    long size,
    // 文件输入流
    InputStream inputStream
) {
}
