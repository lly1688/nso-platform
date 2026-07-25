package com.nso.web.controller;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import com.nso.common.exception.BusinessException;
import com.nso.web.controller.MvpDtos.AuthSession;
import com.nso.web.controller.MvpDtos.ChangeFeedbackRequest;
import com.nso.web.controller.MvpDtos.ChangeImpactDto;
import com.nso.web.controller.MvpDtos.ChangeOrderDto;
import com.nso.web.controller.MvpDtos.ChangeRequest;
import com.nso.web.controller.MvpDtos.CustomerDto;
import com.nso.web.controller.MvpDtos.CustomerRequest;
import com.nso.web.controller.MvpDtos.DocumentVersionDto;
import com.nso.web.controller.MvpDtos.DocumentVersionRequest;
import com.nso.web.controller.MvpDtos.FileUploadResult;
import com.nso.web.controller.MvpDtos.MessageDto;
import com.nso.web.controller.MvpDtos.PageResult;
import com.nso.web.controller.MvpDtos.ProjectDetail;
import com.nso.web.controller.MvpDtos.ProjectDto;
import com.nso.web.controller.MvpDtos.ProjectRequest;
import com.nso.web.controller.MvpDtos.ReportOverview;
import com.nso.web.controller.MvpDtos.RiskDto;
import com.nso.web.controller.MvpDtos.SampleConfirmRequest;
import com.nso.web.controller.MvpDtos.SampleDto;
import com.nso.web.controller.MvpDtos.SampleRequest;
import com.nso.web.controller.MvpDtos.StoredFile;
import com.nso.web.controller.MvpDtos.TaskDto;
import com.nso.web.controller.MvpDtos.TaskFeedbackRequest;
import com.nso.web.controller.MvpDtos.TimelineItem;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MvpDataService {

    private final AtomicLong idSequence = new AtomicLong(1000);
    private final Map<Long, CustomerDto> customers = new ConcurrentHashMap<>();
    private final Map<Long, ProjectDto> projects = new ConcurrentHashMap<>();
    private final Map<Long, DocumentVersionDto> documents = new ConcurrentHashMap<>();
    private final Map<Long, SampleDto> samples = new ConcurrentHashMap<>();
    private final Map<Long, ChangeOrderDto> changes = new ConcurrentHashMap<>();
    private final Map<Long, ChangeImpactDto> impacts = new ConcurrentHashMap<>();
    private final Map<Long, TaskDto> tasks = new ConcurrentHashMap<>();
    private final Map<Long, RiskDto> risks = new ConcurrentHashMap<>();
    private final Map<Long, MessageDto> messages = new ConcurrentHashMap<>();
    private final Map<Long, TimelineItem> timeline = new ConcurrentHashMap<>();
    private final Map<Long, StoredFile> files = new ConcurrentHashMap<>();

    public MvpDataService() {
        resetDemoData();
    }

    public final synchronized void resetDemoData() {
        idSequence.set(1000);
        customers.clear();
        projects.clear();
        documents.clear();
        samples.clear();
        changes.clear();
        impacts.clear();
        tasks.clear();
        risks.clear();
        messages.clear();
        timeline.clear();
        files.clear();

        CustomerDto customer = createCustomer(new CustomerRequest("星河自动化设备有限公司", "自动化设备", "林经理", "13800000001", "ENABLED"));
        ProjectDto project = createProject(new ProjectRequest(
                customer.id(),
                customer.name(),
                "柔性夹具打样订单",
                30,
                LocalDate.now().plusDays(2),
                "周项目",
                "HIGH",
                List.of("铝合金主体", "表面阳极氧化", "首件客户确认")));

        DocumentVersionDto v1 = createDocumentVersion(project.id(), new DocumentVersionRequest(
                "柔性夹具总装图.pdf",
                "DRAWING",
                "V1",
                LocalDate.now().minusDays(3),
                "初始设计版本"));
        publishDocumentVersion(v1.id());

        DocumentVersionDto v2 = createDocumentVersion(project.id(), new DocumentVersionRequest(
                "柔性夹具总装图.pdf",
                "DRAWING",
                "V2",
                LocalDate.now(),
                "调整定位销尺寸并补充检验要求"));
        publishDocumentVersion(v2.id());

        SampleDto sample = createSample(new SampleRequest(
                project.id(),
                "验证定位结构和表面处理",
                2,
                LocalDate.now().plusDays(1),
                "V2",
                "许质检"));
        submitSampleConfirm(sample.id());

        TaskDto productionTask = addTask(project.id(), "PRODUCTION", "夹具首批生产开工", "V1", "陈班长",
                LocalDate.now(), LocalDate.now().plusDays(3));
        tasks.put(productionTask.id(), productionTask);

        ChangeOrderDto change = createChange(new ChangeRequest(
                project.id(),
                "DESIGN",
                "HIGH",
                "定位销直径 8mm",
                "定位销直径 10mm，检验公差收紧",
                "客户装配测试反馈间隙偏大"));
        analyzeChange(change.id());
        approveChange(change.id());
        calculateRisk(project.id());
    }

    public AuthSession login(String username, String password, String code, String clientId) {
        String normalizedClient = clientId == null ? "admin" : clientId;
        String loginName = username == null || username.isBlank() ? ("mp".equals(normalizedClient) ? "mp-user" : "admin") : username;
        if ("admin".equals(normalizedClient) && password != null && !password.isBlank()
                && !List.of("admin123", "123456", "password").contains(password)) {
            throw new BusinessException("账号或密码错误，演示账号 admin/admin123");
        }
        List<String> roles = "mp".equals(normalizedClient) ? List.of("现场人员") : List.of("系统管理员", "项目经理");
        List<String> permissions = "mp".equals(normalizedClient)
                ? List.of("mp:project:list", "mp:task:feedback", "mp:change:feedback")
                : List.of("*:*:*");
        List<String> menus = "mp".equals(normalizedClient)
                ? List.of("工作台", "项目", "打样", "变更", "任务", "消息")
                : List.of("工作台", "客户项目", "技术文件", "样品管理", "变更中心", "任务风险", "统计分析", "系统基础");
        return new AuthSession(1L, loginName, "演示用户", normalizedClient, roles, permissions, menus,
                "mvp-" + normalizedClient + "-" + UUID.randomUUID(),
                "refresh-" + normalizedClient + "-" + UUID.randomUUID());
    }

    public PageResult<CustomerDto> listCustomers(String keyword) {
        List<CustomerDto> rows = customers.values().stream()
                .filter(customer -> keyword == null || keyword.isBlank()
                        || customer.name().contains(keyword)
                        || customer.customerCode().contains(keyword))
                .sorted(Comparator.comparing(CustomerDto::id))
                .toList();
        return new PageResult<>(rows, rows.size());
    }

    public CustomerDto createCustomer(CustomerRequest request) {
        long id = nextId();
        String name = text(request.name(), "未命名客户");
        CustomerDto customer = new CustomerDto(
                id,
                "CUS-" + LocalDate.now().getYear() + "-" + String.format("%04d", id % 10000),
                name,
                text(request.industry(), "非标制造"),
                text(request.contactName(), "联系人"),
                text(request.phone(), "13800000000"),
                text(request.status(), "ENABLED"));
        customers.put(id, customer);
        return customer;
    }

    public PageResult<ProjectDto> listProjects(String keyword) {
        List<ProjectDto> rows = projects.values().stream()
                .map(this::enrichProject)
                .filter(project -> keyword == null || keyword.isBlank()
                        || project.projectNo().contains(keyword)
                        || project.productName().contains(keyword)
                        || project.customerName().contains(keyword))
                .sorted(Comparator.comparing(ProjectDto::id))
                .toList();
        return new PageResult<>(rows, rows.size());
    }

    public ProjectDto createProject(ProjectRequest request) {
        long id = nextId();
        CustomerDto customer = Optional.ofNullable(request.customerId()).map(customers::get).orElse(null);
        String customerName = customer == null ? text(request.customerName(), "临时客户") : customer.name();
        Long customerId = customer == null ? request.customerId() : customer.id();
        ProjectDto project = new ProjectDto(
                id,
                "NSO-" + LocalDate.now().getYear() + "-" + String.format("%04d", id % 10000),
                customerId,
                customerName,
                text(request.productName(), "非标打样项目"),
                request.quantity() == null ? 1 : request.quantity(),
                request.targetDate() == null ? LocalDate.now().plusDays(7) : request.targetDate(),
                text(request.ownerName(), "项目经理"),
                "DRAFT",
                "订单草稿",
                text(request.priority(), "MEDIUM"),
                "LOW",
                "NONE",
                0,
                0L);
        projects.put(id, project);
        addTimeline(id, "PROJECT", "项目建档", "创建项目 " + project.projectNo(), project.ownerName());
        calculateRisk(id);
        return enrichProject(project);
    }

    public ProjectDetail getProjectDetail(Long projectId) {
        ProjectDto project = requireProject(projectId);
        return new ProjectDetail(
                enrichProject(project),
                documentsByProject(projectId),
                samplesByProject(projectId),
                changesByProject(projectId),
                tasksByProject(projectId),
                risksByProject(projectId),
                timelineByProject(projectId));
    }

    public ProjectDto submitReview(Long projectId) {
        ProjectDto project = requireProject(projectId);
        ProjectDto updated = copyProject(project, "REVIEWING", "需求评审中", project.riskLevel(), project.sampleStatus());
        projects.put(projectId, updated);
        addTimeline(projectId, "PROJECT", "提交需求评审", "项目进入需求评审", project.ownerName());
        return enrichProject(updated);
    }

    public PageResult<DocumentVersionDto> listDocuments(Long projectId) {
        List<DocumentVersionDto> rows = documents.values().stream()
                .filter(item -> projectId == null || Objects.equals(item.projectId(), projectId))
                .sorted(Comparator.comparing(DocumentVersionDto::id))
                .toList();
        return new PageResult<>(rows, rows.size());
    }

    public DocumentVersionDto createDocumentVersion(Long projectId, DocumentVersionRequest request) {
        ProjectDto project = requireProject(projectId);
        boolean duplicate = documents.values().stream().anyMatch(item -> Objects.equals(item.projectId(), projectId)
                && item.fileType().equals(text(request.fileType(), "DRAWING"))
                && item.versionNo().equals(text(request.versionNo(), "V1")));
        if (duplicate) {
            throw new BusinessException("DOCUMENT_VERSION_DUPLICATED: 同一项目、文件类型和版本号不得重复");
        }
        long id = nextId();
        DocumentVersionDto version = new DocumentVersionDto(
                id,
                projectId,
                project.projectNo(),
                text(request.fileName(), "技术文件.pdf"),
                text(request.fileType(), "DRAWING"),
                text(request.versionNo(), "V1"),
                "DRAFT",
                request.effectiveDate() == null ? LocalDate.now() : request.effectiveDate(),
                text(request.changeSummary(), "版本说明"),
                false,
                "demo-sha256-" + id,
                "/api/v1/admin/files/" + id + "/download");
        documents.put(id, version);
        addTimeline(projectId, "DOCUMENT", "上传技术版本", version.fileName() + " " + version.versionNo(), "技术负责人");
        return version;
    }

    public DocumentVersionDto publishDocumentVersion(Long versionId) {
        DocumentVersionDto version = requireDocument(versionId);
        documents.values().stream()
                .filter(item -> Objects.equals(item.projectId(), version.projectId()) && item.fileType().equals(version.fileType()))
                .forEach(item -> documents.put(item.id(), new DocumentVersionDto(item.id(), item.projectId(), item.projectNo(),
                        item.fileName(), item.fileType(), item.versionNo(), "INVALID", item.effectiveDate(),
                        item.changeSummary(), false, item.sha256(), item.downloadUrl())));
        DocumentVersionDto published = new DocumentVersionDto(version.id(), version.projectId(), version.projectNo(),
                version.fileName(), version.fileType(), version.versionNo(), "EFFECTIVE", version.effectiveDate(),
                version.changeSummary(), true, version.sha256(), version.downloadUrl());
        documents.put(versionId, published);
        ProjectDto project = requireProject(version.projectId());
        projects.put(project.id(), copyProject(project, "TECH_PUBLISHED", "技术包已发布", project.riskLevel(), project.sampleStatus()));
        addTimeline(project.id(), "DOCUMENT", "发布技术版本", published.fileName() + " " + published.versionNo() + " 已生效", "技术负责人");
        addMessage("技术版本发布", project.projectNo() + " 当前生效版本为 " + published.versionNo(), "DOCUMENT", "DOCUMENT", published.id());
        return published;
    }

    public PageResult<SampleDto> listSamples(Long projectId) {
        List<SampleDto> rows = samples.values().stream()
                .filter(item -> projectId == null || Objects.equals(item.projectId(), projectId))
                .sorted(Comparator.comparing(SampleDto::id))
                .toList();
        return new PageResult<>(rows, rows.size());
    }

    public SampleDto createSample(SampleRequest request) {
        ProjectDto project = requireProject(request.projectId());
        long id = nextId();
        String version = text(request.referencedVersion(), currentVersion(project.id()).map(DocumentVersionDto::versionNo).orElse("V1"));
        SampleDto sample = new SampleDto(
                id,
                project.id(),
                project.projectNo(),
                "SMP-" + LocalDate.now().getYear() + "-" + String.format("%04d", id % 10000),
                text(request.purpose(), "打样验证"),
                request.quantity() == null ? 1 : request.quantity(),
                request.planFinishDate() == null ? LocalDate.now().plusDays(3) : request.planFinishDate(),
                version,
                "MAKING",
                "PENDING",
                text(request.responsibleName(), "样品负责人"),
                "");
        samples.put(id, sample);
        addTask(project.id(), "SAMPLE", "样品制作与检验", version, sample.responsibleName(), LocalDate.now(), sample.planFinishDate());
        projects.put(project.id(), copyProject(project, "SAMPLING", "打样中", project.riskLevel(), "MAKING"));
        addTimeline(project.id(), "SAMPLE", "创建样品单", sample.sampleNo() + " 引用版本 " + version, sample.responsibleName());
        calculateRisk(project.id());
        return sample;
    }

    public SampleDto submitSampleConfirm(Long sampleId) {
        SampleDto sample = requireSample(sampleId);
        SampleDto updated = new SampleDto(sample.id(), sample.projectId(), sample.projectNo(), sample.sampleNo(), sample.purpose(),
                sample.quantity(), sample.planFinishDate(), sample.referencedVersion(), "WAIT_CUSTOMER_CONFIRM",
                "PENDING", sample.responsibleName(), sample.issueSummary());
        samples.put(sampleId, updated);
        ProjectDto project = requireProject(sample.projectId());
        projects.put(project.id(), copyProject(project, "WAIT_CUSTOMER_CONFIRM", "待客户确认", project.riskLevel(), "WAIT_CUSTOMER_CONFIRM"));
        addTimeline(project.id(), "SAMPLE", "提交客户确认", sample.sampleNo() + " 等待客户确认", "销售经理");
        addMessage("样品待确认", project.projectNo() + " 样品已提交客户确认", "SAMPLE", "SAMPLE", sample.id());
        calculateRisk(project.id());
        return updated;
    }

    public SampleDto confirmSample(Long sampleId, SampleConfirmRequest request) {
        SampleDto sample = requireSample(sampleId);
        String conclusion = text(request.conclusion(), "PASS").toUpperCase(Locale.ROOT);
        String status = List.of("PASS", "CONDITIONAL_PASS").contains(conclusion) ? "CONFIRMED" : "REJECTED";
        SampleDto updated = new SampleDto(sample.id(), sample.projectId(), sample.projectNo(), sample.sampleNo(), sample.purpose(),
                sample.quantity(), sample.planFinishDate(), sample.referencedVersion(), status, conclusion,
                sample.responsibleName(), text(request.opinion(), sample.issueSummary()));
        samples.put(sampleId, updated);

        ProjectDto project = requireProject(sample.projectId());
        String stage = "CONFIRMED".equals(status) ? "生产准备中" : "打样整改中";
        String projectStatus = "CONFIRMED".equals(status) ? "PRODUCTION_READY" : "SAMPLING";
        projects.put(project.id(), copyProject(project, projectStatus, stage, project.riskLevel(), status));
        if ("CONFIRMED".equals(status)) {
            addTask(project.id(), "PRODUCTION", "正式生产开工", sample.referencedVersion(), "陈班长",
                    LocalDate.now().plusDays(1), project.targetDate());
        }
        addTimeline(project.id(), "SAMPLE", "客户确认样品", sample.sampleNo() + " 结论：" + conclusion, text(request.confirmer(), "客户确认人"));
        calculateRisk(project.id());
        return updated;
    }

    public PageResult<ChangeOrderDto> listChanges(Long projectId) {
        List<ChangeOrderDto> rows = changes.values().stream()
                .map(this::enrichChange)
                .filter(item -> projectId == null || Objects.equals(item.projectId(), projectId))
                .sorted(Comparator.comparing(ChangeOrderDto::id))
                .toList();
        return new PageResult<>(rows, rows.size());
    }

    public ChangeOrderDto createChange(ChangeRequest request) {
        ProjectDto project = requireProject(request.projectId());
        long id = nextId();
        ChangeOrderDto change = new ChangeOrderDto(
                id,
                project.id(),
                project.projectNo(),
                "ECN-" + LocalDate.now().getYear() + "-" + String.format("%04d", id % 10000),
                text(request.changeType(), "DESIGN"),
                text(request.urgency(), "NORMAL"),
                text(request.beforeContent(), "变更前内容"),
                text(request.afterContent(), "变更后内容"),
                text(request.reason(), "变更原因"),
                "WAIT_IMPACT",
                0,
                0,
                0,
                0);
        changes.put(id, change);
        addTimeline(project.id(), "CHANGE", "发起变更", change.changeNo() + " " + change.reason(), "变更发起人");
        addMessage("变更待影响分析", project.projectNo() + " " + change.changeNo() + " 需要完成影响分析", "CHANGE", "CHANGE", change.id());
        return change;
    }

    public List<ChangeImpactDto> analyzeChange(Long changeId) {
        ChangeOrderDto change = requireChange(changeId);
        if (impacts.values().stream().anyMatch(item -> Objects.equals(item.changeId(), changeId))) {
            return impactsByChange(changeId);
        }
        addImpact(change, "DOCUMENT", "当前生效技术版本", "技术部", "核对图纸/BOM/工艺是否需要升版", "技术负责人");
        addImpact(change, "TASK", "未完成采购与生产任务", "生产计划部", "确认是否暂停旧版本任务并重排", "计划员");
        addImpact(change, "SAMPLE", "样品与检验记录", "质量部", "确认样品是否需要复检或重新打样", "质量负责人");
        ChangeOrderDto updated = new ChangeOrderDto(change.id(), change.projectId(), change.projectNo(), change.changeNo(),
                change.changeType(), change.urgency(), change.beforeContent(), change.afterContent(), change.reason(),
                "WAIT_APPROVAL", change.delayDays(), change.reworkQty(), 3, 0);
        changes.put(changeId, updated);
        addTimeline(change.projectId(), "CHANGE", "完成影响分析", change.changeNo() + " 已生成影响矩阵", "系统规则");
        return impactsByChange(changeId);
    }

    public ChangeOrderDto approveChange(Long changeId) {
        ChangeOrderDto change = requireChange(changeId);
        if (impacts.values().stream().noneMatch(item -> Objects.equals(item.changeId(), changeId))) {
            analyzeChange(changeId);
        }
        ChangeOrderDto approved = new ChangeOrderDto(change.id(), change.projectId(), change.projectNo(), change.changeNo(),
                change.changeType(), change.urgency(), change.beforeContent(), change.afterContent(), change.reason(),
                "EXECUTING", change.delayDays(), change.reworkQty(), change.impactCount(), change.feedbackCount());
        changes.put(changeId, approved);
        if ("HIGH".equalsIgnoreCase(change.urgency())) {
            tasks.values().stream()
                    .filter(task -> Objects.equals(task.projectId(), change.projectId()) && !"DONE".equals(task.status()))
                    .forEach(task -> tasks.put(task.id(), new TaskDto(task.id(), task.projectId(), task.projectNo(), task.taskNo(),
                            task.taskType(), task.title(), task.referencedVersion(), "PAUSED", task.responsibleName(),
                            task.planStart(), task.planFinish(), "高风险变更已批准，需完成影响反馈后继续")));
        }
        addTimeline(change.projectId(), "CHANGE", "批准变更", change.changeNo() + " 进入执行反馈", "项目经理");
        addMessage("变更已批准", change.projectNo() + " " + change.changeNo() + " 已批准，请责任人反馈影响项", "CHANGE", "CHANGE", change.id());
        calculateRisk(change.projectId());
        return enrichChange(approved);
    }

    public ChangeImpactDto feedbackImpact(Long impactId, ChangeFeedbackRequest request) {
        ChangeImpactDto impact = requireImpact(impactId);
        ChangeImpactDto updated = new ChangeImpactDto(impact.id(), impact.changeId(), impact.objectType(), impact.objectName(),
                impact.departmentName(), impact.suggestedAction(), "FEEDBACK_DONE", text(request.result(), "已处理"),
                text(request.responsibleName(), impact.responsibleName()));
        impacts.put(impactId, updated);
        ChangeOrderDto change = requireChange(impact.changeId());
        Integer delayDays = Math.max(change.delayDays(), request.delayDays() == null ? 0 : request.delayDays());
        Integer reworkQty = change.reworkQty() + (request.reworkQty() == null ? 0 : request.reworkQty());
        changes.put(change.id(), new ChangeOrderDto(change.id(), change.projectId(), change.projectNo(), change.changeNo(),
                change.changeType(), change.urgency(), change.beforeContent(), change.afterContent(), change.reason(),
                change.status(), delayDays, reworkQty, change.impactCount(), feedbackCount(change.id())));
        addTimeline(change.projectId(), "CHANGE", "影响项反馈", impact.objectName() + "：" + updated.feedbackResult(), updated.responsibleName());
        calculateRisk(change.projectId());
        return updated;
    }

    public ChangeOrderDto feedbackChange(Long changeId, ChangeFeedbackRequest request) {
        List<ChangeImpactDto> pending = impactsByChange(changeId).stream()
                .filter(item -> !"FEEDBACK_DONE".equals(item.status()))
                .toList();
        if (pending.isEmpty()) {
            return enrichChange(requireChange(changeId));
        }
        pending.forEach(item -> feedbackImpact(item.id(), request));
        return enrichChange(requireChange(changeId));
    }

    public ChangeOrderDto closeChange(Long changeId) {
        ChangeOrderDto change = requireChange(changeId);
        long unfinished = impactsByChange(changeId).stream().filter(item -> !"FEEDBACK_DONE".equals(item.status())).count();
        if (unfinished > 0) {
            throw new BusinessException("CHANGE_IMPACT_INCOMPLETE: 存在未完成影响项，禁止关闭变更");
        }
        ChangeOrderDto closed = new ChangeOrderDto(change.id(), change.projectId(), change.projectNo(), change.changeNo(),
                change.changeType(), change.urgency(), change.beforeContent(), change.afterContent(), change.reason(),
                "CLOSED", change.delayDays(), change.reworkQty(), change.impactCount(), change.feedbackCount());
        changes.put(changeId, closed);
        addTimeline(change.projectId(), "CHANGE", "关闭变更", change.changeNo() + " 已验证关闭", "项目经理");
        addMessage("变更已关闭", change.projectNo() + " " + change.changeNo() + " 已完成验证关闭", "CHANGE", "CHANGE", change.id());
        calculateRisk(change.projectId());
        return enrichChange(closed);
    }

    public PageResult<TaskDto> listTasks(Long projectId) {
        List<TaskDto> rows = tasks.values().stream()
                .filter(item -> projectId == null || Objects.equals(item.projectId(), projectId))
                .sorted(Comparator.comparing(TaskDto::id))
                .toList();
        return new PageResult<>(rows, rows.size());
    }

    public TaskDto getTask(Long taskId) {
        return requireTask(taskId);
    }

    public TaskDto startTask(Long taskId) {
        TaskDto task = requireTask(taskId);
        if ("PRODUCTION".equals(task.taskType())) {
            DocumentVersionDto current = currentVersion(task.projectId())
                    .orElseThrow(() -> new BusinessException("VERSION_MISSING: 项目缺少生效技术版本，禁止开工"));
            if (!current.versionNo().equals(task.referencedVersion())) {
                TaskDto blocked = new TaskDto(task.id(), task.projectId(), task.projectNo(), task.taskNo(), task.taskType(),
                        task.title(), task.referencedVersion(), "BLOCKED", task.responsibleName(), task.planStart(),
                        task.planFinish(), "VERSION_MISMATCH: 当前生效版本为 " + current.versionNo());
                tasks.put(taskId, blocked);
                throw new BusinessException(blocked.blockReason() + "，请切换任务引用版本后再开工");
            }
            boolean sampleConfirmed = samples.values().stream()
                    .anyMatch(sample -> Objects.equals(sample.projectId(), task.projectId()) && "CONFIRMED".equals(sample.status()));
            if (!sampleConfirmed) {
                TaskDto blocked = new TaskDto(task.id(), task.projectId(), task.projectNo(), task.taskNo(), task.taskType(),
                        task.title(), task.referencedVersion(), "BLOCKED", task.responsibleName(), task.planStart(),
                        task.planFinish(), "SAMPLE_NOT_CONFIRMED: 样品未确认，禁止正式生产");
                tasks.put(taskId, blocked);
                throw new BusinessException(blocked.blockReason());
            }
        }
        TaskDto started = new TaskDto(task.id(), task.projectId(), task.projectNo(), task.taskNo(), task.taskType(),
                task.title(), task.referencedVersion(), "IN_PROGRESS", task.responsibleName(), task.planStart(),
                task.planFinish(), "");
        tasks.put(taskId, started);
        addTimeline(task.projectId(), "TASK", "任务开工", task.taskNo() + " " + task.title(), task.responsibleName());
        return started;
    }

    public TaskDto feedbackTask(Long taskId, TaskFeedbackRequest request) {
        TaskDto task = requireTask(taskId);
        String status = "done".equalsIgnoreCase(text(request.result(), "")) || "完成".equals(text(request.result(), ""))
                ? "DONE" : "IN_PROGRESS";
        TaskDto updated = new TaskDto(task.id(), task.projectId(), task.projectNo(), task.taskNo(), task.taskType(),
                task.title(), task.referencedVersion(), status, task.responsibleName(), task.planStart(),
                task.planFinish(), text(request.notes(), ""));
        tasks.put(taskId, updated);
        addTimeline(task.projectId(), "TASK", "任务反馈", task.taskNo() + " " + text(request.notes(), status), task.responsibleName());
        calculateRisk(task.projectId());
        return updated;
    }

    public PageResult<RiskDto> listRisks(Long projectId) {
        projects.keySet().forEach(this::calculateRisk);
        List<RiskDto> rows = risks.values().stream()
                .filter(item -> projectId == null || Objects.equals(item.projectId(), projectId))
                .sorted(Comparator.comparing(RiskDto::score).reversed())
                .toList();
        return new PageResult<>(rows, rows.size());
    }

    public RiskDto calculateRisk(Long projectId) {
        ProjectDto project = requireProject(projectId);
        int score = 10;
        List<String> reasons = new ArrayList<>();
        long left = daysLeft(project.targetDate());
        if (left <= 2) {
            score += 25;
            reasons.add("目标交期剩余 " + left + " 天");
        }
        boolean waitingSample = samples.values().stream().anyMatch(sample -> Objects.equals(sample.projectId(), projectId)
                && List.of("WAIT_CUSTOMER_CONFIRM", "MAKING").contains(sample.status()));
        if (waitingSample) {
            score += 25;
            reasons.add("样品尚未完成客户确认");
        }
        boolean highChange = changes.values().stream().anyMatch(change -> Objects.equals(change.projectId(), projectId)
                && !"CLOSED".equals(change.status())
                && "HIGH".equalsIgnoreCase(change.urgency()));
        if (highChange) {
            score += 25;
            reasons.add("存在未关闭高风险变更");
        }
        long blockedTasks = tasks.values().stream().filter(task -> Objects.equals(task.projectId(), projectId)
                && List.of("BLOCKED", "PAUSED").contains(task.status())).count();
        if (blockedTasks > 0) {
            score += 20;
            reasons.add(blockedTasks + " 个任务处于阻断或暂停状态");
        }
        String level = score >= 80 ? "SERIOUS" : score >= 60 ? "HIGH" : score >= 30 ? "MEDIUM" : "LOW";
        String suggestion = switch (level) {
            case "SERIOUS", "HIGH" -> "优先处理样品确认、变更影响反馈和任务阻断";
            case "MEDIUM" -> "跟进临期任务并确认技术版本";
            default -> "保持常规跟进";
        };
        Long riskId = risks.values().stream()
                .filter(item -> Objects.equals(item.projectId(), projectId))
                .map(RiskDto::id)
                .findFirst()
                .orElseGet(this::nextId);
        RiskDto risk = new RiskDto(riskId, project.id(), project.projectNo(), level, score,
                reasons.isEmpty() ? List.of("暂无关键风险") : reasons, suggestion, "OPEN");
        risks.put(riskId, risk);
        projects.put(projectId, copyProject(project, project.status(), project.stage(), level, project.sampleStatus()));
        return risk;
    }

    public PageResult<MessageDto> listMessages(String status) {
        List<MessageDto> rows = messages.values().stream()
                .filter(item -> status == null || status.isBlank() || status.equals(item.status()))
                .sorted(Comparator.comparing(MessageDto::createdAt).reversed())
                .toList();
        return new PageResult<>(rows, rows.size());
    }

    public MessageDto markMessageRead(Long messageId) {
        MessageDto message = requireMessage(messageId);
        MessageDto updated = new MessageDto(message.id(), message.title(), message.content(), message.type(), "READ",
                message.businessType(), message.businessId(), message.createdAt());
        messages.put(messageId, updated);
        return updated;
    }

    public ReportOverview overview() {
        projects.keySet().forEach(this::calculateRisk);
        long projectCount = projects.size();
        long highRiskCount = risks.values().stream().filter(risk -> List.of("HIGH", "SERIOUS").contains(risk.level())).count();
        long waitingSampleCount = samples.values().stream().filter(sample -> "WAIT_CUSTOMER_CONFIRM".equals(sample.status())).count();
        long openChangeCount = changes.values().stream().filter(change -> !"CLOSED".equals(change.status())).count();
        long unreadMessages = messages.values().stream().filter(message -> "UNREAD".equals(message.status())).count();

        Map<String, Number> metrics = new LinkedHashMap<>();
        metrics.put("projects", projectCount);
        metrics.put("highRisks", highRiskCount);
        metrics.put("waitingSamples", waitingSampleCount);
        metrics.put("openChanges", openChangeCount);
        metrics.put("unreadMessages", unreadMessages);

        List<Map<String, Object>> riskLevels = risks.values().stream()
                .collect(Collectors.groupingBy(RiskDto::level, LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> row("name", entry.getKey(), "value", entry.getValue()))
                .toList();
        List<Map<String, Object>> changeTypes = changes.values().stream()
                .collect(Collectors.groupingBy(ChangeOrderDto::changeType, LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> row("name", entry.getKey(), "value", entry.getValue()))
                .toList();
        List<Map<String, Object>> sampleEfficiency = List.of(
                row("name", "待确认", "value", waitingSampleCount),
                row("name", "已确认", "value", samples.values().stream().filter(sample -> "CONFIRMED".equals(sample.status())).count()),
                row("name", "已驳回", "value", samples.values().stream().filter(sample -> "REJECTED".equals(sample.status())).count()));
        return new ReportOverview(metrics, riskLevels, changeTypes, sampleEfficiency);
    }

    public FileUploadResult upload(MultipartFile multipartFile) {
        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new BusinessException("FILE_EMPTY: 上传文件不能为空");
        }
        try {
            long id = nextId();
            Path uploadDir = Path.of("target", "nso-uploads");
            Files.createDirectories(uploadDir);
            String originalName = Optional.ofNullable(multipartFile.getOriginalFilename()).orElse("upload.bin");
            String safeName = originalName.replaceAll("[\\\\/:*?\"<>|]", "_");
            Path target = uploadDir.resolve(id + "-" + safeName);
            multipartFile.transferTo(target);
            String sha256 = sha256(target);
            StoredFile stored = new StoredFile(id, originalName, multipartFile.getContentType(), multipartFile.getSize(), sha256, target.toString());
            files.put(id, stored);
            return new FileUploadResult(id, stored.fileName(), stored.contentType(), stored.size(), stored.sha256(),
                    "/api/v1/admin/files/" + id + "/download");
        } catch (IOException exception) {
            throw new BusinessException("FILE_UPLOAD_FAILED: " + exception.getMessage());
        }
    }

    public Resource download(Long fileId) {
        StoredFile stored = files.get(fileId);
        if (stored == null) {
            DocumentVersionDto version = documents.get(fileId);
            if (version != null) {
                Path generated = Path.of("target", "nso-uploads", fileId + "-" + version.fileName() + ".txt");
                try {
                    Files.createDirectories(generated.getParent());
                    if (!Files.exists(generated)) {
                        Files.writeString(generated, version.fileName() + " " + version.versionNo() + " " + version.changeSummary());
                    }
                } catch (IOException exception) {
                    throw new BusinessException("FILE_DOWNLOAD_FAILED: " + exception.getMessage());
                }
                return new FileSystemResource(generated);
            }
            throw new BusinessException("FILE_NOT_FOUND: 文件不存在");
        }
        return new FileSystemResource(stored.path());
    }

    public SampleDto publicConfirmation(Long sampleId) {
        return requireSample(sampleId);
    }

    private ProjectDto enrichProject(ProjectDto project) {
        int changeCount = (int) changes.values().stream().filter(item -> Objects.equals(item.projectId(), project.id())).count();
        String sampleStatus = samples.values().stream()
                .filter(item -> Objects.equals(item.projectId(), project.id()))
                .max(Comparator.comparing(SampleDto::id))
                .map(SampleDto::status)
                .orElse(project.sampleStatus());
        String riskLevel = risks.values().stream()
                .filter(item -> Objects.equals(item.projectId(), project.id()))
                .max(Comparator.comparing(RiskDto::score))
                .map(RiskDto::level)
                .orElse(project.riskLevel());
        return new ProjectDto(project.id(), project.projectNo(), project.customerId(), project.customerName(), project.productName(),
                project.quantity(), project.targetDate(), project.ownerName(), project.status(), project.stage(), project.priority(),
                riskLevel, sampleStatus, changeCount, daysLeft(project.targetDate()));
    }

    private ChangeOrderDto enrichChange(ChangeOrderDto change) {
        int impactCount = (int) impactsByChange(change.id()).size();
        int doneCount = feedbackCount(change.id());
        return new ChangeOrderDto(change.id(), change.projectId(), change.projectNo(), change.changeNo(), change.changeType(),
                change.urgency(), change.beforeContent(), change.afterContent(), change.reason(), change.status(),
                change.delayDays(), change.reworkQty(), impactCount, doneCount);
    }

    private int feedbackCount(Long changeId) {
        return (int) impactsByChange(changeId).stream().filter(item -> "FEEDBACK_DONE".equals(item.status())).count();
    }

    private void addImpact(ChangeOrderDto change, String type, String name, String department, String action, String responsible) {
        long id = nextId();
        impacts.put(id, new ChangeImpactDto(id, change.id(), type, name, department, action, "PENDING_FEEDBACK", "", responsible));
    }

    private TaskDto addTask(Long projectId, String type, String title, String version, String responsible, LocalDate start, LocalDate finish) {
        ProjectDto project = requireProject(projectId);
        long id = nextId();
        TaskDto task = new TaskDto(id, projectId, project.projectNo(), "TSK-" + LocalDate.now().getYear() + "-" + String.format("%04d", id % 10000),
                type, title, version, "TODO", responsible, start, finish, "");
        tasks.put(id, task);
        return task;
    }

    private Optional<DocumentVersionDto> currentVersion(Long projectId) {
        return documents.values().stream()
                .filter(item -> Objects.equals(item.projectId(), projectId) && Boolean.TRUE.equals(item.currentVersion()))
                .max(Comparator.comparing(DocumentVersionDto::id));
    }

    private List<DocumentVersionDto> documentsByProject(Long projectId) {
        return documents.values().stream()
                .filter(item -> Objects.equals(item.projectId(), projectId))
                .sorted(Comparator.comparing(DocumentVersionDto::id))
                .toList();
    }

    private List<SampleDto> samplesByProject(Long projectId) {
        return samples.values().stream()
                .filter(item -> Objects.equals(item.projectId(), projectId))
                .sorted(Comparator.comparing(SampleDto::id))
                .toList();
    }

    private List<ChangeOrderDto> changesByProject(Long projectId) {
        return changes.values().stream()
                .filter(item -> Objects.equals(item.projectId(), projectId))
                .map(this::enrichChange)
                .sorted(Comparator.comparing(ChangeOrderDto::id))
                .toList();
    }

    public List<ChangeImpactDto> impactsByChange(Long changeId) {
        return impacts.values().stream()
                .filter(item -> Objects.equals(item.changeId(), changeId))
                .sorted(Comparator.comparing(ChangeImpactDto::id))
                .toList();
    }

    private List<TaskDto> tasksByProject(Long projectId) {
        return tasks.values().stream()
                .filter(item -> Objects.equals(item.projectId(), projectId))
                .sorted(Comparator.comparing(TaskDto::id))
                .toList();
    }

    private List<RiskDto> risksByProject(Long projectId) {
        calculateRisk(projectId);
        return risks.values().stream()
                .filter(item -> Objects.equals(item.projectId(), projectId))
                .sorted(Comparator.comparing(RiskDto::score).reversed())
                .toList();
    }

    private List<TimelineItem> timelineByProject(Long projectId) {
        return timeline.values().stream()
                .filter(item -> Objects.equals(item.projectId(), projectId))
                .sorted(Comparator.comparing(TimelineItem::occurredAt).reversed())
                .toList();
    }

    private ProjectDto copyProject(ProjectDto project, String status, String stage, String riskLevel, String sampleStatus) {
        return new ProjectDto(project.id(), project.projectNo(), project.customerId(), project.customerName(), project.productName(),
                project.quantity(), project.targetDate(), project.ownerName(), status, stage, project.priority(), riskLevel,
                sampleStatus, project.changeCount(), daysLeft(project.targetDate()));
    }

    private void addTimeline(Long projectId, String eventType, String title, String summary, String operatorName) {
        long id = nextId();
        timeline.put(id, new TimelineItem(id, projectId, eventType, title, summary, operatorName, LocalDateTime.now()));
    }

    private MessageDto addMessage(String title, String content, String type, String businessType, Long businessId) {
        long id = nextId();
        MessageDto message = new MessageDto(id, title, content, type, "UNREAD", businessType, businessId, LocalDateTime.now());
        messages.put(id, message);
        return message;
    }

    private Map<String, Object> row(String key1, Object value1, String key2, Object value2) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put(key1, value1);
        row.put(key2, value2);
        return row;
    }

    private String sha256(Path file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(file); DigestInputStream digestInput = new DigestInputStream(input, digest)) {
                byte[] buffer = new byte[8192];
                while (digestInput.read(buffer) != -1) {
                    // Reading through DigestInputStream updates the hash.
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new BusinessException("FILE_HASH_FAILED: " + exception.getMessage());
        }
    }

    private CustomerDto requireCustomer(Long id) {
        CustomerDto customer = customers.get(id);
        if (customer == null) {
            throw new BusinessException("CUSTOMER_NOT_FOUND: 客户不存在");
        }
        return customer;
    }

    private ProjectDto requireProject(Long id) {
        ProjectDto project = projects.get(id);
        if (project == null) {
            throw new BusinessException("PROJECT_NOT_FOUND: 项目不存在");
        }
        return project;
    }

    private DocumentVersionDto requireDocument(Long id) {
        DocumentVersionDto version = documents.get(id);
        if (version == null) {
            throw new BusinessException("DOCUMENT_NOT_FOUND: 技术版本不存在");
        }
        return version;
    }

    private SampleDto requireSample(Long id) {
        SampleDto sample = samples.get(id);
        if (sample == null) {
            throw new BusinessException("SAMPLE_NOT_FOUND: 样品单不存在");
        }
        return sample;
    }

    private ChangeOrderDto requireChange(Long id) {
        ChangeOrderDto change = changes.get(id);
        if (change == null) {
            throw new BusinessException("CHANGE_NOT_FOUND: 变更单不存在");
        }
        return change;
    }

    private ChangeImpactDto requireImpact(Long id) {
        ChangeImpactDto impact = impacts.get(id);
        if (impact == null) {
            throw new BusinessException("CHANGE_IMPACT_NOT_FOUND: 变更影响项不存在");
        }
        return impact;
    }

    private TaskDto requireTask(Long id) {
        TaskDto task = tasks.get(id);
        if (task == null) {
            throw new BusinessException("TASK_NOT_FOUND: 任务不存在");
        }
        return task;
    }

    private MessageDto requireMessage(Long id) {
        MessageDto message = messages.get(id);
        if (message == null) {
            throw new BusinessException("MESSAGE_NOT_FOUND: 消息不存在");
        }
        return message;
    }

    private long daysLeft(LocalDate targetDate) {
        return targetDate == null ? 0 : ChronoUnit.DAYS.between(LocalDate.now(), targetDate);
    }

    private long nextId() {
        return idSequence.incrementAndGet();
    }

    private String text(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    @SuppressWarnings("unused")
    private CustomerDto requireCustomerOrNull(Long id) {
        return id == null ? null : requireCustomer(id);
    }

}
