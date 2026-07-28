package com.nso.web.controller.business;

import com.nso.common.core.domain.AjaxResult;
import com.nso.business.core.NsoDtos.RiskActionCloseRequest;
import com.nso.business.core.NsoDtos.RiskActionRequest;
import com.nso.business.risk.service.IRiskService;
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
public class RiskController {

    private final IRiskService riskService;

    public RiskController(IRiskService riskService) {
        this.riskService = riskService;
    }

    @GetMapping("/risks")
    @PreAuthorize("hasAuthority('risk:view')")
    public AjaxResult<?> risks(@RequestParam(required = false) Long projectId) {
        return AjaxResult.success(riskService.list(projectId));
    }

    @GetMapping("/projects/{id}/risk")
    @PreAuthorize("hasAuthority('risk:view')")
    public AjaxResult<?> projectRisk(@PathVariable Long id) {
        return AjaxResult.success(riskService.calculate(id));
    }

    @GetMapping("/risks/{id}/actions")
    @PreAuthorize("hasAuthority('risk:view')")
    public AjaxResult<?> actions(@PathVariable Long id) {
        return AjaxResult.success(riskService.actions(id));
    }

    @PostMapping("/risks/{id}/actions")
    @PreAuthorize("hasAuthority('risk:dispose')")
    public AjaxResult<?> createAction(@PathVariable Long id, @RequestBody RiskActionRequest request) {
        return AjaxResult.success(riskService.createAction(id, request));
    }

    @PostMapping("/risk-actions/{id}/close")
    @PreAuthorize("hasAuthority('risk:dispose')")
    public AjaxResult<?> closeAction(@PathVariable Long id, @RequestBody RiskActionCloseRequest request) {
        return AjaxResult.success(riskService.closeAction(id, request));
    }
}
