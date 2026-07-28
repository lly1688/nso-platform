package com.nso.business.file;
import com.nso.business.core.NsoDtos.FileUploadResult;
import com.nso.business.file.domain.FileObject;
import com.nso.business.file.mapper.FileObjectMapper;
import com.nso.business.core.TenantContext;
import com.nso.business.project.service.ProjectDataScope;
import com.nso.common.exception.BusinessException;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service public class FileServiceImpl implements IFileService {
 private static final long MAX_FILE_SIZE = 100L * 1024 * 1024;
 private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg", "webp", "xlsx", "xls", "doc", "docx", "dwg", "dxf", "step", "stp", "zip");
 private static final Map<String, Set<String>> ALLOWED_CONTENT_TYPES = Map.ofEntries(
  Map.entry("pdf", Set.of("application/pdf")),
  Map.entry("png", Set.of("image/png")),
  Map.entry("jpg", Set.of("image/jpeg")), Map.entry("jpeg", Set.of("image/jpeg")), Map.entry("webp", Set.of("image/webp")),
  Map.entry("xlsx", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
  Map.entry("xls", Set.of("application/vnd.ms-excel")), Map.entry("doc", Set.of("application/msword")),
  Map.entry("docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
  Map.entry("dwg", Set.of("application/acad", "image/vnd.dwg", "application/octet-stream")),
  Map.entry("dxf", Set.of("image/vnd.dxf", "application/dxf", "application/octet-stream")),
  Map.entry("step", Set.of("model/step", "application/step", "application/octet-stream")),
  Map.entry("stp", Set.of("model/step", "application/step", "application/octet-stream")),
  Map.entry("zip", Set.of("application/zip", "application/x-zip-compressed", "application/octet-stream")));
 private final FileObjectMapper mapper; private final ObjectStoragePort storage; private final ProjectDataScope dataScope;
 public FileServiceImpl(FileObjectMapper mapper,ObjectStoragePort storage,ProjectDataScope dataScope){this.mapper=mapper;this.storage=storage;this.dataScope=dataScope;}
 @Override @Transactional public FileUploadResult upload(Long projectId, MultipartFile file){if(projectId==null)throw new BusinessException("上传文件必须指定项目");dataScope.requireAccess(projectId);if(file==null||file.isEmpty())throw new BusinessException("上传文件不能为空");if(file.getSize()>MAX_FILE_SIZE)throw new BusinessException("单个附件不得超过 100MB");try(InputStream input=file.getInputStream()){String safeName=file.getOriginalFilename()==null?"upload.bin":file.getOriginalFilename().replaceAll("[\\\\/:*?\"<>|]","_");int dot=safeName.lastIndexOf('.');String extension=dot<0?"":safeName.substring(dot+1).toLowerCase();if(!ALLOWED_EXTENSIONS.contains(extension))throw new BusinessException("不支持的文件类型");String contentType=normalizeContentType(file.getContentType());if(!ALLOWED_CONTENT_TYPES.get(extension).contains(contentType))throw new BusinessException("文件扩展名与 MIME 类型不匹配");ObjectStoragePort.StoredObject stored=storage.put(TenantContext.tenantId()+"/"+projectId+"/"+UUID.randomUUID()+"/"+safeName,contentType,file.getSize(),input);FileObject row=new FileObject();row.setTenantId(TenantContext.tenantId());row.setProjectId(projectId);row.setCreatedBy(TenantContext.userId());row.setFileName(safeName);row.setContentType(contentType);row.setFileSize(file.getSize());row.setSha256(stored.sha256());row.setStoragePath(stored.objectKey());mapper.insert(row);return new FileUploadResult(row.getId(),row.getFileName(),row.getContentType(),row.getFileSize(),row.getSha256(),"/api/v1/admin/files/"+row.getId()+"/download");}catch(BusinessException ex){throw ex;}catch(Exception ex){throw new BusinessException("文件上传失败: "+ex.getMessage());}}
 private String normalizeContentType(String value){if(value==null||value.isBlank())return "";int separator=value.indexOf(';');return (separator<0?value:value.substring(0,separator)).trim().toLowerCase();}
 @Override public Resource download(Long fileId){FileObject row=mapper.selectById(fileId);if(row==null||row.getTenantId()==null||row.getTenantId()!=TenantContext.tenantId())throw new BusinessException("文件不存在");if(row.getProjectId()==null)throw BusinessException.accessDenied("FILE_PROJECT_SCOPE","未绑定项目的历史文件不可直接下载",String.valueOf(fileId),"已绑定项目","通过受控技术版本重新上传");dataScope.requireAccess(row.getProjectId());try{return new InputStreamResource(storage.get(row.getStoragePath()));}catch(Exception ex){throw new BusinessException("文件读取失败: "+ex.getMessage());}}
}
