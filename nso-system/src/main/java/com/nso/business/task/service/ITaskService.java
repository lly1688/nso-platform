package com.nso.business.task.service;
import com.nso.business.core.NsoDtos.*;
public interface ITaskService {
 PageResult<TaskDto> list(Long projectId); TaskDto get(Long taskId); TaskDto create(ExecutionTaskRequest request); TaskDto start(Long taskId); TaskDto start(Long taskId, TaskActionRequest request); TaskDto pause(Long taskId, TaskActionRequest request); TaskDto complete(Long taskId, TaskActionRequest request); TaskDto feedback(Long taskId, TaskFeedbackRequest request);
 RiskDto reportException(ExceptionReportRequest request); PageResult<DeliveryRecordDto> deliveries(Long projectId); DeliveryRecordDto createDelivery(DeliveryRequest request);
 TechnicalPackageSyncResultDto syncTechnicalPackageTasks(Long projectId);
}
