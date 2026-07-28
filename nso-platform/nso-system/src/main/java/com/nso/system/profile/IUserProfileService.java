package com.nso.system.profile;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

public interface IUserProfileService {
    ProfileView current(Long userId, Long tenantId);

    ProfileView update(Long userId, Long tenantId, UpdateProfileRequest request);

    ProfileView uploadAvatar(Long userId, Long tenantId, MultipartFile file);

    AvatarContent avatar(Long userId, Long tenantId);

    record UpdateProfileRequest(String nickname, String phone, String email, String gender, Integer version) { }

    record ProfileView(Long id, String username, String nickname, String phone, String email, String gender,
                       String departmentName, List<String> roles, String avatarUrl, Integer version,
                       LocalDateTime createdAt) { }

    record AvatarContent(Resource resource, String contentType) { }
}
