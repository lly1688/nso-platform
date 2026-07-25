package com.nso.web.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import com.nso.common.exception.BusinessException;
import com.nso.web.controller.MvpDtos.ChangeFeedbackRequest;
import com.nso.web.controller.MvpDtos.ChangeOrderDto;
import com.nso.web.controller.MvpDtos.ProjectDto;
import com.nso.web.controller.MvpDtos.ProjectRequest;
import com.nso.web.controller.MvpDtos.SampleConfirmRequest;
import com.nso.web.controller.MvpDtos.TaskDto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MvpDataServiceTest {

    private MvpDataService dataService;

    @BeforeEach
    void setUp() {
        dataService = new MvpDataService();
    }

    @Test
    void shouldBlockProductionWhenTaskReferencesOldDocumentVersion() {
        TaskDto productionTask = dataService.listTasks(null).records().stream()
                .filter(task -> "PRODUCTION".equals(task.taskType()) && "V1".equals(task.referencedVersion()))
                .findFirst()
                .orElseThrow();

        assertThatThrownBy(() -> dataService.startTask(productionTask.id()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("VERSION_MISMATCH");
    }

    @Test
    void shouldRejectClosingChangeBeforeAllImpactFeedbackIsDone() {
        ChangeOrderDto change = dataService.listChanges(null).records().get(0);

        assertThatThrownBy(() -> dataService.closeChange(change.id()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CHANGE_IMPACT_INCOMPLETE");
    }

    @Test
    void shouldCloseChangeAfterImpactFeedback() {
        ChangeOrderDto change = dataService.listChanges(null).records().get(0);

        dataService.feedbackChange(change.id(), new ChangeFeedbackRequest(null, "已重排任务", "今日完成", 3, 20, "计划员"));
        ChangeOrderDto closed = dataService.closeChange(change.id());

        assertThat(closed.status()).isEqualTo("CLOSED");
        assertThat(closed.delayDays()).isEqualTo(3);
        assertThat(closed.reworkQty()).isEqualTo(60);
    }

    @Test
    void shouldCalculateHighRiskForWaitingSampleNearDueDate() {
        ProjectDto project = dataService.listProjects(null).records().get(0);

        assertThat(dataService.calculateRisk(project.id()).level()).isIn("HIGH", "SERIOUS");
    }

    @Test
    void shouldAllowProductionAfterCurrentVersionAndSampleConfirmed() {
        ProjectDto project = dataService.createProject(new ProjectRequest(
                null,
                "测试客户",
                "测试治具",
                1,
                LocalDate.now().plusDays(7),
                "测试经理",
                "MEDIUM",
                null));
        var version = dataService.createDocumentVersion(project.id(), new MvpDtos.DocumentVersionRequest(
                "测试图纸.pdf", "DRAWING", "A", LocalDate.now(), "测试发布"));
        dataService.publishDocumentVersion(version.id());
        var sample = dataService.createSample(new MvpDtos.SampleRequest(
                project.id(), "验证", 1, LocalDate.now().plusDays(1), "A", "质量员"));
        dataService.confirmSample(sample.id(), new SampleConfirmRequest("PASS", "同意投产", "客户"));
        TaskDto task = dataService.listTasks(project.id()).records().stream()
                .filter(item -> "PRODUCTION".equals(item.taskType()) && "A".equals(item.referencedVersion()))
                .findFirst()
                .orElseThrow();

        assertThat(dataService.startTask(task.id()).status()).isEqualTo("IN_PROGRESS");
    }
}
