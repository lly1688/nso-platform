package com.nso.business.file;
import com.nso.business.core.NsoDtos.FileUploadResult;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
public interface IFileService { FileUploadResult upload(Long projectId, MultipartFile file); Resource download(Long fileId); }
