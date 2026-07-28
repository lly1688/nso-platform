package com.nso.web.controller.system;

import com.nso.common.core.domain.AjaxResult;
import com.nso.system.domain.SysDept;
import com.nso.system.service.ISysDeptService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/admin/system/departments", "/api/v1/admin/system/dept"})
@PreAuthorize("hasRole('ADMIN')")
public class SysDeptController {
    private final ISysDeptService departments;
    public SysDeptController(ISysDeptService departments) { this.departments = departments; }
    @GetMapping public AjaxResult<?> list() { return AjaxResult.success(departments.list()); }
    @PostMapping public AjaxResult<?> create(@RequestBody SysDept department) { return AjaxResult.success(departments.create(department)); }
}
