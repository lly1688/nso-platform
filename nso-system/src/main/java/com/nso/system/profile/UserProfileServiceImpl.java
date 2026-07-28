package com.nso.system.profile;

import com.nso.business.file.ObjectStoragePort;
import com.nso.business.file.domain.FileObject;
import com.nso.business.file.mapper.FileObjectMapper;
import com.nso.common.exception.BusinessException;
import com.nso.system.domain.SysDept;
import com.nso.system.domain.SysUser;
import com.nso.system.mapper.SysDeptMapper;
import com.nso.system.mapper.SysUserMapper;
import com.nso.system.service.ISysUserService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class UserProfileServiceImpl implements IUserProfileService {
    private static final long MAX_AVATAR_SIZE = 2L * 1024 * 1024;
    private static final Set<String> AVATAR_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp");
    private static final Map<String, Set<String>> AVATAR_CONTENT_TYPES = Map.of(
            "png", Set.of("image/png"),
            "jpg", Set.of("image/jpeg"),
            "jpeg", Set.of("image/jpeg"),
            "webp", Set.of("image/webp"));
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+() -]{0,32}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Set<String> GENDERS = Set.of("MALE", "FEMALE", "UNSPECIFIED");

    private final ISysUserService users;
    private final SysUserMapper userMapper;
    private final SysDeptMapper departments;
    private final FileObjectMapper files;
    private final ObjectStoragePort storage;

    public UserProfileServiceImpl(ISysUserService users, SysUserMapper userMapper, SysDeptMapper departments,
                                  FileObjectMapper files, ObjectStoragePort storage) {
        this.users = users;
        this.userMapper = userMapper;
        this.departments = departments;
        this.files = files;
        this.storage = storage;
    }

    @Override
    public ProfileView current(Long userId, Long tenantId) {
        return toView(requireUser(userId, tenantId));
    }

    @Override
    @Transactional
    public ProfileView update(Long userId, Long tenantId, UpdateProfileRequest request) {
        if (request == null || request.version() == null) {
            throw new BusinessException("个人资料版本不能为空");
        }
        String nickname = requiredText(request.nickname(), "用户昵称", 64);
        String phone = optionalText(request.phone(), 32);
        String email = optionalText(request.email(), 128);
        String gender = request.gender() == null || request.gender().isBlank()
                ? "UNSPECIFIED" : request.gender().trim().toUpperCase(Locale.ROOT);
        if (!phone.isEmpty() && !PHONE_PATTERN.matcher(phone).matches()) {
            throw new BusinessException("手机号格式不正确");
        }
        if (!email.isEmpty() && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessException("邮箱格式不正确");
        }
        if (!GENDERS.contains(gender)) {
            throw new BusinessException("性别字段不合法");
        }
        int updated = userMapper.updateProfile(userId, tenantId, nickname, phone, email, gender, request.version());
        if (updated == 0) {
            requireUser(userId, tenantId);
            throw new BusinessException("个人资料已被更新，请刷新后重试");
        }
        return current(userId, tenantId);
    }

    @Override
    @Transactional
    public ProfileView uploadAvatar(Long userId, Long tenantId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("头像文件不能为空");
        }
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new BusinessException("头像文件不能超过 2MB");
        }
        String name = safeName(file.getOriginalFilename());
        String extension = extension(name);
        String contentType = normalizeContentType(file.getContentType());
        if (!AVATAR_EXTENSIONS.contains(extension) || !AVATAR_CONTENT_TYPES.get(extension).contains(contentType)) {
            throw new BusinessException("头像仅支持 JPEG、PNG 或 WebP 图片");
        }
        try (InputStream input = file.getInputStream()) {
            ObjectStoragePort.StoredObject stored = storage.put(
                    tenantId + "/profiles/" + userId + "/" + UUID.randomUUID() + "/" + name,
                    contentType, file.getSize(), input);
            FileObject row = new FileObject();
            row.setTenantId(tenantId);
            row.setCreatedBy(userId);
            row.setFileName(name);
            row.setContentType(contentType);
            row.setFileSize(file.getSize());
            row.setSha256(stored.sha256());
            row.setStoragePath(stored.objectKey());
            files.insert(row);
            if (userMapper.updateAvatar(userId, tenantId, row.getId()) != 1) {
                throw new BusinessException("头像保存失败");
            }
            return current(userId, tenantId);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException("头像上传失败: " + exception.getMessage());
        }
    }

    @Override
    public AvatarContent avatar(Long userId, Long tenantId) {
        SysUser user = requireUser(userId, tenantId);
        if (user.getAvatarFileId() == null) {
            throw new BusinessException("尚未上传头像");
        }
        FileObject file = files.selectById(user.getAvatarFileId());
        if (file == null || !tenantId.equals(file.getTenantId()) || !userId.equals(file.getCreatedBy()) || file.getProjectId() != null) {
            throw new BusinessException("头像文件不存在");
        }
        try {
            return new AvatarContent(new InputStreamResource(storage.get(file.getStoragePath())), file.getContentType());
        } catch (Exception exception) {
            throw new BusinessException("头像读取失败: " + exception.getMessage());
        }
    }

    private SysUser requireUser(Long userId, Long tenantId) {
        SysUser user = users.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
        if (!tenantId.equals(user.getTenantId())) {
            throw BusinessException.accessDenied("PROFILE_TENANT_SCOPE", "无权访问其他租户的个人资料",
                    String.valueOf(userId), String.valueOf(tenantId), "切换到所属租户后重试");
        }
        return user;
    }

    private ProfileView toView(SysUser user) {
        String departmentName = "未分配部门";
        if (user.getDeptId() != null) {
            SysDept department = departments.selectById(user.getDeptId());
            if (department != null && department.getDeptName() != null) {
                departmentName = department.getDeptName();
            }
        }
        return new ProfileView(user.getId(), user.getUsername(), user.getNickname(), emptyToNull(user.getPhone()),
                emptyToNull(user.getEmail()), user.getGender() == null ? "UNSPECIFIED" : user.getGender(), departmentName,
                users.roleCodes(user.getId()), user.getAvatarFileId() == null ? null : "/api/v1/admin/profile/avatar",
                user.getVersion() == null ? 0 : user.getVersion(), user.getCreatedAt());
    }

    private String requiredText(String value, String field, int maxLength) {
        String result = optionalText(value, maxLength);
        if (result.isEmpty()) {
            throw new BusinessException(field + "不能为空");
        }
        return result;
    }

    private String optionalText(String value, int maxLength) {
        String result = value == null ? "" : value.trim();
        if (result.length() > maxLength) {
            throw new BusinessException("字段长度超出限制");
        }
        return result;
    }

    private String safeName(String original) {
        String value = original == null || original.isBlank() ? "avatar.png" : original.replaceAll("[\\\\/:*?\"<>|]", "_");
        return value.length() > 128 ? value.substring(value.length() - 128) : value;
    }

    private String extension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index < 0 ? "" : fileName.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "";
        }
        int separator = contentType.indexOf(';');
        return (separator < 0 ? contentType : contentType.substring(0, separator)).trim().toLowerCase(Locale.ROOT);
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
