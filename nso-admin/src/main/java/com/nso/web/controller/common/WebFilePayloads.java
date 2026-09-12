package com.nso.web.controller.common;

import com.nso.business.file.service.FileDownloadPayload;
import com.nso.business.file.service.FileUploadPayload;
import com.nso.shared.exception.BusinessException;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

// Spring MVC 与文件服务载荷的转换工具。
public final class WebFilePayloads {
    private WebFilePayloads() {
    }

    // 将上传文件转换为服务层载荷。
    public static FileUploadPayload upload(MultipartFile file) {
        if (file == null) {
            throw new BusinessException("上传文件不能为空");
        }
        try {
            return new FileUploadPayload(file.getOriginalFilename(), file.getContentType(), file.getSize(), file.getInputStream());
        } catch (IOException exception) {
            throw new BusinessException("读取上传文件失败: " + exception.getMessage());
        }
    }

    // 构造附件下载响应。
    public static ResponseEntity<Resource> attachment(FileDownloadPayload payload) {
        return response(payload, true);
    }

    // 构造内联预览响应。
    public static ResponseEntity<Resource> inline(FileDownloadPayload payload) {
        return response(payload, false);
    }

    private static ResponseEntity<Resource> response(FileDownloadPayload payload, boolean attachment) {
        if (payload == null || payload.inputStream() == null) {
            throw new BusinessException("文件内容不存在");
        }
        String name = payload.fileName() == null || payload.fileName().isBlank() ? "download" : payload.fileName();
        ContentDisposition disposition = attachment
                ? ContentDisposition.attachment().filename(name, StandardCharsets.UTF_8).build()
                : ContentDisposition.inline().filename(name, StandardCharsets.UTF_8).build();
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(mediaType(payload.contentType()));
        if (payload.size() >= 0) {
            builder.contentLength(payload.size());
        }
        return builder.body(new InputStreamResource(payload.inputStream()));
    }

    private static MediaType mediaType(String value) {
        try {
            return value == null || value.isBlank() ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(value);
        } catch (IllegalArgumentException ignored) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
