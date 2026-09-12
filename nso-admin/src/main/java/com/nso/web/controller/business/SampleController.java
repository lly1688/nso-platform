package com.nso.web.controller.business;

import com.nso.shared.core.domain.AjaxResult;
import com.nso.business.core.NsoDtos.*;
import com.nso.business.file.service.IFileService;
import com.nso.web.controller.common.WebFilePayloads;
import com.nso.business.sample.service.ISampleService;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin")

// 打样管理接口 负责打样单的创建、确认和检查
public class SampleController {

    // 样品服务
    private final ISampleService sampleService;
    // 文件服务
    private final IFileService fileService;

    public SampleController(ISampleService sampleService, IFileService fileService) {
        this.sampleService = sampleService;
        this.fileService = fileService;
    }

    // 按条件查询项目样品。
    @GetMapping("/samples")
    @PreAuthorize("hasAnyAuthority('nso:sample:view', 'sample:view')")
    public AjaxResult<?> samples(@RequestParam(required = false) Long projectId,
                                 @RequestParam(required = false) String status,
                                 @RequestParam(required = false) Integer pageNo,
                                 @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(sampleService.list(projectId, status, new PageQuery(pageNo, pageSize)));
    }

    // 创建样品。
    @PostMapping("/samples")
    @PreAuthorize("hasAnyAuthority('nso:sample:create', 'sample:create')")
    public AjaxResult<?> createSample(@RequestBody SampleRequest request) {
        return AjaxResult.success(sampleService.create(request));
    }

    // 查询样品详情。
    @GetMapping("/samples/{id}")
    @PreAuthorize("hasAnyAuthority('nso:sample:view', 'sample:view')")
    public AjaxResult<?> sample(@PathVariable Long id) {

        return AjaxResult.success(sampleService.get(id));
    }

    // 提交样品确认。
    @PostMapping("/samples/{id}/submit-confirm")
    @PreAuthorize("hasAnyAuthority('nso:sample:submit', 'sample:submit')")
    public AjaxResult<?> submitSampleConfirm(@PathVariable Long id) {
        return AjaxResult.success(sampleService.submitConfirm(id));
    }

    // 提交内部样品确认结论。
    @PostMapping("/samples/{id}/confirm")
    @PreAuthorize("hasAnyAuthority('nso:sample:proxy-confirm', 'sample:proxy-confirm')")
    public AjaxResult<?> confirmSample(@PathVariable Long id, @RequestBody SampleConfirmRequest request) {
        return AjaxResult.success(sampleService.confirm(id, request));
    }

    // 上传样品确认凭证。
    @PostMapping("/samples/{id}/confirmation-evidence")
    @PreAuthorize("hasAnyAuthority('nso:sample:proxy-confirm', 'sample:proxy-confirm')")
    public AjaxResult<?> uploadConfirmationEvidence(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        SampleDto sample = sampleService.get(id);
        return AjaxResult.success(fileService.upload(sample.projectId(), WebFilePayloads.upload(file)));
    }

    // 查询样品检验记录。
    @GetMapping("/samples/{id}/checks")
    @PreAuthorize("hasAnyAuthority('nso:sample:view', 'sample:view')")
    public AjaxResult<?> sampleChecks(@PathVariable Long id, @RequestParam(required = false) Integer pageNo,
                                      @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(sampleService.checks(id, new PageQuery(pageNo, pageSize)));
    }

    // 新增样品检验记录。
    @PostMapping("/samples/{id}/checks")
    @PreAuthorize("hasAnyAuthority('nso:sample:inspect', 'sample:inspect')")
    public AjaxResult<?> addSampleCheck(@PathVariable Long id, @RequestBody SampleCheckRequest request) {
        return AjaxResult.success(sampleService.addCheck(id, request));
    }

    // 创建客户确认令牌。
    @PostMapping("/samples/{id}/confirm-token")
    @PreAuthorize("hasAnyAuthority('nso:sample:submit', 'sample:submit')")
    public AjaxResult<?> createSampleConfirmToken(@PathVariable Long id, @RequestBody ConfirmationTokenRequest request) {
        if (request == null || request.contactId() == null) {
            throw new IllegalArgumentException("生成客户确认链接必须选择已授权联系人");
        }
        return AjaxResult.success(sampleService.createConfirmToken(id, request.contactId(), request.validDays() == null ? 7 : request.validDays(), request.maxUseCount() == null ? 1 : request.maxUseCount()));
    }

    // 撤销样品确认令牌。
    @PostMapping("/samples/{id}/confirm-token/revoke")
    @PreAuthorize("hasAnyAuthority('nso:sample:submit', 'sample:submit')")
    public AjaxResult<?> revokeSampleConfirmToken(@PathVariable Long id) {
        sampleService.revokeConfirmTokens(id);
        return AjaxResult.success();
    }

    // 申请样品特殊放行。
    @PostMapping("/samples/{id}/special-releases")
    @PreAuthorize("hasAnyAuthority('nso:sample:release:apply', 'sample:release:apply')")
    public AjaxResult<?> applySpecialRelease(@PathVariable Long id, @RequestBody SpecialReleaseRequest request) {
        return AjaxResult.success(sampleService.applySpecialRelease(id, request));
    }

    // 审批样品特殊放行。
    @PostMapping("/special-releases/{id}/approve")
    @PreAuthorize("hasAnyAuthority('nso:sample:release:approve', 'sample:release:approve')")
    public AjaxResult<?> approveSpecialRelease(@PathVariable Long id) {
        return AjaxResult.success(sampleService.approveSpecialRelease(id));
    }
}
