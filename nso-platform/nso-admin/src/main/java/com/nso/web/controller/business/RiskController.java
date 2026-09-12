package com.nso.web.controller.business;

import com.nso.shared.core.domain.AjaxResult;
import com.nso.business.core.NsoDtos.RiskActionCloseRequest;
import com.nso.business.core.NsoDtos.RiskActionRequest;
import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.RiskOverrideRequest;
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

// 风险管理接口 负责风险的识别、评估和处理
public class RiskController {

    // 风险服务
    private final IRiskService riskService;

    public RiskController(IRiskService riskService) {
        this.riskService = riskService;
    }

    // 按条件查询项目风险。
    @GetMapping("/risks")
    @PreAuthorize("hasAnyAuthority('nso:risk:view', 'risk:view')")
    public AjaxResult<?> risks(@RequestParam(required = false) Long projectId,
                               @RequestParam(required = false) String level,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false) Integer pageNo,
                               @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(riskService.list(projectId, level, status, new PageQuery(pageNo, pageSize)));
    }

    // 重新计算项目风险。
    @GetMapping("/projects/{id}/risk")
    @PreAuthorize("hasAnyAuthority('nso:risk:view', 'risk:view')")
    public AjaxResult<?> projectRisk(@PathVariable Long id) {

        return AjaxResult.success(riskService.calculate(id));
    }

    // 查询风险处置措施。
    @GetMapping("/risks/{id}/actions")
    @PreAuthorize("hasAnyAuthority('nso:risk:view', 'risk:view')")
    public AjaxResult<?> actions(@PathVariable Long id, @RequestParam(required = false) Integer pageNo,
                                 @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(riskService.actions(id, new PageQuery(pageNo, pageSize)));
    }

    // 查询风险明细。
    @GetMapping("/risks/{id}/details")
    @PreAuthorize("hasAnyAuthority('nso:risk:view', 'risk:view')")
    public AjaxResult<?> details(@PathVariable Long id, @RequestParam(required = false) Integer pageNo,
                                 @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(riskService.details(id, new PageQuery(pageNo, pageSize)));
    }

    // 人工调整风险等级。
    @PostMapping("/risks/{id}/override")
    @PreAuthorize("hasAnyAuthority('nso:risk:dispose', 'risk:dispose')")
    public AjaxResult<?> override(@PathVariable Long id, @RequestBody RiskOverrideRequest request) {
        return AjaxResult.success(riskService.override(id, request));
    }

    // 创建风险处置措施。
    @PostMapping("/risks/{id}/actions")
    @PreAuthorize("hasAnyAuthority('nso:risk:dispose', 'risk:dispose')")
    public AjaxResult<?> createAction(@PathVariable Long id, @RequestBody RiskActionRequest request) {
        return AjaxResult.success(riskService.createAction(id, request));
    }

    // 关闭风险处置措施。
    @PostMapping("/risk-actions/{id}/close")
    @PreAuthorize("hasAnyAuthority('nso:risk:dispose', 'risk:dispose')")
    public AjaxResult<?> closeAction(@PathVariable Long id, @RequestBody RiskActionCloseRequest request) {
        return AjaxResult.success(riskService.closeAction(id, request));
    }
}
