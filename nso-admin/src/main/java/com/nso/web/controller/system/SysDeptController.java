package com.nso.web.controller.system;

import com.nso.shared.core.domain.AjaxResult;
import com.nso.system.domain.SysDept;
import com.nso.system.service.ISysDeptService;
import com.nso.business.core.NsoDtos.PageQuery;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping({"/api/v1/admin/system/departments", "/api/v1/admin/system/dept"})

// 部门管理接口 负责部门信息的维护
public class SysDeptController {
    // 系统部门服务
    private final ISysDeptService departments;
    public SysDeptController(ISysDeptService departments) {
        this.departments = departments;
    }
    // 查询部门列表。
    @GetMapping
    @PreAuthorize("hasAuthority('sys:dept:read')")
    public AjaxResult<?> list(@RequestParam(required = false) Integer pageNo,
                              @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(departments.list(new PageQuery(pageNo, pageSize)));
    }
    // 创建部门。
    @PostMapping
    @PreAuthorize("hasAuthority('sys:dept:manage')")
    public AjaxResult<?> create(@RequestBody SysDept department) {
        return AjaxResult.success(departments.create(department));
    }
    // 更新部门。
    @PutMapping("/{deptId}")
    @PreAuthorize("hasAuthority('sys:dept:manage')")
    public AjaxResult<?> update(@PathVariable Long deptId, @RequestBody SysDept department) {
        return AjaxResult.success(departments.update(deptId, department));
    }
}
