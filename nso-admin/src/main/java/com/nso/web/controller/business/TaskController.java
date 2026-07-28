package com.nso.web.controller.business;

import com.nso.common.core.domain.AjaxResult;
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
public class TaskController {

    private final ITaskService taskService;

    public TaskController(ITaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/tasks")
    @PreAuthorize("hasAuthority('task:view')")
    public AjaxResult<?> tasks(@RequestParam(required = false) Long projectId) {
        return AjaxResult.success(taskService.list(projectId));
    }

    @GetMapping("/tasks/{id}")
    @PreAuthorize("hasAuthority('task:view')")
    public AjaxResult<?> task(@PathVariable Long id) {
        return AjaxResult.success(taskService.get(id));
    }

    @PostMapping("/tasks/{id}/start")
    @PreAuthorize("hasAuthority('task:execute')")
    public AjaxResult<?> startTask(@PathVariable Long id, @RequestBody(required = false) TaskActionRequest request) {
        return AjaxResult.success(taskService.start(id, request));
    }

    @PostMapping("/tasks/{id}/pause")
    @PreAuthorize("hasAuthority('task:execute')")
    public AjaxResult<?> pauseTask(@PathVariable Long id, @RequestBody(required = false) TaskActionRequest request) {
        return AjaxResult.success(taskService.pause(id, request));
    }

    @PostMapping("/tasks/{id}/complete")
    @PreAuthorize("hasAuthority('task:execute')")
    public AjaxResult<?> completeTask(@PathVariable Long id, @RequestBody(required = false) TaskActionRequest request) {
        return AjaxResult.success(taskService.complete(id, request));
    }

    @PostMapping("/tasks/{id}/feedback")
    @PreAuthorize("hasAuthority('task:feedback')")
    public AjaxResult<?> feedbackTask(@PathVariable Long id, @RequestBody TaskFeedbackRequest request) {
        return AjaxResult.success(taskService.feedback(id, request));
    }

    @PostMapping("/execution/tasks")
    @PreAuthorize("hasAuthority('task:plan')")
    public AjaxResult<?> createExecutionTask(@RequestBody ExecutionTaskRequest request) {
        return AjaxResult.success(taskService.create(request));
    }

    @PostMapping("/execution/exceptions")
    @PreAuthorize("hasAuthority('task:feedback')")
    public AjaxResult<?> reportException(@RequestBody ExceptionReportRequest request) {
        return AjaxResult.success(taskService.reportException(request));
    }

    @GetMapping("/deliveries")
    @PreAuthorize("hasAuthority('task:view')")
    public AjaxResult<?> deliveries(@RequestParam(required = false) Long projectId) {
        return AjaxResult.success(taskService.deliveries(projectId));
    }

    @PostMapping("/deliveries")
    @PreAuthorize("hasAuthority('task:execute')")
    public AjaxResult<?> createDelivery(@RequestBody DeliveryRequest request) {
        return AjaxResult.success(taskService.createDelivery(request));
    }
}
