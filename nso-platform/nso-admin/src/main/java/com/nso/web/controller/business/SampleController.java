package com.nso.web.controller.business;

import com.nso.common.core.domain.AjaxResult;
import com.nso.business.core.NsoDtos.*;
import com.nso.business.sample.service.ISampleService;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class SampleController {

    private final ISampleService sampleService;

    public SampleController(ISampleService sampleService) {
        this.sampleService = sampleService;
    }

    @GetMapping("/samples")
    @PreAuthorize("hasAuthority('sample:view')")
    public AjaxResult<?> samples(@RequestParam(required = false) Long projectId) {
        return AjaxResult.success(sampleService.list(projectId));
    }

    @PostMapping("/samples")
    @PreAuthorize("hasAuthority('sample:create')")
    public AjaxResult<?> createSample(@RequestBody SampleRequest request) {
        return AjaxResult.success(sampleService.create(request));
    }

    @GetMapping("/samples/{id}")
    @PreAuthorize("hasAuthority('sample:view')")
    public AjaxResult<?> sample(@PathVariable Long id) {
        return AjaxResult.success(sampleService.get(id));
    }

    @PostMapping("/samples/{id}/submit-confirm")
    @PreAuthorize("hasAuthority('sample:submit')")
    public AjaxResult<?> submitSampleConfirm(@PathVariable Long id) {
        return AjaxResult.success(sampleService.submitConfirm(id));
    }

    @PostMapping("/samples/{id}/confirm")
    @PreAuthorize("hasAuthority('sample:proxy-confirm')")
    public AjaxResult<?> confirmSample(@PathVariable Long id, @RequestBody SampleConfirmRequest request) {
        return AjaxResult.success(sampleService.confirm(id, request));
    }

    @GetMapping("/samples/{id}/checks")
    @PreAuthorize("hasAuthority('sample:view')")
    public AjaxResult<?> sampleChecks(@PathVariable Long id) {
        return AjaxResult.success(sampleService.checks(id));
    }

    @PostMapping("/samples/{id}/checks")
    @PreAuthorize("hasAuthority('sample:inspect')")
    public AjaxResult<?> addSampleCheck(@PathVariable Long id, @RequestBody SampleCheckRequest request) {
        return AjaxResult.success(sampleService.addCheck(id, request));
    }

    @PostMapping("/samples/{id}/confirm-token")
    @PreAuthorize("hasAuthority('sample:submit')")
    public AjaxResult<?> createSampleConfirmToken(@PathVariable Long id) {
        return AjaxResult.success(sampleService.createConfirmToken(id, 7, 1));
    }

    @PostMapping("/samples/{id}/special-releases")
    @PreAuthorize("hasAuthority('sample:release:apply')")
    public AjaxResult<?> applySpecialRelease(@PathVariable Long id, @RequestBody SpecialReleaseRequest request) {
        return AjaxResult.success(sampleService.applySpecialRelease(id, request));
    }

    @PostMapping("/special-releases/{id}/approve")
    @PreAuthorize("hasAuthority('sample:release:approve')")
    public AjaxResult<?> approveSpecialRelease(@PathVariable Long id) {
        return AjaxResult.success(sampleService.approveSpecialRelease(id));
    }
}
