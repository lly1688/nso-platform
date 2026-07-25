package com.nso.web.controller.miniapp;

import com.nso.common.core.domain.AjaxResult;
import com.nso.web.controller.MvpDtos.ChangeFeedbackRequest;
import com.nso.web.controller.MvpDtos.ChangeRequest;
import com.nso.web.controller.MvpDtos.LoginRequest;
import com.nso.web.controller.MvpDtos.SampleConfirmRequest;
import com.nso.web.controller.MvpDtos.SampleRequest;
import com.nso.web.controller.MvpDtos.TaskFeedbackRequest;
import com.nso.web.controller.MvpDataService;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/mp")
public class MpMvpController {

    private final MvpDataService dataService;

    public MpMvpController(MvpDataService dataService) {
        this.dataService = dataService;
    }

    @PostMapping({"/auth/wechat-login", "/auth/login"})
    public AjaxResult<?> login(@RequestBody(required = false) LoginRequest request) {
        String username = request == null ? "mp-user" : request.username();
        String code = request == null ? "" : request.code();
        return AjaxResult.success(dataService.login(username, null, code, "mp"));
    }

    @GetMapping({"/auth/me", "/auth/profile"})
    public AjaxResult<?> profile() {
        return AjaxResult.success(dataService.login("mp-user", null, null, "mp"));
    }

    @PostMapping("/auth/refresh")
    public AjaxResult<?> refresh() {
        return AjaxResult.success(dataService.login("mp-user", null, null, "mp"));
    }

    @PostMapping("/auth/logout")
    public AjaxResult<Void> logout() {
        return AjaxResult.success();
    }

    @GetMapping("/projects")
    public AjaxResult<?> projects(@RequestParam(required = false) String keyword) {
        return AjaxResult.success(dataService.listProjects(keyword));
    }

    @GetMapping("/projects/{id}")
    public AjaxResult<?> project(@PathVariable Long id) {
        return AjaxResult.success(dataService.getProjectDetail(id));
    }

    @GetMapping("/samples")
    public AjaxResult<?> samples(@RequestParam(required = false) Long projectId) {
        return AjaxResult.success(dataService.listSamples(projectId));
    }

    @PostMapping("/samples")
    public AjaxResult<?> createSample(@RequestBody SampleRequest request) {
        return AjaxResult.success(dataService.createSample(request));
    }

    @PostMapping("/samples/{id}/confirm")
    public AjaxResult<?> confirmSample(@PathVariable Long id, @RequestBody SampleConfirmRequest request) {
        return AjaxResult.success(dataService.confirmSample(id, request));
    }

    @GetMapping("/changes")
    public AjaxResult<?> changes(@RequestParam(required = false) Long projectId) {
        return AjaxResult.success(dataService.listChanges(projectId));
    }

    @PostMapping("/changes")
    public AjaxResult<?> createChange(@RequestBody ChangeRequest request) {
        return AjaxResult.success(dataService.createChange(request));
    }

    @PostMapping("/changes/{id}/approve")
    public AjaxResult<?> approveChange(@PathVariable Long id) {
        return AjaxResult.success(dataService.approveChange(id));
    }

    @PostMapping("/changes/{id}/feedback")
    public AjaxResult<?> feedbackChange(@PathVariable Long id, @RequestBody ChangeFeedbackRequest request) {
        return AjaxResult.success(dataService.feedbackChange(id, request));
    }

    @GetMapping("/tasks")
    public AjaxResult<?> tasks(@RequestParam(required = false) Long projectId) {
        return AjaxResult.success(dataService.listTasks(projectId));
    }

    @GetMapping("/tasks/{id}")
    public AjaxResult<?> task(@PathVariable Long id) {
        return AjaxResult.success(dataService.getTask(id));
    }

    @PostMapping("/tasks/{id}/start")
    public AjaxResult<?> startTask(@PathVariable Long id) {
        return AjaxResult.success(dataService.startTask(id));
    }

    @PostMapping("/tasks/{id}/feedback")
    public AjaxResult<?> feedbackTask(@PathVariable Long id, @RequestBody TaskFeedbackRequest request) {
        return AjaxResult.success(dataService.feedbackTask(id, request));
    }

    @GetMapping("/risks")
    public AjaxResult<?> risks(@RequestParam(required = false) Long projectId) {
        return AjaxResult.success(dataService.listRisks(projectId));
    }

    @GetMapping("/messages")
    public AjaxResult<?> messages(@RequestParam(required = false) String status) {
        return AjaxResult.success(dataService.listMessages(status));
    }

    @PostMapping(value = "/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AjaxResult<?> upload(@RequestParam("file") MultipartFile file) {
        return AjaxResult.success(dataService.upload(file));
    }
}
