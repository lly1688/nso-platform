package com.nso.system.service;

import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.system.domain.SysDept;

import java.util.List;

/**
 * 部门管理服务接口。
 */
public interface ISysDeptService {

    /**
     * 查询部门列表。
     *
     * @return 部门列表
     */
    List<SysDept> list();

    /**
     * 分页查询部门列表。
     *
     * @param pageQuery 分页参数
     * @return 部门分页结果
     */
    PageResult<SysDept> list(PageQuery pageQuery);

    /**
     * 创建部门。
     *
     * @param dept 部门信息
     * @return 新建的部门
     */
    SysDept create(SysDept dept);

    /**
     * 更新部门。
     *
     * @param deptId 部门编号
     * @param request 部门信息
     * @return 更新后的部门
     */
    SysDept update(Long deptId, SysDept request);
}
