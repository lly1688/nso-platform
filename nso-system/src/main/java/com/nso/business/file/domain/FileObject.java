package com.nso.business.file.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

// 文件对象实体。
@Data
@TableName("nso_file_object")
public class FileObject {

    // 文件编号
    @TableId
    private Long id;

    // 租户编号
    private Long tenantId;

    // 项目编号
    private Long projectId;

    // 创建人编号
    private Long createdBy;

    // 文件名称
    private String fileName;

    // 内容类型
    private String contentType;

    // 文件字节数
    private Long fileSize;

    // 上传时基于实际文件字节计算并落库，下载时作为完整性校验的预期摘要。
    // 文件摘要
    private String sha256;

    // 存储路径
    private String storagePath;
}
