package com.nso.web.controller.business;

import com.nso.shared.core.domain.AjaxResult;
import com.nso.business.core.NsoDtos.*;
import com.nso.business.task.service.ITaskService;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")

// 任务管理接口 负责任务的分配、跟踪和交付
public class TaskController {

    // 任务服务
    private final ITaskService taskService;

    public TaskController(ITaskService taskService) {
        this.taskService = taskService;
    }

    // 按条件查询项目任务。
    @GetMapping("/tasks")
    @PreAuthorize("hasAnyAuthority('nso:task:view', 'task:view')")
    public AjaxResult<?> tasks(@RequestParam(required = false) Long projectId,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false) String taskType,
                               @RequestParam(required = false) Integer pageNo,
                               @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(taskService.list(projectId, status, taskType, new PageQuery(pageNo, pageSize)));
    }

    // 查询任务详情。
    @GetMapping("/tasks/{id}")
    @PreAuthorize("hasAnyAuthority('nso:task:view', 'task:view')")
    public AjaxResult<?> task(@PathVariable Long id) {

        return AjaxResult.success(taskService.get(id));
    }

    // 启动任务。
    @PostMapping("/tasks/{id}/start")
    @PreAuthorize("hasAnyAuthority('nso:task:execute', 'task:execute')")
    public AjaxResult<?> startTask(@PathVariable Long id, @RequestBody(required = false) TaskActionRequest request) {
        return AjaxResult.success(taskService.start(id, request));
    }

    // 暂停任务。
    @PostMapping("/tasks/{id}/pause")
    @PreAuthorize("hasAnyAuthority('nso:task:execute', 'task:execute')")
    public AjaxResult<?> pauseTask(@PathVariable Long id, @RequestBody(required = false) TaskActionRequest request) {
        return AjaxResult.success(taskService.pause(id, request));
    }

    // 完成任务。
    @PostMapping("/tasks/{id}/complete")
    @PreAuthorize("hasAnyAuthority('nso:task:execute', 'task:execute')")
    public AjaxResult<?> completeTask(@PathVariable Long id, @RequestBody(required = false) TaskActionRequest request) {
        return AjaxResult.success(taskService.complete(id, request));
    }

    // 提交任务反馈。
    @PostMapping("/tasks/{id}/feedback")
    @PreAuthorize("hasAnyAuthority('nso:task:feedback', 'task:feedback')")
    public AjaxResult<?> feedbackTask(@PathVariable Long id, @RequestBody TaskFeedbackRequest request) {
        return AjaxResult.success(taskService.feedback(id, request));
    }

    // 创建执行任务。
    @PostMapping("/execution/tasks")
    @PreAuthorize("hasAnyAuthority('nso:task:plan', 'task:plan')")
    public AjaxResult<?> createExecutionTask(@RequestBody ExecutionTaskRequest request) {
        return AjaxResult.success(taskService.create(request));
    }

    // 上报执行异常。
    @PostMapping("/execution/exceptions")
    @PreAuthorize("hasAnyAuthority('nso:task:feedback', 'task:feedback')")
    public AjaxResult<?> reportException(@RequestBody ExceptionReportRequest request) {
        return AjaxResult.success(taskService.reportException(request));
    }

    // 按条件查询项目交付记录。
    @GetMapping("/deliveries")
    @PreAuthorize("hasAnyAuthority('nso:task:view', 'task:view')")
    public AjaxResult<?> deliveries(@RequestParam(required = false) Long projectId,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) Integer pageNo,
                                    @RequestParam(required = false) Integer pageSize) {
        return AjaxResult.success(taskService.deliveries(projectId, status, new PageQuery(pageNo, pageSize)));
    }

    // 创建项目交付记录。
    @PostMapping("/deliveries")
    @PreAuthorize("hasAnyAuthority('nso:task:execute', 'task:execute')")
    public AjaxResult<?> createDelivery(@RequestBody DeliveryRequest request) {
        return AjaxResult.success(taskService.createDelivery(request));
    }
}
