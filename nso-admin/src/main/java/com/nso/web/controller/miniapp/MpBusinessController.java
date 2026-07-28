package com.nso.web.controller.miniapp;

import com.nso.common.core.domain.AjaxResult;
import com.nso.business.core.NsoDtos.*;
import com.nso.framework.security.NsoAuthenticationService;
import com.nso.framework.security.NsoPrincipal;
import com.nso.business.project.service.IProjectService;
import com.nso.business.document.service.IDocumentService;
import com.nso.business.sample.service.ISampleService;
import com.nso.business.change.service.IChangeService;
import com.nso.business.task.service.ITaskService;
import com.nso.business.risk.service.IRiskService;
import com.nso.business.message.service.IMessageService;
import com.nso.business.report.service.IReportService;
import com.nso.business.file.IFileService;
import com.nso.business.support.IFlowCodeService;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/mp")
public class MpBusinessController {

    private final NsoAuthenticationService authenticationService;
    private final IProjectService projectService;
    private final IDocumentService documentService;
    private final ISampleService sampleService;
    private final IChangeService changeService;
    private final ITaskService taskService;
    private final IRiskService riskService;
    private final IMessageService messageService;
    private final IReportService reportService;
    private final IFileService fileService;
    private final IFlowCodeService flowCodes;

    public MpBusinessController(NsoAuthenticationService authenticationService,
                                IProjectService projectService, IDocumentService documentService, ISampleService sampleService,
                                IChangeService changeService, ITaskService taskService, IRiskService riskService,
                                IMessageService messageService, IReportService reportService, IFileService fileService, IFlowCodeService flowCodes) {
        this.authenticationService = authenticationService;
        this.projectService = projectService;
        this.documentService = documentService;
        this.sampleService = sampleService;
        this.changeService = changeService;
        this.taskService = taskService;
        this.riskService = riskService;
        this.messageService = messageService;
        this.reportService = reportService;
        this.fileService = fileService;
        this.flowCodes = flowCodes;
    }

    @PostMapping({"/auth/wechat-login", "/auth/login"})
    public AjaxResult<?> login(@RequestBody(required = false) LoginRequest request) {
        if (request == null) {
            return AjaxResult.error("请求体不能为空");
        }
        if (request.username() != null && !request.username().isBlank() && request.password() != null && !request.password().isBlank()) {
            return AjaxResult.success(authenticationService.passwordLogin(request.username(), request.password(), "mp"));
        }
        return AjaxResult.success(authenticationService.wechatLogin(request.code()));
    }

    @GetMapping({"/auth/me", "/auth/profile"})
    public AjaxResult<?> profile(@AuthenticationPrincipal NsoPrincipal principal) {
        return AjaxResult.success(authenticationService.current(principal));
    }

    @PostMapping("/auth/refresh")
    public AjaxResult<?> refresh(@RequestBody RefreshRequest request) {
        return AjaxResult.success(authenticationService.refresh(request.refreshToken(), "mp"));
    }

    @PostMapping("/auth/logout")
    public AjaxResult<Void> logout(@RequestBody(required = false) RefreshRequest request,
                                   @org.springframework.web.bind.annotation.RequestHeader(value = "Authorization", required = false) String authorization) {
        authenticationService.logout(bearer(authorization), request == null ? null : request.refreshToken());
        return AjaxResult.success();
    }

    @GetMapping("/projects")
    @PreAuthorize("hasAuthority('project:view')")
    public AjaxResult<?> projects(@RequestParam(required = false) String keyword) {
        return AjaxResult.success(projectService.list(keyword));
    }

    @GetMapping("/customer/projects")
    @PreAuthorize("hasAuthority('customer:portal')")
    public AjaxResult<?> customerProjects(@RequestParam(required = false) String keyword) {
        return AjaxResult.success(projectService.customerProjects(keyword));
    }

    @GetMapping("/customer/projects/{id}/samples")
    @PreAuthorize("hasAuthority('customer:portal')")
    public AjaxResult<?> customerSamples(@PathVariable Long id) {
        return AjaxResult.success(sampleService.customerSamples(id));
    }

    @GetMapping("/customer/samples")
    @PreAuthorize("hasAuthority('customer:portal')")
    public AjaxResult<?> customerSampleList(@RequestParam(required = false) Long projectId) {
        return AjaxResult.success(sampleService.customerSamples(projectId));
    }

    @PostMapping("/customer/samples/{id}/confirm")
    @PreAuthorize("hasAuthority('customer:portal')")
    public AjaxResult<?> customerConfirm(@PathVariable Long id, @RequestBody SampleConfirmRequest request, @AuthenticationPrincipal NsoPrincipal principal) {
        if (request == null) return AjaxResult.error("确认结论不能为空");
        return AjaxResult.success(sampleService.confirmCustomer(id, new SampleConfirmRequest(request.conclusion(), request.opinion(), principal.username(), request.evidenceFileId())));
    }

    @GetMapping("/workbench")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public AjaxResult<?> workbench() {
        return AjaxResult.success(reportService.workbench());
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('dashboard:view')")
    public AjaxResult<?> dashboard(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return AjaxResult.success(reportService.dashboard(period, startDate, endDate));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('project:view')")
    public AjaxResult<?> search(@RequestParam(required = false) String keyword) {
        return AjaxResult.success(reportService.search(keyword));
    }

    @GetMapping("/projects/{id}")
    @PreAuthorize("hasAuthority('project:view')")
    public AjaxResult<?> project(@PathVariable Long id) {
        return AjaxResult.success(reportService.projectDetail(id));
    }

    @PostMapping("/projects")
    @PreAuthorize("hasAuthority('project:create')")
    public AjaxResult<?> createProject(@RequestBody ProjectRequest request) {
        return AjaxResult.success(projectService.create(request));
    }

    @GetMapping("/documents")
    @PreAuthorize("hasAuthority('document:view')")
    public AjaxResult<?> documents(@RequestParam(required = false) Long projectId) {
        return AjaxResult.success(documentService.documents(projectId));
    }

    @GetMapping("/documents/{id}")
    @PreAuthorize("hasAuthority('document:view')")
    public AjaxResult<?> document(@PathVariable Long id) {
        return AjaxResult.success(documentService.getVersion(id));
    }

    @PostMapping("/documents/{projectId}/versions")
    @PreAuthorize("hasAuthority('document:upload')")
    public AjaxResult<?> createDocumentVersion(@PathVariable Long projectId, @RequestBody DocumentVersionRequest request) {
        return AjaxResult.success(documentService.createVersion(projectId, request));
    }

    @PostMapping("/document-versions/{id}/publish")
    @PreAuthorize("hasAuthority('document:publish')")
    public AjaxResult<?> publishDocumentVersion(@PathVariable Long id) {
        return AjaxResult.success(documentService.publishVersion(id));
    }

    @GetMapping("/samples")
    @PreAuthorize("hasAuthority('sample:view')")
    public AjaxResult<?> samples(@RequestParam(required = false) Long projectId) {
        return AjaxResult.success(sampleService.list(projectId));
    }

    @PostMapping("/samples")
    @PreAuthorize("hasAuthority('sample:create')")
    public AjaxResult<?> createSample(@RequestBody SampleRequest request) {
        return AjaxResult.success(sampleService.create(request));
    }

    @PostMapping("/samples/{id}/confirm")
    @PreAuthorize("hasAuthority('sample:proxy-confirm')")
    public AjaxResult<?> confirmSample(@PathVariable Long id, @RequestBody SampleConfirmRequest request) {
        return AjaxResult.success(sampleService.confirm(id, request));
    }

    @GetMapping("/samples/{id}/checks")
    @PreAuthorize("hasAuthority('sample:view')")
    public AjaxResult<?> sampleChecks(@PathVariable Long id) {
        return AjaxResult.success(sampleService.checks(id));
    }

    @PostMapping("/samples/{id}/checks")
    @PreAuthorize("hasAuthority('sample:inspect')")
    public AjaxResult<?> addSampleCheck(@PathVariable Long id, @RequestBody SampleCheckRequest request) {
        return AjaxResult.success(sampleService.addCheck(id, request));
    }

    @GetMapping("/changes")
    @PreAuthorize("hasAuthority('change:view')")
    public AjaxResult<?> changes(@RequestParam(required = false) Long projectId) {
        return AjaxResult.success(changeService.list(projectId));
    }

    @PostMapping("/changes")
    @PreAuthorize("hasAuthority('change:create')")
    public AjaxResult<?> createChange(@RequestBody ChangeRequest request) {
        return AjaxResult.success(changeService.create(request));
    }

    @PostMapping("/changes/{id}/analyze-impact")
    @PreAuthorize("hasAuthority('change:analyze')")
    public AjaxResult<?> analyzeChange(@PathVariable Long id) {
        return AjaxResult.success(changeService.analyze(id));
    }

    @GetMapping("/changes/{id}")
    @PreAuthorize("hasAuthority('change:view')")
    public AjaxResult<?> change(@PathVariable Long id) {
        return AjaxResult.success(changeService.get(id));
    }

    @GetMapping("/changes/{id}/impacts")
    @PreAuthorize("hasAuthority('change:view')")
    public AjaxResult<?> changeImpacts(@PathVariable Long id) {
        return AjaxResult.success(changeService.impacts(id));
    }

    @PostMapping("/changes/{id}/approve")
    @PreAuthorize("hasAuthority('change:approve')")
    public AjaxResult<?> approveChange(@PathVariable Long id, @RequestBody(required = false) ChangeApprovalRequest request) {
        return AjaxResult.success(changeService.approve(id, request));
    }

    @PostMapping("/changes/{id}/feedback")
    @PreAuthorize("hasAuthority('change:feedback')")
    public AjaxResult<?> feedbackChange(@PathVariable Long id, @RequestBody ChangeFeedbackRequest request) {
        return AjaxResult.success(changeService.feedbackChange(id, request));
    }

    @PostMapping("/change-impacts/{id}/feedback")
    @PreAuthorize("hasAuthority('change:feedback')")
    public AjaxResult<?> feedbackChangeImpact(@PathVariable Long id, @RequestBody ChangeFeedbackRequest request) {
        return AjaxResult.success(changeService.feedbackImpact(id, request));
    }

    @PostMapping("/changes/{id}/close")
    @PreAuthorize("hasAuthority('change:close')")
    public AjaxResult<?> closeChange(@PathVariable Long id) {
        return AjaxResult.success(changeService.close(id));
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

    @PostMapping("/execution/exceptions")
    @PreAuthorize("hasAuthority('task:feedback')")
    public AjaxResult<?> reportException(@RequestBody ExceptionReportRequest request) {
        return AjaxResult.success(taskService.reportException(request));
    }

    @GetMapping("/risks")
    @PreAuthorize("hasAuthority('risk:view')")
    public AjaxResult<?> risks(@RequestParam(required = false) Long projectId) {
        return AjaxResult.success(riskService.list(projectId));
    }

    @GetMapping("/messages")
    @PreAuthorize("hasAuthority('message:view')")
    public AjaxResult<?> messages(@RequestParam(required = false) String status) {
        return AjaxResult.success(messageService.list(status));
    }

    @PostMapping("/messages/{id}/read")
    @PreAuthorize("hasAuthority('message:view')")
    public AjaxResult<?> markMessageRead(@PathVariable Long id) {
        return AjaxResult.success(messageService.markRead(id));
    }

    @GetMapping("/scan/{code}")
    @PreAuthorize("hasAuthority('project:view')")
    public AjaxResult<?> scan(@PathVariable String code, @AuthenticationPrincipal NsoPrincipal principal) {
        return AjaxResult.success(flowCodes.resolveScan(code, principal == null ? java.util.List.of() : principal.permissions()));
    }

    @PostMapping(value = "/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('document:upload')")
    public AjaxResult<?> upload(@RequestParam Long projectId, @RequestParam("file") MultipartFile file) {
        return AjaxResult.success(fileService.upload(projectId, file));
    }

    private String bearer(String authorization) {
        return authorization != null && authorization.startsWith("Bearer ") ? authorization.substring(7) : null;
    }
}
