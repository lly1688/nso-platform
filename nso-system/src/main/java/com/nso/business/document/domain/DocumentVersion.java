package com.nso.business.document.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDate;

// 技术文档版本实体。
@Data
@TableName("nso_document_version")
public class DocumentVersion {

    // 文档版本编号
    @TableId
    private Long id;

    // 租户编号
    private Long tenantId;

    // 项目编号
    private Long projectId;

    // 文档编号
    private Long documentId;

    // 文件对象编号
    private Long fileObjectId;

    // 文件名称
    private String fileName;

    // 文件类型
    private String fileType;

    // 版本号
    private String versionNo;

    // 版本状态
    private String status;

    // 生效日期
    private LocalDate effectiveDate;

    // 变更摘要
    private String changeSummary;

    // 是否为当前版本
    private Integer currentVersion;

    // 文件摘要
    private String sha256;

    // 存储路径
    private String storagePath;

    // 发布人名称
    private String publishBy;

    // 上传人编号
    private Long uploadedBy;

    // 发布人编号
    private Long publishedBy;

    // 数据版本
    @Version
    private Integer version;

    // 删除标志
    @TableLogic
    private Integer deleted;
}
