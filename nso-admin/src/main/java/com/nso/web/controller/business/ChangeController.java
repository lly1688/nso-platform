package com.nso.web.controller.business;

import com.nso.common.core.domain.AjaxResult;
import com.nso.business.core.NsoDtos.*;
import com.nso.business.change.service.IChangeService;
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
public class ChangeController {

    private final IChangeService changeService;

    public ChangeController(IChangeService changeService) {
        this.changeService = changeService;
    }

    @GetMapping("/changes")
    @PreAuthorize("hasAuthority('change:view')")
    public AjaxResult<?> changes(@RequestParam(required = false) Long projectId) {
        return AjaxResult.success(changeService.list(projectId));
    }

    @PostMapping("/changes")
    @PreAuthorize("hasAuthority('change:create')")
    public AjaxResult<?> createChange(@RequestBody ChangeRequest request) {
        return AjaxResult.success(changeService.create(request));
    }

    @GetMapping("/changes/{id}")
    @PreAuthorize("hasAuthority('change:view')")
    public AjaxResult<?> change(@PathVariable Long id) {
        return AjaxResult.success(changeService.get(id));
    }

    @PostMapping("/changes/{id}/analyze-impact")
    @PreAuthorize("hasAuthority('change:analyze')")
    public AjaxResult<?> analyzeChange(@PathVariable Long id) {
        return AjaxResult.success(changeService.analyze(id));
    }

    @GetMapping("/changes/{id}/impacts")
    @PreAuthorize("hasAuthority('change:view')")
    public AjaxResult<?> changeImpacts(@PathVariable Long id) {
        return AjaxResult.success(changeService.impacts(id));
    }

    @PostMapping("/changes/{id}/approve")
    @PreAuthorize("hasAuthority('change:approve')")
    public AjaxResult<?> approveChange(@PathVariable Long id, @RequestBody(required = false) ChangeApprovalRequest request) {
        return AjaxResult.success(changeService.approve(id, request));
    }

    @PostMapping("/changes/{id}/feedback")
    @PreAuthorize("hasAuthority('change:feedback')")
    public AjaxResult<?> feedbackChange(@PathVariable Long id, @RequestBody ChangeFeedbackRequest request) {
        return AjaxResult.success(changeService.feedbackChange(id, request));
    }

    @PostMapping("/change-impacts/{id}/feedback")
    @PreAuthorize("hasAuthority('change:feedback')")
    public AjaxResult<?> feedbackImpact(@PathVariable Long id, @RequestBody ChangeFeedbackRequest request) {
        return AjaxResult.success(changeService.feedbackImpact(id, request));
    }

    @PostMapping("/changes/{id}/close")
    @PreAuthorize("hasAuthority('change:close')")
    public AjaxResult<?> closeChange(@PathVariable Long id) {
        return AjaxResult.success(changeService.close(id));
    }
}
