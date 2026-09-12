package com.nso.web.controller.business;

import com.nso.shared.core.domain.AjaxResult;
import com.nso.business.core.NsoDtos.*;
import com.nso.business.support.IPlatformSupportService;
import com.nso.business.file.service.IFileService;
import com.nso.framework.security.NsoAuthenticationService;
import com.nso.framework.security.NsoPrincipal;
import com.nso.system.profile.IUserProfileService;
import com.nso.web.controller.common.WebFilePayloads;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin")

// 管理后台业务接口 负责管理后台的综合业务操作
public class AdminBusinessController {

    // NSO认证服务
    private final NsoAuthenticationService authenticationService;
    // 文件服务
    private final IFileService fileService;
    // 平台支持服务
    private final IPlatformSupportService supportService;
    // 用户资料服务
    private final IUserProfileService profiles;

    public AdminBusinessController(
            NsoAuthenticationService authenticationService,
            IFileService fileService,
            IPlatformSupportService supportService,
            IUserProfileService profiles
    ) {
        this.authenticationService = authenticationService;
        this.fileService = fileService;
        this.supportService = supportService;
        this.profiles = profiles;
    }

    // 执行账号密码登录。
    @PostMapping("/auth/login")
    public AjaxResult<?> login(@RequestBody(required = false) LoginRequest request) {
        if (request == null) {
            return AjaxResult.error("请求体不能为空");
        }
        return AjaxResult.success(authenticationService.passwordLogin(request.username(), request.password(), "admin"));
    }

    // 刷新访问令牌。
    @PostMapping("/auth/refresh")
    public AjaxResult<?> refresh(@RequestBody RefreshRequest request) {
        return AjaxResult.success(authenticationService.refresh(request.refreshToken(), "admin"));
    }

    // 注销当前登录会话。
    @PostMapping("/auth/logout")
    public AjaxResult<Void> logout(@RequestBody(required = false) RefreshRequest request,
                                    @org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String authorization) {
        authenticationService.logout(bearer(authorization), request == null ? null : request.refreshToken());
        return AjaxResult.success();
    }

    // 查询当前登录用户。
    @GetMapping("/auth/me")
    public AjaxResult<?> me(@AuthenticationPrincipal NsoPrincipal principal) {
        return AjaxResult.success(authenticationService.current(principal));
    }

    // 查询当前用户资料。
    @GetMapping("/profile")
    public AjaxResult<?> profile(@AuthenticationPrincipal NsoPrincipal principal) {
        return AjaxResult.success(profiles.current(principal.userId(), principal.tenantId()));
    }

    // 更新当前用户资料。
    @PutMapping("/profile")
    public AjaxResult<?> updateProfile(@AuthenticationPrincipal NsoPrincipal principal,
                                        @RequestBody IUserProfileService.UpdateProfileRequest request) {
        return AjaxResult.success(profiles.update(principal.userId(), principal.tenantId(), request));
    }

    // 修改当前用户密码。
    @PutMapping("/profile/password")
    public AjaxResult<Void> changePassword(@AuthenticationPrincipal NsoPrincipal principal,
                                            @RequestBody ChangePasswordRequest request) {
        authenticationService.changePassword(principal, request == null ? null : request.currentPassword(),
                request == null ? null : request.newPassword());
        return AjaxResult.success();
    }

    // 上传当前用户头像。
    @PostMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AjaxResult<?> uploadAvatar(@AuthenticationPrincipal NsoPrincipal principal,
                                        @RequestParam("file") MultipartFile file) {
        return AjaxResult.success(profiles.uploadAvatar(principal.userId(), principal.tenantId(), WebFilePayloads.upload(file)));
    }

    // 下载当前用户头像。
    @GetMapping("/profile/avatar")
    public ResponseEntity<Resource> avatar(@AuthenticationPrincipal NsoPrincipal principal) {
        IUserProfileService.AvatarContent content = profiles.avatar(principal.userId(), principal.tenantId());
        return WebFilePayloads.inline(content.content());
    }


    // 上传项目文件。
    @PostMapping(value = "/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('nso:document:upload', 'document:upload')")
    public AjaxResult<?> upload(@RequestParam Long projectId, @RequestParam("file") MultipartFile file) {
        return AjaxResult.success(fileService.upload(projectId, WebFilePayloads.upload(file)));
    }

    // 下载项目文件。
    @GetMapping("/files/{id}/download")
    @PreAuthorize("hasAnyAuthority('nso:document:view', 'document:view', 'nso:sample:view', 'sample:view', 'nso:sample:proxy-confirm', 'sample:proxy-confirm', 'nso:exception:view', 'exception:view')")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        return WebFilePayloads.attachment(fileService.download(id));
    }

    @GetMapping("/files/{id}/inline")
    @PreAuthorize("hasAnyAuthority('nso:document:view', 'document:view', 'nso:sample:view', 'sample:view', 'nso:sample:proxy-confirm', 'sample:proxy-confirm', 'nso:exception:view', 'exception:view')")
    public ResponseEntity<Resource> inline(@PathVariable Long id) {
        return WebFilePayloads.inline(fileService.download(id));
    }


    // 查询当前用户保存的视图。
    @GetMapping("/views")
    @PreAuthorize("hasAnyAuthority('nso:dashboard:view', 'dashboard:view')")
    public AjaxResult<?> savedViews(@RequestParam(required = false) String targetType,
                                    @RequestParam(required = false) Integer pageNo,
                                    @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(supportService.listViews(targetType, new PageQuery(pageNo, pageSize)));
    }

    // 保存用户视图。
    @PostMapping("/views")
    @PreAuthorize("hasAnyAuthority('nso:dashboard:view', 'dashboard:view')")
    public AjaxResult<?> saveView(@RequestBody SavedViewRequest request) {
        return AjaxResult.success(supportService.saveView(request));
    }


    // 导出任务
    @GetMapping("/exports")
    @PreAuthorize("hasAnyAuthority('nso:report:view', 'report:view')")
    public AjaxResult<?> exports(@RequestParam(required = false) Integer pageNo,
                                 @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(supportService.listExports(new PageQuery(pageNo, pageSize)));
    }

    // 创建导出任务。
    @PostMapping("/exports")
    @PreAuthorize("hasAnyAuthority('nso:report:view', 'report:view')")
    public AjaxResult<?> createExport(@RequestBody ExportRequest request) {
        return AjaxResult.success(supportService.createExport(request));
    }

    // 下载导出文件。
    @GetMapping("/exports/{id}/download")
    @PreAuthorize("hasAnyAuthority('nso:report:view', 'report:view')")
    public ResponseEntity<Resource> downloadExport(@PathVariable Long id) {
        Resource resource = supportService.downloadExport(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }


    // 获取导入模板。
    @GetMapping("/imports/templates/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public AjaxResult<?> importTemplate(@PathVariable String type) {
        return AjaxResult.success(supportService.importTemplate(type));
    }

    // 查询导入任务。
    @GetMapping("/imports")
    @PreAuthorize("hasRole('ADMIN')")
    public AjaxResult<?> imports(@RequestParam(required = false) String importType,
                                 @RequestParam(required = false) Integer pageNo,
                                 @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(supportService.listImports(importType, new PageQuery(pageNo, pageSize)));
    }

    // 导入结构化数据行。
    @PostMapping("/imports")
    @PreAuthorize("hasRole('ADMIN')")
    public AjaxResult<?> importRows(@RequestBody ImportRequest request) {
        return AjaxResult.success(supportService.importRows(request));
    }

    // 查询规则参数。
    @GetMapping("/rules")
    @PreAuthorize("hasRole('ADMIN')")
    public AjaxResult<?> rules(@RequestParam(required = false) String ruleCode,
                               @RequestParam(required = false) Integer pageNo,
                               @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(supportService.listRules(ruleCode, new PageQuery(pageNo, pageSize)));
    }

    // 保存规则参数。
    @PostMapping("/rules")
    @PreAuthorize("hasRole('ADMIN')")
    public AjaxResult<?> saveRule(@RequestBody RuleParamRequest request) {
        return AjaxResult.success(supportService.saveRule(request));
    }

    // 发布规则参数。
    @PostMapping("/rules/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public AjaxResult<?> publishRule(@PathVariable Long id) {
        return AjaxResult.success(supportService.publishRule(id));
    }


    // 查询业务审计日志。
    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public AjaxResult<?> auditLogs(@RequestParam(required = false) String businessType,
                                   @RequestParam(required = false) Integer pageNo,
                                   @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(supportService.auditLogs(businessType, new PageQuery(pageNo, pageSize)));
    }

    private String bearer(String authorization) {
        return authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7) : null;
    }

    public record ChangePasswordRequest(
        // 当前密码
        String currentPassword,
        // 新密码
        String newPassword
    ) {
    }
}
