package com.nso.web.controller.business;

import com.nso.common.core.domain.AjaxResult;
import com.nso.web.controller.MvpDtos.ChangeFeedbackRequest;
import com.nso.web.controller.MvpDtos.ChangeRequest;
import com.nso.web.controller.MvpDtos.CustomerRequest;
import com.nso.web.controller.MvpDtos.DocumentVersionRequest;
import com.nso.web.controller.MvpDtos.LoginRequest;
import com.nso.web.controller.MvpDtos.ProjectRequest;
import com.nso.web.controller.MvpDtos.SampleConfirmRequest;
import com.nso.web.controller.MvpDtos.SampleRequest;
import com.nso.web.controller.MvpDtos.TaskFeedbackRequest;
import com.nso.web.controller.MvpDataService;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminMvpController {

    private final MvpDataService dataService;

    public AdminMvpController(MvpDataService dataService) {
        this.dataService = dataService;
    }

    @PostMapping("/auth/login")
    public AjaxResult<?> login(@RequestBody(required = false) LoginRequest request) {
        String username = request == null ? "admin" : request.username();
        String password = request == null ? "admin123" : request.password();
        return AjaxResult.success(dataService.login(username, password, null, "admin"));
    }

    @PostMapping("/auth/refresh")
    public AjaxResult<?> refresh() {
        return AjaxResult.success(dataService.login("admin", "admin123", null, "admin"));
    }

    @PostMapping("/auth/logout")
    public AjaxResult<Void> logout() {
        return AjaxResult.success();
    }

    @GetMapping("/auth/me")
    public AjaxResult<?> me() {
        return AjaxResult.success(dataService.login("admin", "admin123", null, "admin"));
    }

    @GetMapping("/customers")
    public AjaxResult<?> customers(@RequestParam(required = false) String keyword) {
        return AjaxResult.success(dataService.listCustomers(keyword));
    }

    @PostMapping("/customers")
    public AjaxResult<?> createCustomer(@RequestBody CustomerRequest request) {
        return AjaxResult.success(dataService.createCustomer(request));
    }

    @GetMapping("/projects")
    public AjaxResult<?> projects(@RequestParam(required = false) String keyword) {
        return AjaxResult.success(dataService.listProjects(keyword));
    }

    @PostMapping("/projects")
    public AjaxResult<?> createProject(@RequestBody ProjectRequest request) {
        return AjaxResult.success(dataService.createProject(request));
    }

    @GetMapping("/projects/{id}")
    public AjaxResult<?> projectDetail(@PathVariable Long id) {
        return AjaxResult.success(dataService.getProjectDetail(id));
    }

    @PostMapping("/projects/{id}/submit-review")
    public AjaxResult<?> submitReview(@PathVariable Long id) {
        return AjaxResult.success(dataService.submitReview(id));
    }

    @GetMapping("/documents")
    public AjaxResult<?> documents(@RequestParam(required = false) Long projectId) {
        return AjaxResult.success(dataService.listDocuments(projectId));
    }

    @PostMapping("/documents/{projectId}/versions")
    public AjaxResult<?> createDocumentVersion(@PathVariable Long projectId, @RequestBody DocumentVersionRequest request) {
        return AjaxResult.success(dataService.createDocumentVersion(projectId, request));
    }

    @PostMapping("/document-versions/{id}/publish")
    public AjaxResult<?> publishDocumentVersion(@PathVariable Long id) {
        return AjaxResult.success(dataService.publishDocumentVersion(id));
    }

    @GetMapping("/samples")
    public AjaxResult<?> samples(@RequestParam(required = false) Long projectId) {
        return AjaxResult.success(dataService.listSamples(projectId));
    }

    @PostMapping("/samples")
    public AjaxResult<?> createSample(@RequestBody SampleRequest request) {
        return AjaxResult.success(dataService.createSample(request));
    }

    @PostMapping("/samples/{id}/submit-confirm")
    public AjaxResult<?> submitSampleConfirm(@PathVariable Long id) {
        return AjaxResult.success(dataService.submitSampleConfirm(id));
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

    @PostMapping("/changes/{id}/analyze-impact")
    public AjaxResult<?> analyzeChange(@PathVariable Long id) {
        return AjaxResult.success(dataService.analyzeChange(id));
    }

    @GetMapping("/changes/{id}/impacts")
    public AjaxResult<?> changeImpacts(@PathVariable Long id) {
        return AjaxResult.success(dataService.impactsByChange(id));
    }

    @PostMapping("/changes/{id}/approve")
    public AjaxResult<?> approveChange(@PathVariable Long id) {
        return AjaxResult.success(dataService.approveChange(id));
    }

    @PostMapping("/changes/{id}/feedback")
    public AjaxResult<?> feedbackChange(@PathVariable Long id, @RequestBody ChangeFeedbackRequest request) {
        return AjaxResult.success(dataService.feedbackChange(id, request));
    }

    @PostMapping("/change-impacts/{id}/feedback")
    public AjaxResult<?> feedbackImpact(@PathVariable Long id, @RequestBody ChangeFeedbackRequest request) {
        return AjaxResult.success(dataService.feedbackImpact(id, request));
    }

    @PostMapping("/changes/{id}/close")
    public AjaxResult<?> closeChange(@PathVariable Long id) {
        return AjaxResult.success(dataService.closeChange(id));
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

    @GetMapping("/projects/{id}/risk")
    public AjaxResult<?> projectRisk(@PathVariable Long id) {
        return AjaxResult.success(dataService.calculateRisk(id));
    }

    @GetMapping("/messages")
    public AjaxResult<?> messages(@RequestParam(required = false) String status) {
        return AjaxResult.success(dataService.listMessages(status));
    }

    @PostMapping("/messages/{id}/read")
    public AjaxResult<?> markMessageRead(@PathVariable Long id) {
        return AjaxResult.success(dataService.markMessageRead(id));
    }

    @GetMapping("/reports/overview")
    public AjaxResult<?> reportOverview() {
        return AjaxResult.success(dataService.overview());
    }

    @PostMapping(value = "/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AjaxResult<?> upload(@RequestParam("file") MultipartFile file) {
        return AjaxResult.success(dataService.upload(file));
    }

    @GetMapping("/files/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Resource resource = dataService.download(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
