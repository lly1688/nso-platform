package com.nso.web.controller.business;

import com.nso.common.core.domain.AjaxResult;
import com.nso.business.core.NsoDtos.*;
import com.nso.business.support.IPlatformSupportService;
import com.nso.business.file.IFileService;
import com.nso.framework.security.NsoAuthenticationService;
import com.nso.framework.security.NsoPrincipal;
import com.nso.system.profile.IUserProfileService;

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
public class AdminBusinessController {

    private final NsoAuthenticationService authenticationService;
    private final IFileService fileService;
    private final IPlatformSupportService supportService;
    private final IUserProfileService profiles;

    public AdminBusinessController(NsoAuthenticationService authenticationService,
                                   IFileService fileService, IPlatformSupportService supportService,
                                   IUserProfileService profiles) {
        this.authenticationService = authenticationService;
        this.fileService = fileService;
        this.supportService = supportService;
        this.profiles = profiles;
    }

    // ── Auth ──

    @PostMapping("/auth/login")
    public AjaxResult<?> login(@RequestBody(required = false) LoginRequest request) {
        if (request == null) {
            return AjaxResult.error("请求体不能为空");
        }
        return AjaxResult.success(authenticationService.passwordLogin(request.username(), request.password(), "admin"));
    }

    @PostMapping("/auth/refresh")
    public AjaxResult<?> refresh(@RequestBody RefreshRequest request) {
        return AjaxResult.success(authenticationService.refresh(request.refreshToken(), "admin"));
    }

    @PostMapping("/auth/logout")
    public AjaxResult<Void> logout(@RequestBody(required = false) RefreshRequest request,
                                   @org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String authorization) {
        authenticationService.logout(bearer(authorization), request == null ? null : request.refreshToken());
        return AjaxResult.success();
    }

    @GetMapping("/auth/me")
    public AjaxResult<?> me(@AuthenticationPrincipal NsoPrincipal principal) {
        return AjaxResult.success(authenticationService.current(principal));
    }

    @GetMapping("/profile")
    public AjaxResult<?> profile(@AuthenticationPrincipal NsoPrincipal principal) {
        return AjaxResult.success(profiles.current(principal.userId(), principal.tenantId()));
    }

    @PutMapping("/profile")
    public AjaxResult<?> updateProfile(@AuthenticationPrincipal NsoPrincipal principal,
                                       @RequestBody IUserProfileService.UpdateProfileRequest request) {
        return AjaxResult.success(profiles.update(principal.userId(), principal.tenantId(), request));
    }

    @PutMapping("/profile/password")
    public AjaxResult<Void> changePassword(@AuthenticationPrincipal NsoPrincipal principal,
                                           @RequestBody ChangePasswordRequest request) {
        authenticationService.changePassword(principal, request == null ? null : request.currentPassword(),
                request == null ? null : request.newPassword());
        return AjaxResult.success();
    }

    @PostMapping(value = "/profile/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AjaxResult<?> uploadAvatar(@AuthenticationPrincipal NsoPrincipal principal,
                                      @RequestParam("file") MultipartFile file) {
        return AjaxResult.success(profiles.uploadAvatar(principal.userId(), principal.tenantId(), file));
    }

    @GetMapping("/profile/avatar")
    public ResponseEntity<Resource> avatar(@AuthenticationPrincipal NsoPrincipal principal) {
        IUserProfileService.AvatarContent content = profiles.avatar(principal.userId(), principal.tenantId());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(content.contentType())).body(content.resource());
    }

    // ── File upload / download ──

    @PostMapping(value = "/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('document:upload')")
    public AjaxResult<?> upload(@RequestParam Long projectId, @RequestParam("file") MultipartFile file) {
        return AjaxResult.success(fileService.upload(projectId, file));
    }

    @GetMapping("/files/{id}/download")
    @PreAuthorize("hasAuthority('document:view')")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Resource resource = fileService.download(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    // ── Saved views ──

    @GetMapping("/views")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public AjaxResult<?> savedViews(@RequestParam(required = false) String targetType) {
        return AjaxResult.success(supportService.listViews(targetType));
    }

    @PostMapping("/views")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public AjaxResult<?> saveView(@RequestBody SavedViewRequest request) {
        return AjaxResult.success(supportService.saveView(request));
    }

    // ── Export ──

    @GetMapping("/exports")
    @PreAuthorize("hasAuthority('report:view')")
    public AjaxResult<?> exports() {
        return AjaxResult.success(supportService.listExports());
    }

    @PostMapping("/exports")
    @PreAuthorize("hasAuthority('report:view')")
    public AjaxResult<?> createExport(@RequestBody ExportRequest request) {
        return AjaxResult.success(supportService.createExport(request));
    }

    @GetMapping("/exports/{id}/download")
    @PreAuthorize("hasAuthority('report:view')")
    public ResponseEntity<Resource> downloadExport(@PathVariable Long id) {
        Resource resource = supportService.downloadExport(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    // ── Import ──

    @GetMapping("/imports/templates/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public AjaxResult<?> importTemplate(@PathVariable String type) {
        return AjaxResult.success(supportService.importTemplate(type));
    }

    @GetMapping("/imports")
    @PreAuthorize("hasRole('ADMIN')")
    public AjaxResult<?> imports(@RequestParam(required = false) String importType) {
        return AjaxResult.success(supportService.listImports(importType));
    }

    @PostMapping("/imports")
    @PreAuthorize("hasRole('ADMIN')")
    public AjaxResult<?> importRows(@RequestBody ImportRequest request) {
        return AjaxResult.success(supportService.importRows(request));
    }

    // ── Rules ──

    @GetMapping("/rules")
    @PreAuthorize("hasRole('ADMIN')")
    public AjaxResult<?> rules(@RequestParam(required = false) String ruleCode) {
        return AjaxResult.success(supportService.listRules(ruleCode));
    }

    @PostMapping("/rules")
    @PreAuthorize("hasRole('ADMIN')")
    public AjaxResult<?> saveRule(@RequestBody RuleParamRequest request) {
        return AjaxResult.success(supportService.saveRule(request));
    }

    @PostMapping("/rules/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public AjaxResult<?> publishRule(@PathVariable Long id) {
        return AjaxResult.success(supportService.publishRule(id));
    }

    // ── Audit ──

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public AjaxResult<?> auditLogs(@RequestParam(required = false) String businessType) {
        return AjaxResult.success(supportService.auditLogs(businessType));
    }

    private String bearer(String authorization) {
        return authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7) : null;
    }

    public record ChangePasswordRequest(String currentPassword, String newPassword) { }
}
