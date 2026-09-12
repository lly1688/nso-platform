package com.nso.business.file.service;

import com.nso.business.core.NsoDtos.FileUploadResult;

/**
 * 文件管理服务接口。
 */
public interface IFileService {

    /**
     * 上传项目文件。
     *
     * @param projectId 项目编号
     * @param file 上传文件
     * @return 文件上传结果
     */
    FileUploadResult upload(Long projectId, FileUploadPayload file);

    /**
     * 下载文件。
     *
     * @param fileId 文件编号
     * @return 文件下载载荷
     */
    FileDownloadPayload download(Long fileId);
}
