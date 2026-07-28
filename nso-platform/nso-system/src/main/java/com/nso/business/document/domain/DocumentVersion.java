package com.nso.business.document.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;
import java.time.LocalDate;

@Data @TableName("nso_document_version")
public class DocumentVersion {
    @TableId private Long id; private Long tenantId; private Long projectId; private Long documentId; private Long fileObjectId;
    private String fileName; private String fileType; private String versionNo; private String status;
    private LocalDate effectiveDate; private String changeSummary; private Integer currentVersion;
    private String sha256; private String storagePath; private String publishBy;
    private Long uploadedBy; private Long publishedBy;
    @Version private Integer version; @TableLogic private Integer deleted;
}
