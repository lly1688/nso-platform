package com.nso.business.change.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nso.business.change.domain.ChangeImpact;
import com.nso.business.change.domain.ChangeOrder;
import com.nso.business.change.mapper.ChangeImpactMapper;
import com.nso.business.change.mapper.ChangeOrderMapper;
import com.nso.business.core.NsoDtos.ChangeImpactDto;
import com.nso.business.core.NsoDtos.ChangeOrderDto;
import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.business.core.PageSupport;
import com.nso.business.project.domain.Project;
import com.nso.business.project.mapper.ProjectMapper;
import com.nso.business.project.service.ProjectDataScope;
import com.nso.shared.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.util.List;

// 变更单及影响项的可见范围查询与 DTO 聚合组件。
@Component

// 变更查询协调器 协调变更单的多维度查询
public class ChangeQueryCoordinator {
    // 变更Order数据映射
    private final ChangeOrderMapper changes;
    // 变更影响数据映射
    private final ChangeImpactMapper impacts;
    // 项目数据映射
    private final ProjectMapper projects;
    // 项目数据范围
    private final ProjectDataScope dataScope;

    public ChangeQueryCoordinator(
            ChangeOrderMapper changes,
            ChangeImpactMapper impacts,
            ProjectMapper projects,
            ProjectDataScope dataScope) {
        this.changes = changes;
        this.impacts = impacts;
        this.projects = projects;
        this.dataScope = dataScope;
    }

    // 查询项目变更单。
    public PageResult<ChangeOrderDto> list(Long projectId) {
        List<Long> visibleIds = dataScope.visibleProjectIds();
        if (visibleIds != null && visibleIds.isEmpty()) {
            return new PageResult<>(List.of(), 0);
        }
        List<ChangeOrderDto> rows = changes.selectList(
                        Wrappers.<ChangeOrder>lambdaQuery()
                                .eq(projectId != null, ChangeOrder::getProjectId, projectId)
                                .in(visibleIds != null, ChangeOrder::getProjectId,
                                        visibleIds == null ? List.of() : visibleIds)
                                .orderByDesc(ChangeOrder::getId))
                .stream()
                .map(this::toDto)
                .toList();
        return new PageResult<>(rows, rows.size());
    }

    // 按条件查询项目变更单。
    public PageResult<ChangeOrderDto> list(Long projectId, String status, String changeType, PageQuery pageQuery) {
        PageQuery page = PageSupport.normalize(pageQuery);
        List<Long> visibleIds = dataScope.visibleProjectIds();
        if (visibleIds != null && visibleIds.isEmpty()) {
            return PageResult.empty(page);
        }
        Page<ChangeOrder> entityPage = changes.selectPage(PageSupport.page(page),
                Wrappers.<ChangeOrder>lambdaQuery()
                        .eq(projectId != null, ChangeOrder::getProjectId, projectId)
                        .eq(status != null && !status.isBlank(), ChangeOrder::getStatus, status)
                        .eq(changeType != null && !changeType.isBlank(), ChangeOrder::getChangeType, changeType)
                        .in(visibleIds != null, ChangeOrder::getProjectId, visibleIds == null ? List.of() : visibleIds)
                        .orderByDesc(ChangeOrder::getId));
        return PageSupport.result(entityPage, page, this::toDto);
    }

    // 查询变更影响项。
    public List<ChangeImpactDto> impacts(Long changeId) {
        requireChange(changeId);
        return impacts.selectList(Wrappers.<ChangeImpact>lambdaQuery()
                        .eq(ChangeImpact::getChangeId, changeId)
                        .orderByAsc(ChangeImpact::getId))
                .stream()
                .map(this::toDto)
                .toList();
    }

    // 分页查询变更影响项。
    public PageResult<ChangeImpactDto> impactsPage(Long changeId, PageQuery pageQuery) {
        requireChange(changeId);
        PageQuery page = PageSupport.normalize(pageQuery);
        Page<ChangeImpact> entityPage = impacts.selectPage(PageSupport.page(page),
                Wrappers.<ChangeImpact>lambdaQuery()
                        .eq(ChangeImpact::getChangeId, changeId)
                        .orderByAsc(ChangeImpact::getId));
        return PageSupport.result(entityPage, page, this::toDto);
    }

    // 校验并读取当前租户变更单。
    public ChangeOrder requireChange(Long id) {
        ChangeOrder row = changes.selectById(id);
        if (row == null) {
            throw new BusinessException("变更单不存在");
        }
        dataScope.requireAccess(row.getProjectId());
        return row;
    }

    // 校验并读取当前租户项目。
    public Project requireProject(Long id) {
        Project row = projects.selectById(id);
        if (row == null) {
            throw new BusinessException("项目不存在");
        }
        dataScope.requireAccess(id);
        return row;
    }

    // 转换变更单数据。
    public ChangeOrderDto toDto(ChangeOrder row) {
        int total = impacts.selectCount(Wrappers.<ChangeImpact>lambdaQuery()
                .eq(ChangeImpact::getChangeId, row.getId())).intValue();
        int completed = impacts.selectCount(Wrappers.<ChangeImpact>lambdaQuery()
                .eq(ChangeImpact::getChangeId, row.getId())
                .eq(ChangeImpact::getStatus, "DONE")).intValue();
        return new ChangeOrderDto(row.getId(),
                row.getProjectId(),
                requireProject(row.getProjectId()).getProjectNo(),
                row.getChangeNo(),
                row.getChangeType(),
                row.getUrgency(),
                row.getBeforeContent(),
                row.getAfterContent(),
                row.getReason(),
                row.getStatus(),
                row.getDelayDays(),
                row.getReworkQty(),
                total,
                completed);
    }

    // 转换变更影响项数据。
    public ChangeImpactDto toDto(ChangeImpact row) {
        return new ChangeImpactDto(row.getId(),
                row.getChangeId(),
                row.getObjectType(),
                row.getObjectName(),
                row.getDepartmentName(),
                row.getSuggestedAction(),
                row.getStatus(),
                row.getFeedbackResult(),
                row.getResponsibleName(),
                row.getVersion());
    }
}
