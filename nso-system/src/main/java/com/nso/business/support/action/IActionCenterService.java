package com.nso.business.support.action;

import com.nso.business.core.NsoDtos.ActionCenterSummaryDto;
import com.nso.business.core.NsoDtos.ActionItemDto;
import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;

import java.util.List;

/**
 * 行动中心服务接口。
 */
public interface IActionCenterService {

    /**
     * 分页查询当前用户行动项。
     *
     * @param sourceType 来源类型
     * @param priority 优先级
     * @param pageQuery 分页参数
     * @return 行动项分页结果
     */
    PageResult<ActionItemDto> myActions(String sourceType, String priority, Long projectId, String dueState, PageQuery pageQuery);

    default PageResult<ActionItemDto> myActions(String sourceType, String priority, PageQuery pageQuery) {
        return myActions(sourceType, priority, null, null, pageQuery);
    }

    /**
     * 查询行动中心摘要。
     *
     * @return 行动中心摘要
     */
    ActionCenterSummaryDto summary();

    /**
     * 查询项目阻塞项。
     *
     * @param projectId 项目编号
     * @return 阻塞行动项
     */
    List<ActionItemDto> projectBlockers(Long projectId);

    /**
     * 刷新行动项投影。
     *
     * @return 刷新数量
     */
    int refreshProjection();

    /**
     * 升级逾期行动项。
     *
     * @return 升级数量
     */
    int escalateOverdue();
}
