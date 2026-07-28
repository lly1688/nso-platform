package com.nso.web.controller.publicapi;

import com.nso.common.core.domain.AjaxResult;
import com.nso.business.core.NsoDtos.SampleConfirmRequest;
import com.nso.business.sample.service.ISampleService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public")
public class PublicConfirmationController {

    private final ISampleService sampleService;

    public PublicConfirmationController(ISampleService sampleService) {
        this.sampleService = sampleService;
    }

    @GetMapping("/confirm/{token}")
    public AjaxResult<?> confirmationByToken(@PathVariable String token) {
        return AjaxResult.success(sampleService.publicConfirmation(token));
    }

    @PostMapping("/confirm/{token}/decision")
    public AjaxResult<?> submitDecision(@PathVariable String token, @RequestBody SampleConfirmRequest request) {
        return AjaxResult.success(sampleService.submitPublicConfirmation(token, request));
    }
}
