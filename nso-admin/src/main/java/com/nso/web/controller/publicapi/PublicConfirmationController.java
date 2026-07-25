package com.nso.web.controller.publicapi;

import com.nso.common.core.domain.AjaxResult;
import com.nso.web.controller.MvpDtos.SampleConfirmRequest;
import com.nso.web.controller.MvpDataService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/sample-confirmations")
public class PublicConfirmationController {

    private final MvpDataService dataService;

    public PublicConfirmationController(MvpDataService dataService) {
        this.dataService = dataService;
    }

    @GetMapping("/{sampleId}")
    public AjaxResult<?> confirmation(@PathVariable Long sampleId) {
        return AjaxResult.success(dataService.publicConfirmation(sampleId));
    }

    @PostMapping("/{sampleId}/submit")
    public AjaxResult<?> submit(@PathVariable Long sampleId, @RequestBody SampleConfirmRequest request) {
        return AjaxResult.success(dataService.confirmSample(sampleId, request));
    }
}
