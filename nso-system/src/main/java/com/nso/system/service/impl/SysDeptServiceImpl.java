package com.nso.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nso.business.core.TenantContext;
import com.nso.common.exception.BusinessException;
import com.nso.system.domain.SysDept;
import com.nso.system.mapper.SysDeptMapper;
import com.nso.system.service.ISysDeptService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SysDeptServiceImpl implements ISysDeptService {
    private final SysDeptMapper departments;

    public SysDeptServiceImpl(SysDeptMapper departments) { this.departments = departments; }

    @Override
    public List<SysDept> list() {
        return departments.selectList(Wrappers.<SysDept>lambdaQuery()
                .eq(SysDept::getTenantId, TenantContext.tenantId())
                .orderByAsc(SysDept::getSortNo).orderByAsc(SysDept::getId));
    }

    @Override
    @Transactional
    public SysDept create(SysDept dept) {
        if (dept == null || dept.getDeptName() == null || dept.getDeptName().isBlank()) {
            throw new BusinessException("部门名称不能为空");
        }
        dept.setTenantId(TenantContext.tenantId());
        dept.setDeptName(dept.getDeptName().trim());
        dept.setSortNo(dept.getSortNo() == null ? 0 : dept.getSortNo());
        dept.setStatus(dept.getStatus() == null || dept.getStatus().isBlank() ? "ENABLED" : dept.getStatus());
        departments.insert(dept);
        return dept;
    }
}
