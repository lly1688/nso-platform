package com.nso.business.file.domain;
import com.baomidou.mybatisplus.annotation.TableId; import com.baomidou.mybatisplus.annotation.TableName; import lombok.Data;
@Data @TableName("nso_file_object") public class FileObject { @TableId private Long id; private Long tenantId; private Long projectId; private Long createdBy; private String fileName; private String contentType; private Long fileSize; private String sha256; private String storagePath; }
