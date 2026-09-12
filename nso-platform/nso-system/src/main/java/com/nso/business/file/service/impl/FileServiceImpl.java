package com.nso.business.file.service.impl;

import com.nso.business.core.NsoDtos.FileUploadResult;
import com.nso.business.core.TenantContext;
import com.nso.business.file.ObjectStoragePort;
import com.nso.business.file.domain.FileObject;
import com.nso.business.file.mapper.FileObjectMapper;
import com.nso.business.file.service.FileDownloadPayload;
import com.nso.business.file.service.FileUploadPayload;
import com.nso.business.file.service.IFileService;
import com.nso.business.project.service.ProjectDataScope;
import com.nso.shared.exception.BusinessException;
import com.nso.shared.util.FileContentSignature;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service

// 文件管理 服务层处理
public class FileServiceImpl implements IFileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);
    private static final long MAX_FILE_SIZE = 200L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "png", "jpg", "jpeg", "webp", "xlsx", "xls", "doc", "docx", "dwg", "dxf", "step", "stp", "zip");
    private static final Map<String, Set<String>> ALLOWED_CONTENT_TYPES = Map.ofEntries(
            Map.entry("pdf", Set.of("application/pdf")),
            Map.entry("png", Set.of("image/png")),
            Map.entry("jpg", Set.of("image/jpeg")),
            Map.entry("jpeg", Set.of("image/jpeg")),
            Map.entry("webp", Set.of("image/webp")),
            Map.entry("xlsx", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            Map.entry("xls", Set.of("application/vnd.ms-excel")),
            Map.entry("doc", Set.of("application/msword")),
            Map.entry("docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
            Map.entry("dwg", Set.of("application/acad", "image/vnd.dwg", "application/octet-stream")),
            Map.entry("dxf", Set.of("image/vnd.dxf", "application/dxf", "application/octet-stream")),
            Map.entry("step", Set.of("model/step", "application/step", "application/octet-stream")),
            Map.entry("stp", Set.of("model/step", "application/step", "application/octet-stream")),
            Map.entry("zip", Set.of("application/zip", "application/x-zip-compressed", "application/octet-stream")));

    // 文件对象数据映射
    private final FileObjectMapper mapper;
    // 对象存储端口
    private final ObjectStoragePort storage;
    // 项目数据范围
    private final ProjectDataScope dataScope;

    public FileServiceImpl(FileObjectMapper mapper, ObjectStoragePort storage, ProjectDataScope dataScope) {
        this.mapper = mapper;
        this.storage = storage;
        this.dataScope = dataScope;
    }

    // 上传项目文件。
    @Override
    @Transactional
    public FileUploadResult upload(Long projectId, FileUploadPayload file) {
        if (projectId == null) {
            throw new BusinessException("上传文件必须指定项目");
        }
        dataScope.requireAccess(projectId);
        if (file == null || file.inputStream() == null || file.size() <= 0) {
            throw new BusinessException("上传文件不能为空");
        }
        if (file.size() > MAX_FILE_SIZE) {
            throw new BusinessException("单个附件不得超过 200MB");
        }
        ObjectStoragePort.StoredObject stored = null;
        try (InputStream rawInput = file.inputStream()) {
            String safeName = file.originalFilename() == null ? "upload.bin"
                    : file.originalFilename().replaceAll("[\\\\/:*?\"<>|]", "_");
            int dot = safeName.lastIndexOf('.');
            String extension = dot < 0 ? "" : safeName.substring(dot + 1).toLowerCase();
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                throw new BusinessException("不支持的文件类型");
            }
            String contentType = normalizeContentType(file.contentType());
            if (!ALLOWED_CONTENT_TYPES.get(extension).contains(contentType)) {
                throw new BusinessException("文件扩展名与 MIME 类型不匹配");
            }
            try (InputStream input = FileContentSignature.verify(extension, rawInput)) {
                // 对象存储在写入时同步计算实际上传内容的 SHA-256。
                stored = storage.put(
                        TenantContext.tenantId() + "/" + projectId + "/" + UUID.randomUUID() + "/" + safeName,
                        contentType,
                        file.size(),
                        input);
            }
            FileObject row = new FileObject();
            row.setTenantId(TenantContext.tenantId());
            row.setProjectId(projectId);
            row.setCreatedBy(TenantContext.userId());
            row.setFileName(safeName);
            row.setContentType(contentType);
            row.setFileSize(file.size());
            // 持久化上传时计算的摘要，供后续下载时重新计算并比对。
            row.setSha256(stored.sha256());
            row.setStoragePath(stored.objectKey());
            mapper.insert(row);
            return new FileUploadResult(
                    row.getId(),
                    row.getFileName(),
                    row.getContentType(),
                    row.getFileSize(),
                    row.getSha256(),
                    "/api/v1/admin/files/" + row.getId() + "/download");
        } catch (Exception ex) {
            if (stored != null) {
                try {
                    storage.delete(stored.objectKey());
                } catch (Exception cleanupException) {
                    log.warn("Failed to delete orphaned upload object key={}", stored.objectKey(), cleanupException);
                }
            }
            if (ex instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException("文件上传失败：" + ex.getMessage());
        }
    }

    private String normalizeContentType(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        int separator = value.indexOf(';');
        return (separator < 0 ? value : value.substring(0, separator)).trim().toLowerCase();
    }

    // 下载文件并校验访问范围。
    @Override
    public FileDownloadPayload download(Long fileId) {
        FileObject row = mapper.selectById(fileId);
        if (row == null || row.getTenantId() == null || row.getTenantId() != TenantContext.tenantId()) {
            throw new BusinessException("文件不存在");
        }
        if (row.getProjectId() == null) {
            throw BusinessException.accessDenied(
                    "FILE_PROJECT_SCOPE",
                    "未绑定项目的历史文件不可直接下载",
                    String.valueOf(fileId),
                    "已绑定项目",
                    "通过受控技术版本重新上传");
        }
        dataScope.requireAccess(row.getProjectId());
        try {
            // 用落库摘要包装对象存储输入流，完整读取至 EOF 后校验文件内容是否发生变化。
            return new FileDownloadPayload(row.getFileName(), row.getContentType(), row.getFileSize(), row.getSha256(),
                    new com.nso.business.file.service.IntegrityCheckingInputStream(storage.get(row.getStoragePath()), row.getSha256()));
        } catch (Exception ex) {
            throw new BusinessException("文件读取失败：" + ex.getMessage());
        }
    }
}
