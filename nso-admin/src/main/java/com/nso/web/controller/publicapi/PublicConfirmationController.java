package com.nso.web.controller.publicapi;

import com.nso.shared.core.domain.AjaxResult;
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

// 公开确认接口 负责无需登录的公开确认操作
public class PublicConfirmationController {

    // 样品服务
    private final ISampleService sampleService;

    public PublicConfirmationController(ISampleService sampleService) {
        this.sampleService = sampleService;
    }

    // 查询公开样品确认信息。
    @GetMapping("/confirm/{token}")
    public AjaxResult<?> confirmationByToken(@PathVariable String token) {
        return AjaxResult.success(sampleService.publicConfirmation(token));
    }

    // 提交公开样品确认结论。
    @PostMapping("/confirm/{token}/decision")
    public AjaxResult<?> submitDecision(@PathVariable String token, @RequestBody SampleConfirmRequest request) {
        return AjaxResult.success(sampleService.submitPublicConfirmation(token, request));
    }
}
