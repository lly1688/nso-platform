package com.nso.web.controller.business;

import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.support.action.IActionCenterService;
import com.nso.shared.core.domain.AjaxResult;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")

// 行动中心接口 负责待办行动项的管理
public class ActionCenterController {
    // 操作中心服务
    private final IActionCenterService actionCenter;

    public ActionCenterController(IActionCenterService actionCenter) {
        this.actionCenter = actionCenter;
    }

    // 查询当前用户行动项。
    @GetMapping("/actions")
    @PreAuthorize("hasAnyAuthority('nso:action:view', 'action:view')")
    public AjaxResult<?> actions(
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) String dueState,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(actionCenter.myActions(sourceType, priority, projectId, dueState, new PageQuery(pageNo, pageSize)));
    }

    // 查询行动中心摘要。
    @GetMapping("/actions/summary")
    @PreAuthorize("hasAnyAuthority('nso:action:view', 'action:view')")
    public AjaxResult<?> summary() {
        return AjaxResult.success(actionCenter.summary());
    }

    // 刷新行动项投影。
    @PostMapping("/actions/refresh")
    @PreAuthorize("hasAnyAuthority('nso:action:manage', 'action:manage')")
    public AjaxResult<?> refresh() {
        return AjaxResult.success(java.util.Map.of("refreshed", actionCenter.refreshProjection()));
    }
}
