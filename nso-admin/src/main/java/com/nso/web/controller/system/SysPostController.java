package com.nso.web.controller.system;

import com.nso.shared.core.domain.AjaxResult;
import com.nso.system.domain.SysPost;
import com.nso.system.service.IPersonnelOrganizationService;
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
@RequestMapping("/api/v1/admin/system/posts")
@PreAuthorize("hasAuthority('sys:post:manage')")

// 岗位管理接口 负责岗位信息的维护
public class SysPostController {
    // 人员组织服务
    private final IPersonnelOrganizationService personnel;
    public SysPostController(IPersonnelOrganizationService personnel) {
        this.personnel = personnel;
    }
    // 查询岗位列表。
    @GetMapping
    public AjaxResult<?> list(@RequestParam(required = false) Integer pageNo,
                              @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(personnel.listPosts(new PageQuery(pageNo, pageSize)));
    }
    // 创建岗位。
    @PostMapping
    public AjaxResult<?> create(@RequestBody SysPost post) {
        return AjaxResult.success(personnel.createPost(post));
    }
    // 更新岗位。
    @PutMapping("/{postId}")

    public AjaxResult<?> update(@PathVariable Long postId, @RequestBody SysPost post) {
        return AjaxResult.success(personnel.updatePost(postId, post));

    }
}
