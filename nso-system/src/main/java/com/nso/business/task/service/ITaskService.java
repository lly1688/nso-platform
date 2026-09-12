package com.nso.business.task.service;

import com.nso.business.core.NsoDtos.*;

/**
 * 执行任务与交付服务接口。
 */
public interface ITaskService {

    /**
     * 查询项目任务。
     *
     * @param projectId 项目编号
     * @return 任务分页结果
     */
    PageResult<TaskDto> list(Long projectId);

    /**
     * 按条件分页查询项目任务。
     *
     * @param projectId 项目编号
     * @param status 任务状态
     * @param taskType 任务类型
     * @param pageQuery 分页参数
     * @return 任务分页结果
     */
    PageResult<TaskDto> list(Long projectId, String status, String taskType, PageQuery pageQuery);

    /**
     * 查询任务详情。
     *
     * @param taskId 任务编号
     * @return 任务详情
     */
    TaskDto get(Long taskId);

    /**
     * 创建执行任务。
     *
     * @param request 任务信息
     * @return 新建的任务
     */
    TaskDto create(ExecutionTaskRequest request);

    /**
     * 启动任务。
     *
     * @param taskId 任务编号
     * @return 更新后的任务
     */
    TaskDto start(Long taskId);

    /**
     * 根据操作内容启动任务。
     *
     * @param taskId 任务编号
     * @param request 操作内容
     * @return 更新后的任务
     */
    TaskDto start(Long taskId, TaskActionRequest request);

    /**
     * 暂停任务。
     *
     * @param taskId 任务编号
     * @param request 操作内容
     * @return 更新后的任务
     */
    TaskDto pause(Long taskId, TaskActionRequest request);

    /**
     * 完成任务。
     *
     * @param taskId 任务编号
     * @param request 操作内容
     * @return 更新后的任务
     */
    TaskDto complete(Long taskId, TaskActionRequest request);

    /**
     * 提交任务反馈。
     *
     * @param taskId 任务编号
     * @param request 反馈内容
     * @return 更新后的任务
     */
    TaskDto feedback(Long taskId, TaskFeedbackRequest request);

    /**
     * 上报执行异常。
     *
     * @param request 异常内容
     * @return 生成的风险
     */
    RiskDto reportException(ExceptionReportRequest request);

    /**
     * 查询项目交付记录。
     *
     * @param projectId 项目编号
     * @return 交付记录分页结果
     */
    PageResult<DeliveryRecordDto> deliveries(Long projectId);

    /**
     * 按状态分页查询项目交付记录。
     *
     * @param projectId 项目编号
     * @param status 交付状态
     * @param pageQuery 分页参数
     * @return 交付记录分页结果
     */
    PageResult<DeliveryRecordDto> deliveries(Long projectId, String status, PageQuery pageQuery);

    /**
     * 检查项目交付就绪状态。
     *
     * @param projectId 项目编号
     * @return 交付就绪结果
     */
    DeliveryReadinessDto deliveryReadiness(Long projectId);

    /**
     * 创建项目交付记录。
     *
     * @param request 交付信息
     * @return 新建的交付记录
     */
    DeliveryRecordDto createDelivery(DeliveryRequest request);

    /**
     * 同步技术包执行任务。
     *
     * @param projectId 项目编号
     * @return 任务同步结果
     */
    TechnicalPackageSyncResultDto syncTechnicalPackageTasks(Long projectId);
}
