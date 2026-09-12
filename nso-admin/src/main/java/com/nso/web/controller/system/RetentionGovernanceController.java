package com.nso.web.controller.system;

import com.nso.shared.core.domain.AjaxResult;
import com.nso.system.service.IRetentionGovernanceService;
import com.nso.business.core.NsoDtos.PageQuery;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/admin/system/retention-policies")
@PreAuthorize("hasAuthority('sys:authorization:audit')")

// 数据保留治理接口 负责数据保留策略的管理
public class RetentionGovernanceController {
    // 留存治理服务
    private final IRetentionGovernanceService retention;
    public RetentionGovernanceController(IRetentionGovernanceService retention) {
        this.retention = retention;
    }
    // 查询数据保留策略。
    @GetMapping
    public AjaxResult<?> list(@RequestParam(required = false) Integer pageNo,
                                          @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(retention.policies(new PageQuery(pageNo, pageSize)));
    }
    // 更新数据保留策略。
    @PutMapping("/{policyCode}")
    public AjaxResult<?> update(@PathVariable String policyCode, @RequestBody PolicyRequest request) {
        return AjaxResult.success(retention.updatePolicy(policyCode, request.retentionDays(), request.archiveAfterDays(), request.enabled()));
    }
    // 执行指定数据保留策略。
    @PostMapping("/{policyCode}/run")
    public AjaxResult<?> run(@PathVariable String policyCode, @RequestBody(required = false) RunRequest request) {
        return AjaxResult.success(retention.runPolicy(policyCode, request == null ? null : request.confirmationRef()));
    }
    public record PolicyRequest(
        // 保留天数
        Integer retentionDays,
        // 归档等待天数
        Integer archiveAfterDays,
        // 启用状态
        Boolean enabled
    ) {

    }
    public record RunRequest(
        // 操作确认凭证
        String confirmationRef
    ) {
    }
}
