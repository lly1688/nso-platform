package com.nso.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nso.business.core.TenantContext;
import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.business.core.PageSupport;
import com.nso.shared.exception.BusinessException;
import com.nso.system.domain.SysPost;
import com.nso.system.domain.SysUser;
import com.nso.system.mapper.SysPostMapper;
import com.nso.system.mapper.SysUserMapper;
import com.nso.system.service.IPersonnelOrganizationService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service

// 人员组织 服务层处理
public class PersonnelOrganizationServiceImpl implements IPersonnelOrganizationService {
    // 系统岗位数据映射
    private final SysPostMapper posts;
    // 系统用户数据映射
    private final SysUserMapper users;
    // JDBC模板
    private final JdbcTemplate jdbc;

    public PersonnelOrganizationServiceImpl(SysPostMapper posts, SysUserMapper users, JdbcTemplate jdbc) {
        this.posts = posts;
        this.users = users;
        this.jdbc = jdbc;
    }

    // 查询岗位列表。
    @Override
    public List<SysPost> listPosts() {
        return posts.selectList(Wrappers.<SysPost>lambdaQuery().eq(SysPost::getTenantId, TenantContext.tenantId())
                .orderByAsc(SysPost::getSortNo).orderByAsc(SysPost::getId));
    }

    // 分页查询岗位列表。
    @Override
    public PageResult<SysPost> listPosts(PageQuery pageQuery) {
        PageQuery page = PageSupport.normalize(pageQuery);
        Page<SysPost> entityPage = posts.selectPage(PageSupport.page(page), Wrappers.<SysPost>lambdaQuery()
                .eq(SysPost::getTenantId, TenantContext.tenantId())
                .orderByAsc(SysPost::getSortNo).orderByAsc(SysPost::getId));
        return new PageResult<>(entityPage.getRecords(), entityPage.getTotal(), page.pageNoValue(), page.pageSizeValue());
    }

    // 创建岗位。
    @Override
    @Transactional
    public SysPost createPost(SysPost post) {
        if (post == null || blank(post.getPostCode()) || blank(post.getPostName())) throw new BusinessException("岗位编码和岗位名称不能为空");
        String code = post.getPostCode().trim().toUpperCase();
        if (posts.selectCount(Wrappers.<SysPost>lambdaQuery().eq(SysPost::getTenantId, TenantContext.tenantId()).eq(SysPost::getPostCode, code)) > 0) {
            throw new BusinessException("岗位编码已存在");
        }
        post.setTenantId(TenantContext.tenantId());
        post.setPostCode(code);
        post.setPostName(post.getPostName().trim());
        post.setSortNo(post.getSortNo() == null ? 0 : post.getSortNo());
        post.setStatus(blank(post.getStatus()) ? "ENABLED" : post.getStatus().trim().toUpperCase());
        posts.insert(post);
        return post;
    }

    // 更新岗位。
    @Override
    @Transactional
    public SysPost updatePost(Long postId, SysPost request) {
        SysPost post = requirePost(postId);
        if (request == null) throw new BusinessException("岗位参数不能为空");
        if (!blank(request.getPostName())) post.setPostName(request.getPostName().trim());
        if (!blank(request.getStatus())) post.setStatus(request.getStatus().trim().toUpperCase());
        if (request.getSortNo() != null) post.setSortNo(request.getSortNo());
        posts.updateById(post);
        return posts.selectById(postId);
    }

    // 替换用户岗位关联。
    @Override
    @Transactional
    public void replaceUserPosts(Long userId, List<Long> postIds, Long primaryPostId) {
        SysUser user = users.selectById(userId);
        if (user == null || !Long.valueOf(TenantContext.tenantId()).equals(user.getTenantId()) || !"INTERNAL".equals(user.getUserType())) throw new BusinessException("只能维护当前企业的内部账号岗位");
        List<Long> normalized = postIds == null ? List.of() : postIds.stream().filter(id -> id != null).distinct().toList();
        if (primaryPostId != null && !normalized.contains(primaryPostId)) throw new BusinessException("主岗位必须包含在岗位列表中");
        for (Long postId : normalized) requirePost(postId);
        jdbc.update("DELETE FROM sys_user_post WHERE tenant_id=? AND user_id=?", TenantContext.tenantId(), userId);
        for (Long postId : normalized) {
            jdbc.update("INSERT INTO sys_user_post (user_id,post_id,tenant_id,primary_flag) VALUES (?,?,?,?)", userId, postId,
                    TenantContext.tenantId(), postId.equals(primaryPostId) ? 1 : 0);
        }
        users.incrementAuthVersion(userId);
    }

    // 查询用户岗位编号。
    @Override
    public List<Long> userPostIds(Long userId) {
        return jdbc.queryForList("SELECT post_id FROM sys_user_post WHERE tenant_id=? AND user_id=? ORDER BY primary_flag DESC, post_id", Long.class,
                TenantContext.tenantId(), userId);
    }

    private SysPost requirePost(Long postId) {
        SysPost post = posts.selectById(postId);
        if (post == null || !Long.valueOf(TenantContext.tenantId()).equals(post.getTenantId()) || Integer.valueOf(1).equals(post.getDeleted())) throw new BusinessException("岗位不存在或不属于当前企业");
        return post;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
