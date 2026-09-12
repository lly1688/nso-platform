package com.nso.system.service;

import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.system.domain.SysPost;

import java.util.List;

/**
 * 人员岗位组织服务接口。
 */
public interface IPersonnelOrganizationService {

    /**
     * 查询岗位列表。
     *
     * @return 岗位列表
     */
    List<SysPost> listPosts();

    /**
     * 分页查询岗位列表。
     *
     * @param pageQuery 分页参数
     * @return 岗位分页结果
     */
    PageResult<SysPost> listPosts(PageQuery pageQuery);

    /**
     * 创建岗位。
     *
     * @param post 岗位信息
     * @return 新建的岗位
     */
    SysPost createPost(SysPost post);

    /**
     * 更新岗位。
     *
     * @param postId 岗位编号
     * @param post 岗位信息
     * @return 更新后的岗位
     */
    SysPost updatePost(Long postId, SysPost post);

    /**
     * 替换用户岗位关联。
     *
     * @param userId 用户编号
     * @param postIds 岗位编号列表
     * @param primaryPostId 主岗位编号
     */
    void replaceUserPosts(Long userId, List<Long> postIds, Long primaryPostId);

    /**
     * 查询用户岗位编号。
     *
     * @param userId 用户编号
     * @return 岗位编号列表
     */
    List<Long> userPostIds(Long userId);
}
