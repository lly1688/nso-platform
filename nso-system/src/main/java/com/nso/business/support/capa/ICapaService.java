package com.nso.business.support.capa;

import com.nso.business.core.NsoDtos.CapaActionDto;
import com.nso.business.core.NsoDtos.CapaCaseCreateRequest;
import com.nso.business.core.NsoDtos.CapaCaseDto;
import com.nso.business.core.NsoDtos.CapaCorrectiveTaskRequest;
import com.nso.business.core.NsoDtos.CapaEvidenceDto;
import com.nso.business.core.NsoDtos.CapaEvidenceRequest;
import com.nso.business.core.NsoDtos.CapaTransitionRequest;
import com.nso.business.core.NsoDtos.ExceptionReportRequest;
import com.nso.business.core.NsoDtos.PageQuery;
import com.nso.business.core.NsoDtos.PageResult;
import com.nso.business.core.NsoDtos.RiskDto;

import java.util.List;

/**
 * CAPA 异常闭环服务接口。
 */
public interface ICapaService {

    /**
     * 分页查询异常单。
     *
     * @param projectId 项目编号
     * @param status 异常状态
     * @param pageQuery 分页参数
     * @return 异常单分页结果
     */
    PageResult<CapaCaseDto> list(Long projectId, String status, PageQuery pageQuery);

    /**
     * 查询项目未关闭异常单。
     *
     * @param projectId 项目编号
     * @return 异常单列表
     */
    List<CapaCaseDto> openCases(Long projectId);

    /**
     * 查询异常单详情。
     *
     * @param caseId 异常单编号
     * @return 异常单详情
     */
    CapaCaseDto get(Long caseId);

    /**
     * 创建异常单。
     *
     * @param request 异常内容
     * @return 新建的异常单
     */
    CapaCaseDto create(CapaCaseCreateRequest request);

    /**
     * 兼容旧入口上报异常风险。
     *
     * @param request 异常内容
     * @return 生成的风险
     */
    RiskDto reportLegacy(ExceptionReportRequest request);

    /**
     * 执行异常单状态迁移。
     *
     * @param caseId 异常单编号
     * @param request 迁移内容
     * @return 更新后的异常单
     */
    CapaCaseDto transition(Long caseId, CapaTransitionRequest request);

    /**
     * 添加异常证据。
     *
     * @param caseId 异常单编号
     * @param request 证据内容
     * @return 新建的证据
     */
    CapaEvidenceDto addEvidence(Long caseId, CapaEvidenceRequest request);

    /**
     * 创建纠正任务。
     *
     * @param caseId 异常单编号
     * @param request 任务内容
     * @return 新建的纠正任务
     */
    CapaActionDto createCorrectiveTask(Long caseId, CapaCorrectiveTaskRequest request);

    /**
     * 查询异常处置行动。
     *
     * @param caseId 异常单编号
     * @return 处置行动列表
     */
    List<CapaActionDto> actions(Long caseId);

    /**
     * 在审批驳回后重新打开异常单。
     *
     * @param caseId 异常单编号
     * @return 更新后的异常单
     */
    CapaCaseDto reopenAfterApprovalRejection(Long caseId);

    /**
     * 升级逾期异常单。
     *
     * @return 升级数量
     */
    int escalateOverdue();
}
