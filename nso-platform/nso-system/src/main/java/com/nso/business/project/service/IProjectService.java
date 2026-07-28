package com.nso.business.project.service;

import com.nso.business.core.NsoDtos.PageResult;
import com.nso.business.core.NsoDtos.ProjectDto;
import com.nso.business.core.NsoDtos.CustomerProjectDto;
import com.nso.business.core.NsoDtos.ProjectMemberDto;
import com.nso.business.core.NsoDtos.ProjectMemberRequest;
import com.nso.business.core.NsoDtos.ProjectRequest;
import com.nso.business.core.NsoDtos.RequirementDto;
import com.nso.business.core.NsoDtos.RequirementRequest;

public interface IProjectService {
    PageResult<ProjectDto> list(String keyword);
    PageResult<CustomerProjectDto> customerProjects(String keyword);
    ProjectDto create(ProjectRequest request);
    ProjectDto submitReview(Long projectId);
    PageResult<RequirementDto> requirements(Long projectId);
    RequirementDto saveRequirement(Long projectId, RequirementRequest request);
    RequirementDto confirmRequirement(Long requirementId, RequirementRequest request);
    PageResult<ProjectMemberDto> members(Long projectId);
    ProjectMemberDto addMember(Long projectId, ProjectMemberRequest request);
    ProjectDto get(Long projectId);
}
