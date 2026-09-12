package com.nso.system.profile;

import com.nso.business.file.service.FileDownloadPayload;
import com.nso.business.file.service.FileUploadPayload;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 当前用户资料服务接口。
 */
public interface IUserProfileService {

    /**
     * 查询当前用户资料。
     *
     * @param userId 用户编号
     * @param tenantId 租户编号
     * @return 用户资料
     */
    ProfileView current(Long userId, Long tenantId);

    /**
     * 更新当前用户资料。
     *
     * @param userId 用户编号
     * @param tenantId 租户编号
     * @param request 资料内容
     * @return 更新后的用户资料
     */
    ProfileView update(Long userId, Long tenantId, UpdateProfileRequest request);

    /**
     * 上传当前用户头像。
     *
     * @param userId 用户编号
     * @param tenantId 租户编号
     * @param file 头像文件
     * @return 更新后的用户资料
     */
    ProfileView uploadAvatar(Long userId, Long tenantId, FileUploadPayload file);

    /**
     * 下载当前用户头像。
     *
     * @param userId 用户编号
     * @param tenantId 租户编号
     * @return 头像内容
     */
    AvatarContent avatar(Long userId, Long tenantId);

    /**
     * 用户资料更新内容。
     *
     * @param nickname 用户昵称
     * @param phone 手机号码
     * @param email 邮箱地址
     * @param gender 用户性别
     * @param version 数据版本号
     */
    record UpdateProfileRequest(String nickname, String phone, String email, String gender, Integer version) {
    }

    /**
     * 用户资料视图。
     *
     * @param id 用户编号
     * @param username 用户名
     * @param nickname 用户昵称
     * @param phone 手机号码
     * @param email 邮箱地址
     * @param gender 用户性别
     * @param departmentName 部门名称
     * @param roles 角色标识列表
     * @param avatarUrl 头像地址
     * @param version 数据版本号
     * @param createdAt 创建时间
     */
    record ProfileView(Long id, String username, String nickname, String phone, String email, String gender,
                       String departmentName, List<String> roles, String avatarUrl, Integer version,
                       LocalDateTime createdAt) {
    }

    /**
     * 头像内容。
     *
     * @param content 文件下载载荷
     */
    record AvatarContent(FileDownloadPayload content) {
    }
}
