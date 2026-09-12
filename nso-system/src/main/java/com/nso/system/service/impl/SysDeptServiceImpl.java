package com.nso.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nso.business.core.TenantContext;
import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.business.core.PageSupport;
import com.nso.shared.exception.BusinessException;
import com.nso.system.domain.SysDept;
import com.nso.system.mapper.SysDeptMapper;
import com.nso.system.mapper.SysUserMapper;
import com.nso.system.domain.SysUser;
import com.nso.system.service.ISysDeptService;
import com.nso.system.security.UserLifecycle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service

// 部门管理 服务层处理
public class SysDeptServiceImpl implements ISysDeptService {
    // 系统部门数据映射
    private final SysDeptMapper departments;
    // 系统用户数据映射
    private final SysUserMapper users;

    public SysDeptServiceImpl(SysDeptMapper departments, SysUserMapper users) {
        this.departments = departments;
        this.users = users;
    }

    // 查询部门列表。
    @Override
    public List<SysDept> list() {
        return departments.selectList(Wrappers.<SysDept>lambdaQuery()
                .eq(SysDept::getTenantId, TenantContext.tenantId())
                .orderByAsc(SysDept::getSortNo).orderByAsc(SysDept::getId));
    }

    // 分页查询部门列表。
    @Override
    public PageResult<SysDept> list(PageQuery pageQuery) {
        PageQuery page = PageSupport.normalize(pageQuery);
        Page<SysDept> entityPage = departments.selectPage(PageSupport.page(page), Wrappers.<SysDept>lambdaQuery()
                .eq(SysDept::getTenantId, TenantContext.tenantId())
                .orderByAsc(SysDept::getSortNo).orderByAsc(SysDept::getId));
        return new PageResult<>(entityPage.getRecords(), entityPage.getTotal(), page.pageNoValue(), page.pageSizeValue());
    }

    // 创建部门。
    @Override
    @Transactional
    public SysDept create(SysDept dept) {
        if (dept == null || dept.getDeptName() == null || dept.getDeptName().isBlank()) {
            throw new BusinessException("部门名称不能为空");
        }
        dept.setTenantId(TenantContext.tenantId());
        applyHierarchy(dept, null);
        verifyLeader(dept.getLeaderUserId());
        if (dept.getLeaderUserId() != null) dept.setLeaderName(users.selectById(dept.getLeaderUserId()).getNickname());
        dept.setDeptName(dept.getDeptName().trim());
        dept.setSortNo(dept.getSortNo() == null ? 0 : dept.getSortNo());
        dept.setStatus(dept.getStatus() == null || dept.getStatus().isBlank() ? "ENABLED" : dept.getStatus());
        departments.insert(dept);
        return dept;
    }

    // 更新部门。
    @Override
    @Transactional
    public SysDept update(Long deptId, SysDept request) {
        SysDept dept = departments.selectById(deptId);
        if (dept == null || !Long.valueOf(TenantContext.tenantId()).equals(dept.getTenantId())) throw new BusinessException("部门不存在或不属于当前企业");
        if (request == null) throw new BusinessException("部门参数不能为空");
        if (request.getDeptName() != null && !request.getDeptName().isBlank()) dept.setDeptName(request.getDeptName().trim());
        if (request.getLeaderName() != null) dept.setLeaderName(request.getLeaderName().trim());
        if (request.getPhone() != null) dept.setPhone(request.getPhone().trim());
        if (request.getSortNo() != null) dept.setSortNo(request.getSortNo());
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            String status = request.getStatus().trim().toUpperCase();
            if (!List.of("ENABLED", "DISABLED").contains(status)) throw new BusinessException("部门状态仅支持 ENABLED 和 DISABLED");
            if ("DISABLED".equals(status) && departments.selectCount(Wrappers.<SysDept>lambdaQuery().eq(SysDept::getParentId, deptId).eq(SysDept::getStatus, "ENABLED")) > 0) {
                throw new BusinessException("存在启用的下级部门，不能停用当前部门");
            }
            dept.setStatus(status);
        }
        if (request.getParentId() != null && !request.getParentId().equals(dept.getParentId())) {
            dept.setParentId(request.getParentId());
            applyHierarchy(dept, deptId);
        }
        if (request.getLeaderUserId() != null) {
            verifyLeader(request.getLeaderUserId());
            dept.setLeaderUserId(request.getLeaderUserId());
            dept.setLeaderName(users.selectById(request.getLeaderUserId()).getNickname());
        }
        departments.updateById(dept);
        refreshDescendantAncestors(dept);
        return departments.selectById(deptId);
    }

    private void applyHierarchy(SysDept dept, Long selfId) {
        Long parentId = dept.getParentId();
        if (parentId == null) {
            dept.setAncestors("0,");
            return;
        }
        if (selfId != null && selfId.equals(parentId)) throw new BusinessException("部门不能设置自身为上级部门");
        SysDept parent = departments.selectById(parentId);
        if (parent == null || !Long.valueOf(TenantContext.tenantId()).equals(parent.getTenantId()) || !"ENABLED".equals(parent.getStatus())) throw new BusinessException("上级部门不存在、已停用或不属于当前企业");
        if (selfId != null && parent.getAncestors() != null && parent.getAncestors().contains("," + selfId + ",")) throw new BusinessException("不能将下级部门设置为上级部门");
        dept.setAncestors((parent.getAncestors() == null ? "0," : parent.getAncestors()) + parent.getId() + ",");
    }

    private void refreshDescendantAncestors(SysDept parent) {
        for (SysDept child : departments.selectList(Wrappers.<SysDept>lambdaQuery().eq(SysDept::getParentId, parent.getId()))) {
            child.setAncestors((parent.getAncestors() == null ? "0," : parent.getAncestors()) + parent.getId() + ",");
            departments.updateById(child);
            refreshDescendantAncestors(child);
        }
    }

    private void verifyLeader(Long leaderUserId) {
        if (leaderUserId == null) return;
        SysUser user = users.selectById(leaderUserId);
        if (user == null || !Long.valueOf(TenantContext.tenantId()).equals(user.getTenantId()) || !UserLifecycle.isActive(user.getStatus()) || !"INTERNAL".equals(user.getUserType())) {
            throw new BusinessException("部门负责人必须是当前企业的启用内部账号");
        }
    }
}
